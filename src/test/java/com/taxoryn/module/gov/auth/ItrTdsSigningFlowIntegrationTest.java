package com.taxoryn.module.gov.auth;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.dto.*;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthPurpose;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.repository.GovAuthSessionRepository;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.itr.controller.ItrAuthController;
import com.taxoryn.module.itr.dto.*;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.service.ItrAuthenticationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.tds.controller.TdsAuthController;
import com.taxoryn.module.tds.dto.*;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.service.TdsAuthenticationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

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
class ItrTdsSigningFlowIntegrationTest {

    @Autowired
    private ItrAuthenticationService itrAuthService;

    @Autowired
    private ItrAuthController itrAuthController;

    @Autowired
    private TdsAuthenticationService tdsAuthService;

    @Autowired
    private TdsAuthController tdsAuthController;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovAuthSessionRepository authSessionRepository;

    @Autowired
    private ItrProfileRepository itrProfileRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private SecurityUser userA;
    private GovConnectionDto itrConnectionA;
    private GovConnectionDto tdsConnectionA;
    private GovConnectionDto gstConnectionA;
    private GovConnectionDto itrConnectionB;
    private ItrProfileEntity itrProfileA;
    private TdsProfileEntity tdsProfileA;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR TDS Org A " + UUID.randomUUID())
                .legalName("ITR TDS Org A Pvt Ltd")
                .email("itrtda.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR TDS Org B " + UUID.randomUUID())
                .legalName("ITR TDS Org B Pvt Ltd")
                .email("itrtda.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        userA = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgA.getId())
                .email("admin@itrtda-orga.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_WRITE", "TDS_VIEW", "TDS_CREATE", "TDS_WRITE", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );

        // 1. Setup ITR connection for ORG_A
        itrConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("Income Tax Portal Org A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itr_cred_***")
                .rawSecret("SecretKeyItrA123")
                .build());
        connectionService.activateConnection(itrConnectionA.getId());

        // 2. Setup TDS connection for ORG_A
        tdsConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TDS Portal Org A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("tds_cred_***")
                .rawSecret("SecretKeyTdsA123")
                .build());
        connectionService.activateConnection(tdsConnectionA.getId());

        // 3. Setup GST connection for ORG_A
        gstConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Portal Org A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_cred_***")
                .rawSecret("SecretKeyGstA123")
                .build());
        connectionService.activateConnection(gstConnectionA.getId());

        // 4. Setup ITR Profile for ORG_A
        itrProfileA = ItrProfileEntity.builder()
                .clientId(UUID.randomUUID())
                .pan("ABCDE1234F")
                .build();
        itrProfileA.setOrganizationId(orgA.getId());
        itrProfileA = itrProfileRepository.save(itrProfileA);

        // 5. Setup TDS Profile for ORG_A
        tdsProfileA = TdsProfileEntity.builder()
                .clientId(UUID.randomUUID())
                .tan("ABCD12345E")
                .active(true)
                .build();
        tdsProfileA.setOrganizationId(orgA.getId());
        tdsProfileA = tdsProfileRepository.save(tdsProfileA);

        // 6. Setup ITR connection for ORG_B
        TenantContext.setTenantId(orgB.getId());
        itrConnectionB = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("Income Tax Portal Org B")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConnectionB.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itr_cred_***")
                .rawSecret("SecretKeyItrB123")
                .build());
        connectionService.activateConnection(itrConnectionB.getId());

        TenantContext.setTenantId(orgA.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // A. ITR EVC Flow Tests
    // =========================================================================

    @Test
    @DisplayName("ITR EVC: 1. Start EVC challenge successfully")
    void testItrEvc_StartChallengeSuccess() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .pan(itrProfileA.getPan())
                .profileId(itrProfileA.getId())
                .correlationId("corr-itr-evc-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        assertThat(challenge).isNotNull();
        assertThat(challenge.getSessionId()).isNotNull();
        assertThat(challenge.getConnectionId()).isEqualTo(itrConnectionA.getId());
        assertThat(challenge.getPurpose()).isEqualTo(GovAuthPurpose.ITR_EVC);
        assertThat(challenge.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(challenge.getAuthorizationState()).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);
        assertThat(challenge.getChallengeReference()).contains("mock-evc-challenge-");
        assertThat(challenge.getAttemptsRemaining()).isEqualTo(3);
    }

    @Test
    @DisplayName("ITR EVC: 2. Verify valid EVC successfully")
    void testItrEvc_VerifyValidSuccess() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-itr-evc-02")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .options(Map.of("mockOutcome", "SUCCESS"))
                .build());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(result.getAuthorizationState()).isEqualTo(GovAuthorizationState.AUTHORIZATION_COMPLETED);
        assertThat(result.getVerificationReference()).contains("MOCK_EVC_VERIFIED_");
    }

    @Test
    @DisplayName("ITR EVC: 3. Verify invalid EVC code fails gracefully")
    void testItrEvc_VerifyInvalidFails() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-itr-evc-03")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("000000")
                .actionReference("INVALID_EVC")
                .options(Map.of("mockOutcome", "INVALID_EVC"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(result.getFailureCode()).isEqualTo("INVALID_EVC");
        assertThat(result.getSafeFailureMessage()).contains("invalid or incorrect");
    }

    @Test
    @DisplayName("ITR EVC: 4. Expired EVC challenge is rejected")
    void testItrEvc_ExpiredChallengeFails() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-itr-evc-04")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .actionReference("EXPIRED")
                .options(Map.of("mockOutcome", "EXPIRED"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.EXPIRED);
        assertThat(result.getFailureCode()).isEqualTo("SESSION_EXPIRED");
    }

    @Test
    @DisplayName("ITR EVC: 5. Max attempts exceeded locks out challenge")
    void testItrEvc_MaxAttemptsExceeded() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-itr-evc-05")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("999999")
                .actionReference("MAX_ATTEMPTS")
                .options(Map.of("mockOutcome", "MAX_ATTEMPTS"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(result.getFailureCode()).isEqualTo("MAX_ATTEMPTS_EXCEEDED");
    }

    @Test
    @DisplayName("ITR EVC: 6. Provider unavailable failure handled cleanly")
    void testItrEvc_ProviderUnavailable() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-itr-evc-06")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .actionReference("PROVIDER_UNAVAILABLE")
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(result.getFailureCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }

    // =========================================================================
    // B. TDS EVC Flow Tests
    // =========================================================================

    @Test
    @DisplayName("TDS EVC: 7. Start TDS EVC challenge successfully")
    void testTdsEvc_StartChallengeSuccess() {
        GovEvcChallengeDto challenge = tdsAuthService.startEvc(TdsEvcStartRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .tan(tdsProfileA.getTan())
                .profileId(tdsProfileA.getId())
                .correlationId("corr-tds-evc-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        assertThat(challenge).isNotNull();
        assertThat(challenge.getConnectionId()).isEqualTo(tdsConnectionA.getId());
        assertThat(challenge.getPurpose()).isEqualTo(GovAuthPurpose.TDS_EVC);
        assertThat(challenge.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(challenge.getChallengeReference()).contains("mock-evc-challenge-");
    }

    @Test
    @DisplayName("TDS EVC: 8. Verify valid TDS EVC successfully")
    void testTdsEvc_VerifyValidSuccess() {
        GovEvcChallengeDto challenge = tdsAuthService.startEvc(TdsEvcStartRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .correlationId("corr-tds-evc-02")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = tdsAuthService.verifyEvc(challenge.getSessionId(), TdsEvcVerifyRequest.builder()
                .verificationCode("654321")
                .options(Map.of("mockOutcome", "SUCCESS"))
                .build());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(result.getPurpose()).isEqualTo(GovAuthPurpose.TDS_EVC);
    }

    @Test
    @DisplayName("TDS EVC: 9. Invalid TDS EVC fails cleanly")
    void testTdsEvc_VerifyInvalidFails() {
        GovEvcChallengeDto challenge = tdsAuthService.startEvc(TdsEvcStartRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .correlationId("corr-tds-evc-03")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = tdsAuthService.verifyEvc(challenge.getSessionId(), TdsEvcVerifyRequest.builder()
                .verificationCode("000000")
                .actionReference("INVALID_EVC")
                .options(Map.of("mockOutcome", "INVALID_EVC"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getFailureCode()).isEqualTo("INVALID_EVC");
    }

    // =========================================================================
    // C. ITR DSC Flow Tests
    // =========================================================================

    @Test
    @DisplayName("ITR DSC: 10. Start DSC signing session")
    void testItrDsc_StartSigningSession() {
        GovDscSigningSessionDto session = itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(itrConnectionA.getId())
                .documentDigest("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .correlationId("corr-itr-dsc-01")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getPurpose()).isEqualTo(GovAuthPurpose.ITR_DSC);
        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(session.getDocumentDigest()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(session.getSigningChallengeReference()).contains("mock-dsc-challenge-");
    }

    @Test
    @DisplayName("ITR DSC: 11. Verify valid digital signature successfully")
    void testItrDsc_VerifyValidSignature() {
        GovDscSigningSessionDto session = itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(itrConnectionA.getId())
                .documentDigest("digest123")
                .correlationId("corr-itr-dsc-02")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        GovDscVerifyResultDto result = itrAuthService.verifyDsc(session.getSessionId(), ItrDscVerifyRequest.builder()
                .signatureReference("MOCK_DSC_SIGNATURE_itr02")
                .certificateReference("MOCK_DSC_CERT_itr02")
                .options(Map.of("mockOutcome", "SUCCESS"))
                .build());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(result.getCertificateSubject()).contains("Authorized Signatory (ITR)");
        assertThat(result.getSigningAlgorithm()).isEqualTo("SHA256withRSA");
    }

    @Test
    @DisplayName("ITR DSC: 12. Invalid digital signature fails verification")
    void testItrDsc_InvalidSignatureFails() {
        GovDscSigningSessionDto session = itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(itrConnectionA.getId())
                .documentDigest("digest123")
                .correlationId("corr-itr-dsc-03")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        GovDscVerifyResultDto result = itrAuthService.verifyDsc(session.getSessionId(), ItrDscVerifyRequest.builder()
                .signatureReference("CORRUPTED_SIGNATURE")
                .actionReference("INVALID_SIGNATURE")
                .options(Map.of("mockOutcome", "INVALID_SIGNATURE"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getFailureCode()).isEqualTo("INVALID_SIGNATURE");
        assertThat(result.getSafeFailureMessage()).contains("does not match payload digest");
    }

    @Test
    @DisplayName("ITR DSC: 13. Expired certificate fails verification")
    void testItrDsc_ExpiredCertificateFails() {
        GovDscSigningSessionDto session = itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(itrConnectionA.getId())
                .documentDigest("digest123")
                .correlationId("corr-itr-dsc-04")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        GovDscVerifyResultDto result = itrAuthService.verifyDsc(session.getSessionId(), ItrDscVerifyRequest.builder()
                .signatureReference("MOCK_DSC_SIGNATURE_exp")
                .actionReference("CERTIFICATE_EXPIRED")
                .options(Map.of("mockOutcome", "CERTIFICATE_EXPIRED"))
                .build());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getFailureCode()).isEqualTo("CERTIFICATE_EXPIRED");
        assertThat(result.getSafeFailureMessage()).contains("Certificate (DSC) has expired");
    }

    // =========================================================================
    // D. TDS DSC Flow Tests
    // =========================================================================

    @Test
    @DisplayName("TDS DSC: 14. Start TDS DSC signing and verify valid signature")
    void testTdsDsc_StartAndVerifyValid() {
        GovDscSigningSessionDto session = tdsAuthService.startDsc(TdsDscSignRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .documentDigest("fvu_digest_999")
                .correlationId("corr-tds-dsc-01")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        assertThat(session.getPurpose()).isEqualTo(GovAuthPurpose.TDS_DSC);

        GovDscVerifyResultDto result = tdsAuthService.verifyDsc(session.getSessionId(), TdsDscVerifyRequest.builder()
                .signatureReference("MOCK_DSC_SIGNATURE_tds01")
                .options(Map.of("mockOutcome", "SUCCESS"))
                .build());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);
        assertThat(result.getCertificateSubject()).contains("Authorized Deductor (TDS)");
    }

    // =========================================================================
    // E. Purpose & Domain Isolation Tests
    // =========================================================================

    @Test
    @DisplayName("Isolation: 15. ITR request using TDS or GST connection is rejected")
    void testIsolation_ItrRejectsNonItrConnection() {
        assertThatThrownBy(() -> itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Income Tax provider connection is required");

        assertThatThrownBy(() -> itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(gstConnectionA.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Income Tax provider connection is required");
    }

    @Test
    @DisplayName("Isolation: 16. TDS request using ITR or GST connection is rejected")
    void testIsolation_TdsRejectsNonTdsConnection() {
        assertThatThrownBy(() -> tdsAuthService.startEvc(TdsEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TDS provider connection is required");

        assertThatThrownBy(() -> tdsAuthService.startDsc(TdsDscSignRequest.builder()
                .connectionId(gstConnectionA.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TDS provider connection is required");
    }

    @Test
    @DisplayName("Isolation: 17. EVC session cannot be verified via DSC service")
    void testIsolation_EvcSessionCannotBeVerifiedAsDsc() {
        GovEvcChallengeDto evc = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-iso-evc")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        assertThatThrownBy(() -> itrAuthService.verifyDsc(evc.getSessionId(), ItrDscVerifyRequest.builder()
                .signatureReference("SIG123")
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Session auth method is not DSC");
    }

    @Test
    @DisplayName("Isolation: 18. DSC session cannot be verified via EVC service")
    void testIsolation_DscSessionCannotBeVerifiedAsEvc() {
        GovDscSigningSessionDto dsc = itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-iso-dsc")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        assertThatThrownBy(() -> itrAuthService.verifyEvc(dsc.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Session auth method is not EVC");
    }

    // =========================================================================
    // F. Multi-Tenancy & Security Tests
    // =========================================================================

    @Test
    @DisplayName("Multi-Tenancy: 19. Cross-tenant ITR and TDS connection access is denied")
    void testMultiTenancy_CrossTenantAccessDenied() {
        TenantContext.setTenantId(orgA.getId());

        // Attempting to access ORG_B connection from ORG_A context
        assertThatThrownBy(() -> itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionB.getId())
                .build()))
                .isInstanceOf(RuntimeException.class);
    }

    // =========================================================================
    // G. Lifecycle & Idempotency Tests
    // =========================================================================

    @Test
    @DisplayName("Lifecycle: 20. Terminal state cannot be reused for verification")
    void testLifecycle_TerminalStateCannotBeReused() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-term-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        // Verify once to reach terminal AUTHENTICATED status
        itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .options(Map.of("mockOutcome", "SUCCESS"))
                .build());

        // Second verification must fail
        assertThatThrownBy(() -> itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot verify EVC on session in terminal state");
    }

    @Test
    @DisplayName("Lifecycle: 21. Cancelled session transitions to REVOKED and cannot be verified")
    void testLifecycle_CancelledSessionCannotBeVerified() {
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-cancel-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovAuthSessionDto cancelled = itrAuthService.cancel(challenge.getSessionId());
        assertThat(cancelled.getStatus()).isEqualTo(GovAuthStatus.REVOKED);

        assertThatThrownBy(() -> itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode("123456")
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot verify EVC on session in terminal state");
    }

    // =========================================================================
    // H. Secret Safety & Audit Verification
    // =========================================================================

    @Test
    @DisplayName("Secret Safety: 22. No OTP/EVC or private keys persisted or audited")
    void testSecretSafety_NoSecretsPersistedOrAudited() {
        String secretCode = "889977";
        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .correlationId("corr-sec-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        itrAuthService.verifyEvc(challenge.getSessionId(), ItrEvcVerifyRequest.builder()
                .verificationCode(secretCode)
                .options(Map.of("mockOutcome", "SUCCESS"))
                .build());

        // Verify entity in DB
        GovAuthSessionEntity entity = authSessionRepository.findById(challenge.getSessionId()).orElseThrow();
        if (entity.getMetadata() != null) {
            assertThat(entity.getMetadata()).doesNotContain(secretCode);
        }
        if (entity.getActionPrompt() != null) {
            assertThat(entity.getActionPrompt()).doesNotContain(secretCode);
        }

        // Verify Audit events were recorded safely
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()),
                any(),
                any(),
                any(),
                any(),
                any(),
                detailsCaptor.capture()
        );

        for (Map<String, Object> details : detailsCaptor.getAllValues()) {
            assertThat(details.toString()).doesNotContain(secretCode);
            assertThat(details.toString()).doesNotContain("SecretKeyItrA123");
        }
    }
}
