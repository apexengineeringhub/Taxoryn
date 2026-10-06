package com.taxoryn.module.gov.reliability;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import com.taxoryn.module.gov.reliability.model.GovOperationOutcome;
import com.taxoryn.module.gov.reliability.policy.GovRetryPolicy;
import com.taxoryn.module.gov.reliability.service.GovOperationRecoveryService;
import com.taxoryn.module.gov.reliability.service.GovReliabilityService;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class GovReliabilityIntegrationTest {

    @Autowired
    private GovReliabilityService reliabilityService;

    @Autowired
    private GovOperationRecoveryService recoveryService;

    @Autowired
    private GovernmentIntegrationService govIntegrationService;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Reliability Org A " + UUID.randomUUID())
                .legalName("Reliability Org A Pvt Ltd")
                .email("rel.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Reliability Org B " + UUID.randomUUID())
                .legalName("Reliability Org B Pvt Ltd")
                .email("rel.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // 1 - 7: BASIC OPERATIONS & OUTCOMES
    // =========================================================================

    @Test
    @DisplayName("1. Successful operation execution and normalization")
    void testSuccessfulOperation() {
        GovIntegrationRequest request = createRequest("SUBMIT_GSTR1", Map.of("invoiceCount", 10));

        GovIntegrationResult result = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result.getProviderReferenceId()).contains("ARN-");

        GovOperationOutcome outcome = reliabilityService.normalizeOutcome(result);
        assertThat(outcome).isEqualTo(GovOperationOutcome.SUCCESS);
    }

    @Test
    @DisplayName("2. Timeout handling and classification as TRANSIENT")
    void testTimeoutHandling() {
        GovIntegrationRequest request = createRequest("SUBMIT_ITR", Map.of("mockOutcome", "TIMEOUT"));

        GovIntegrationResult result = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.TIMEOUT);
        assertThat(result.isTransientError()).isTrue();

        GovFailureClassification classification = reliabilityService.classifyFailure(result.getErrorCode(), 504, null);
        assertThat(classification).isEqualTo(GovFailureClassification.TRANSIENT);
        assertThat(classification.isRetryable()).isTrue();

        GovOperationOutcome outcome = reliabilityService.normalizeOutcome(result);
        assertThat(outcome).isEqualTo(GovOperationOutcome.TIMEOUT);
    }

    @Test
    @DisplayName("3. Connection failure handling and retry eligibility")
    void testConnectionFailure() {
        GovFailureClassification classification = reliabilityService.classifyFailure(GovErrorCode.TIMEOUT, 408, "Connection reset by peer");
        assertThat(classification).isEqualTo(GovFailureClassification.TRANSIENT);
        assertThat(classification.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("4. HTTP 429 Rate Limit classification and normalization")
    void testRateLimitHandling() {
        GovIntegrationRequest request = createRequest("GENERATE_EWAY_BILL", Map.of("mockOutcome", "RATE_LIMITED"));

        GovIntegrationResult result = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.RATE_LIMITED);

        GovFailureClassification classification = reliabilityService.classifyFailure(result.getErrorCode(), 429, null);
        assertThat(classification).isEqualTo(GovFailureClassification.TRANSIENT);

        GovOperationOutcome outcome = reliabilityService.normalizeOutcome(result);
        assertThat(outcome).isEqualTo(GovOperationOutcome.RATE_LIMITED);
    }

    @Test
    @DisplayName("5-7. HTTP 502, 503, 504 Provider Unavailable classifications")
    void testGatewayErrors() {
        assertThat(GovFailureClassification.fromHttpStatus(502)).isEqualTo(GovFailureClassification.TRANSIENT);
        assertThat(GovFailureClassification.fromHttpStatus(503)).isEqualTo(GovFailureClassification.TRANSIENT);
        assertThat(GovFailureClassification.fromHttpStatus(504)).isEqualTo(GovFailureClassification.TRANSIENT);

        GovRetryPolicy policy = GovRetryPolicy.defaultPolicy();
        assertThat(policy.shouldRetryHttp(502, 1)).isTrue();
        assertThat(policy.shouldRetryHttp(503, 1)).isTrue();
        assertThat(policy.shouldRetryHttp(504, 1)).isTrue();
    }

    // =========================================================================
    // 8 - 9: RETRY BEHAVIOR
    // =========================================================================

    @Test
    @DisplayName("8. Retry succeeds on subsequent attempt using idempotent key")
    void testRetrySucceeds() {
        String idempKey = "idemp-retry-succ-" + UUID.randomUUID();

        // Attempt 1: Transient Timeout
        GovIntegrationRequest req1 = GovIntegrationRequest.builder()
                .organizationId(orgA.getId())
                .providerType(GovProviderType.GST)
                .operationType("FILE_GSTR3B")
                .idempotencyKey(idempKey)
                .correlationId("corr-retry-01")
                .requestData(Map.of("mockOutcome", "TIMEOUT"))
                .build();

        GovIntegrationResult res1 = govIntegrationService.executeOperation(req1);
        assertThat(res1.isSuccess()).isFalse();
        assertThat(res1.getStatus()).isEqualTo(GovOperationStatus.RETRYING);

        // Attempt 2: Success on same idempotency key
        GovIntegrationRequest req2 = GovIntegrationRequest.builder()
                .organizationId(orgA.getId())
                .providerType(GovProviderType.GST)
                .operationType("FILE_GSTR3B")
                .idempotencyKey(idempKey)
                .correlationId("corr-retry-02")
                .requestData(Map.of("mockOutcome", "SUCCESS"))
                .build();

        GovIntegrationResult res2 = govIntegrationService.executeOperation(req2);
        assertThat(res2.isSuccess()).isTrue();
        assertThat(res2.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(res2.getOperationId()).isEqualTo(res1.getOperationId());
    }

    @Test
    @DisplayName("9. Exhausted retries transition operation to terminal FAILED status")
    void testRetryExhausted() {
        GovIntegrationRequest request = createRequest("SUBMIT_TDS", Map.of("mockOutcome", "TIMEOUT"));

        GovRetryPolicy policy = GovRetryPolicy.builder()
                .maxAttempts(3)
                .initialBackoffMillis(0)
                .build();

        GovIntegrationResult result = reliabilityService.executeWithRetry(request, policy);

        assertThat(result.isSuccess()).isFalse();
        GovOperationDto op = govIntegrationService.getOperation(result.getOperationId());
        assertThat(op.getAttemptCount()).isEqualTo(3);
        assertThat(op.getStatus()).isEqualTo(GovOperationStatus.FAILED);
    }

    // =========================================================================
    // 10 - 13: NON-RETRYABLE FAILURES (400, 401, 403, 422)
    // =========================================================================

    @Test
    @DisplayName("10. HTTP 400 Validation Failure is NOT retried")
    void test400NotRetried() {
        GovIntegrationRequest request = createRequest("VALIDATE_PAN", Map.of("mockOutcome", "VALIDATION_FAILED"));
        GovIntegrationResult result = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.VALIDATION_FAILED);

        GovOperationDto op = govIntegrationService.getOperation(result.getOperationId());
        assertThat(op.getAttemptCount()).isEqualTo(1); // Exactly 1 attempt
        assertThat(op.getStatus()).isEqualTo(GovOperationStatus.FAILED);
    }

    @Test
    @DisplayName("11. HTTP 401 Authentication Required is NOT retried")
    void test401NotRetried() {
        GovIntegrationRequest request = createRequest("VERIFY_EVC", Map.of("mockOutcome", "AUTH_REQUIRED"));
        GovIntegrationResult result = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.AUTH_REQUIRED);

        GovOperationDto op = govIntegrationService.getOperation(result.getOperationId());
        assertThat(op.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("12. HTTP 403 Forbidden is NOT retried")
    void test403NotRetried() {
        GovIntegrationRequest request = createRequest("DSC_SIGN", Map.of("mockOutcome", "FORBIDDEN"));
        GovIntegrationResult result = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.FORBIDDEN);

        GovOperationDto op = govIntegrationService.getOperation(result.getOperationId());
        assertThat(op.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("13. HTTP 422 Business / Domain failure is NOT retried")
    void test422NotRetried() {
        GovFailureClassification classification = GovFailureClassification.fromHttpStatus(422);
        assertThat(classification).isEqualTo(GovFailureClassification.BUSINESS);
        assertThat(classification.isRetryable()).isFalse();

        GovRetryPolicy policy = GovRetryPolicy.defaultPolicy();
        assertThat(policy.shouldRetryHttp(422, 1)).isFalse();
    }

    // =========================================================================
    // 14 - 15: IDEMPOTENCY & DUPLICATION
    // =========================================================================

    @Test
    @DisplayName("14. Duplicate operation returns cached authoritative success idempotently")
    void testDuplicateOperationReturnsCached() {
        GovIntegrationRequest request = createRequest("FILE_GSTR1", Map.of("period", "082026"));

        GovIntegrationResult res1 = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());
        GovIntegrationResult res2 = reliabilityService.executeWithRetry(request, GovRetryPolicy.immediatePolicy());

        assertThat(res2.isSuccess()).isTrue();
        assertThat(res2.getOperationId()).isEqualTo(res1.getOperationId());
        assertThat(res2.getProviderReferenceId()).isEqualTo(res1.getProviderReferenceId());
    }

    @Test
    @DisplayName("15. Idempotent retry preserves logical operation identity")
    void testIdempotentRetryIdentity() {
        String idempKey = "idemp-preserve-" + UUID.randomUUID();

        GovIntegrationRequest req = GovIntegrationRequest.builder()
                .organizationId(orgA.getId())
                .providerType(GovProviderType.TDS)
                .operationType("VERIFY_TAN")
                .idempotencyKey(idempKey)
                .correlationId("corr-preserve-01")
                .requestData(Map.of("tan", "BLRP00001A"))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(req);
        assertThat(result.getIdempotencyKey()).isEqualTo(idempKey);

        GovOperationDto op = govIntegrationService.getOperationByIdempotencyKey(idempKey);
        assertThat(op.getId()).isEqualTo(result.getOperationId());
    }

    // =========================================================================
    // 16 - 17: UNKNOWN OUTCOME & APPLICATION RESTART RECOVERY
    // =========================================================================

    @Test
    @DisplayName("16. Ambiguous outcome reconciliation")
    void testAmbiguousOutcomeReconciliation() {
        GovIntegrationOperationEntity opEntity = GovIntegrationOperationEntity.builder()
                .providerType(GovProviderType.GST)
                .operationType("FILE_GSTR3B")
                .correlationId("corr-ambig-01")
                .idempotencyKey("idemp-ambig-01")
                .status(GovOperationStatus.IN_PROGRESS)
                .attemptCount(3)
                .maxAttempts(3)
                .build();
        opEntity.setOrganizationId(orgA.getId());
        opEntity = operationRepository.save(opEntity);

        GovOperationDto reconciled = recoveryService.reconcileAmbiguousOutcome(opEntity.getId());
        assertThat(reconciled.getStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(reconciled.getErrorCode()).isEqualTo(GovErrorCode.TIMEOUT);
    }

    @Test
    @DisplayName("17. Application restart recovery engine discovers and recovers in-flight operations")
    void testApplicationRestartRecovery() {
        GovIntegrationOperationEntity interruptedOp = GovIntegrationOperationEntity.builder()
                .providerType(GovProviderType.GST)
                .operationType("RESTART_RECOVER_OP")
                .correlationId("corr-restart-01")
                .idempotencyKey("idemp-restart-01")
                .status(GovOperationStatus.RETRYING)
                .attemptCount(1)
                .maxAttempts(3)
                .requestMetadata("{\"taxPeriod\":\"072026\"}")
                .build();
        GovIntegrationOperationEntity savedOp = operationRepository.save(interruptedOp);
        UUID targetOpId = savedOp.getId();

        // Discover recoverable operations
        List<GovOperationDto> recoverable = recoveryService.findRecoverableOperations(Instant.now().plus(Duration.ofMinutes(1)));
        assertThat(recoverable.stream().anyMatch(o -> o.getId().equals(targetOpId))).isTrue();

        // Perform recovery
        GovIntegrationResult recoveryResult = recoveryService.recoverOperation(targetOpId);
        assertThat(recoveryResult.isSuccess()).isTrue();

        GovOperationDto updated = govIntegrationService.getOperation(targetOpId);
        assertThat(updated.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
    }

    // =========================================================================
    // 18 - 20: TENANT ISOLATION, SECRET REDACTION, AND STATE SAFETY
    // =========================================================================

    @Test
    @DisplayName("18. Strict multi-tenant isolation on recovery and retry")
    void testTenantIsolation() {
        GovIntegrationRequest requestA = createRequest("TENANT_A_OP", Map.of());
        GovIntegrationResult resultA = govIntegrationService.executeOperation(requestA);

        // Switch to Tenant B
        TenantContext.setTenantId(orgB.getId());

        // Tenant B cannot recover Tenant A's operation
        assertThatThrownBy(() -> recoveryService.recoverOperation(resultA.getOperationId()))
                .isInstanceOf(AppException.class);

        // Tenant B cannot reconcile Tenant A's operation
        assertThatThrownBy(() -> recoveryService.reconcileAmbiguousOutcome(resultA.getOperationId()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("19. Sensitive data protection and secret redaction in reliability logs")
    void testSensitiveDataRedaction() {
        Map<String, Object> sensitiveData = new HashMap<>();
        sensitiveData.put("token", "secret_bearer_token_xyz");
        sensitiveData.put("otp", "123456");
        sensitiveData.put("password", "MyP@ssword!");
        sensitiveData.put("safeField", "public_info");

        Map<String, Object> sanitized = GovReliabilitySanitizer.sanitizeMap(sensitiveData);

        assertThat(sanitized.get("token")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("otp")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("password")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("safeField")).isEqualTo("public_info");

        String sanitizedStr = GovReliabilitySanitizer.sanitizeString("Bearer token=abc123secret!");
        assertThat(sanitizedStr).doesNotContain("abc123secret!");
    }

    @Test
    @DisplayName("20. Operation state remains non-terminal (RETRYING) after transient failure")
    void testOperationStateSafety() {
        GovIntegrationRequest request = createRequest("TRANSIENT_STATE_CHECK", Map.of("mockOutcome", "TIMEOUT"));

        GovIntegrationResult result = govIntegrationService.executeOperation(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovOperationStatus.RETRYING);

        GovOperationDto op = govIntegrationService.getOperation(result.getOperationId());
        assertThat(op.getStatus()).isEqualTo(GovOperationStatus.RETRYING);
        assertThat(op.getStatus().isTerminal()).isFalse();
    }

    // =========================================================================
    // 21. METRICS OBSERVABILITY
    // =========================================================================

    @Test
    @DisplayName("21. Reliability metrics snapshot correctly tracks successes and failures")
    void testMetricsObservability() {
        GovIntegrationRequest successReq = createRequest("METRIC_SUCCESS", Map.of());
        GovIntegrationRequest failReq = createRequest("METRIC_FAIL", Map.of("mockOutcome", "RATE_LIMITED"));

        reliabilityService.executeWithRetry(successReq, GovRetryPolicy.immediatePolicy());
        reliabilityService.executeWithRetry(failReq, GovRetryPolicy.immediatePolicy());

        GovReliabilityMetricsSnapshot snapshot = reliabilityService.getMetrics();
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getOperationsTotal()).isGreaterThan(0);
        assertThat(snapshot.getOperationsSuccess()).isGreaterThan(0);
        assertThat(snapshot.getOperationsFailed()).isGreaterThan(0);
    }

    private GovIntegrationRequest createRequest(String opType, Map<String, Object> data) {
        return GovIntegrationRequest.builder()
                .organizationId(orgA.getId())
                .providerType(GovProviderType.GST)
                .operationType(opType)
                .businessEntityType("TEST_ENTITY")
                .businessEntityId(UUID.randomUUID())
                .correlationId("corr-" + UUID.randomUUID().toString().substring(0, 8))
                .idempotencyKey("idemp-" + UUID.randomUUID())
                .requestData(data)
                .build();
    }
}
