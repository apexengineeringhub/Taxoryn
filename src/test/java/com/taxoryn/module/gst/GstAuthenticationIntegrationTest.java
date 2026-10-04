package com.taxoryn.module.gst;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.controller.GstAuthController;
import com.taxoryn.module.gst.dto.GstAuthContinueRequest;
import com.taxoryn.module.gst.dto.GstAuthSessionDto;
import com.taxoryn.module.gst.dto.GstAuthSessionRequest;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;
import com.taxoryn.module.gst.model.GstTokenStatus;
import com.taxoryn.module.gst.service.GstAuthenticationService;
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
class GstAuthenticationIntegrationTest {

    @Autowired
    private GstAuthenticationService gstAuthService;

    @Autowired
    private GstAuthController gstAuthController;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private GovConnectionDto gstConnectionA;
    private GovConnectionDto itdConnectionA;
    private GovConnectionDto gstConnectionB;
    private SecurityUser userA;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("GST Auth Org A " + UUID.randomUUID())
                .legalName("GST Auth Org A Pvt Ltd")
                .email("gstauth.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("GST Auth Org B " + UUID.randomUUID())
                .legalName("GST Auth Org B Pvt Ltd")
                .email("gstauth.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        userA = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgA.getId())
                .email("admin@gstauth-orga.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_MANAGE", "GST_WRITE", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );

        // 1. GST Connection for Org A
        gstConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Production Portal Org A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_cred_***")
                .rawSecret("SecretGstKeyOrgA123")
                .build());
        connectionService.activateConnection(gstConnectionA.getId());

        // 2. Income Tax Connection for Org A (for wrong provider test)
        itdConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Portal Org A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itdConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_cred_***")
                .rawSecret("SecretItdKeyOrgA123")
                .build());
        connectionService.activateConnection(itdConnectionA.getId());

        // 3. GST Connection for Org B (for multi-tenant tests)
        TenantContext.setTenantId(orgB.getId());
        gstConnectionB = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Portal Org B")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConnectionB.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_cred_b_***")
                .rawSecret("SecretGstKeyOrgB123")
                .build());
        connectionService.activateConnection(gstConnectionB.getId());

        TenantContext.setTenantId(orgA.getId());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // =========================================================================
    // 1. GST Authentication Lifecycle
    // =========================================================================

    @Test
    @DisplayName("General GST Authentication: Success creates ACTIVE token with MOCK_GST_SESSION_ ref")
    void testGstAuthentication_Success() {
        GstAuthSessionDto session = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("corr-gst-001")
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(session.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);
        assertThat(session.getPurpose()).isEqualTo(GstAuthenticationPurpose.GST);
        assertThat(session.getTokenReference()).startsWith("MOCK_GST_SESSION_");
        assertThat(session.isRequiresUserAction()).isFalse();
        assertThat(session.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Interactive GST Authentication: Pending -> Continue -> ACTIVE Token")
    void testGstAuthentication_InteractiveFlow() {
        // Start interactive
        GstAuthSessionDto pending = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .authMethod(GovAuthMethod.OTP)
                .correlationId("corr-gst-otp-001")
                .options(Map.of("mockOutcome", "REQUIRES_ACTION"))
                .build());

        assertThat(pending.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(pending.getTokenStatus()).isEqualTo(GstTokenStatus.AUTHENTICATION_REQUIRED);
        assertThat(pending.isRequiresUserAction()).isTrue();
        assertThat(pending.getActionPrompt()).contains("OTP");

        // Continue
        GstAuthSessionDto completed = gstAuthService.continueAuthorization(pending.getSessionId(),
                GstAuthContinueRequest.builder()
                        .actionReference("otp-ack-123456")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build(),
                GstAuthenticationPurpose.GST);

        assertThat(completed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(completed.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);
        assertThat(completed.isRequiresUserAction()).isFalse();
    }

    @Test
    @DisplayName("GST Authentication Failures: Credentials rejected, Gateway unavailable, Timeout")
    void testGstAuthentication_Failures() {
        // 1. Invalid credentials
        GstAuthSessionDto failed = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .options(Map.of("mockOutcome", "FAILED"))
                .build());

        assertThat(failed.getTokenStatus()).isEqualTo(GstTokenStatus.FAILED);
        assertThat(failed.getFailureCode()).isEqualTo("INVALID_CREDENTIALS");

        // 2. Gateway unavailable (503)
        GstAuthSessionDto unavail = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());

        assertThat(unavail.getTokenStatus()).isEqualTo(GstTokenStatus.FAILED);
        assertThat(unavail.getFailureCode()).isEqualTo("PROVIDER_UNAVAILABLE");

        // 3. Timeout
        GstAuthSessionDto timeout = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());

        assertThat(timeout.getTokenStatus()).isEqualTo(GstTokenStatus.FAILED);
        assertThat(timeout.getFailureCode()).isEqualTo("TIMEOUT");
    }

    // =========================================================================
    // 2. E-Way Bill & E-Invoice Authentication & Token Lifecycle
    // =========================================================================

    @Test
    @DisplayName("E-Way Bill Authentication: Creates active token with MOCK_EWAY_SESSION_ ref")
    void testEWayBillAuthentication_Success() {
        GstAuthSessionDto session = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.EWAY_BILL)
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("corr-eway-001")
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(session.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);
        assertThat(session.getPurpose()).isEqualTo(GstAuthenticationPurpose.EWAY_BILL);
        assertThat(session.getTokenReference()).startsWith("MOCK_EWAY_SESSION_");
    }

    @Test
    @DisplayName("E-Invoice Authentication: Creates active token with MOCK_EINV_SESSION_ ref")
    void testEInvoiceAuthentication_Success() {
        GstAuthSessionDto session = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.E_INVOICE)
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("corr-einv-001")
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(session.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);
        assertThat(session.getPurpose()).isEqualTo(GstAuthenticationPurpose.E_INVOICE);
        assertThat(session.getTokenReference()).startsWith("MOCK_EINV_SESSION_");
    }

    // =========================================================================
    // 3. Purpose Isolation & Cross-Purpose Protection
    // =========================================================================

    @Test
    @DisplayName("Purpose Isolation: GST session cannot satisfy E-Way or E-Invoice request")
    void testPurposeIsolation_Protection() {
        // Authenticate for general GST
        gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .correlationId("corr-iso-gst")
                .build());

        // Query active token for EWAY_BILL -> must return null (not reused!)
        GstAuthSessionDto ewayToken = gstAuthService.getActiveToken(gstConnectionA.getId(), GstAuthenticationPurpose.EWAY_BILL);
        assertThat(ewayToken).isNull();

        // Query active token for E_INVOICE -> must return null
        GstAuthSessionDto einvToken = gstAuthService.getActiveToken(gstConnectionA.getId(), GstAuthenticationPurpose.E_INVOICE);
        assertThat(einvToken).isNull();

        // Query active token for GST -> returns active token
        GstAuthSessionDto gstToken = gstAuthService.getActiveToken(gstConnectionA.getId(), GstAuthenticationPurpose.GST);
        assertThat(gstToken).isNotNull();
        assertThat(gstToken.getPurpose()).isEqualTo(GstAuthenticationPurpose.GST);
    }

    // =========================================================================
    // 4. Token Refresh & Expiration
    // =========================================================================

    @Test
    @DisplayName("Token Refresh: Rotates token reference and extends expiry")
    void testTokenRefresh() {
        GstAuthSessionDto session = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.EWAY_BILL)
                .correlationId("corr-refresh-001")
                .build());

        GstAuthSessionDto refreshed = gstAuthService.refreshToken(session.getSessionId(), GstAuthenticationPurpose.EWAY_BILL, Map.of());

        assertThat(refreshed.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);
        assertThat(refreshed.getTokenReference()).contains("_ROTATED");
        assertThat(refreshed.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Revoke Session: Transitions token to REVOKED")
    void testRevokeSession() {
        GstAuthSessionDto session = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .build());

        GstAuthSessionDto revoked = gstAuthService.revoke(session.getSessionId(), GstAuthenticationPurpose.GST);
        assertThat(revoked.getTokenStatus()).isEqualTo(GstTokenStatus.REVOKED);
        assertThat(revoked.getStatus()).isEqualTo(GovAuthStatus.REVOKED);
    }

    // =========================================================================
    // 5. Security, Multi-Tenancy & Wrong Provider Connection
    // =========================================================================

    @Test
    @DisplayName("Non-GST Provider Connection is rejected")
    void testNonGstConnection_Rejected() {
        assertThatThrownBy(() -> gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(itdConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("GST provider is required");
    }

    @Test
    @DisplayName("Cross-Tenant GST Authentication Access Denied")
    void testCrossTenantAccess_Denied() {
        // Org A cannot start auth on Org B's connection
        assertThatThrownBy(() -> gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionB.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .build()))
                .isInstanceOf(GovConnectionNotFoundException.class);

        // Org A authenticates own connection
        GstAuthSessionDto sessionA = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .build());

        // Switch to Org B
        TenantContext.setTenantId(orgB.getId());

        assertThatThrownBy(() -> gstAuthService.getAuthenticationStatus(sessionA.getSessionId(), GstAuthenticationPurpose.GST, Map.of()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to current organization");

        assertThatThrownBy(() -> gstAuthService.revoke(sessionA.getSessionId(), GstAuthenticationPurpose.GST))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to current organization");
    }

    // =========================================================================
    // 6. Zero Secret Leakage Verification
    // =========================================================================

    @Test
    @DisplayName("Zero Secret Leakage: DTO and Audit logs contain no secrets or passwords")
    void testZeroSecretLeakage() {
        GstAuthSessionDto session = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.E_INVOICE)
                .correlationId("corr-leak-gst-01")
                .build());

        // Verify DTO fields
        assertThat(session.getTokenReference()).doesNotContain("SecretGstKeyOrgA123");
        assertThat(session.getSafeAuthorizationReference()).doesNotContain("SecretGstKeyOrgA123");

        // Verify Audit Logs
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> auditDetailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()),
                any(),
                any(),
                eq("GST_AUTH_SESSION"),
                any(),
                any(),
                auditDetailsCaptor.capture()
        );

        List<Map<String, Object>> allAuditDetails = auditDetailsCaptor.getAllValues();
        for (Map<String, Object> details : allAuditDetails) {
            assertThat(details.values().toString())
                    .doesNotContain("SecretGstKeyOrgA123")
                    .doesNotContain("password");
        }
    }

    // =========================================================================
    // 7. REST Controller Integration
    // =========================================================================

    @Test
    @DisplayName("REST Controller: Authenticate, Continue, Status, Refresh, Revoke, Active Token")
    void testGstAuthController_EndToEnd() {
        // 1. POST /sessions
        ResponseEntity<?> resp = gstAuthController.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.EWAY_BILL)
                .build());
        assertThat(resp.getStatusCode().value()).isEqualTo(201);

        GstAuthSessionDto dto = ((com.taxoryn.core.response.ApiResponse<GstAuthSessionDto>) resp.getBody()).getData();
        assertThat(dto.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);
        assertThat(dto.getPurpose()).isEqualTo(GstAuthenticationPurpose.EWAY_BILL);

        // 2. GET /sessions/{id}
        ResponseEntity<?> statusResp = gstAuthController.getSessionStatus(dto.getSessionId(), GstAuthenticationPurpose.EWAY_BILL, null);
        assertThat(statusResp.getStatusCode().value()).isEqualTo(200);

        // 3. POST /sessions/{id}/refresh
        ResponseEntity<?> refreshResp = gstAuthController.refreshToken(dto.getSessionId(), GstAuthenticationPurpose.EWAY_BILL);
        assertThat(refreshResp.getStatusCode().value()).isEqualTo(200);

        // 4. GET /connections/{connId}/token
        ResponseEntity<?> tokenResp = gstAuthController.getActiveToken(gstConnectionA.getId(), GstAuthenticationPurpose.EWAY_BILL);
        assertThat(tokenResp.getStatusCode().value()).isEqualTo(200);
        GstAuthSessionDto activeDto = ((com.taxoryn.core.response.ApiResponse<GstAuthSessionDto>) tokenResp.getBody()).getData();
        assertThat(activeDto.getTokenStatus()).isEqualTo(GstTokenStatus.ACTIVE);

        // 5. POST /sessions/{id}/revoke
        ResponseEntity<?> revokeResp = gstAuthController.revokeSession(dto.getSessionId(), GstAuthenticationPurpose.EWAY_BILL);
        assertThat(revokeResp.getStatusCode().value()).isEqualTo(200);
        GstAuthSessionDto revokedDto = ((com.taxoryn.core.response.ApiResponse<GstAuthSessionDto>) revokeResp.getBody()).getData();
        assertThat(revokedDto.getTokenStatus()).isEqualTo(GstTokenStatus.REVOKED);
    }
}
