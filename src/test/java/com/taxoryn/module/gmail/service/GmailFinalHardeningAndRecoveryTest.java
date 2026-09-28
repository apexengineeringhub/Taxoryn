package com.taxoryn.module.gmail.service;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.dto.GoogleOAuthTokenResponse;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.entity.GmailSyncHistoryEntity;
import com.taxoryn.module.gmail.entity.GmailSyncStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import com.taxoryn.module.gmail.health.GmailHealthIndicator;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.repository.GmailSyncHistoryRepository;
import com.taxoryn.module.gmail.scheduler.GmailSyncScheduler;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailFinalHardeningAndRecoveryTest {

    @Mock
    private GmailApiClient apiClient;

    @Mock
    private TokenEncryptionService encryptionService;

    @Mock
    private GmailAccountRepository accountRepository;

    @Mock
    private GmailConversationRepository conversationRepository;

    @Mock
    private GmailSyncHistoryRepository syncHistoryRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private GmailProperties properties;
    private GmailOAuthServiceImpl oAuthService;
    private GmailSyncServiceImpl syncService;
    private GmailSyncScheduler scheduler;
    private GmailHealthIndicator healthIndicator;

    private UUID tenantA;
    private UUID tenantB;
    private UUID accountIdA;
    private UUID accountIdB;
    private UUID accountIdC;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        accountIdA = UUID.randomUUID();
        accountIdB = UUID.randomUUID();
        accountIdC = UUID.randomUUID();

        properties = new GmailProperties();
        properties.setEnabled(true);
        properties.getClientId();
        properties.getSync().setEnabled(true);
        properties.getRetry().setEnabled(true);
        properties.getHealth().setEnabled(true);

        oAuthService = new GmailOAuthServiceImpl(
                properties,
                apiClient,
                encryptionService,
                accountRepository,
                conversationRepository,
                userRepository,
                auditService
        );

        syncService = new GmailSyncServiceImpl(
                oAuthService,
                apiClient,
                encryptionService,
                accountRepository,
                conversationRepository,
                syncHistoryRepository,
                clientRepository,
                employeeRepository,
                auditService
        );

        scheduler = new GmailSyncScheduler(properties, syncService, accountRepository);
        healthIndicator = new GmailHealthIndicator(properties, accountRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Idempotency: Duplicate sync runs do not duplicate conversations or corrupt state")
    void testIdempotentSyncPreservesSingleConversation() {
        TenantContext.setTenantId(tenantA);

        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .tokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        account.setId(accountIdA);
        account.setOrganizationId(tenantA);

        when(accountRepository.findByIdAndOrganizationId(accountIdA, tenantA)).thenReturn(Optional.of(account));
        when(encryptionService.decrypt("enc_token")).thenReturn("valid_token");

        // Thread metadata setup
        GmailThreadModels.ThreadListResponse listResponse = GmailThreadModels.ThreadListResponse.builder()
                .threads(List.of(GmailThreadModels.ThreadSummary.builder().id("thread_100").build()))
                .build();
        when(apiClient.listThreads(eq("valid_token"), isNull(), isNull(), anyInt())).thenReturn(listResponse);

        GmailThreadModels.HeaderEntry hSub = new GmailThreadModels.HeaderEntry("Subject", "Audit Report 2026");
        GmailThreadModels.HeaderEntry hFrom = new GmailThreadModels.HeaderEntry("From", "client@corp.com");
        GmailThreadModels.HeaderEntry hTo = new GmailThreadModels.HeaderEntry("To", "practice@taxfirm.com");
        GmailThreadModels.MessagePayload payload = new GmailThreadModels.MessagePayload(List.of(hSub, hFrom, hTo));

        GmailThreadModels.MessageMetadata msg = GmailThreadModels.MessageMetadata.builder()
                .id("msg_1")
                .threadId("thread_100")
                .snippet("Initial email snippet")
                .internalDate(Instant.now().toEpochMilli())
                .payload(payload)
                .build();

        GmailThreadModels.ThreadDetail detail = GmailThreadModels.ThreadDetail.builder()
                .id("thread_100")
                .historyId("hist_1")
                .messages(List.of(msg))
                .build();
        when(apiClient.getThreadMetadata("valid_token", "thread_100")).thenReturn(detail);

        // Run 1: First sync (new conversation created)
        when(conversationRepository.findByOrganizationIdAndThreadId(tenantA, "thread_100")).thenReturn(Optional.empty());
        GmailSyncService.GmailSyncResult result1 = syncService.syncAccount(tenantA, accountIdA, GmailSyncType.FULL);
        assertThat(result1.getStatus()).isEqualTo(GmailSyncStatus.SUCCESS);
        assertThat(result1.getThreadsSynced()).isEqualTo(1);

        // Run 2: Second sync (existing conversation updated in place, never duplicated)
        GmailConversationEntity existingConv = GmailConversationEntity.builder()
                .gmailAccountId(accountIdA)
                .threadId("thread_100")
                .subject("Audit Report 2026")
                .status(GmailConversationStatus.OPEN)
                .build();
        existingConv.setId(UUID.randomUUID());
        existingConv.setOrganizationId(tenantA);

        when(conversationRepository.findByOrganizationIdAndThreadId(tenantA, "thread_100")).thenReturn(Optional.of(existingConv));
        GmailSyncService.GmailSyncResult result2 = syncService.syncAccount(tenantA, accountIdA, GmailSyncType.FULL);
        assertThat(result2.getStatus()).isEqualTo(GmailSyncStatus.SUCCESS);
        assertThat(result2.getThreadsSynced()).isEqualTo(1);

        verify(conversationRepository, times(2)).save(any(GmailConversationEntity.class));
    }

    @Test
    @DisplayName("2. History Expiration Recovery: History API null/expired falls back to recent threads query")
    void testHistoryExpirationFallback() {
        TenantContext.setTenantId(tenantA);

        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token")
                .lastHistoryId("expired_hist_999")
                .tokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        account.setId(accountIdA);
        account.setOrganizationId(tenantA);

        when(accountRepository.findByIdAndOrganizationId(accountIdA, tenantA)).thenReturn(Optional.of(account));
        when(encryptionService.decrypt("enc_token")).thenReturn("valid_token");

        // History API returns null (indicating 404/expired startHistoryId)
        when(apiClient.listHistory("valid_token", "expired_hist_999", null)).thenReturn(null);

        // Fallback: listThreads is called
        GmailThreadModels.ThreadListResponse fallbackList = GmailThreadModels.ThreadListResponse.builder()
                .threads(List.of(GmailThreadModels.ThreadSummary.builder().id("thread_fallback").build()))
                .build();
        when(apiClient.listThreads(eq("valid_token"), isNull(), isNull(), anyInt())).thenReturn(fallbackList);

        GmailThreadModels.MessagePayload payload = new GmailThreadModels.MessagePayload(
                List.of(new GmailThreadModels.HeaderEntry("Subject", "Fallback Sub"),
                        new GmailThreadModels.HeaderEntry("From", "a@b.com"),
                        new GmailThreadModels.HeaderEntry("To", "c@d.com"))
        );
        GmailThreadModels.MessageMetadata msg = GmailThreadModels.MessageMetadata.builder()
                .id("msg_fb")
                .threadId("thread_fallback")
                .payload(payload)
                .internalDate(Instant.now().toEpochMilli())
                .build();
        GmailThreadModels.ThreadDetail detail = GmailThreadModels.ThreadDetail.builder()
                .id("thread_fallback")
                .historyId("new_valid_hist_100")
                .messages(List.of(msg))
                .build();
        when(apiClient.getThreadMetadata("valid_token", "thread_fallback")).thenReturn(detail);

        GmailSyncService.GmailSyncResult result = syncService.syncAccount(tenantA, accountIdA, GmailSyncType.INCREMENTAL);

        assertThat(result.getStatus()).isEqualTo(GmailSyncStatus.SUCCESS);
        assertThat(result.getThreadsSynced()).isEqualTo(1);
        verify(apiClient).listThreads(eq("valid_token"), isNull(), isNull(), anyInt());
        verify(accountRepository).save(argThat(acc -> "new_valid_hist_100".equals(acc.getLastHistoryId())));
    }

    @Test
    @DisplayName("3. Token Auto-Refresh: Expired access token is refreshed automatically prior to sync")
    void testTokenAutoRefreshDuringSync() {
        TenantContext.setTenantId(tenantA);

        // Expired token (past timestamp)
        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_old_access")
                .encryptedRefreshToken("enc_valid_refresh")
                .tokenExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();
        account.setId(accountIdA);
        account.setOrganizationId(tenantA);

        when(accountRepository.findByIdAndOrganizationId(accountIdA, tenantA)).thenReturn(Optional.of(account));
        when(encryptionService.decrypt("enc_valid_refresh")).thenReturn("plain_refresh_token");

        GoogleOAuthTokenResponse refreshed = GoogleOAuthTokenResponse.builder()
                .accessToken("new_access_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .scope("scope")
                .build();
        when(apiClient.refreshAccessToken("plain_refresh_token")).thenReturn(refreshed);
        when(encryptionService.encrypt("new_access_token")).thenReturn("enc_new_access");
        when(encryptionService.decrypt("enc_new_access")).thenReturn("new_access_token");

        when(accountRepository.save(any(GmailAccountEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(apiClient.listThreads(eq("new_access_token"), isNull(), isNull(), anyInt()))
                .thenReturn(GmailThreadModels.ThreadListResponse.builder().threads(List.of()).build());

        GmailSyncService.GmailSyncResult result = syncService.syncAccount(tenantA, accountIdA, GmailSyncType.FULL);

        assertThat(result.getStatus()).isEqualTo(GmailSyncStatus.SUCCESS);
        verify(apiClient).refreshAccessToken("plain_refresh_token");
        verify(accountRepository, atLeastOnce()).save(argThat(a -> "enc_new_access".equals(a.getEncryptedAccessToken())));
    }

    @Test
    @DisplayName("4. Token Refresh Failure: Revoked refresh token sets AUTH_EXPIRED and records sync failure")
    void testTokenRefreshFailureMarksAuthExpired() {
        TenantContext.setTenantId(tenantA);

        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_old_access")
                .encryptedRefreshToken("enc_revoked_refresh")
                .tokenExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();
        account.setId(accountIdA);
        account.setOrganizationId(tenantA);

        when(accountRepository.findByIdAndOrganizationId(accountIdA, tenantA)).thenReturn(Optional.of(account));
        when(encryptionService.decrypt("enc_revoked_refresh")).thenReturn("revoked_refresh");
        when(apiClient.refreshAccessToken("revoked_refresh")).thenThrow(new RuntimeException("invalid_grant: Token has been revoked"));
        when(accountRepository.save(any(GmailAccountEntity.class))).thenAnswer(i -> i.getArgument(0));

        GmailSyncService.GmailSyncResult result = syncService.syncAccount(tenantA, accountIdA, GmailSyncType.INCREMENTAL);

        assertThat(result.getStatus()).isEqualTo(GmailSyncStatus.FAILED);
        assertThat(account.getStatus()).isEqualTo(GmailAccountStatus.AUTH_EXPIRED);
        assertThat(account.getSyncErrorMessage()).contains("invalid_grant");
        verify(syncHistoryRepository).save(argThat(h -> h.getStatus() == GmailSyncStatus.FAILED));
    }

    @Test
    @DisplayName("5. Multi-Mailbox Isolation: Scheduler processes all mailboxes; failure on B does not abort A and C")
    void testMultiMailboxSchedulerIsolation() {
        GmailAccountEntity mbA = GmailAccountEntity.builder().emailAddress("a@firm.com").status(GmailAccountStatus.CONNECTED).build();
        mbA.setId(accountIdA);
        mbA.setOrganizationId(tenantA);

        GmailAccountEntity mbB = GmailAccountEntity.builder().emailAddress("b@firm.com").status(GmailAccountStatus.CONNECTED).build();
        mbB.setId(accountIdB);
        mbB.setOrganizationId(tenantA);

        GmailAccountEntity mbC = GmailAccountEntity.builder().emailAddress("c@firm.com").status(GmailAccountStatus.CONNECTED).build();
        mbC.setId(accountIdC);
        mbC.setOrganizationId(tenantB);

        when(accountRepository.findAll()).thenReturn(List.of(mbA, mbB, mbC));

        // mbA succeeds
        when(accountRepository.findByIdAndOrganizationId(accountIdA, tenantA)).thenReturn(Optional.of(mbA));
        mbA.setTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        mbA.setEncryptedAccessToken("enc_a");
        when(encryptionService.decrypt("enc_a")).thenReturn("token_a");
        when(apiClient.listThreads(eq("token_a"), isNull(), isNull(), anyInt()))
                .thenReturn(GmailThreadModels.ThreadListResponse.builder().threads(List.of()).build());

        // mbB fails with network exception
        when(accountRepository.findByIdAndOrganizationId(accountIdB, tenantA)).thenReturn(Optional.of(mbB));
        mbB.setTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        mbB.setEncryptedAccessToken("enc_b");
        when(encryptionService.decrypt("enc_b")).thenReturn("token_b");
        when(apiClient.listThreads(eq("token_b"), isNull(), isNull(), anyInt()))
                .thenThrow(new RuntimeException("Google 503 Backend Error"));

        // mbC succeeds
        when(accountRepository.findByIdAndOrganizationId(accountIdC, tenantB)).thenReturn(Optional.of(mbC));
        mbC.setTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        mbC.setEncryptedAccessToken("enc_c");
        when(encryptionService.decrypt("enc_c")).thenReturn("token_c");
        when(apiClient.listThreads(eq("token_c"), isNull(), isNull(), anyInt()))
                .thenReturn(GmailThreadModels.ThreadListResponse.builder().threads(List.of()).build());

        scheduler.runPeriodicSync();

        // Verify all 3 mailboxes were attempted
        verify(apiClient).listThreads(eq("token_a"), isNull(), isNull(), anyInt());
        verify(apiClient).listThreads(eq("token_b"), isNull(), isNull(), anyInt());
        verify(apiClient).listThreads(eq("token_c"), isNull(), isNull(), anyInt());

        // Verify history was saved for all 3
        verify(syncHistoryRepository, times(3)).save(any(GmailSyncHistoryEntity.class));

        // TenantContext safely cleared
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("6. Health Indicator & Telemetry: Provides telemetry without leaking credentials")
    void testHealthIndicatorSafetyAndMetrics() {
        GmailAccountEntity connected1 = GmailAccountEntity.builder().status(GmailAccountStatus.CONNECTED).build();
        GmailAccountEntity connected2 = GmailAccountEntity.builder().status(GmailAccountStatus.CONNECTED).build();
        GmailAccountEntity expired = GmailAccountEntity.builder().status(GmailAccountStatus.AUTH_EXPIRED).build();

        when(accountRepository.findAll()).thenReturn(List.of(connected1, connected2, expired));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalMailboxes", 3L);
        assertThat(health.getDetails()).containsEntry("connectedMailboxes", 2L);
        assertThat(health.getDetails()).containsEntry("authExpiredMailboxes", 1L);
        assertThat(health.getDetails()).containsKey("warning");

        // Never expose sensitive fields
        assertThat(health.getDetails()).doesNotContainKey("clientSecret");
        assertThat(health.getDetails()).doesNotContainKey("accessToken");
        assertThat(health.getDetails()).doesNotContainKey("refreshToken");
    }

    @Test
    @DisplayName("7. Channel Configuration: Email channels default cleanly from properties")
    void testChannelConfigurationDefaults() {
        assertThat(properties.getChannels().getInfo().isEnabled()).isTrue();
        assertThat(properties.getChannels().getInfo().getEmail()).isEqualTo("info@taxoryn.com");

        assertThat(properties.getChannels().getSupport().isEnabled()).isTrue();
        assertThat(properties.getChannels().getSupport().getEmail()).isEqualTo("support@taxoryn.com");

        assertThat(properties.getChannels().getAdmin().isEnabled()).isTrue();
        assertThat(properties.getChannels().getAdmin().getEmail()).isEqualTo("admin@taxoryn.com");
    }
}
