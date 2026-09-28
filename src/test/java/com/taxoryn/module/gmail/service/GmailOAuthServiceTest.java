package com.taxoryn.module.gmail.service;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.dto.GmailAccountConnectRequest;
import com.taxoryn.module.gmail.dto.GmailAccountDto;
import com.taxoryn.module.gmail.dto.GoogleOAuthTokenResponse;
import com.taxoryn.module.gmail.dto.GoogleUserInfoResponse;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailOAuthServiceTest {

    @Mock
    private GmailProperties properties;

    @Mock
    private GmailApiClient apiClient;

    @Mock
    private TokenEncryptionService encryptionService;

    @Mock
    private GmailAccountRepository accountRepository;

    @Mock
    private GmailConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private GmailOAuthServiceImpl oAuthService;
    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(orgId);

        oAuthService = new GmailOAuthServiceImpl(
                properties,
                apiClient,
                encryptionService,
                accountRepository,
                conversationRepository,
                userRepository,
                auditService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Verify authorization URL generation with state and scopes")
    void testGenerateAuthorizationUrl() {
        when(properties.getAuthUrl()).thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
        when(properties.getClientId()).thenReturn("test-client-id");
        when(properties.getRedirectUri()).thenReturn("http://localhost:5173/auth/gmail/callback");
        when(properties.getScope()).thenReturn("https://www.googleapis.com/auth/gmail.metadata");

        String authUrl = oAuthService.generateAuthorizationUrl(orgId, userId, null);

        assertThat(authUrl).contains("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("access_type=offline");
        assertThat(authUrl).contains(orgId.toString());
    }

    @Test
    @DisplayName("Verify connectAccount exchanges auth code, encrypts tokens, and persists account")
    void testConnectAccount() {
        GmailAccountConnectRequest request = GmailAccountConnectRequest.builder()
                .authCode("4/0AY0e-sample-auth-code")
                .redirectUri("http://localhost:5173/auth/gmail/callback")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .build();

        GoogleOAuthTokenResponse tokenResponse = GoogleOAuthTokenResponse.builder()
                .accessToken("ya29.sample-access-token")
                .refreshToken("1//sample-refresh-token")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();

        GoogleUserInfoResponse userInfo = GoogleUserInfoResponse.builder()
                .email("practice@taxfirm.com")
                .name("Practice Firm")
                .build();

        when(apiClient.exchangeAuthCode("4/0AY0e-sample-auth-code", "http://localhost:5173/auth/gmail/callback"))
                .thenReturn(tokenResponse);
        when(apiClient.getUserInfo("ya29.sample-access-token"))
                .thenReturn(userInfo);
        when(encryptionService.encrypt("ya29.sample-access-token"))
                .thenReturn("enc_access_token");
        when(encryptionService.encrypt("1//sample-refresh-token"))
                .thenReturn("enc_refresh_token");
        when(accountRepository.findByOrganizationIdAndEmailAddress(orgId, "practice@taxfirm.com"))
                .thenReturn(Optional.empty());

        when(accountRepository.save(any(GmailAccountEntity.class)))
                .thenAnswer(inv -> {
                    GmailAccountEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        GmailAccountDto result = oAuthService.connectAccount(orgId, userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getEmailAddress()).isEqualTo("practice@taxfirm.com");
        assertThat(result.getStatus()).isEqualTo(GmailAccountStatus.CONNECTED);
        assertThat(result.getAccountType()).isEqualTo(GmailAccountType.PRACTICE_SHARED);

        verify(auditService).logEvent(eq(orgId), eq(userId), eq("GMAIL_ACCOUNT_CONNECTED"), eq("GMAIL_ACCOUNT"), any(), isNull(), any());
    }

    @Test
    @DisplayName("Verify getValidAuthenticatedAccount refreshes expired access token")
    void testGetValidAuthenticatedAccountRefreshesExpiredToken() {
        UUID accountId = UUID.randomUUID();
        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("test@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_old_acc")
                .encryptedRefreshToken("enc_ref")
                .tokenExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES)) // expired
                .build();
        account.setOrganizationId(orgId);

        when(accountRepository.findByIdAndOrganizationId(accountId, orgId))
                .thenReturn(Optional.of(account));
        when(encryptionService.decrypt("enc_ref"))
                .thenReturn("plain_ref");

        GoogleOAuthTokenResponse refreshed = GoogleOAuthTokenResponse.builder()
                .accessToken("ya29.new-access-token")
                .expiresIn(3600L)
                .build();
        when(apiClient.refreshAccessToken("plain_ref"))
                .thenReturn(refreshed);
        when(encryptionService.encrypt("ya29.new-access-token"))
                .thenReturn("enc_new_acc");
        when(accountRepository.save(any(GmailAccountEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GmailAccountEntity valid = oAuthService.getValidAuthenticatedAccount(orgId, accountId);

        assertThat(valid).isNotNull();
        assertThat(valid.getEncryptedAccessToken()).isEqualTo("enc_new_acc");
        verify(apiClient).refreshAccessToken("plain_ref");
    }

    @Test
    @DisplayName("Verify disconnectAccount blanks tokens and updates status")
    void testDisconnectAccount() {
        UUID accountId = UUID.randomUUID();
        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("disconnect@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token")
                .build();
        account.setOrganizationId(orgId);

        when(accountRepository.findByIdAndOrganizationId(accountId, orgId))
                .thenReturn(Optional.of(account));

        oAuthService.disconnectAccount(orgId, accountId);

        assertThat(account.getStatus()).isEqualTo(GmailAccountStatus.DISCONNECTED);
        assertThat(account.getEncryptedAccessToken()).isNull();
        assertThat(account.getEncryptedRefreshToken()).isNull();
        verify(accountRepository).save(account);
        verify(auditService).logEvent(eq(orgId), isNull(), eq("GMAIL_ACCOUNT_DISCONNECTED"), eq("GMAIL_ACCOUNT"), eq(accountId.toString()), eq("CONNECTED"), eq("DISCONNECTED"));
    }
}
