package com.taxoryn.module.gov.auth;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.consent.dto.ApproveConsentRequest;
import com.taxoryn.module.consent.dto.ConsentAuthorizationRequest;
import com.taxoryn.module.consent.dto.ConsentAuthorizationResult;
import com.taxoryn.module.consent.dto.ConsentDto;
import com.taxoryn.module.consent.dto.CreateConsentRequest;
import com.taxoryn.module.consent.dto.RevokeConsentRequest;
import com.taxoryn.module.consent.entity.TaxpayerConsentEntity;
import com.taxoryn.module.consent.model.ConsentMethod;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.model.ConsentStatus;
import com.taxoryn.module.consent.model.DelegationType;
import com.taxoryn.module.consent.repository.TaxpayerConsentRepository;
import com.taxoryn.module.consent.service.ConsentAuthorizationService;
import com.taxoryn.module.consent.service.ConsentManagementService;
import com.taxoryn.module.consent.service.GovernmentOperationAuthorizationService;
import com.taxoryn.module.gov.auth.dto.*;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthPurpose;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.repository.GovAuthSessionRepository;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.dto.GstAuthContinueRequest;
import com.taxoryn.module.gst.dto.GstAuthSessionDto;
import com.taxoryn.module.gst.dto.GstAuthSessionRequest;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;
import com.taxoryn.module.gst.service.GstAuthenticationService;
import com.taxoryn.module.itr.dto.ItrDscSignRequest;
import com.taxoryn.module.itr.dto.ItrDscVerifyRequest;
import com.taxoryn.module.itr.dto.ItrEvcStartRequest;
import com.taxoryn.module.itr.dto.ItrEvcVerifyRequest;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.service.ItrAuthenticationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.tds.dto.TdsDscSignRequest;
import com.taxoryn.module.tds.dto.TdsDscVerifyRequest;
import com.taxoryn.module.tds.dto.TdsEvcStartRequest;
import com.taxoryn.module.tds.dto.TdsEvcVerifyRequest;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.service.TdsAuthenticationService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
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

import java.time.Duration;
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
class Phase26HardeningAndSecurityIntegrationTest {

    @Autowired
    private GovernmentAuthenticationService govAuthService;

    @Autowired
    private GstAuthenticationService gstAuthService;

    @Autowired
    private ItrAuthenticationService itrAuthService;

    @Autowired
    private TdsAuthenticationService tdsAuthService;

    @Autowired
    private ConsentManagementService consentService;

    @Autowired
    private ConsentAuthorizationService consentAuthorizationService;

    @Autowired
    private GovernmentOperationAuthorizationService govOpAuthService;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovAuthSessionRepository authSessionRepository;

    @Autowired
    private TaxpayerConsentRepository consentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ItrProfileRepository itrProfileRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private ClientEntity clientA;
    private ClientEntity clientB;
    private UserEntity userA;
    private UserEntity userB;
    private SecurityUser secUserA;
    private SecurityUser secUserB;

    private GovConnectionDto gstConnectionA;
    private GovConnectionDto itrConnectionA;
    private GovConnectionDto tdsConnectionA;
    private GovConnectionDto gstConnectionB;

    private ItrProfileEntity itrProfileA;
    private TdsProfileEntity tdsProfileA;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Hardening Org A " + UUID.randomUUID())
                .legalName("Hardening Org A Pvt Ltd")
                .email("hard.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Hardening Org B " + UUID.randomUUID())
                .legalName("Hardening Org B Pvt Ltd")
                .email("hard.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Setup Tenant A User and Client
        TenantContext.setTenantId(orgA.getId());

        clientA = ClientEntity.builder()
                .displayName("Hardening Client A")
                .clientCode("CLNT_HA_" + UUID.randomUUID().toString().substring(0, 5))
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .build();
        clientA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clientA);

        userA = UserEntity.builder()
                .organizationId(orgA.getId())
                .email("delegate.a." + UUID.randomUUID() + "@taxoryn.com")
                .firstName("Delegate")
                .lastName("User A")
                .passwordHash("secret_hash")
                .build();
        userA = userRepository.save(userA);

        secUserA = SecurityUser.builder()
                .userId(userA.getId())
                .organizationId(orgA.getId())
                .email(userA.getEmail())
                .roles(Set.of("ROLE_PRACTITIONER"))
                .permissions(Set.of("GST_MANAGE", "ITR_MANAGE", "TDS_MANAGE", "CLIENT_VIEW", "CLIENT_WRITE", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(secUserA, null, secUserA.getAuthorities()));

        gstConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Primary Connection A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("GSTN_AUTH_KEY_A")
                .rawSecret("GSTN_AUTH_SECRET_A")
                .build());
        connectionService.activateConnection(gstConnectionA.getId());

        itrConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITR Primary Connection A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("ABCDE1234F")
                .rawSecret("SECRET_ITR_PASS")
                .build());
        connectionService.activateConnection(itrConnectionA.getId());

        tdsConnectionA = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TDS Primary Connection A")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("BLRP00001A")
                .rawSecret("SECRET_TDS_PASS")
                .build());
        connectionService.activateConnection(tdsConnectionA.getId());

        itrProfileA = ItrProfileEntity.builder()
                .clientId(clientA.getId())
                .pan("ABCDE1234F")
                .active(true)
                .build();
        itrProfileA.setOrganizationId(orgA.getId());
        itrProfileA = itrProfileRepository.save(itrProfileA);

        tdsProfileA = TdsProfileEntity.builder()
                .clientId(clientA.getId())
                .tan("BLRP00001A")
                .active(true)
                .build();
        tdsProfileA.setOrganizationId(orgA.getId());
        tdsProfileA = tdsProfileRepository.save(tdsProfileA);

        // Setup Tenant B User, Client and Connection
        TenantContext.setTenantId(orgB.getId());

        clientB = ClientEntity.builder()
                .displayName("Hardening Client B")
                .clientCode("CLNT_HB_" + UUID.randomUUID().toString().substring(0, 5))
                .pan("XYZPQ5678R")
                .gstin("27XYZPQ5678R1Z2")
                .build();
        clientB.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientB);

        userB = UserEntity.builder()
                .organizationId(orgB.getId())
                .email("delegate.b." + UUID.randomUUID() + "@taxoryn.com")
                .firstName("Delegate")
                .lastName("User B")
                .passwordHash("secret_hash")
                .build();
        userB = userRepository.save(userB);

        secUserB = SecurityUser.builder()
                .userId(userB.getId())
                .organizationId(orgB.getId())
                .email(userB.getEmail())
                .roles(Set.of("ROLE_PRACTITIONER"))
                .permissions(Set.of("GST_MANAGE", "ITR_MANAGE", "TDS_MANAGE", "CLIENT_VIEW", "CLIENT_WRITE", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(secUserB, null, secUserB.getAuthorities()));

        gstConnectionB = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Primary Connection B")
                .build());
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConnectionB.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("GSTN_AUTH_KEY_B")
                .rawSecret("GSTN_AUTH_SECRET_B")
                .build());
        connectionService.activateConnection(gstConnectionB.getId());

        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. AUTHENTICATION LIFECYCLE & GOVERNANCE (1 - 10)
    // =========================================================================

    @Test
    @DisplayName("1-2. Start and complete successful government authentication")
    void testStartAndCompleteSuccessfulAuth() {
        setAuthContext(orgA.getId(), secUserA);

        GovAuthSessionDto session = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("corr-test-start")
                .build());

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getStatus()).isIn(GovAuthStatus.AUTHENTICATED, GovAuthStatus.AUTHENTICATION_PENDING);

        GovAuthorizationState state = govAuthService.getAuthorizationState(session.getSessionId());
        assertThat(state).isNotNull();
    }

    @Test
    @DisplayName("3-4. Pending authorization and failed authentication state")
    void testPendingAndFailedAuthState() {
        setAuthContext(orgA.getId(), secUserA);

        GovAuthSessionDto session = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "PENDING"))
                .build());

        assertThat(session.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_PENDING);
        assertThat(session.getAuthorizationState()).isEqualTo(GovAuthorizationState.USER_ACTION_REQUIRED);

        GovAuthSessionDto failed = govAuthService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder()
                        .actionReference("FAIL")
                        .options(Map.of("mockOutcome", "FAILED"))
                        .build());

        assertThat(failed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATION_FAILED);
        assertThat(failed.getFailureCode()).isNotNull();
    }

    @Test
    @DisplayName("5. Expired authentication cannot authorize operations")
    void testExpiredAuthenticationCannotAuthorize() {
        setAuthContext(orgA.getId(), secUserA);

        GovAuthSessionEntity expiredEntity = GovAuthSessionEntity.builder()
                .connectionId(gstConnectionA.getId())
                .providerType(GovProviderType.GST)
                .authMethod(GovAuthMethod.OAUTH2)
                .status(GovAuthStatus.AUTHENTICATED)
                .expiresAt(Instant.now().minus(Duration.ofHours(2)))
                .build();
        expiredEntity.setOrganizationId(orgA.getId());
        authSessionRepository.save(expiredEntity);

        GovAuthSessionDto active = govAuthService.getActiveSessionForConnection(gstConnectionA.getId());
        assertThat(active).isNull();

        GovAuthSessionDto status = govAuthService.getAuthenticationStatus(expiredEntity.getId());
        assertThat(status.getStatus()).isEqualTo(GovAuthStatus.EXPIRED);
    }

    @Test
    @DisplayName("6. Revoked authentication session cannot be reused or continued")
    void testRevokedSessionCannotBeReused() {
        setAuthContext(orgA.getId(), secUserA);

        GovAuthSessionDto session = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        GovAuthSessionDto revoked = govAuthService.revokeAuthentication(session.getSessionId());
        assertThat(revoked.getStatus()).isEqualTo(GovAuthStatus.REVOKED);

        assertThatThrownBy(() -> govAuthService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder().actionReference("TEST").build()))
                .isInstanceOf(GovIntegrationException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("7. Duplicate authentication request returns active session idempotently")
    void testDuplicateAuthenticationIdempotency() {
        setAuthContext(orgA.getId(), secUserA);

        GovAuthSessionDto sess1 = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("idemp-1")
                .build());

        GovAuthSessionDto sess2 = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .correlationId("idemp-2")
                .build());

        assertThat(sess2.getSessionId()).isEqualTo(sess1.getSessionId());
    }

    @Test
    @DisplayName("8-10. Continuation with valid, invalid, and wrong tenant contexts")
    void testContinuationScenarios() {
        setAuthContext(orgA.getId(), secUserA);

        GovAuthSessionDto session = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .options(Map.of("mockOutcome", "PENDING"))
                .build());

        // Valid continuation
        GovAuthSessionDto continued = govAuthService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder().actionReference("MOCK_CODE_123").build());
        assertThat(continued.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);

        // Cross-tenant continuation attempt
        setAuthContext(orgB.getId(), secUserB);
        assertThatThrownBy(() -> govAuthService.continueAuthorization(session.getSessionId(),
                GovAuthContinueRequest.builder().actionReference("ATTACK").build()))
                .isInstanceOf(AppException.class);
    }

    // =========================================================================
    // 2. GST AUTHENTICATION & PURPOSE ISOLATION (11 - 17)
    // =========================================================================

    @Test
    @DisplayName("11-15. GST authentication token lifecycle: authenticate, refresh, revoke")
    void testGstTokenLifecycle() {
        setAuthContext(orgA.getId(), secUserA);

        GstAuthSessionDto gstSession = gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        assertThat(gstSession).isNotNull();
        assertThat(gstSession.getPurpose()).isEqualTo(GstAuthenticationPurpose.GST);
        assertThat(gstSession.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);

        GstAuthSessionDto refreshed = gstAuthService.refreshToken(gstSession.getSessionId(),
                GstAuthenticationPurpose.GST, Map.of());
        assertThat(refreshed.getStatus()).isEqualTo(GovAuthStatus.AUTHENTICATED);

        GstAuthSessionDto revoked = gstAuthService.revoke(gstSession.getSessionId(), GstAuthenticationPurpose.GST);
        assertThat(revoked.getStatus()).isEqualTo(GovAuthStatus.REVOKED);
    }

    @Test
    @DisplayName("16-17. GST purpose isolation: EWAY_BILL and E_INVOICE cannot hijack GST token without separate auth")
    void testGstPurposeIsolation() {
        setAuthContext(orgA.getId(), secUserA);

        gstAuthService.authenticate(GstAuthSessionRequest.builder()
                .connectionId(gstConnectionA.getId())
                .purpose(GstAuthenticationPurpose.GST)
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        // Active token for EWAY_BILL must be null until explicitly authenticated for EWAY_BILL
        GstAuthSessionDto ewayToken = gstAuthService.getActiveToken(gstConnectionA.getId(), GstAuthenticationPurpose.EWAY_BILL);
        assertThat(ewayToken).isNull();

        GstAuthSessionDto einvoiceToken = gstAuthService.getActiveToken(gstConnectionA.getId(), GstAuthenticationPurpose.E_INVOICE);
        assertThat(einvoiceToken).isNull();
    }

    // =========================================================================
    // 3. ITR EVC & DSC HARDENING (18 - 30)
    // =========================================================================

    @Test
    @DisplayName("18-24. ITR EVC complete lifecycle: start, verify, invalid, expired, replay, attempt limits")
    void testItrEvcLifecycle() {
        setAuthContext(orgA.getId(), secUserA);

        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .pan(itrProfileA.getPan())
                .profileId(itrProfileA.getId())
                .correlationId("corr-itr-evc-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        assertThat(challenge).isNotNull();
        assertThat(challenge.getChallengeReference()).isNotNull();

        // Invalid code returns unverified result
        GovEvcVerifyResultDto invalidRes = itrAuthService.verifyEvc(challenge.getSessionId(),
                ItrEvcVerifyRequest.builder().verificationCode("000000").actionReference("INVALID_EVC").options(Map.of("mockOutcome", "INVALID_EVC")).build());
        assertThat(invalidRes.isVerified()).isFalse();

        // Valid verification on a new challenge
        GovEvcChallengeDto validChallenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .pan(itrProfileA.getPan())
                .profileId(itrProfileA.getId())
                .correlationId("corr-itr-evc-valid")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        GovEvcVerifyResultDto result = itrAuthService.verifyEvc(validChallenge.getSessionId(),
                ItrEvcVerifyRequest.builder().verificationCode("123456").options(Map.of("mockOutcome", "SUCCESS")).build());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getVerificationReference()).isNotNull();

        // Replay attempt must fail (already verified session is terminal for challenge)
        assertThatThrownBy(() -> itrAuthService.verifyEvc(validChallenge.getSessionId(),
                ItrEvcVerifyRequest.builder().verificationCode("123456").build()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("25-30. ITR DSC signing and certificate verification")
    void testItrDscLifecycle() {
        setAuthContext(orgA.getId(), secUserA);

        GovDscSigningSessionDto dscSession = itrAuthService.startDsc(ItrDscSignRequest.builder()
                .connectionId(itrConnectionA.getId())
                .documentDigest("SHA256:abc123itrdigest")
                .correlationId("corr-itr-dsc-01")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        assertThat(dscSession).isNotNull();
        assertThat(dscSession.getDocumentDigest()).isEqualTo("SHA256:abc123itrdigest");

        GovDscVerifyResultDto verifyResult = itrAuthService.verifyDsc(dscSession.getSessionId(),
                ItrDscVerifyRequest.builder()
                        .signatureReference("MOCK_DSC_SIGNATURE_itr01")
                        .certificateReference("MOCK_DSC_CERT_itr01")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build());

        assertThat(verifyResult.isVerified()).isTrue();
        assertThat(verifyResult.getCertificateSubject()).isNotNull();
    }

    // =========================================================================
    // 4. TDS EVC & DSC HARDENING (31 - 42)
    // =========================================================================

    @Test
    @DisplayName("31-36. TDS EVC lifecycle: start, verify, failure, and attempt tracking")
    void testTdsEvcLifecycle() {
        setAuthContext(orgA.getId(), secUserA);

        GovEvcChallengeDto challenge = tdsAuthService.startEvc(TdsEvcStartRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .tan(tdsProfileA.getTan())
                .profileId(tdsProfileA.getId())
                .correlationId("corr-tds-evc-01")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        assertThat(challenge).isNotNull();

        GovEvcVerifyResultDto verifyResult = tdsAuthService.verifyEvc(challenge.getSessionId(),
                TdsEvcVerifyRequest.builder().verificationCode("123456").options(Map.of("mockOutcome", "SUCCESS")).build());

        assertThat(verifyResult.isVerified()).isTrue();
    }

    @Test
    @DisplayName("37-42. TDS DSC signing and certificate verification")
    void testTdsDscLifecycle() {
        setAuthContext(orgA.getId(), secUserA);

        GovDscSigningSessionDto dscSession = tdsAuthService.startDsc(TdsDscSignRequest.builder()
                .connectionId(tdsConnectionA.getId())
                .tan(tdsProfileA.getTan())
                .profileId(tdsProfileA.getId())
                .documentDigest("SHA256:tds987digest")
                .correlationId("corr-tds-dsc-01")
                .options(Map.of("mockOutcome", "SIGNING_PENDING"))
                .build());

        assertThat(dscSession).isNotNull();

        GovDscVerifyResultDto verifyResult = tdsAuthService.verifyDsc(dscSession.getSessionId(),
                TdsDscVerifyRequest.builder()
                        .signatureReference("MOCK_DSC_SIGNATURE_tds01")
                        .certificateReference("MOCK_DSC_CERT_tds01")
                        .options(Map.of("mockOutcome", "SUCCESS"))
                        .build());

        assertThat(verifyResult.isVerified()).isTrue();
    }

    // =========================================================================
    // 5. CONSENT & DELEGATION HARDENING (43 - 50)
    // =========================================================================

    @Test
    @DisplayName("43-46. Consent active, missing, expired, and revoked states")
    void testConsentStates() {
        setAuthContext(orgA.getId(), secUserA);

        ConsentDto consent = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .consentMethod(ConsentMethod.IN_APP)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build());

        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.ACTIVE);

        // Active consent evaluates to authorized
        ConsentAuthorizationResult res1 = consentAuthorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .requiredScope(ConsentScope.GST_RETURN)
                .build());
        assertThat(res1.isAuthorized()).isTrue();

        // Revoke consent
        consentService.revokeConsent(consent.getId(), RevokeConsentRequest.builder().reason("Client request").build());

        ConsentAuthorizationResult resRevoked = consentAuthorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .requiredScope(ConsentScope.GST_RETURN)
                .build());
        assertThat(resRevoked.isAuthorized()).isFalse();
        assertThat(resRevoked.getReasonCode()).isEqualTo("REVOKED");
    }

    @Test
    @DisplayName("47-48. Scope isolation: cross-domain and wrong-scope denial")
    void testScopeIsolation() {
        setAuthContext(orgA.getId(), secUserA);

        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .consentMethod(ConsentMethod.IN_APP)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build());

        // ITR_RETURN cannot authorize ITR_DSC
        ConsentAuthorizationResult dscRes = consentAuthorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .requiredScope(ConsentScope.ITR_DSC)
                .build());
        assertThat(dscRes.isAuthorized()).isFalse();

        // ITR_RETURN cannot authorize TDS_RETURN
        ConsentAuthorizationResult tdsRes = consentAuthorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .requiredScope(ConsentScope.TDS_RETURN)
                .build());
        assertThat(tdsRes.isAuthorized()).isFalse();
    }

    @Test
    @DisplayName("49-50. ALL scope requires explicit grant; child scopes do not imply ALL")
    void testAllScopeRules() {
        // 1. ALL explicitly granted satisfies child scopes
        assertThat(ConsentScope.ALL.satisfies(ConsentScope.GST_RETURN)).isTrue();
        assertThat(ConsentScope.ALL.satisfies(ConsentScope.ITR_DSC)).isTrue();
        assertThat(ConsentScope.ALL.satisfies(ConsentScope.TDS_EVC)).isTrue();

        // 2. Child scope does not imply ALL
        assertThat(ConsentScope.GST_RETURN.satisfies(ConsentScope.ALL)).isFalse();
        assertThat(ConsentScope.ITR.satisfies(ConsentScope.ALL)).isFalse();
        assertThat(ConsentScope.TDS.satisfies(ConsentScope.ALL)).isFalse();
    }

    // =========================================================================
    // 6. COMPOSITE AUTHORIZATION & CORE INVARIANTS (51 - 58)
    // =========================================================================

    @Test
    @DisplayName("51. Full composite authorization pipeline: RBAC + Client + Delegate + Consent + Gov Auth = SUCCESS")
    void testFullCompositeAuthorizationSuccess() {
        setAuthContext(orgA.getId(), secUserA);

        // 1. Create active consent for GST_RETURN
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .consentMethod(ConsentMethod.IN_APP)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build());

        // 2. Full gate evaluation with active gov session
        ConsentAuthorizationResult result = govOpAuthService.authorizeGovernmentOperation(
                clientA.getId(), userA.getId(), ConsentScope.GST_RETURN, true);

        assertThat(result.isAuthorized()).isTrue();
    }

    @Test
    @DisplayName("52-58. Composite authorization failure modes & Core Invariants 1-12")
    void testCompositeAuthorizationFailures() {
        setAuthContext(orgA.getId(), secUserA);

        // Invariant 1 & 3: RBAC/Auth without Consent -> DENIED
        ConsentAuthorizationResult resNoConsent = govOpAuthService.authorizeGovernmentOperation(
                clientA.getId(), userA.getId(), ConsentScope.GST_RETURN, true);
        assertThat(resNoConsent.isAuthorized()).isFalse();
        assertThat(resNoConsent.getReasonCode()).isEqualTo("NO_ACTIVE_DELEGATION");

        // Invariant 2: Consent without Government Auth -> DENIED
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .consentMethod(ConsentMethod.IN_APP)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build());

        ConsentAuthorizationResult resNoGovAuth = govOpAuthService.authorizeGovernmentOperation(
                clientA.getId(), userA.getId(), ConsentScope.GST_RETURN, false);
        assertThat(resNoGovAuth.isAuthorized()).isFalse();
        assertThat(resNoGovAuth.getReasonCode()).isEqualTo("AUTHENTICATION_REQUIRED");

        // Invariant 11: Wrong client access -> DENIED
        ConsentAuthorizationResult resWrongClient = govOpAuthService.authorizeGovernmentOperation(
                clientB.getId(), userA.getId(), ConsentScope.GST_RETURN, true);
        assertThat(resWrongClient.isAuthorized()).isFalse();
        assertThat(resWrongClient.getReasonCode()).isEqualTo("CLIENT_NOT_FOUND");
    }

    // =========================================================================
    // 7. MULTI-TENANT ISOLATION (59 - 63)
    // =========================================================================

    @Test
    @DisplayName("59-63. Strict cross-tenant isolation on sessions, consents, clients, connections, delegates")
    void testCrossTenantIsolation() {
        // Tenant A creates consent and session
        setAuthContext(orgA.getId(), secUserA);

        ConsentDto consentA = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .consentMethod(ConsentMethod.IN_APP)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build());

        GovAuthSessionDto sessionA = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(gstConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        // Switch to Tenant B
        setAuthContext(orgB.getId(), secUserB);

        // 1. Cross-tenant consent retrieval -> 404
        assertThatThrownBy(() -> consentService.getConsentById(consentA.getId()))
                .isInstanceOf(AppException.class);

        // 2. Cross-tenant auth session retrieval -> 404
        assertThatThrownBy(() -> govAuthService.getAuthenticationStatus(sessionA.getSessionId()))
                .isInstanceOf(AppException.class);

        // 3. Cross-tenant connection access -> 404
        assertThatThrownBy(() -> connectionService.getConnection(gstConnectionA.getId()))
                .isInstanceOf(RuntimeException.class);

        // 4. Cross-tenant consent creation using Client A under Tenant B -> Fails
        assertThatThrownBy(() -> consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(userB.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .consentMethod(ConsentMethod.IN_APP)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build()))
                .isInstanceOf(AppException.class);
    }

    // =========================================================================
    // 8. SECRET LEAKAGE PREVENTION & AUDIT SAFETY (64 - 70)
    // =========================================================================

    @Test
    @DisplayName("64-70. Zero secret leakage across entities, DTOs, audit trails, and exception messages")
    void testZeroSecretLeakage() {
        setAuthContext(orgA.getId(), secUserA);

        GovEvcChallengeDto challenge = itrAuthService.startEvc(ItrEvcStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .pan(itrProfileA.getPan())
                .profileId(itrProfileA.getId())
                .correlationId("corr-itr-evc-sec")
                .options(Map.of("mockOutcome", "CHALLENGE_CREATED"))
                .build());

        // 1. DTO must not expose generated OTP/EVC code in challenge reference
        assertThat(challenge.getChallengeReference()).doesNotContain("123456");

        // 2. Audit logs must never contain raw credentials or OTPs
        ArgumentCaptor<Map<String, Object>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()), any(), any(), any(), any(), any(), auditCaptor.capture());

        for (Map<String, Object> payload : auditCaptor.getAllValues()) {
            if (payload != null) {
                assertThat(payload.toString())
                        .doesNotContain("SECRET_ITR_PASS")
                        .doesNotContain("SECRET_TDS_PASS")
                        .doesNotContain("GSTN_AUTH_SECRET_A")
                        .doesNotContain("123456");
            }
        }
    }

    // =========================================================================
    // 9. INVARIANT 12: FILED IMMUTABILITY
    // =========================================================================

    @Test
    @DisplayName("Invariant 12: Authentication success does not modify ITR or TDS profile active state")
    void testAuthenticationDoesNotMutateFilingStatus() {
        setAuthContext(orgA.getId(), secUserA);

        assertThat(itrProfileA.isActive()).isTrue();

        govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(itrConnectionA.getId())
                .authMethod(GovAuthMethod.OAUTH2)
                .build());

        ItrProfileEntity refreshedProfile = itrProfileRepository.findById(itrProfileA.getId()).orElseThrow();
        assertThat(refreshedProfile.isActive()).isTrue();
    }

    private void setAuthContext(UUID organizationId, SecurityUser user) {
        TenantContext.setTenantId(organizationId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
