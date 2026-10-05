package com.taxoryn.module.gov.observability;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.controller.GovOperationsController;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.observability.dto.GovOperationsDiagnosticsDto;
import com.taxoryn.module.gov.observability.dto.GovOperationsSummaryDto;
import com.taxoryn.module.gov.observability.health.GovIntegrationHealthIndicator;
import com.taxoryn.module.gov.observability.service.GovOperationsObservabilityService;
import com.taxoryn.module.gov.observability.util.GovAuditEventConstants;
import com.taxoryn.module.gov.observability.util.GovCorrelationContext;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxEventType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class GovObservabilityIntegrationTest {

    @Autowired
    private GovOperationsObservabilityService observabilityService;

    @Autowired
    private GovOperationsController operationsController;

    @Autowired
    private GovIntegrationHealthIndicator healthIndicator;

    @Autowired
    private GovReliabilityMetrics reliabilityMetrics;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private GovOutboxEventRepository outboxRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        GovCorrelationContext.clearContext();
        reliabilityMetrics.reset();

        outboxRepository.deleteAll();
        operationRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant Alpha")
                .email("alpha-" + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant Beta")
                .email("beta-" + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        GovCorrelationContext.clearContext();
        reliabilityMetrics.reset();
    }

    // ==========================================
    // 1. METRICS RECORDER TESTS
    // ==========================================

    @Test
    @DisplayName("GovReliabilityMetrics records operations counters accurately")
    void testMetricsOperationsCounters() {
        reliabilityMetrics.recordSuccess(GovProviderType.GST, "GST_RETURN_FILE", 120);
        reliabilityMetrics.recordRetry(GovProviderType.GST, "GST_RETURN_FILE", 1);
        reliabilityMetrics.recordFailure(GovProviderType.GST, "GST_RETURN_FILE", GovErrorCode.TIMEOUT, GovFailureClassification.TRANSIENT);
        reliabilityMetrics.recordFailure(GovProviderType.GST, "GST_RETURN_FILE", GovErrorCode.RATE_LIMITED, GovFailureClassification.TRANSIENT);
        reliabilityMetrics.recordFailure(GovProviderType.GST, "GST_RETURN_FILE", GovErrorCode.PROVIDER_UNAVAILABLE, GovFailureClassification.TRANSIENT);
        reliabilityMetrics.recordFailure(GovProviderType.GST, "GST_RETURN_FILE", GovErrorCode.AUTH_REQUIRED, GovFailureClassification.AUTHENTICATION);

        GovReliabilityMetricsSnapshot snapshot = reliabilityMetrics.getSnapshot();
        assertThat(snapshot.getOperationsTotal()).isEqualTo(5);
        assertThat(snapshot.getOperationsSuccess()).isEqualTo(1);
        assertThat(snapshot.getOperationsFailed()).isEqualTo(4);
        assertThat(snapshot.getRetriesTotal()).isEqualTo(1);
        assertThat(snapshot.getTimeoutsTotal()).isEqualTo(1);
        assertThat(snapshot.getRateLimitsTotal()).isEqualTo(1);
        assertThat(snapshot.getProviderUnavailableTotal()).isEqualTo(1);
        assertThat(snapshot.getAuthFailuresTotal()).isEqualTo(1);
        assertThat(snapshot.getSnapshotTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("GovReliabilityMetrics records outbox counters accurately")
    void testMetricsOutboxCounters() {
        reliabilityMetrics.recordOutboxEnqueued();
        reliabilityMetrics.recordOutboxEnqueued();
        reliabilityMetrics.recordOutboxCompleted();
        reliabilityMetrics.recordOutboxFailed();
        reliabilityMetrics.recordOutboxStaleRecovered();

        GovReliabilityMetricsSnapshot snapshot = reliabilityMetrics.getSnapshot();
        assertThat(snapshot.getOutboxEnqueuedTotal()).isEqualTo(2);
        assertThat(snapshot.getOutboxCompletedTotal()).isEqualTo(1);
        assertThat(snapshot.getOutboxFailedTotal()).isEqualTo(1);
        assertThat(snapshot.getOutboxStaleRecoveredTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("GovReliabilityMetrics records reconciliation counters accurately")
    void testMetricsReconciliationCounters() {
        reliabilityMetrics.recordReconciliationAttempt();
        reliabilityMetrics.recordReconciliationAttempt();
        reliabilityMetrics.recordReconciliationSuccess();
        reliabilityMetrics.recordReconciliationStatusChanged();
        reliabilityMetrics.recordReconciliationFailure();

        GovReliabilityMetricsSnapshot snapshot = reliabilityMetrics.getSnapshot();
        assertThat(snapshot.getReconciliationsAttemptedTotal()).isEqualTo(2);
        assertThat(snapshot.getReconciliationsSucceededTotal()).isEqualTo(1);
        assertThat(snapshot.getReconciliationsStatusChangedTotal()).isEqualTo(1);
        assertThat(snapshot.getReconciliationsFailedTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("GovReliabilityMetrics reset resets all counters to zero")
    void testMetricsReset() {
        reliabilityMetrics.recordSuccess(GovProviderType.GST, "OP", 50);
        reliabilityMetrics.recordOutboxEnqueued();
        reliabilityMetrics.recordReconciliationAttempt();

        reliabilityMetrics.reset();
        GovReliabilityMetricsSnapshot snapshot = reliabilityMetrics.getSnapshot();
        assertThat(snapshot.getOperationsTotal()).isZero();
        assertThat(snapshot.getOutboxEnqueuedTotal()).isZero();
        assertThat(snapshot.getReconciliationsAttemptedTotal()).isZero();
    }

    @Test
    @DisplayName("GovReliabilityMetrics snapshot preserves point-in-time state")
    void testMetricsSnapshotPreservation() {
        reliabilityMetrics.recordSuccess(GovProviderType.GST, "OP1", 10);
        GovReliabilityMetricsSnapshot snapshot1 = reliabilityMetrics.getSnapshot();

        reliabilityMetrics.recordSuccess(GovProviderType.GST, "OP2", 20);
        GovReliabilityMetricsSnapshot snapshot2 = reliabilityMetrics.getSnapshot();

        assertThat(snapshot1.getOperationsTotal()).isEqualTo(1);
        assertThat(snapshot2.getOperationsTotal()).isEqualTo(2);
    }

    // ==========================================
    // 2. CORRELATION CONTEXT & MDC TESTS
    // ==========================================

    @Test
    @DisplayName("GovCorrelationContext propagates MDC keys and executes Supplier")
    void testCorrelationContextSupplier() {
        String corrId = "CORR-" + UUID.randomUUID();
        UUID tenantId = orgA.getId();

        String result = GovCorrelationContext.executeWithContext(corrId, tenantId, "GST_FILE", GovProviderType.GST, () -> {
            assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isEqualTo(corrId);
            assertThat(MDC.get(GovCorrelationContext.TENANT_ID_MDC_KEY)).isEqualTo(tenantId.toString());
            assertThat(MDC.get(GovCorrelationContext.OPERATION_TYPE_MDC_KEY)).isEqualTo("GST_FILE");
            assertThat(MDC.get(GovCorrelationContext.PROVIDER_TYPE_MDC_KEY)).isEqualTo(GovProviderType.GST.name());
            return "SUCCESS";
        });

        assertThat(result).isEqualTo("SUCCESS");
        // Ensure MDC cleared after block
        assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(GovCorrelationContext.TENANT_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("GovCorrelationContext propagates MDC keys and executes Runnable")
    void testCorrelationContextRunnable() {
        String corrId = "CORR-RUNNABLE-" + UUID.randomUUID();
        UUID tenantId = orgA.getId();
        AtomicBoolean executed = new AtomicBoolean(false);

        GovCorrelationContext.executeWithContext(corrId, tenantId, "TDS_FILE", GovProviderType.TDS, () -> {
            assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isEqualTo(corrId);
            assertThat(MDC.get(GovCorrelationContext.TENANT_ID_MDC_KEY)).isEqualTo(tenantId.toString());
            executed.set(true);
        });

        assertThat(executed.get()).isTrue();
        assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("GovCorrelationContext restores previous MDC context even on exception")
    void testCorrelationContextRestoresOnException() {
        String outerCorr = "OUTER-CORR-EX";
        GovCorrelationContext.setContext(outerCorr, orgA.getId(), "OP_EX", GovProviderType.GST);

        assertThatThrownBy(() -> {
            GovCorrelationContext.executeWithContext("INNER-CORR-EX", orgB.getId(), "INNER_OP", GovProviderType.INCOME_TAX, () -> {
                throw new IllegalStateException("Simulated execution failure");
            });
        }).isInstanceOf(IllegalStateException.class);

        // Verify outer context was restored
        assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isEqualTo(outerCorr);
        assertThat(MDC.get(GovCorrelationContext.TENANT_ID_MDC_KEY)).isEqualTo(orgA.getId().toString());
    }

    @Test
    @DisplayName("GovCorrelationContext handles nested contexts and restores previous state")
    void testCorrelationContextNesting() {
        String outerCorr = "OUTER-CORR";
        String innerCorr = "INNER-CORR";

        GovCorrelationContext.executeWithContext(outerCorr, orgA.getId(), "OUTER_OP", GovProviderType.GST, () -> {
            assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isEqualTo(outerCorr);

            GovCorrelationContext.executeWithContext(innerCorr, orgB.getId(), "INNER_OP", GovProviderType.INCOME_TAX, () -> {
                assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isEqualTo(innerCorr);
                assertThat(MDC.get(GovCorrelationContext.TENANT_ID_MDC_KEY)).isEqualTo(orgB.getId().toString());
            });

            // Outer restored
            assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isEqualTo(outerCorr);
            assertThat(MDC.get(GovCorrelationContext.TENANT_ID_MDC_KEY)).isEqualTo(orgA.getId().toString());
        });

        assertThat(MDC.get(GovCorrelationContext.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("GovCorrelationContext generates new correlation ID if none provided")
    void testCorrelationContextGeneratesId() {
        String generated = GovCorrelationContext.getOrCreateCorrelationId(null);
        assertThat(generated).isNotBlank();
    }

    // ==========================================
    // 3. OPERATIONAL SUMMARY & DIAGNOSTICS TESTS
    // ==========================================

    @Test
    @DisplayName("Observability service computes accurate operations summary for tenant")
    void testOperationsSummary() {
        // Create operations in various statuses for Org A
        createOperation(orgA.getId(), GovOperationStatus.CREATED);
        createOperation(orgA.getId(), GovOperationStatus.IN_PROGRESS);
        createOperation(orgA.getId(), GovOperationStatus.SUCCEEDED);
        createOperation(orgA.getId(), GovOperationStatus.FAILED);
        createOperation(orgA.getId(), GovOperationStatus.RETRYING);
        createOperation(orgA.getId(), GovOperationStatus.CANCELLED);

        // Create an operation for Org B (should not be counted)
        createOperation(orgB.getId(), GovOperationStatus.SUCCEEDED);

        // Create outbox events for Org A
        createOutboxEvent(orgA.getId(), GovOutboxStatus.PENDING);
        createOutboxEvent(orgA.getId(), GovOutboxStatus.COMPLETED);

        TenantContext.setTenantId(orgA.getId());
        GovOperationsSummaryDto summary = observabilityService.getOperationsSummary(orgA.getId());

        assertThat(summary.getOrganizationId()).isEqualTo(orgA.getId());
        assertThat(summary.getTotalOperations()).isEqualTo(6);
        assertThat(summary.getCreatedOperations()).isEqualTo(1);
        assertThat(summary.getInProgressOperations()).isEqualTo(1);
        assertThat(summary.getSucceededOperations()).isEqualTo(1);
        assertThat(summary.getFailedOperations()).isEqualTo(1);
        assertThat(summary.getRetryingOperations()).isEqualTo(1);
        assertThat(summary.getCancelledOperations()).isEqualTo(1);

        assertThat(summary.getPendingOutboxEvents()).isEqualTo(1);
        assertThat(summary.getCompletedOutboxEvents()).isEqualTo(1);
        assertThat(summary.getProviderAvailability()).isNotEmpty();
        assertThat(summary.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("Observability service provides sanitized diagnostics for valid operation")
    void testOperationDiagnostics() {
        GovIntegrationOperationEntity op = createOperation(orgA.getId(), GovOperationStatus.FAILED);
        op.setErrorCode(GovErrorCode.VALIDATION_FAILED);
        op.setErrorMessage("Authorization failed for client_secret=SUPER_SECRET_123");
        op.setProviderReferenceId("REF-12345");

        TenantContext.setTenantId(orgA.getId());
        op = operationRepository.save(op);

        // Associate an outbox event
        createOutboxEventForOperation(orgA.getId(), op.getId(), GovOutboxStatus.FAILED, "password=secret123");

        GovOperationsDiagnosticsDto diag = observabilityService.getOperationDiagnostics(op.getId());

        assertThat(diag.getOperationId()).isEqualTo(op.getId());
        assertThat(diag.getOrganizationId()).isEqualTo(orgA.getId());
        assertThat(diag.getStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(diag.getErrorCode()).isEqualTo(GovErrorCode.VALIDATION_FAILED);
        assertThat(diag.getProviderReferenceId()).isEqualTo("REF-12345");

        // Verify sanitization in diagnostics
        assertThat(diag.getErrorMessage()).doesNotContain("SUPER_SECRET_123");
        assertThat(diag.getErrorMessage()).contains("[REDACTED]");

        // Verify outbox events included and sanitized
        assertThat(diag.getOutboxEvents()).hasSize(1);
        assertThat(diag.getOutboxEvents().get(0).getPayload()).doesNotContain("secret123");
        assertThat(diag.getOutboxEvents().get(0).getPayload()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Observability service enforces tenant boundary on diagnostics")
    void testDiagnosticsTenantIsolation() {
        GovIntegrationOperationEntity opOrgB = createOperation(orgB.getId(), GovOperationStatus.SUCCEEDED);

        // Tenant A tries to access Org B's diagnostics
        TenantContext.setTenantId(orgA.getId());
        assertThatThrownBy(() -> observabilityService.getOperationDiagnostics(opOrgB.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Observability service rejects summary request without active tenant context")
    void testSummaryRequiresTenant() {
        TenantContext.clear();
        assertThatThrownBy(() -> observabilityService.getOperationsSummary(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==========================================
    // 4. ACTUATOR HEALTH INDICATOR TESTS
    // ==========================================

    @Test
    @DisplayName("GovIntegrationHealthIndicator reports health details safely")
    void testActuatorHealthIndicator() {
        reliabilityMetrics.recordSuccess(GovProviderType.GST, "OP_TEST", 80);
        reliabilityMetrics.recordOutboxEnqueued();
        reliabilityMetrics.recordReconciliationAttempt();

        createOutboxEvent(orgA.getId(), GovOutboxStatus.PENDING);
        createOutboxEvent(orgA.getId(), GovOutboxStatus.FAILED);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isIn(Status.UP, new Status("DEGRADED"));
        Map<String, Object> details = health.getDetails();

        assertThat(details).containsKey("providers");
        assertThat(details).containsKey("outboxPendingBacklog");
        assertThat(details).containsKey("outboxFailedBacklog");
        assertThat(details).containsKey("operationsTotal");
        assertThat(details).containsKey("operationsSuccess");

        assertThat((Long) details.get("outboxPendingBacklog")).isGreaterThanOrEqualTo(1);
        assertThat((Long) details.get("outboxFailedBacklog")).isGreaterThanOrEqualTo(1);
        assertThat((Long) details.get("operationsTotal")).isGreaterThanOrEqualTo(1);

        // Zero secret leakage check
        String detailsStr = details.toString();
        assertThat(detailsStr).doesNotContain("password");
        assertThat(detailsStr).doesNotContain("secret");
        assertThat(detailsStr).doesNotContain("Bearer");
    }

    // ==========================================
    // 5. REST CONTROLLER TESTS
    // ==========================================

    @Test
    @WithMockUser(authorities = {"GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE", "ROLE_ORG_ADMIN"})
    @DisplayName("GovOperationsController returns summary endpoint successfully")
    void testControllerSummary() {
        createOperation(orgA.getId(), GovOperationStatus.SUCCEEDED);
        TenantContext.setTenantId(orgA.getId());

        ResponseEntity<ApiResponse<GovOperationsSummaryDto>> response = operationsController.getOperationsSummary(orgA.getId());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getTotalOperations()).isEqualTo(1);
    }

    @Test
    @WithMockUser(authorities = {"GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE", "ROLE_ORG_ADMIN"})
    @DisplayName("GovOperationsController returns metrics endpoint successfully")
    void testControllerMetrics() {
        reliabilityMetrics.recordSuccess(GovProviderType.GST, "METRIC_OP", 100);

        ResponseEntity<ApiResponse<GovReliabilityMetricsSnapshot>> response = operationsController.getMetrics();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getOperationsTotal()).isEqualTo(1);
    }

    @Test
    @WithMockUser(authorities = {"GOV_INTEGRATION_VIEW", "GOV_INTEGRATION_MANAGE", "ROLE_ORG_ADMIN"})
    @DisplayName("GovOperationsController returns diagnostics endpoint successfully")
    void testControllerDiagnostics() {
        GovIntegrationOperationEntity op = createOperation(orgA.getId(), GovOperationStatus.SUCCEEDED);
        TenantContext.setTenantId(orgA.getId());

        ResponseEntity<ApiResponse<GovOperationsDiagnosticsDto>> response = operationsController.getOperationDiagnostics(op.getId());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getOperationId()).isEqualTo(op.getId());
    }

    // ==========================================
    // 6. AUDIT CONSTANTS & SECURITY VERIFICATION
    // ==========================================

    @Test
    @DisplayName("GovAuditEventConstants values are properly defined and unique")
    void testAuditEventConstants() {
        assertThat(GovAuditEventConstants.GOV_OPERATION_INITIATED).isEqualTo("GOV_OPERATION_INITIATED");
        assertThat(GovAuditEventConstants.GOV_OPERATION_SUCCEEDED).isEqualTo("GOV_OPERATION_SUCCEEDED");
        assertThat(GovAuditEventConstants.GOV_OPERATION_FAILED).isEqualTo("GOV_OPERATION_FAILED");
        assertThat(GovAuditEventConstants.GOV_OUTBOX_EVENT_ENQUEUED).isEqualTo("GOV_OUTBOX_EVENT_ENQUEUED");
        assertThat(GovAuditEventConstants.GOV_OUTBOX_EVENT_COMPLETED).isEqualTo("GOV_OUTBOX_EVENT_COMPLETED");
        assertThat(GovAuditEventConstants.GOV_OUTBOX_EVENT_FAILED).isEqualTo("GOV_OUTBOX_EVENT_FAILED");
        assertThat(GovAuditEventConstants.GOV_RECONCILIATION_STARTED).isEqualTo("GOV_RECONCILIATION_STARTED");
        assertThat(GovAuditEventConstants.GOV_RECONCILIATION_STATUS_CHANGED).isEqualTo("GOV_RECONCILIATION_STATUS_CHANGED");
        assertThat(GovAuditEventConstants.GOV_RECONCILIATION_COMPLETED).isEqualTo("GOV_RECONCILIATION_COMPLETED");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private GovIntegrationOperationEntity createOperation(UUID orgId, GovOperationStatus status) {
        UUID prev = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(orgId);
            GovIntegrationOperationEntity op = GovIntegrationOperationEntity.builder()
                    .providerType(GovProviderType.GST)
                    .operationType("GST_GSTR1_FILE")
                    .businessEntityType("GST_RETURN")
                    .businessEntityId(UUID.randomUUID())
                    .correlationId("CORR-" + UUID.randomUUID())
                    .idempotencyKey("IDEMP-" + UUID.randomUUID())
                    .status(status)
                    .attemptCount(status == GovOperationStatus.SUCCEEDED ? 1 : 0)
                    .maxAttempts(3)
                    .build();
            op.setOrganizationId(orgId);
            return operationRepository.save(op);
        } finally {
            if (prev != null) {
                TenantContext.setTenantId(prev);
            } else {
                TenantContext.clear();
            }
        }
    }

    private GovOutboxEventEntity createOutboxEvent(UUID orgId, GovOutboxStatus status) {
        UUID prev = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(orgId);
            GovOutboxEventEntity event = GovOutboxEventEntity.builder()
                    .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                    .aggregateType("GovIntegrationOperation")
                    .aggregateId(UUID.randomUUID())
                    .providerType(GovProviderType.GST)
                    .correlationId("CORR-" + UUID.randomUUID())
                    .status(status)
                    .attemptCount(status == GovOutboxStatus.COMPLETED ? 1 : 0)
                    .maxAttempts(3)
                    .availableAt(Instant.now())
                    .build();
            event.setOrganizationId(orgId);
            return outboxRepository.save(event);
        } finally {
            if (prev != null) {
                TenantContext.setTenantId(prev);
            } else {
                TenantContext.clear();
            }
        }
    }

    private GovOutboxEventEntity createOutboxEventForOperation(UUID orgId, UUID operationId, GovOutboxStatus status, String payload) {
        UUID prev = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(orgId);
            GovOutboxEventEntity event = GovOutboxEventEntity.builder()
                    .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                    .aggregateType("GovIntegrationOperation")
                    .aggregateId(operationId)
                    .operationId(operationId)
                    .providerType(GovProviderType.GST)
                    .correlationId("CORR-" + UUID.randomUUID())
                    .status(status)
                    .payload(payload != null ? com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer.sanitizeString(payload) : null)
                    .attemptCount(1)
                    .maxAttempts(3)
                    .availableAt(Instant.now())
                    .build();
            event.setOrganizationId(orgId);
            return outboxRepository.save(event);
        } finally {
            if (prev != null) {
                TenantContext.setTenantId(prev);
            } else {
                TenantContext.clear();
            }
        }
    }
}
