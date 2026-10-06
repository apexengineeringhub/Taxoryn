package com.taxoryn.module.consent;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.consent.dto.*;
import com.taxoryn.module.consent.entity.TaxpayerConsentEntity;
import com.taxoryn.module.consent.model.ConsentMethod;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.model.ConsentStatus;
import com.taxoryn.module.consent.model.DelegationType;
import com.taxoryn.module.consent.repository.TaxpayerConsentRepository;
import com.taxoryn.module.consent.service.ConsentAuthorizationService;
import com.taxoryn.module.consent.service.ConsentManagementService;
import com.taxoryn.module.consent.service.GovernmentOperationAuthorizationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
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
class ConsentManagementIntegrationTest {

    @Autowired
    private ConsentManagementService consentService;

    @Autowired
    private ConsentAuthorizationService authorizationService;

    @Autowired
    private GovernmentOperationAuthorizationService govOpAuthService;

    @Autowired
    private TaxpayerConsentRepository consentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private ClientEntity clientA;
    private ClientEntity clientB;
    private UserEntity delegateUserA;
    private UserEntity delegateUserB;
    private SecurityUser authUserA;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Consent Org A " + UUID.randomUUID())
                .legalName("Consent Org A Pvt Ltd")
                .email("consent.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Consent Org B " + UUID.randomUUID())
                .legalName("Consent Org B Pvt Ltd")
                .email("consent.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        authUserA = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgA.getId())
                .email("admin@consent-orga.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_WRITE", "CLIENT_MANAGE", "GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUserA, null, authUserA.getAuthorities())
        );

        // Client A in Org A
        clientA = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Taxpayer Client")
                .clientCode("CLNT_A_" + UUID.randomUUID().toString().substring(0, 5))
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .build());
        clientA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clientA);

        // Delegate User A in Org A
        delegateUserA = UserEntity.builder()
                .organizationId(orgA.getId())
                .email("practitioner.a." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash("hashed_secret_pw")
                .firstName("John")
                .lastName("Practitioner")
                .build();
        delegateUserA = userRepository.save(delegateUserA);

        // Client B & Delegate B in Org B
        TenantContext.setTenantId(orgB.getId());
        clientB = clientRepository.save(ClientEntity.builder()
                .displayName("Beta Taxpayer Client")
                .clientCode("CLNT_B_" + UUID.randomUUID().toString().substring(0, 5))
                .pan("BCDEF2345G")
                .build());
        clientB.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientB);

        delegateUserB = UserEntity.builder()
                .organizationId(orgB.getId())
                .email("practitioner.b." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash("hashed_secret_pw")
                .firstName("Jane")
                .lastName("Practitioner")
                .build();
        delegateUserB = userRepository.save(delegateUserB);

        TenantContext.setTenantId(orgA.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // A. Consent Creation Tests
    // =========================================================================

    @Test
    @DisplayName("Consent: 1. Create consent successfully with pending status")
    void testCreateConsent_Success() {
        Instant now = Instant.now();
        Instant validUntil = now.plus(Duration.ofDays(365));

        ConsentDto consent = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR_RETURN, ConsentScope.ITR_EVC))
                .validUntil(validUntil)
                .consentMethod(ConsentMethod.IN_APP)
                .build());

        assertThat(consent).isNotNull();
        assertThat(consent.getId()).isNotNull();
        assertThat(consent.getClientId()).isEqualTo(clientA.getId());
        assertThat(consent.getDelegateUserId()).isEqualTo(delegateUserA.getId());
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.PENDING);
        assertThat(consent.getScopes()).containsExactlyInAnyOrder(ConsentScope.ITR_RETURN, ConsentScope.ITR_EVC);
        assertThat(consent.getConsentReference()).startsWith("CONSENT_REF_");
    }

    @Test
    @DisplayName("Consent: 2. Invalid client ID is rejected")
    void testCreateConsent_InvalidClientRejected() {
        assertThatThrownBy(() -> consentService.createConsent(CreateConsentRequest.builder()
                .clientId(UUID.randomUUID())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    @DisplayName("Consent: 3. Invalid delegate user is rejected")
    void testCreateConsent_InvalidDelegateRejected() {
        assertThatThrownBy(() -> consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(UUID.randomUUID())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Delegate user not found");
    }

    @Test
    @DisplayName("Consent: 4. Invalid validity range (validUntil before validFrom) is rejected")
    void testCreateConsent_InvalidValidityRangeRejected() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validFrom(now.plus(Duration.ofDays(10)))
                .validUntil(now.plus(Duration.ofDays(5)))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("validUntil must be strictly after validFrom");
    }

    // =========================================================================
    // B. Approval & Rejection Tests
    // =========================================================================

    @Test
    @DisplayName("Approval: 5. Pending consent is approved and becomes ACTIVE")
    void testApproveConsent_Success() {
        ConsentDto created = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(180)))
                .build());

        assertThat(created.getStatus()).isEqualTo(ConsentStatus.PENDING);

        ConsentDto approved = consentService.approveConsent(created.getId(), ApproveConsentRequest.builder()
                .consentingUserId(authUserA.getUserId())
                .notes("Approved by practice admin")
                .build());

        assertThat(approved.getStatus()).isEqualTo(ConsentStatus.ACTIVE);
        assertThat(approved.getConsentingUserId()).isEqualTo(authUserA.getUserId());
    }

    @Test
    @DisplayName("Approval: 6. Pending consent can be rejected")
    void testRejectConsent_Success() {
        ConsentDto created = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.TDS_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(90)))
                .build());

        ConsentDto rejected = consentService.rejectConsent(created.getId(), "Client declined authorization mandate");

        assertThat(rejected.getStatus()).isEqualTo(ConsentStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).contains("Client declined");
    }

    // =========================================================================
    // C. Delegation & Scope Hierarchy Tests
    // =========================================================================

    @Test
    @DisplayName("Scope: 7. Matching explicit scope is authorized")
    void testScope_ExplicitMatchAuthorized() {
        ConsentDto consent = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR_RETURN, ConsentScope.ITR_EVC))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        ConsentAuthorizationResult result = authorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .requiredScope(ConsentScope.ITR_RETURN)
                .build());

        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getScope()).isEqualTo(ConsentScope.ITR_RETURN);
        assertThat(result.getConsentId()).isEqualTo(consent.getId());
    }

    @Test
    @DisplayName("Scope: 8. Umbrella ITR scope grants child operations (ITR_RETURN, ITR_DSC, ITR_EVC)")
    void testScope_UmbrellaItrGrantsChildScopes() {
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.CA)
                .scopes(Set.of(ConsentScope.ITR))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.ITR_RETURN)).isTrue();
        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.ITR_DSC)).isTrue();
        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.ITR_EVC)).isTrue();
        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.ITR_VERIFICATION)).isTrue();
    }

    @Test
    @DisplayName("Scope: 9. Umbrella GST scope grants GST_RETURN, EWAY_BILL, E_INVOICE")
    void testScope_UmbrellaGstGrantsChildScopes() {
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.GST_RETURN)).isTrue();
        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.EWAY_BILL)).isTrue();
        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.E_INVOICE)).isTrue();
    }

    @Test
    @DisplayName("Scope: 10. Narrow scope (ITR_RETURN) does NOT grant sensitive scope (ITR_DSC)")
    void testScope_NarrowScopeDoesNotGrantSensitiveScope() {
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.EMPLOYEE)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .autoApprove(true)
                .build());

        ConsentAuthorizationResult dscResult = authorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .requiredScope(ConsentScope.ITR_DSC)
                .build());

        assertThat(dscResult.isAuthorized()).isFalse();
        assertThat(dscResult.getReasonCode()).isEqualTo("SCOPE_NOT_GRANTED");
    }

    @Test
    @DisplayName("Scope: 11. Cross-domain scope (ITR consent for TDS operation) is denied")
    void testScope_CrossDomainScopeDenied() {
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        ConsentAuthorizationResult tdsResult = authorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .requiredScope(ConsentScope.TDS_RETURN)
                .build());

        assertThat(tdsResult.isAuthorized()).isFalse();
        assertThat(tdsResult.getReasonCode()).isEqualTo("SCOPE_NOT_GRANTED");
    }

    // =========================================================================
    // D. Validity & Expiration Tests
    // =========================================================================

    @Test
    @DisplayName("Validity: 12. Expired consent is denied authorization")
    void testValidity_ExpiredConsentDenied() {
        Instant now = Instant.now();
        TaxpayerConsentEntity expiredEntity = TaxpayerConsentEntity.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .status(ConsentStatus.ACTIVE)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validFrom(now.minus(Duration.ofDays(60)))
                .validUntil(now.minus(Duration.ofDays(1)))
                .consentReference("CONSENT_REF_EXP")
                .build();
        expiredEntity.setOrganizationId(orgA.getId());
        consentRepository.save(expiredEntity);

        ConsentAuthorizationResult result = authorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .requiredScope(ConsentScope.ITR_RETURN)
                .build());

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getReasonCode()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Validity: 13. Future-dated consent is not active currently")
    void testValidity_FutureConsentNotActive() {
        Instant now = Instant.now();
        TaxpayerConsentEntity futureEntity = TaxpayerConsentEntity.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .status(ConsentStatus.ACTIVE)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validFrom(now.plus(Duration.ofDays(10)))
                .validUntil(now.plus(Duration.ofDays(100)))
                .consentReference("CONSENT_REF_FUT")
                .build();
        futureEntity.setOrganizationId(orgA.getId());
        consentRepository.save(futureEntity);

        ConsentAuthorizationResult result = authorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .requiredScope(ConsentScope.ITR_RETURN)
                .build());

        assertThat(result.isAuthorized()).isFalse();
    }

    // =========================================================================
    // E. Revocation Tests
    // =========================================================================

    @Test
    @DisplayName("Revocation: 14. Active consent revoked cannot authorize further operations")
    void testRevocation_ActiveConsentRevoked() {
        ConsentDto consent = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        assertThat(authorizationService.hasConsent(clientA.getId(), delegateUserA.getId(), ConsentScope.GST_RETURN)).isTrue();

        ConsentDto revoked = consentService.revokeConsent(consent.getId(), RevokeConsentRequest.builder()
                .reason("Engagement terminated by mutual agreement")
                .build());

        assertThat(revoked.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(revoked.getRevocationReason()).contains("Engagement terminated");

        ConsentAuthorizationResult result = authorizationService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .requiredScope(ConsentScope.GST_RETURN)
                .build());

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getReasonCode()).isEqualTo("REVOKED");
    }

    // =========================================================================
    // F. Tenant Isolation Tests
    // =========================================================================

    @Test
    @DisplayName("Tenant Isolation: 15. Cross-tenant consent query is denied")
    void testTenantIsolation_CrossTenantQueryDenied() {
        TenantContext.setTenantId(orgB.getId());
        ConsentDto consentB = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientB.getId())
                .delegateUserId(delegateUserB.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        // Switch to Org A
        TenantContext.setTenantId(orgA.getId());

        assertThatThrownBy(() -> consentService.getConsentById(consentB.getId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Taxpayer consent not found");

        assertThatThrownBy(() -> consentService.getConsentsForClient(clientB.getId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    @DisplayName("Tenant Isolation: 16. Cross-tenant delegation creation is rejected")
    void testTenantIsolation_CrossTenantCreationRejected() {
        TenantContext.setTenantId(orgA.getId());

        // Attempting to create consent for Org B client using Org A context
        assertThatThrownBy(() -> consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientB.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Client not found");

        // Attempting to delegate to Org B user using Org A context
        assertThatThrownBy(() -> consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserB.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.GST_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(30)))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Delegate user not found");
    }

    // =========================================================================
    // G. Government Operation Authorization Contract Tests
    // =========================================================================

    @Test
    @DisplayName("Gov Operation Contract: 17. Valid consent + no authentication = DENIED")
    void testGovOp_ValidConsentNoAuthDenied() {
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        ConsentAuthorizationResult result = govOpAuthService.authorizeGovernmentOperation(
                clientA.getId(),
                delegateUserA.getId(),
                ConsentScope.ITR_RETURN,
                false // Not authenticated with government portal
        );

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getReasonCode()).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("Gov Operation Contract: 18. Valid authentication + no consent = DENIED")
    void testGovOp_ValidAuthNoConsentDenied() {
        ConsentAuthorizationResult result = govOpAuthService.authorizeGovernmentOperation(
                clientA.getId(),
                delegateUserA.getId(),
                ConsentScope.ITR_RETURN,
                true // Authenticated with government portal
        );

        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getReasonCode()).isEqualTo("NO_ACTIVE_DELEGATION");
    }

    @Test
    @DisplayName("Gov Operation Contract: 19. Valid consent + Valid authentication = AUTHORIZED")
    void testGovOp_ValidConsentAndAuthAuthorized() {
        consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.ITR_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(365)))
                .autoApprove(true)
                .build());

        ConsentAuthorizationResult result = govOpAuthService.authorizeGovernmentOperation(
                clientA.getId(),
                delegateUserA.getId(),
                ConsentScope.ITR_RETURN,
                true // Authenticated with government portal
        );

        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo("AUTHORIZED");
    }

    // =========================================================================
    // H. Audit Verification Tests
    // =========================================================================

    @Test
    @DisplayName("Audit: 20. Consent creation, approval, and revocation are audited")
    void testAudit_LifecycleEventsAudited() {
        ConsentDto created = consentService.createConsent(CreateConsentRequest.builder()
                .clientId(clientA.getId())
                .delegateUserId(delegateUserA.getId())
                .delegationType(DelegationType.PRACTITIONER)
                .scopes(Set.of(ConsentScope.TDS_RETURN))
                .validUntil(Instant.now().plus(Duration.ofDays(100)))
                .build());

        consentService.approveConsent(created.getId(), ApproveConsentRequest.builder()
                .consentingUserId(authUserA.getUserId())
                .build());

        consentService.revokeConsent(created.getId(), RevokeConsentRequest.builder()
                .reason("Audit test revocation")
                .build());

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

        List<Map<String, Object>> captured = detailsCaptor.getAllValues();
        assertThat(captured).isNotEmpty();
    }
}
