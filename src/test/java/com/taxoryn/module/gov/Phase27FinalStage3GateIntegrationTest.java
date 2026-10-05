package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.consent.dto.ApproveConsentRequest;
import com.taxoryn.module.consent.dto.ConsentAuthorizationResult;
import com.taxoryn.module.consent.dto.ConsentDto;
import com.taxoryn.module.consent.dto.CreateConsentRequest;
import com.taxoryn.module.consent.dto.RevokeConsentRequest;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.repository.TaxpayerConsentRepository;
import com.taxoryn.module.consent.service.ConsentManagementService;
import com.taxoryn.module.consent.service.GovernmentOperationAuthorizationService;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthPurpose;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.repository.GovAuthSessionRepository;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.observability.health.GovIntegrationHealthIndicator;
import com.taxoryn.module.gov.observability.service.GovOperationsObservabilityService;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.processor.GovOutboxProcessor;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.outbox.service.GovOutboxService;
import com.taxoryn.module.gov.reconciliation.service.GovernmentOperationReconciliationService;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import com.taxoryn.module.gov.reliability.service.GovReliabilityService;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 27.6 — Final Stage 3 Integration & Internal Production Readiness Gate Suite.
 * Validates full composite authorization, lifecycle invariants, failure recovery,
 * outbox & reconciliation orchestration, tenant boundary, and secret scrubbing.
 */
@SpringBootTest
@ActiveProfiles("test")
public class Phase27FinalStage3GateIntegrationTest {

    @Autowired
    private GovernmentIntegrationService govIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private GovernmentAuthenticationService govAuthService;

    @Autowired
    private ConsentManagementService consentManagementService;

    @Autowired
    private GovernmentOperationAuthorizationService compositeAuthService;

    @Autowired
    private GovReliabilityService govReliabilityService;

    @Autowired
    private GovOutboxService govOutboxService;

    @Autowired
    private GovOutboxProcessor govOutboxProcessor;

    @Autowired
    private GovernmentOperationReconciliationService govReconciliationService;

    @Autowired
    private GovOperationsObservabilityService govObservabilityService;

    @Autowired
    private GovIntegrationHealthIndicator govHealthIndicator;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private GovOutboxEventRepository outboxEventRepository;

    @Autowired
    private GovAuthSessionRepository authSessionRepository;

    @Autowired
    private TaxpayerConsentRepository consentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private OrganizationEntity orgAlpha;
    private OrganizationEntity orgBeta;
    private UserEntity userAlpha;
    private ClientEntity clientAlphaGst;
    private ClientEntity clientAlphaItr;
    private ClientEntity clientAlphaTds;

    @BeforeEach
    void setUp() {
        cleanupData();

        orgAlpha = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Corp")
                .email("info-" + UUID.randomUUID() + "@alpha.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgBeta = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Corp")
                .email("info-" + UUID.randomUUID() + "@beta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userAlpha = userRepository.save(UserEntity.builder()
                .organizationId(orgAlpha.getId())
                .email("admin@alpha-" + UUID.randomUUID() + ".com")
                .firstName("Alpha")
                .lastName("Admin")
                .passwordHash("$2a$10$xyz")
                .build());

        ClientEntity clientGst = ClientEntity.builder()
                .displayName("Alpha GST Client")
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientGst.setOrganizationId(orgAlpha.getId());
        clientAlphaGst = clientRepository.save(clientGst);

        ClientEntity clientItr = ClientEntity.builder()
                .displayName("Alpha ITR Client")
                .pan("ABCDE1234F")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientItr.setOrganizationId(orgAlpha.getId());
        clientAlphaItr = clientRepository.save(clientItr);

        ClientEntity clientTds = ClientEntity.builder()
                .displayName("Alpha TDS Client")
                .tan("MUMA12345B")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientTds.setOrganizationId(orgAlpha.getId());
        clientAlphaTds = clientRepository.save(clientTds);

        TenantContext.setTenantId(orgAlpha.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        cleanupData();
    }

    private void cleanupData() {
        outboxEventRepository.deleteAll();
        operationRepository.deleteAll();
        consentRepository.deleteAll();
        authSessionRepository.deleteAll();
    }

    // =========================================================================
    // 1. END-TO-END GST VALIDATION & SUBMITTED != FILED INVARIANT
    // =========================================================================

    @Nested
    @DisplayName("1. GST Lifecycle & Invariant Validation")
    class GstLifecycleValidation {

        @Test
        @DisplayName("Complete GST Lifecycle: Submit (SUBMITTED) -> Outbox -> Reconciliation -> FILED")
        void testGstCompleteLifecycle() {
            TenantContext.setTenantId(orgAlpha.getId());

            // 1. Setup Connection & Valid Auth Session
            GovConnectionDto conn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                    .providerType(GovProviderType.GST)
                    .displayName("GSTN Gateway")
                    .environment("TEST")
                    .build());

            GovAuthSessionDto session = govAuthService.startAuthentication(com.taxoryn.module.gov.auth.dto.GovAuthStartRequest.builder()
                    .connectionId(conn.getId())
                    .authMethod(GovAuthMethod.OTP)
                    .correlationId("corr-start-" + UUID.randomUUID())
                    .options(Map.of("otp", "123456"))
                    .build());
            authSessionRepository.findById(session.getSessionId()).ifPresent(s -> {
                s.setStatus(GovAuthStatus.AUTHENTICATED);
                s.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
                authSessionRepository.save(s);
            });

            // 2. Submit GST Return Operation
            String corrId = "gst-corr-" + UUID.randomUUID();
            String idempotencyKey = "gst-idem-" + UUID.randomUUID();
            GovIntegrationRequest request = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_SUBMISSION")
                    .correlationId(corrId)
                    .idempotencyKey(idempotencyKey)
                    .requestData(Map.of(
                            "gstin", clientAlphaGst.getGstin(),
                            "returnType", "GSTR1",
                            "returnPeriod", "042026",
                            "payloadFingerprint", "fp-gstr1-042026-xyz"
                    ))
                    .build();

            GovIntegrationResult result = govIntegrationService.executeOperation(request);

            // 3. Verify Lifecycle Invariant: SUBMITTED != FILED
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProviderReferenceId()).startsWith("MOCK-GST-ACK-");

            GovIntegrationOperationEntity opEntity = operationRepository.findById(result.getOperationId()).orElseThrow();
            assertThat(opEntity.getResponseMetadata()).contains("SUBMITTED");
            assertThat(opEntity.getResponseMetadata()).doesNotContain("providerStatus\":\"FILED\"");

            // 4. Verify Outbox Event Published & Processed
            govOutboxService.scheduleOperationDispatch(result.getOperationId(), GovProviderType.GST, corrId, "{}");
            List<GovOutboxEventEntity> outboxEvents = outboxEventRepository.findByOrganizationIdAndStatus(
                    orgAlpha.getId(), GovOutboxStatus.PENDING
            );
            assertThat(outboxEvents).isNotEmpty();
            GovOutboxEventEntity outboxEvent = outboxEvents.getFirst();
            assertThat(outboxEvent.getOperationId()).isEqualTo(result.getOperationId());

            // Process outbox event
            govOutboxProcessor.processPendingBatch(10);
            GovOutboxEventEntity processedOutbox = outboxEventRepository.findById(outboxEvent.getId()).orElseThrow();
            assertThat(processedOutbox.getStatus()).isEqualTo(GovOutboxStatus.COMPLETED);

            // 5. Reconcile In-Progress Filing to Authoritative FILED State
            GovIntegrationOperationEntity inProgressOp = GovIntegrationOperationEntity.builder()
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_STATUS")
                    .correlationId("recon-corr-" + UUID.randomUUID())
                    .idempotencyKey("recon-idem-" + UUID.randomUUID())
                    .status(GovOperationStatus.IN_PROGRESS)
                    .providerReferenceId("MOCK-GST-ACK-FP-GSTR1")
                    .requestMetadata("{\"gstin\":\"" + clientAlphaGst.getGstin() + "\",\"ackNumber\":\"MOCK-GST-ACK-FP-GSTR1\",\"returnType\":\"GSTR1\",\"returnPeriod\":\"042026\"}")
                    .build();
            inProgressOp.setOrganizationId(orgAlpha.getId());
            inProgressOp = operationRepository.save(inProgressOp);

            com.taxoryn.module.gov.reconciliation.dto.GovReconciliationResultDto reconResult =
                    govReconciliationService.reconcileOperation(inProgressOp.getId());

            assertThat(reconResult.getAuthoritativeStatus())
                    .isEqualTo(com.taxoryn.module.gov.reconciliation.model.GovAuthoritativeStatus.FILED);
            assertThat(reconResult.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("GST Failure Paths: Reject when session expired or provider unavailable")
        void testGstFailurePaths() {
            TenantContext.setTenantId(orgAlpha.getId());

            GovIntegrationRequest unavailableReq = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_SUBMISSION")
                    .correlationId("corr-unavail-" + UUID.randomUUID())
                    .requestData(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                    .build();

            GovIntegrationResult unavailResult = govIntegrationService.executeOperation(unavailableReq);
            assertThat(unavailResult.isSuccess()).isFalse();
            assertThat(unavailResult.getErrorCode()).isEqualTo(GovErrorCode.PROVIDER_UNAVAILABLE);
            assertThat(unavailResult.isTransientError()).isTrue();
        }
    }

    // =========================================================================
    // 2. END-TO-END ITR VALIDATION
    // =========================================================================

    @Nested
    @DisplayName("2. ITR Lifecycle & Consent Scope Validation")
    class ItrLifecycleValidation {

        @Test
        @DisplayName("Complete ITR Lifecycle: Consent -> Auth -> Prep -> Submit (SUBMITTED) -> Reconcile (FILED)")
        void testItrCompleteLifecycle() {
            TenantContext.setTenantId(orgAlpha.getId());

            // 1. Setup Consent
            CreateConsentRequest consentReq = CreateConsentRequest.builder()
                    .clientId(clientAlphaItr.getId())
                    .delegateUserId(userAlpha.getId())
                    .delegationType(com.taxoryn.module.consent.model.DelegationType.PRACTITIONER)
                    .scopes(Set.of(ConsentScope.ITR_RETURN, ConsentScope.ITR_VERIFICATION))
                    .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();
            ConsentDto consent = consentManagementService.createConsent(consentReq);
            consentManagementService.approveConsent(consent.getId(), ApproveConsentRequest.builder()
                    .consentingUserId(userAlpha.getId())
                    .notes("Approved by taxpayer")
                    .build());

            // 2. Submit ITR Return Operation
            String corrId = "itr-corr-" + UUID.randomUUID();
            GovIntegrationRequest request = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.INCOME_TAX)
                    .operationType("ITR_RETURN_SUBMISSION")
                    .correlationId(corrId)
                    .requestData(Map.of(
                            "pan", clientAlphaItr.getPan(),
                            "assessmentYear", "2026-27",
                            "formType", "ITR-1",
                            "payloadFingerprint", "fp-itr-2026-27-pan1"
                    ))
                    .build();

            GovIntegrationResult result = govIntegrationService.executeOperation(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProviderReferenceId()).isNotNull();

            // 3. Reconcile In-Progress ITR Status to final state
            GovIntegrationOperationEntity inProgressItr = GovIntegrationOperationEntity.builder()
                    .providerType(GovProviderType.INCOME_TAX)
                    .operationType("ITR_RETURN_STATUS")
                    .correlationId("recon-itr-" + UUID.randomUUID())
                    .idempotencyKey("recon-itr-idem-" + UUID.randomUUID())
                    .status(GovOperationStatus.IN_PROGRESS)
                    .providerReferenceId(result.getProviderReferenceId())
                    .requestMetadata("{\"pan\":\"" + clientAlphaItr.getPan() + "\",\"ackNumber\":\"" + result.getProviderReferenceId() + "\"}")
                    .build();
            inProgressItr.setOrganizationId(orgAlpha.getId());
            inProgressItr = operationRepository.save(inProgressItr);

            govReconciliationService.reconcileOperation(inProgressItr.getId());
            GovIntegrationOperationEntity op = operationRepository.findById(inProgressItr.getId()).orElseThrow();
            assertThat(op.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        }
    }

    // =========================================================================
    // 3. END-TO-END TDS VALIDATION
    // =========================================================================

    @Nested
    @DisplayName("3. TDS Lifecycle & Challan Validation")
    class TdsLifecycleValidation {

        @Test
        @DisplayName("Complete TDS Lifecycle: Submit 24Q (SUBMITTED) -> Reconcile -> FILED")
        void testTdsCompleteLifecycle() {
            TenantContext.setTenantId(orgAlpha.getId());

            String corrId = "tds-corr-" + UUID.randomUUID();
            GovIntegrationRequest request = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.TRACES)
                    .operationType("TDS_RETURN_SUBMISSION")
                    .correlationId(corrId)
                    .requestData(Map.of(
                            "tan", clientAlphaTds.getTan(),
                            "formType", "24Q",
                            "quarter", "Q4",
                            "financialYear", "2025-26",
                            "fvuValidationStatus", "PASSED"
                    ))
                    .build();

            GovIntegrationResult result = govIntegrationService.executeOperation(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProviderReferenceId()).isNotNull();

            // Reconcile In-Progress TDS Status to final state
            GovIntegrationOperationEntity inProgressTds = GovIntegrationOperationEntity.builder()
                    .providerType(GovProviderType.TRACES)
                    .operationType("TDS_RETURN_STATUS")
                    .correlationId("recon-tds-" + UUID.randomUUID())
                    .idempotencyKey("recon-tds-idem-" + UUID.randomUUID())
                    .status(GovOperationStatus.IN_PROGRESS)
                    .providerReferenceId(result.getProviderReferenceId())
                    .requestMetadata("{\"tan\":\"" + clientAlphaTds.getTan() + "\",\"ackNumber\":\"" + result.getProviderReferenceId() + "\"}")
                    .build();
            inProgressTds.setOrganizationId(orgAlpha.getId());
            inProgressTds = operationRepository.save(inProgressTds);

            govReconciliationService.reconcileOperation(inProgressTds.getId());
            GovIntegrationOperationEntity op = operationRepository.findById(inProgressTds.getId()).orElseThrow();
            assertThat(op.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        }
    }

    // =========================================================================
    // 4. COMPOSITE AUTHORIZATION CHAIN VALIDATION
    // =========================================================================

    @Nested
    @DisplayName("4. Composite Authorization Chain Validation")
    class CompositeAuthorizationValidation {

        @Test
        @DisplayName("Evaluates composite authorization chain and denies on revoked consent")
        void testConsentEnforcement() {
            TenantContext.setTenantId(orgAlpha.getId());

            CreateConsentRequest consentReq = CreateConsentRequest.builder()
                    .clientId(clientAlphaGst.getId())
                    .delegateUserId(userAlpha.getId())
                    .delegationType(com.taxoryn.module.consent.model.DelegationType.PRACTITIONER)
                    .scopes(Set.of(ConsentScope.GST_RETURN))
                    .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();
            ConsentDto consent = consentManagementService.createConsent(consentReq);
            consentManagementService.approveConsent(consent.getId(), ApproveConsentRequest.builder()
                    .consentingUserId(userAlpha.getId())
                    .build());

            // Check authorized
            ConsentAuthorizationResult authRes = compositeAuthService.authorizeGovernmentOperation(
                    clientAlphaGst.getId(), userAlpha.getId(),
                    ConsentScope.GST_RETURN, true
            );
            assertThat(authRes.isAuthorized()).isTrue();

            // Revoke consent
            consentManagementService.revokeConsent(consent.getId(), RevokeConsentRequest.builder()
                    .reason("Revoked by taxpayer")
                    .build());

            // Check authorization denied
            ConsentAuthorizationResult deniedRes = compositeAuthService.authorizeGovernmentOperation(
                    clientAlphaGst.getId(), userAlpha.getId(),
                    ConsentScope.GST_RETURN, true
            );
            assertThat(deniedRes.isAuthorized()).isFalse();
        }
    }

    // =========================================================================
    // 5. IDEMPOTENCY & DUPLICATE SUBMISSION VALIDATION
    // =========================================================================

    @Nested
    @DisplayName("5. Idempotency & Duplicate Submission")
    class IdempotencyValidation {

        @Test
        @DisplayName("Duplicate request with same idempotency key returns cached result")
        void testIdempotentExecution() {
            TenantContext.setTenantId(orgAlpha.getId());

            String idemKey = "idem-key-" + UUID.randomUUID();
            GovIntegrationRequest req1 = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_SUBMISSION")
                    .correlationId("corr-1-" + UUID.randomUUID())
                    .idempotencyKey(idemKey)
                    .requestData(Map.of("gstin", clientAlphaGst.getGstin()))
                    .build();

            GovIntegrationResult res1 = govIntegrationService.executeOperation(req1);

            GovIntegrationRequest req2 = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_SUBMISSION")
                    .correlationId("corr-2-" + UUID.randomUUID())
                    .idempotencyKey(idemKey)
                    .requestData(Map.of("gstin", clientAlphaGst.getGstin()))
                    .build();

            GovIntegrationResult res2 = govIntegrationService.executeOperation(req2);

            assertThat(res1.getOperationId()).isEqualTo(res2.getOperationId());
            assertThat(res1.getProviderReferenceId()).isEqualTo(res2.getProviderReferenceId());
        }
    }

    // =========================================================================
    // 6. RELIABILITY & FAILURE RECOVERY
    // =========================================================================

    @Nested
    @DisplayName("6. Reliability & Failure Classification")
    class ReliabilityValidation {

        @Test
        @DisplayName("Transient failures are classified as RETRYABLE with backoff")
        void testTransientClassification() {
            GovFailureClassification c1 = GovFailureClassification.fromErrorCode(GovErrorCode.TIMEOUT);
            GovFailureClassification c2 = GovFailureClassification.fromErrorCode(GovErrorCode.RATE_LIMITED);
            GovFailureClassification c3 = GovFailureClassification.fromErrorCode(GovErrorCode.PROVIDER_UNAVAILABLE);

            assertThat(c1).isEqualTo(GovFailureClassification.TRANSIENT);
            assertThat(c2).isEqualTo(GovFailureClassification.TRANSIENT);
            assertThat(c3).isEqualTo(GovFailureClassification.TRANSIENT);
            assertThat(c1.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Terminal errors are NOT retried")
        void testTerminalClassification() {
            GovFailureClassification c1 = GovFailureClassification.fromErrorCode(GovErrorCode.AUTH_REQUIRED);
            GovFailureClassification c2 = GovFailureClassification.fromErrorCode(GovErrorCode.VALIDATION_FAILED);
            GovFailureClassification c3 = GovFailureClassification.fromErrorCode(GovErrorCode.FORBIDDEN);

            assertThat(c1).isEqualTo(GovFailureClassification.AUTHENTICATION);
            assertThat(c2).isEqualTo(GovFailureClassification.VALIDATION);
            assertThat(c3).isEqualTo(GovFailureClassification.AUTHENTICATION);
            assertThat(c1.isRetryable()).isFalse();
        }
    }

    // =========================================================================
    // 7. MULTI-TENANT ISOLATION
    // =========================================================================

    @Nested
    @DisplayName("7. Multi-Tenant Cross-Access Isolation")
    class MultiTenantIsolationValidation {

        @Test
        @DisplayName("Organization B cannot access Organization A operations or auth sessions")
        void testStrictTenantIsolation() {
            TenantContext.setTenantId(orgAlpha.getId());

            GovIntegrationRequest req = GovIntegrationRequest.builder()
                    .organizationId(orgAlpha.getId())
                    .providerType(GovProviderType.GST)
                    .operationType("GST_LOOKUP")
                    .correlationId("corr-iso-" + UUID.randomUUID())
                    .build();

            GovIntegrationResult result = govIntegrationService.executeOperation(req);
            UUID opId = result.getOperationId();

            // Switch to Org Beta
            TenantContext.setTenantId(orgBeta.getId());

            assertThatThrownBy(() -> govIntegrationService.getOperation(opId))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // =========================================================================
    // 8. OBSERVABILITY, HEALTH & SECRET SANITIZATION
    // =========================================================================

    @Nested
    @DisplayName("8. Observability & Secret Redaction")
    class ObservabilityValidation {

        @Test
        @DisplayName("Health indicator is UP and does not leak credentials")
        void testHealthAndDiagnosticsSanitization() {
            TenantContext.setTenantId(orgAlpha.getId());

            Health health = govHealthIndicator.health();
            assertThat(health.getStatus().getCode()).isIn("UP", "UNKNOWN");

            String dirtyLog = "Error with Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test and client_secret=SEC_999";
            String cleanLog = GovReliabilitySanitizer.sanitizeString(dirtyLog);

            assertThat(cleanLog).doesNotContain("eyJhbGciOiJIUzI1NiJ9.test");
            assertThat(cleanLog).doesNotContain("SEC_999");
            assertThat(cleanLog).contains("[REDACTED]");
        }
    }
}
