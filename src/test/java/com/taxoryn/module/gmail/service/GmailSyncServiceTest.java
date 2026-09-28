package com.taxoryn.module.gmail.service;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailSyncHistoryEntity;
import com.taxoryn.module.gmail.entity.GmailSyncStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.repository.GmailSyncHistoryRepository;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailSyncServiceTest {

    @Mock
    private GmailOAuthService oAuthService;

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
    private AuditService auditService;

    private GmailSyncServiceImpl syncService;
    private UUID orgId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        TenantContext.setTenantId(orgId);

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
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Verify full sync indexes new threads and auto-matches client by email")
    void testSyncAccountFullIndexesAndMatchesClient() {
        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .build();
        account.setId(accountId);
        account.setOrganizationId(orgId);

        when(oAuthService.getValidAuthenticatedAccount(orgId, accountId)).thenReturn(account);
        when(encryptionService.decrypt("enc_token")).thenReturn("ya29.access_token");

        // Thread List
        GmailThreadModels.ThreadListResponse threadList = GmailThreadModels.ThreadListResponse.builder()
                .threads(List.of(GmailThreadModels.ThreadSummary.builder().id("th_101").build()))
                .build();
        when(apiClient.listThreads(eq("ya29.access_token"), isNull(), isNull(), eq(50)))
                .thenReturn(threadList);

        // Thread Detail with messages
        GmailThreadModels.HeaderEntry hSub = new GmailThreadModels.HeaderEntry("Subject", "ITR-6 Filing Request");
        GmailThreadModels.HeaderEntry hFrom = new GmailThreadModels.HeaderEntry("From", "Acme CFO <cfo@acmecorp.com>");
        GmailThreadModels.HeaderEntry hTo = new GmailThreadModels.HeaderEntry("To", "practice@taxfirm.com");
        GmailThreadModels.MessagePayload payload = new GmailThreadModels.MessagePayload(List.of(hSub, hFrom, hTo));

        GmailThreadModels.MessageMetadata msg1 = GmailThreadModels.MessageMetadata.builder()
                .id("msg_1")
                .threadId("th_101")
                .snippet("Please find attached the financial statements...")
                .internalDate(Instant.now().toEpochMilli())
                .labelIds(List.of("INBOX", "UNREAD"))
                .payload(payload)
                .build();

        GmailThreadModels.ThreadDetail detail = GmailThreadModels.ThreadDetail.builder()
                .id("th_101")
                .historyId("998822")
                .messages(List.of(msg1))
                .build();

        when(apiClient.getThreadMetadata("ya29.access_token", "th_101")).thenReturn(detail);
        when(conversationRepository.findByOrganizationIdAndThreadId(orgId, "th_101")).thenReturn(Optional.empty());

        // Client match
        UUID clientId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        ClientEntity matchedClient = ClientEntity.builder()
                .displayName("Acme Corp")
                .email("cfo@acmecorp.com")
                .locationId(locationId)
                .build();
        matchedClient.setId(clientId);

        when(clientRepository.findAllByOrganizationId(orgId)).thenReturn(List.of(matchedClient));

        // Execute Sync
        GmailSyncService.GmailSyncResult result = syncService.syncAccount(orgId, accountId, GmailSyncType.FULL);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GmailSyncStatus.SUCCESS);
        assertThat(result.getThreadsSynced()).isEqualTo(1);

        // Verify conversation saved with matched client
        ArgumentCaptor<GmailConversationEntity> convCaptor = ArgumentCaptor.forClass(GmailConversationEntity.class);
        verify(conversationRepository).save(convCaptor.capture());

        GmailConversationEntity savedConv = convCaptor.getValue();
        assertThat(savedConv.getThreadId()).isEqualTo("th_101");
        assertThat(savedConv.getSubject()).isEqualTo("ITR-6 Filing Request");
        assertThat(savedConv.getClientId()).isEqualTo(clientId);
        assertThat(savedConv.getLocationId()).isEqualTo(locationId);
        assertThat(savedConv.getSenderEmail()).isEqualTo("cfo@acmecorp.com");
        assertThat(savedConv.getSenderName()).isEqualTo("Acme CFO");
        assertThat(savedConv.getIsUnread()).isTrue();

        // Verify sync history record
        verify(syncHistoryRepository).save(any(GmailSyncHistoryEntity.class));
    }

    @Test
    @DisplayName("Verify incremental sync uses Gmail History API when lastHistoryId is present")
    void testSyncAccountIncrementalWithHistoryApi() {
        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token")
                .lastHistoryId("123450")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .build();
        account.setId(accountId);
        account.setOrganizationId(orgId);

        when(oAuthService.getValidAuthenticatedAccount(orgId, accountId)).thenReturn(account);
        when(encryptionService.decrypt("enc_token")).thenReturn("ya29.access_token");

        // History response
        GmailThreadModels.MessageAdded added = new GmailThreadModels.MessageAdded(
                GmailThreadModels.MessageMetadata.builder().id("msg_inc").threadId("th_inc_1").build()
        );
        GmailThreadModels.HistoryRecord record = new GmailThreadModels.HistoryRecord("123455", List.of(added));
        GmailThreadModels.HistoryResponse historyResponse = new GmailThreadModels.HistoryResponse(List.of(record), "123456", null);

        when(apiClient.listHistory("ya29.access_token", "123450", null)).thenReturn(historyResponse);

        // Thread detail
        GmailThreadModels.HeaderEntry hSub = new GmailThreadModels.HeaderEntry("Subject", "GST Query");
        GmailThreadModels.HeaderEntry hFrom = new GmailThreadModels.HeaderEntry("From", "client@corp.com");
        GmailThreadModels.HeaderEntry hTo = new GmailThreadModels.HeaderEntry("To", "practice@taxfirm.com");
        GmailThreadModels.MessagePayload payload = new GmailThreadModels.MessagePayload(List.of(hSub, hFrom, hTo));

        GmailThreadModels.MessageMetadata msg = GmailThreadModels.MessageMetadata.builder()
                .id("msg_inc")
                .threadId("th_inc_1")
                .snippet("GST Query snippet")
                .internalDate(Instant.now().toEpochMilli())
                .labelIds(List.of("INBOX"))
                .payload(payload)
                .build();

        GmailThreadModels.ThreadDetail detail = GmailThreadModels.ThreadDetail.builder()
                .id("th_inc_1")
                .historyId("123456")
                .messages(List.of(msg))
                .build();

        when(apiClient.getThreadMetadata("ya29.access_token", "th_inc_1")).thenReturn(detail);
        when(conversationRepository.findByOrganizationIdAndThreadId(orgId, "th_inc_1")).thenReturn(Optional.empty());
        when(clientRepository.findAllByOrganizationId(orgId)).thenReturn(List.of());

        GmailSyncService.GmailSyncResult result = syncService.syncAccount(orgId, accountId, GmailSyncType.INCREMENTAL);

        assertThat(result.getStatus()).isEqualTo(GmailSyncStatus.SUCCESS);
        assertThat(result.getThreadsSynced()).isEqualTo(1);
        verify(apiClient, never()).listThreads(any(), any(), any(), anyInt());
        verify(accountRepository).save(argThat(acc -> "123456".equals(acc.getLastHistoryId())));
    }

    @Test
    @DisplayName("Verify syncAccount records failure when OAuth fails to resolve account")
    void testSyncAccountOAuthFailure() {
        when(oAuthService.getValidAuthenticatedAccount(orgId, accountId))
                .thenThrow(new RuntimeException("OAuth token revoked by Google"));

        GmailSyncService.GmailSyncResult result = syncService.syncAccount(orgId, accountId, GmailSyncType.INCREMENTAL);

        assertThat(result.getStatus()).isEqualTo(GmailSyncStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("OAuth token revoked by Google");
        verify(syncHistoryRepository).save(argThat(h -> h.getStatus() == GmailSyncStatus.FAILED));
    }

    @Test
    @DisplayName("Verify syncAllActiveAccounts iterates all connected mailboxes with tenant isolation")
    void testSyncAllActiveAccounts() {
        GmailAccountEntity acc = GmailAccountEntity.builder()
                .emailAddress("active@firm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc")
                .build();
        acc.setId(accountId);
        acc.setOrganizationId(orgId);

        when(accountRepository.findAll()).thenReturn(List.of(acc));
        when(oAuthService.getValidAuthenticatedAccount(orgId, accountId)).thenReturn(acc);
        when(encryptionService.decrypt("enc")).thenReturn("token");
        when(apiClient.listThreads(any(), any(), any(), anyInt()))
                .thenReturn(GmailThreadModels.ThreadListResponse.builder().threads(List.of()).build());

        int total = syncService.syncAllActiveAccounts();

        assertThat(total).isEqualTo(0);
        assertThat(TenantContext.getTenantId()).isNull();
    }
}

