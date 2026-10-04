package com.taxoryn.module.gov.auth;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.controller.GovAuthController;
import com.taxoryn.module.gov.auth.dto.GovAuthContinueRequest;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.repository.GovAuthSessionRepository;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class GovAuthorizationFlowIntegrationTest {

    @Autowired
    private GovernmentAuthenticationService authService;

    @Autowired
    private GovAuthController authController;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovAuthSessionRepository authSessionRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private GovConnectionDto connectionA;
    private GovConnectionDto connectionB;
    private SecurityUser userA;

    @BeforeEach
    void setUp() {
        authSessionRepository.deleteAll();

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Auth Flow Org A " + UUID.randomUUID())
                .legalName("Auth Flow Org A Pvt Ltd")
                .email("authflow.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Auth Flow Org B " + UUID.randomUUID())
                .legalName("Auth Flow Org B Pvt Ltd")
                .email("authflow.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        userA = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgA.getId())
                .email("admin@authflow-orga.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );

        connectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Flow Gateway Org A")
                .build());

        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_cred_***")
                .rawSecret("SecretGstKeyOrgA123")
                .build());

        connectionService.activateConnection(connectionA.getId());

        TenantContext.setTenantId(orgB.getId());
        connectionB = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Flow Gateway Org B")
                .build());

        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionB.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_cred_***")
                .rawSecret("SecretItdKeyOrgB123")
                .build());

        connectionService.activateConnection(connectionB.getId());

        TenantContext.setTenantId(orgA.getId());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // =========================================================================
    // 1. Authorization Flow Lifecycle Scenarios
    // =========================================================================

    @Test
    @DisplayName("OAuth2 Interactive Flow: Start -> User Action Required -> Continue -> Authenticated")
    void testOAuth2Flow_InteractiveSuccess() {
        // 1. Start OAuth2 flow with REQUIRES_ACTION
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("corr-oauth-001")
                .options(Map.of("mockOutcome", "REQUIRES_ACTION"))
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(session.getAuthorizationState()).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);
        assertThat(session.isRequiresUserAction()).isTrue();
        assertThat(session.getActionPrompt()).contains("OAuth redirect portal");
        assertThat(session.getSafeAuthorizationReference()).contains("https://mock-gov-portal.taxoryn.internal/oauth/authorize");
        assertThat(session.getProviderSessionReference()).contains("mock-gov-sess-");

        // 2. Query fine-grained authorization state
        GovAuthorizationState state = authService.getAuthorizationState(session.getSessionId());
        assertThat(state).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);

        // 3. Continue authorization upon user completing portal login
        GovAuthSessionDto completed = authService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder()
                        .actionReference("mock-oauth-callback-token-123")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build());

        assertThat(completed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(completed.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_COMPLETED);
        assertThat(completed.isRequiresUserAction()).isFalse();
        assertThat(completed.getAuthenticatedAt()).isNotNull();
        assertThat(completed.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("OTP Challenge Flow: Start -> OTP Sent -> Continue with OTP -> Authenticated")
    void testOtpFlow_InteractiveSuccess() {
        // 1. Start OTP flow
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OTP)
                .correlationId("corr-otp-001")
                .options(Map.of("mockOutcome", "OTP_SENT"))
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(session.getAuthorizationState()).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);
        assertThat(session.getActionPrompt()).contains("6-digit OTP");
        assertThat(session.getSafeAuthorizationReference()).contains("mock-otp-challenge-");

        // 2. Complete flow with challenge submission
        GovAuthSessionDto completed = authService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder()
                        .actionReference("user-entered-otp-challenge-ack")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build());

        assertThat(completed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(completed.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_COMPLETED);
        assertThat(completed.isRequiresUserAction()).isFalse();
    }

    @Test
    @DisplayName("DSC Token Signing Flow: Start -> Pending Signing -> Continue -> Authenticated")
    void testDscFlow_InteractiveSuccess() {
        // 1. Start DSC flow
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.DSC)
                .correlationId("corr-dsc-001")
                .options(Map.of("mockOutcome", "REQUIRES_ACTION"))
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(session.getAuthorizationState()).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);
        assertThat(session.getActionPrompt()).contains("digital signature token");

        // 2. Complete flow with signature challenge
        GovAuthSessionDto completed = authService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder()
                        .actionReference("dsc-signature-verified")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build());

        assertThat(completed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(completed.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_COMPLETED);
    }

    @Test
    @DisplayName("Authorization Failure Flow: User Rejection or Invalid Credentials")
    void testAuthorizationFlow_FailureHandling() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "REQUIRES_ACTION"))
                .build());

        // Continue with rejection
        GovAuthSessionDto failed = authService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder()
                        .actionReference("REJECT")
                        .options(Map.of("mockOutcome", "FAILED"))
                        .build());

        assertThat(failed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(failed.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_FAILED);
        assertThat(failed.getFailureCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(failed.getSafeFailureMessage()).contains("User rejected authorization");
    }

    @Test
    @DisplayName("Gateway Failure Modes: Provider Unavailable and Timeout")
    void testGatewayFailureModes() {
        // Provider Unavailable simulation
        GovAuthSessionDto unavail = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());

        assertThat(unavail.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(unavail.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_FAILED);
        assertThat(unavail.getFailureCode()).isEqualTo("PROVIDER_UNAVAILABLE");

        // Timeout simulation
        GovAuthSessionDto timeout = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());

        assertThat(timeout.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(timeout.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_FAILED);
        assertThat(timeout.getFailureCode()).isEqualTo("TIMEOUT");
    }

    // =========================================================================
    // 2. State Machine & Expiration Guardrails
    // =========================================================================

    @Test
    @DisplayName("Cannot continue a REVOKED authorization session")
    void testCannotContinueRevokedSession() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        authService.revokeAuthentication(session.getSessionId());

        assertThatThrownBy(() -> authService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder().actionReference("test").build()))
                .isInstanceOf(GovIntegrationException.class)
                .hasMessageContaining("Cannot continue a revoked");
    }

    @Test
    @DisplayName("Cannot continue an EXPIRED authorization session")
    void testCannotContinueExpiredSession() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "EXPIRED"))
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.EXPIRED);

        assertThatThrownBy(() -> authService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder().actionReference("test").build()))
                .isInstanceOf(GovIntegrationException.class)
                .hasMessageContaining("Cannot continue an expired");
    }

    // =========================================================================
    // 3. Security, Multi-Tenancy & Zero Secret Leakage
    // =========================================================================

    @Test
    @DisplayName("Cross-tenant authorization continuation denied")
    void testCrossTenantAuthorization_Denied() {
        GovAuthSessionDto sessionA = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "REQUIRES_ACTION"))
                .build());

        // Switch to Org B
        TenantContext.setTenantId(orgB.getId());

        assertThatThrownBy(() -> authService.continueAuthorization(sessionA.getSessionId(),
                GovAuthContinueRequest.builder().actionReference("token").build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to current organization");

        assertThatThrownBy(() -> authService.getAuthorizationState(sessionA.getSessionId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to current organization");
    }

    @Test
    @DisplayName("Zero Secret Leakage: DTO, Database and Audit contain no secrets")
    void testZeroSecretLeakage() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OTP)
                .correlationId("corr-leak-001")
                .options(Map.of("mockOutcome", "OTP_SENT"))
                .build());

        // Verify DTO does not contain passwords, raw OTPs, or private keys
        assertThat(session.getActionPrompt()).doesNotContain("SecretGstKeyOrgA123");
        assertThat(session.getSafeAuthorizationReference()).doesNotContain("SecretGstKeyOrgA123");

        // Verify entity persistence in database
        GovAuthSessionEntity entity = authSessionRepository.findById(session.getSessionId()).orElseThrow();
        if (entity.getMetadata() != null) {
            assertThat(entity.getMetadata()).doesNotContain("SecretGstKeyOrgA123");
        }
        if (entity.getSafeFailureMessage() != null) {
            assertThat(entity.getSafeFailureMessage()).doesNotContain("SecretGstKeyOrgA123");
        }

        // Verify Audit Logs
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> auditDetailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()),
                any(),
                any(),
                eq("GOV_AUTH_SESSION"),
                any(),
                any(),
                auditDetailsCaptor.capture()
        );

        List<Map<String, Object>> allAuditDetails = auditDetailsCaptor.getAllValues();
        for (Map<String, Object> details : allAuditDetails) {
            assertThat(details.values().toString())
                    .doesNotContain("SecretGstKeyOrgA123")
                    .doesNotContain("password")
                    .doesNotContain("token");
        }
    }

    // =========================================================================
    // 4. REST Controller Authorization Endpoints
    // =========================================================================

    @Test
    @DisplayName("REST Controller: POST /authorize and GET /authorization")
    void testControllerAuthorizationEndpoints() {
        // Start via controller
        ResponseEntity<?> startResp = authController.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "REQUIRES_ACTION"))
                .build());
        assertThat(startResp.getStatusCode().value()).isEqualTo(201);

        GovAuthSessionDto startBody = ((com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>) startResp.getBody()).getData();
        assertThat(startBody.getAuthorizationState()).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);

        // Continue via controller
        ResponseEntity<?> continueResp = authController.continueAuthorization(startBody.getSessionId(),
                GovAuthContinueRequest.builder()
                        .actionReference("mock-portal-callback")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build());
        assertThat(continueResp.getStatusCode().value()).isEqualTo(200);

        GovAuthSessionDto continueBody = ((com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>) continueResp.getBody()).getData();
        assertThat(continueBody.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_COMPLETED);
        assertThat(continueBody.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);

        // Query authorization state via controller
        ResponseEntity<?> getResp = authController.getAuthorizationSession(startBody.getSessionId());
        assertThat(getResp.getStatusCode().value()).isEqualTo(200);
        GovAuthSessionDto getBody = ((com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>) getResp.getBody()).getData();
        assertThat(getBody.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_COMPLETED);
    }
}
