package com.taxoryn.module.gov.reconciliation;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEnqueueRequest;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEventDto;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxEventType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.processor.GovOutboxProcessor;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.outbox.service.GovOutboxService;
import com.taxoryn.module.gov.reconciliation.dto.GovReconciliationBatchResultDto;
import com.taxoryn.module.gov.reconciliation.dto.GovReconciliationResultDto;
import com.taxoryn.module.gov.reconciliation.model.GovAuthoritativeStatus;
import com.taxoryn.module.gov.reconciliation.scheduler.GovReconciliationScheduler;
import com.taxoryn.module.gov.reconciliation.service.GovernmentOperationReconciliationService;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class GovReconciliationIntegrationTest {

    @Autowired
    private GovernmentOperationReconciliationService reconciliationService;

    @Autowired
    private GovReconciliationScheduler reconciliationScheduler;

    @Autowired
    private GovOutboxService outboxService;

    @Autowired
    private GovOutboxProcessor outboxProcessor;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private GovOutboxEventRepository outboxRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private GovernmentProviderRegistry providerRegistry;

    private GovernmentProviderAdapter mockAdapter;

    @MockBean
    private AuditService auditService;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = createTestOrganization("Org-Rec-A").getId();
        tenantB = createTestOrganization("Org-Rec-B").getId();
        TenantContext.setTenantId(tenantA);

        mockAdapter = mock(GovernmentProviderAdapter.class);
        when(mockAdapter.getProviderType()).thenReturn(GovProviderType.GST);
        when(providerRegistry.getAdapter(any(GovProviderType.class))).thenReturn(Optional.of(mockAdapter));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        outboxRepository.deleteAll();
        operationRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private OrganizationEntity createTestOrganization(String name) {
        OrganizationEntity org = OrganizationEntity.builder()
                .name(name)
                .email(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        return organizationRepository.save(org);
    }

    private GovIntegrationOperationEntity createTestOperation(UUID orgId, GovOperationStatus status, GovProviderType providerType, String opType) {
        GovIntegrationOperationEntity op = GovIntegrationOperationEntity.builder()
                .providerType(providerType)
                .operationType(opType)
                .businessEntityType("RETURN")
                .businessEntityId(UUID.randomUUID())
                .correlationId("corr-" + UUID.randomUUID())
                .idempotencyKey("idemp-" + UUID.randomUUID())
                .status(status)
                .attemptCount(1)
                .maxAttempts(3)
                .build();
        op.setOrganizationId(orgId);
        return operationRepository.save(op);
    }

    @Test
    @DisplayName("1. CREATED / SUBMITTED operation is eligible for reconciliation")
    void testCreatedOperation_IsEligibleForReconciliation() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.CREATED, GovProviderType.GST, "SUBMIT_GSTR1");

        List<GovOperationDto> eligible = reconciliationService.findEligibleOperations(10);
        assertThat(eligible).extracting(GovOperationDto::getId).contains(op.getId());
    }

    @Test
    @DisplayName("2. IN_PROGRESS / PROCESSING operation is eligible for reconciliation")
    void testInProgressOperation_IsEligibleForReconciliation() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        List<GovOperationDto> eligible = reconciliationService.findEligibleOperations(10);
        assertThat(eligible).extracting(GovOperationDto::getId).contains(op.getId());
    }

    @Test
    @DisplayName("3. SUCCEEDED / FILED operation is not unnecessarily reconciled")
    void testFiledOperation_IsNotReconciled() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.SUCCEEDED, GovProviderType.GST, "SUBMIT_GSTR1");

        List<GovOperationDto> eligible = reconciliationService.findEligibleOperations(10);
        assertThat(eligible).extracting(GovOperationDto::getId).doesNotContain(op.getId());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.isChanged()).isFalse();
        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        verify(mockAdapter, never()).execute(any());
    }

    @Test
    @DisplayName("4. Provider PROCESSING updates state correctly to IN_PROGRESS and sets backoff")
    void testProviderProcessing_UpdatesStateToInProgress() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.CREATED, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .responseMetadata(Map.of("status", "PROCESSING"))
                .build());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.PROCESSING);
        assertThat(result.isChanged()).isTrue();

        GovIntegrationOperationEntity updated = operationRepository.findById(op.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);
        assertThat(updated.getNextReconciliationAt()).isAfter(Instant.now());
        assertThat(updated.getReconciliationAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("5. Provider FILED transitions operation to SUCCEEDED with provider reference ID")
    void testProviderFiled_TransitionsToSucceeded() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("ARN-GST-2026-9999")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.FILED);
        assertThat(result.getProviderReferenceId()).isEqualTo("ARN-GST-2026-9999");
        assertThat(result.isChanged()).isTrue();

        GovIntegrationOperationEntity updated = operationRepository.findById(op.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getNextReconciliationAt()).isNull();
    }

    @Test
    @DisplayName("6. Provider REJECTED transitions operation to FAILED with VALIDATION_FAILED error")
    void testProviderRejected_TransitionsToFailed() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.VALIDATION_FAILED, "Schema validation mismatch in Section B", false,
                Map.of("status", "REJECTED")));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.REJECTED);
        assertThat(result.getErrorCode()).isEqualTo("VALIDATION_FAILED");

        GovIntegrationOperationEntity updated = operationRepository.findById(op.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("7. Provider FAILED transitions operation to FAILED with error details")
    void testProviderFailed_TransitionsToFailed() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.UNKNOWN, "Gateway processing abort", false, null));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.FAILED);
    }

    @Test
    @DisplayName("8. Provider UNKNOWN preserves recoverable state and never transitions to SUCCEEDED")
    void testProviderUnknown_PreservesRecoverableState() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.TIMEOUT, "Gateway read timeout", true, null));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.UNKNOWN);

        GovIntegrationOperationEntity updated = operationRepository.findById(op.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);
        assertThat(updated.getNextReconciliationAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("9. CRITICAL INVARIANT: FILED / SUCCEEDED never regresses to PROCESSING or CREATED")
    void testFiledNeverRegressesToProcessing() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.SUCCEEDED, GovProviderType.GST, "SUBMIT_GSTR1");
        op.setProviderReferenceId("ARN-FROZEN-1234");
        operationRepository.save(op);

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .responseMetadata(Map.of("status", "PROCESSING"))
                .build());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result.isChanged()).isFalse();

        GovIntegrationOperationEntity updated = operationRepository.findById(op.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(updated.getProviderReferenceId()).isEqualTo("ARN-FROZEN-1234");
    }

    @Test
    @DisplayName("10. Reconciliation is idempotent and safe across multiple executions")
    void testReconciliationIsIdempotent() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("ARN-IDEMP-001")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        GovReconciliationResultDto firstRun = reconciliationService.reconcileOperation(op.getId());
        assertThat(firstRun.isChanged()).isTrue();
        assertThat(firstRun.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);

        GovReconciliationResultDto secondRun = reconciliationService.reconcileOperation(op.getId());
        assertThat(secondRun.isChanged()).isFalse();
        assertThat(secondRun.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("11. Duplicate reconciliation produces no duplicate operation records")
    void testDuplicateReconciliation_NoDuplicateOperations() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("ARN-DUPL-001")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        reconciliationService.reconcileOperation(op.getId());
        reconciliationService.reconcileOperation(op.getId());

        long totalOps = operationRepository.count();
        assertThat(totalOps).isEqualTo(1);
    }

    @Test
    @DisplayName("12. Concurrent workers do not claim or reconcile the same operation simultaneously")
    void testConcurrentWorkers_AtomicClaiming() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        Instant now = Instant.now();
        Instant lock1 = now.plus(Duration.ofMinutes(5));
        int worker1 = operationRepository.claimOperationForReconciliation(op.getId(), GovOperationStatus.IN_PROGRESS, lock1, now);
        int worker2 = operationRepository.claimOperationForReconciliation(op.getId(), GovOperationStatus.IN_PROGRESS, lock1, now);

        assertThat(worker1).isEqualTo(1);
        assertThat(worker2).isEqualTo(0);
    }

    @Test
    @DisplayName("13. Batch processing is bounded by batch size")
    void testBatchProcessing_Bounded() {
        for (int i = 0; i < 5; i++) {
            createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "BATCH_OP_" + i);
        }

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .responseMetadata(Map.of("status", "PROCESSING"))
                .build());

        GovReconciliationBatchResultDto batchResult = reconciliationService.reconcileBatch(3);

        assertThat(batchResult.getTotalProcessed()).isEqualTo(3);
        assertThat(batchResult.getResults()).hasSize(3);
    }

    @Test
    @DisplayName("14. Tenant isolation: Tenant A cannot reconcile Tenant B operations")
    void testTenantIsolation_CrossTenantAccessForbidden() {
        TenantContext.setTenantId(tenantA);
        GovIntegrationOperationEntity opA = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_A");

        TenantContext.setTenantId(tenantB);
        assertThatThrownBy(() -> reconciliationService.reconcileOperation(opA.getId()))
                .isInstanceOf(com.taxoryn.core.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("15. Provider timeout handling preserves recoverable state")
    void testProviderTimeout_PreservesRecoverableState() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.TIMEOUT, "HTTP 504 Gateway Timeout", true, null));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.UNKNOWN);
    }

    @Test
    @DisplayName("16. Provider rate-limit handling preserves recoverable state")
    void testProviderRateLimited_PreservesRecoverableState() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.RATE_LIMITED, "HTTP 429 Too Many Requests", true, null));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.UNKNOWN);
    }

    @Test
    @DisplayName("17. Authentication-required handling flags REQUIRES_ACTION")
    void testAuthenticationRequired_FlagsRequiresAction() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.AUTH_REQUIRED, "Session token expired", false, null));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.getAuthoritativeStatus()).isEqualTo(GovAuthoritativeStatus.REQUIRES_ACTION);
    }

    @Test
    @DisplayName("18. Restart/recovery compatibility recovers in-flight operations")
    void testRestartRecoveryCompatibility() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");
        op.setNextReconciliationAt(null);
        operationRepository.save(op);

        List<GovOperationDto> eligible = reconciliationService.findEligibleOperations(10);
        assertThat(eligible).extracting(GovOperationDto::getId).contains(op.getId());
    }

    @Test
    @DisplayName("19. Outbox integration compatibility: GOV_OPERATION_RECONCILIATION triggers reconciliation")
    void testOutboxIntegration_TriggersReconciliation() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("ARN-OUTBOX-RECON-01")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        GovOutboxEventDto outboxEvent = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_RECONCILIATION)
                .operationId(op.getId())
                .build());

        outboxProcessor.processPendingBatch(10);

        GovIntegrationOperationEntity reconciled = operationRepository.findById(op.getId()).orElseThrow();
        assertThat(reconciled.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(reconciled.getProviderReferenceId()).isEqualTo("ARN-OUTBOX-RECON-01");
    }

    @Test
    @DisplayName("20. GST provider compatibility")
    void testGstProviderCompatibility() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "GSTR3B_FILE");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("GST-ARN-3B-2026")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.getProviderType()).isEqualTo(GovProviderType.GST);
        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("21. ITR provider compatibility")
    void testItrProviderCompatibility() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.INCOME_TAX, "ITR1_FILE");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("ITR-ACK-100234")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.getProviderType()).isEqualTo(GovProviderType.INCOME_TAX);
        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("22. TDS provider compatibility")
    void testTdsProviderCompatibility() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.TRACES, "TDS_26Q_FILE");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("PRN-26Q-998877")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());
        assertThat(result.getProviderType()).isEqualTo(GovProviderType.TRACES);
        assertThat(result.getNewStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("23. Sensitive secrets are never leaked into error messages or payloads")
    void testSensitiveInformation_NotLeaked() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.failure(
                op.getId(), tenantA, GovProviderType.GST, "FETCH_STATUS", op.getCorrelationId(), op.getIdempotencyKey(),
                GovErrorCode.VALIDATION_FAILED, "Validation failed with password=SecretKey123 and token=Bearer_xyz999", false, null));

        GovReconciliationResultDto result = reconciliationService.reconcileOperation(op.getId());

        assertThat(result.getErrorMessage()).doesNotContain("SecretKey123");
        assertThat(result.getErrorMessage()).contains("password=[REDACTED]");
    }

    @Test
    @DisplayName("24. Audit events are recorded on reconciliation start, status change, and completion")
    void testAuditEvents_Recorded() {
        GovIntegrationOperationEntity op = createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SUBMIT_GSTR1");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .providerReferenceId("ARN-AUDIT-TEST")
                .responseMetadata(Map.of("status", "FILED"))
                .build());

        reconciliationService.reconcileOperation(op.getId());

        verify(auditService, atLeastOnce()).logEvent(
                eq(tenantA),
                any(),
                eq("GOV_RECONCILIATION_STARTED"),
                eq("GovIntegrationOperation"),
                eq(op.getId().toString()),
                any(),
                any()
        );

        verify(auditService, atLeastOnce()).logEvent(
                eq(tenantA),
                any(),
                eq("GOV_RECONCILIATION_STATUS_CHANGED"),
                eq("GovIntegrationOperation"),
                eq(op.getId().toString()),
                eq("IN_PROGRESS"),
                eq("SUCCEEDED")
        );
    }

    @Test
    @DisplayName("25. Scheduler respects configuration and executes batch")
    void testScheduler_ExecutesBatch() {
        createTestOperation(tenantA, GovOperationStatus.IN_PROGRESS, GovProviderType.GST, "SCHEDULER_OP");

        when(mockAdapter.execute(any(GovIntegrationRequest.class))).thenReturn(GovIntegrationResult.builder()
                .success(true)
                .responseMetadata(Map.of("status", "PROCESSING"))
                .build());

        reconciliationScheduler.scheduledReconciliation();

        verify(mockAdapter, atLeastOnce()).execute(any());
    }
}
