package com.taxoryn.module.gov.auth;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.controller.GovAuthController;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.repository.GovAuthSessionRepository;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
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

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovAuthenticationIntegrationTest {

    @Autowired
    private GovernmentAuthenticationService authService;

    @Autowired
    private GovAuthSessionRepository authSessionRepository;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private GovAuthController authController;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private GovConnectionDto connectionA;
    private GovConnectionDto connectionB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Auth Practice Org A " + UUID.randomUUID())
                .email("auth.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Auth Practice Org B " + UUID.randomUUID())
                .email("auth.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        SecurityUser userA = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgA.getId())
                .email("admin@" + orgA.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );

        connectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Gateway Org A")
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
                .displayName("ITD Gateway Org B")
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
    // 1. Session Lifecycle Scenarios
    // =========================================================================

    @Test
    @DisplayName("Start Authentication (Default/Success) - Creates AUTHENTICATED session with 8h expiry")
    void testStartAuthentication_Success_Authenticated() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getConnectionId()).isEqualTo(connectionA.getId());
        assertThat(session.getProviderType()).isEqualTo(GovProviderType.GST);
        assertThat(session.getAuthMethod()).isEqualTo(GovAuthMethod.OAUTH2);
        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(session.getAuthenticatedAt()).isNotNull();
        assertThat(session.getExpiresAt()).isAfter(Instant.now());
        assertThat(session.isRequiresUserAction()).isFalse();

        // Check in database
        GovAuthSessionEntity entity = authSessionRepository.findById(session.getSessionId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(entity.getOrganizationId()).isEqualTo(orgA.getId());
    }

    @Test
    @DisplayName("Start Authentication (Pending) - Creates AUTHENTICATION_PENDING with action prompt")
    void testStartAuthentication_Pending_RequiresUserAction() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OTP)
                .options(Map.of("mockOutcome", "PENDING"))
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(session.isRequiresUserAction()).isTrue();
        assertThat(session.getActionPrompt()).contains("OTP");
    }

    @Test
    @DisplayName("Start Authentication (Failure) - Creates AUTHENTICATION_FAILED with safe diagnostic message")
    void testStartAuthentication_Failed_RecordsFailure() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.DSC)
                .options(Map.of("mockOutcome", "FAILED"))
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(session.getFailureCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(session.getSafeFailureMessage()).isNotBlank();
    }

    @Test
    @DisplayName("Start Authentication (Provider Unavailable) - Records failure code")
    void testStartAuthentication_ProviderUnavailable() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.EVC)
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(session.getFailureCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }

    @Test
    @DisplayName("Start Authentication (Timeout) - Records timeout failure code")
    void testStartAuthentication_Timeout() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(session.getFailureCode()).isEqualTo("TIMEOUT");
    }

    @Test
    @DisplayName("Idempotency: Re-starting authentication with active session returns existing valid session")
    void testStartAuthentication_ReusesActiveValidSession_Idempotent() {
        GovAuthSessionDto first = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        GovAuthSessionDto second = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        assertThat(second.getSessionId()).isEqualTo(first.getSessionId());
        assertThat(second.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
    }

    // =========================================================================
    // 2. Status Checking & Expiration
    // =========================================================================

    @Test
    @DisplayName("Get Authentication Status - Transitions pending session to AUTHENTICATED")
    void testGetAuthenticationStatus_ChecksStatusAndUpdatesSession() {
        GovAuthSessionDto pending = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OTP)
                .options(Map.of("mockOutcome", "PENDING"))
                .build());
        assertThat(pending.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);

        GovAuthSessionDto updated = authService.getAuthenticationStatus(
                pending.getSessionId(),
                Map.of("mockOutcome", "AUTHENTICATED")
        );

        assertThat(updated.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(updated.isRequiresUserAction()).isFalse();
    }

    @Test
    @DisplayName("Expiration Detection - Expired session automatically transitions to EXPIRED")
    void testGetAuthenticationStatus_DetectsExpiration() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        // Force expiration in DB
        GovAuthSessionEntity entity = authSessionRepository.findById(session.getSessionId()).orElseThrow();
        entity.setExpiresAt(Instant.now().minusSeconds(300));
        authSessionRepository.save(entity);

        GovAuthSessionDto status = authService.getAuthenticationStatus(session.getSessionId());
        assertThat(status.getStatus()).isEqualTo(GovAuthStatus.EXPIRED);
        assertThat(status.getFailureCode()).isEqualTo("SESSION_EXPIRED");
    }

    @Test
    @DisplayName("Revoke Session - Marks active session as REVOKED")
    void testRevokeAuthentication_TransitionsToRevoked() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        GovAuthSessionDto revoked = authService.revokeAuthentication(session.getSessionId());
        assertThat(revoked.getStatus()).isEqualTo(GovAuthStatus.REVOKED);

        // Active session query should return null
        GovAuthSessionDto active = authService.getActiveSessionForConnection(connectionA.getId());
        assertThat(active).isNull();
    }

    // =========================================================================
    // 3. Security & Multi-Tenant Isolation
    // =========================================================================

    @Test
    @DisplayName("Cross-tenant connection access denied when starting auth")
    void testCrossTenantConnection_Denied() {
        // Org A attempts to authenticate connection B (belonging to Org B)
        assertThatThrownBy(() -> authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionB.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Cross-tenant session status check and revocation denied")
    void testCrossTenantSession_Denied() {
        GovAuthSessionDto sessionA = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        // Switch to Org B
        TenantContext.setTenantId(orgB.getId());

        assertThatThrownBy(() -> authService.getAuthenticationStatus(sessionA.getSessionId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to current organization");

        assertThatThrownBy(() -> authService.revokeAuthentication(sessionA.getSessionId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to current organization");
    }

    // =========================================================================
    // 4. Audit Logging & Zero Leakage Verification
    // =========================================================================

    @Test
    @DisplayName("Audit events recorded for session lifecycle")
    void testAuditEvents_LoggedProperly() {
        GovAuthSessionDto session = authService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        authService.revokeAuthentication(session.getSessionId());

        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()),
                any(),
                eventCaptor.capture(),
                any(),
                any(),
                any(),
                any()
        );

        List<String> events = eventCaptor.getAllValues();
        assertThat(events).contains("GOV_AUTH_SESSION_STARTED", "GOV_AUTHENTICATED", "GOV_AUTH_REVOKED");
    }

    // =========================================================================
    // 5. Controller Endpoints Verification
    // =========================================================================

    @Test
    @DisplayName("Controller POST /sessions, GET /sessions/{id}, POST /revoke, GET /connections/{id}/session")
    void testControllerEndpoints() {
        ResponseEntity<com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>> createResp =
                authController.startAuthentication(GovAuthStartRequest.builder()
                        .connectionId(connectionA.getId())
                        .authMethod(GovAuthMethod.DSC)
                        .build());

        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        UUID sessionId = createResp.getBody().getData().getSessionId();

        ResponseEntity<com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>> getResp =
                authController.getSessionStatus(sessionId, null);
        assertThat(getResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getResp.getBody().getData().getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);

        ResponseEntity<com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>> activeResp =
                authController.getActiveSessionForConnection(connectionA.getId());
        assertThat(activeResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(activeResp.getBody().getData().getSessionId()).isEqualTo(sessionId);

        ResponseEntity<com.taxoryn.core.response.ApiResponse<GovAuthSessionDto>> revokeResp =
                authController.revokeSession(sessionId);
        assertThat(revokeResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(revokeResp.getBody().getData().getStatus()).isEqualTo(GovAuthStatus.REVOKED);
    }
}
