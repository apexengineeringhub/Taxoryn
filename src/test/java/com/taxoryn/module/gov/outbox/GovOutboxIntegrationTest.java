package com.taxoryn.module.gov.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEnqueueRequest;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEventDto;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.handler.GovOutboxHandler;
import com.taxoryn.module.gov.outbox.model.GovOutboxEventType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.processor.GovOutboxProcessor;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.outbox.service.GovOutboxService;
import com.taxoryn.module.gov.reliability.service.GovOperationRecoveryService;
import com.taxoryn.module.gov.reliability.service.GovReliabilityService;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class GovOutboxIntegrationTest {

    @Autowired
    private GovOutboxService outboxService;

    @Autowired
    private GovOutboxProcessor outboxProcessor;

    @Autowired
    private GovOutboxEventRepository outboxRepository;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private GovReliabilityService reliabilityService;

    @MockBean
    private GovOperationRecoveryService recoveryService;

    @MockBean
    private AuditService auditService;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = createTestOrganization("Org-Alpha").getId();
        tenantB = createTestOrganization("Org-Beta").getId();
        TenantContext.setTenantId(tenantA);
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

    @Test
    @DisplayName("Enqueue outbox event persists in PENDING status with correct tenant scoping")
    void testEnqueueOutboxEvent_PersistsInPendingStatus() {
        GovOutboxEnqueueRequest request = GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .aggregateType("TestAggregate")
                .aggregateId(UUID.randomUUID())
                .providerType(GovProviderType.GST)
                .correlationId("corr-12345")
                .payload("{\"key\":\"value\"}")
                .build();

        GovOutboxEventDto dto = outboxService.enqueue(request);

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getOrganizationId()).isEqualTo(tenantA);
        assertThat(dto.getStatus()).isEqualTo(GovOutboxStatus.PENDING);
        assertThat(dto.getAttemptCount()).isEqualTo(0);
        assertThat(dto.getMaxAttempts()).isEqualTo(3);
        assertThat(dto.getAvailableAt()).isNotNull();

        Optional<GovOutboxEventEntity> saved = outboxRepository.findById(dto.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getCorrelationId()).isEqualTo("corr-12345");
    }

    @Test
    @DisplayName("Enqueue outbox event sanitizes sensitive secrets in payload")
    void testEnqueueOutboxEvent_SanitizesSensitiveSecrets() {
        GovOutboxEnqueueRequest request = GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .providerType(GovProviderType.INCOME_TAX)
                .correlationId("corr-sec-test")
                .payload("password=SecretSuperPass123&token=eyJhbGciOiJIUzI1NiJ9")
                .build();

        GovOutboxEventDto dto = outboxService.enqueue(request);

        assertThat(dto.getPayload()).doesNotContain("SecretSuperPass123");
        assertThat(dto.getPayload()).contains("password=[REDACTED]");
    }

    @Test
    @DisplayName("Schedule operation dispatch helper creates outbox event linked to operation")
    void testScheduleOperationDispatch() {
        UUID opId = UUID.randomUUID();
        GovOutboxEventDto dto = outboxService.scheduleOperationDispatch(
                opId,
                GovProviderType.TRACES,
                "corr-tds-001",
                "{\"tan\":\"BLRP12345D\"}"
        );

        assertThat(dto.getOperationId()).isEqualTo(opId);
        assertThat(dto.getEventType()).isEqualTo(GovOutboxEventType.GOV_OPERATION_DISPATCH);
        assertThat(dto.getProviderType()).isEqualTo(GovProviderType.TRACES);
    }

    @Test
    @DisplayName("Cancel outbox event succeeds for pending event and updates status")
    void testCancelOutboxEvent_SucceedsForPendingEvent() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .build());

        GovOutboxEventDto cancelled = outboxService.cancel(dto.getId());

        assertThat(cancelled.getStatus()).isEqualTo(GovOutboxStatus.CANCELLED);
        assertThat(cancelled.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Cancel outbox event fails for terminal completed event")
    void testCancelOutboxEvent_FailsForTerminalCompletedEvent() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .build());

        GovOutboxEventEntity entity = outboxRepository.findById(dto.getId()).orElseThrow();
        entity.markCompleted();
        outboxRepository.save(entity);

        assertThatThrownBy(() -> outboxService.cancel(dto.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("terminal status");
    }

    @Test
    @DisplayName("Count pending returns accurate tenant-scoped count")
    void testCountPending_TenantScoped() {
        outboxService.enqueue(GovOutboxEnqueueRequest.builder().eventType("EVT1").build());
        outboxService.enqueue(GovOutboxEnqueueRequest.builder().eventType("EVT2").build());

        assertThat(outboxService.countPending()).isEqualTo(2);

        TenantContext.setTenantId(tenantB);
        assertThat(outboxService.countPending()).isEqualTo(0);
    }

    @Test
    @DisplayName("Find pending events ignores future delayed events")
    void testFindPendingEvents_IgnoresFutureDelayedEvents() {
        // Enqueue 1 immediate event
        outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType("IMMEDIATE")
                .build());

        // Enqueue 1 future delayed event
        outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType("FUTURE")
                .delay(Duration.ofHours(1))
                .build());

        List<GovOutboxEventEntity> pending = outboxRepository.findPendingEvents(
                GovOutboxStatus.PENDING,
                Instant.now(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("IMMEDIATE");
    }

    @Test
    @DisplayName("Process pending batch claims and dispatches successfully")
    void testProcessPendingBatch_Success() {
        GovIntegrationOperationEntity op = GovIntegrationOperationEntity.builder()
                .providerType(GovProviderType.GST)
                .operationType("FETCH_STATUS")
                .correlationId("corr-process-1")
                .idempotencyKey("idemp-process-1")
                .status(GovOperationStatus.CREATED)
                .build();
        op.setOrganizationId(tenantA);
        op = operationRepository.save(op);

        when(reliabilityService.executeWithRetry(any(GovIntegrationRequest.class)))
                .thenReturn(GovIntegrationResult.builder()
                        .operationId(op.getId())
                        .organizationId(tenantA)
                        .providerType(GovProviderType.GST)
                        .operationType("FETCH_STATUS")
                        .correlationId("corr-process-1")
                        .idempotencyKey("idemp-process-1")
                        .status(GovOperationStatus.SUCCEEDED)
                        .success(true)
                        .build());

        outboxService.scheduleOperationDispatch(op.getId(), GovProviderType.GST, "corr-process-1", null);

        int processed = outboxProcessor.processPendingBatch(10);
        assertThat(processed).isEqualTo(1);

        List<GovOutboxEventEntity> completedEvents = outboxRepository.findByOrganizationIdAndStatus(tenantA, GovOutboxStatus.COMPLETED);
        assertThat(completedEvents).hasSize(1);
        assertThat(completedEvents.get(0).getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Atomic claiming prevents duplicate execution by concurrent workers")
    void testAtomicClaiming_PreventsDuplicates() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .build());

        Instant now = Instant.now();
        int worker1Claim = outboxRepository.claimEvent(dto.getId(), GovOutboxStatus.PENDING, GovOutboxStatus.PROCESSING, now, now);
        int worker2Claim = outboxRepository.claimEvent(dto.getId(), GovOutboxStatus.PENDING, GovOutboxStatus.PROCESSING, now, now);

        assertThat(worker1Claim).isEqualTo(1);
        assertThat(worker2Claim).isEqualTo(0);
    }

    @Test
    @DisplayName("Execution failure retries with exponential backoff when attempts remain")
    void testExecutionFailure_Retries() {
        GovIntegrationOperationEntity op = GovIntegrationOperationEntity.builder()
                .providerType(GovProviderType.GST)
                .operationType("FETCH_STATUS")
                .correlationId("corr-retry-test")
                .idempotencyKey("idemp-retry-test")
                .status(GovOperationStatus.CREATED)
                .build();
        op.setOrganizationId(tenantA);
        op = operationRepository.save(op);

        when(reliabilityService.executeWithRetry(any(GovIntegrationRequest.class)))
                .thenReturn(GovIntegrationResult.builder()
                        .operationId(op.getId())
                        .organizationId(tenantA)
                        .providerType(GovProviderType.GST)
                        .operationType("FETCH_STATUS")
                        .correlationId("corr-retry-test")
                        .idempotencyKey("idemp-retry-test")
                        .status(GovOperationStatus.FAILED)
                        .success(false)
                        .errorCode(GovErrorCode.TIMEOUT)
                        .errorMessage("Mock gateway timeout")
                        .build());

        outboxService.scheduleOperationDispatch(op.getId(), GovProviderType.GST, "corr-retry-test", null);

        int processed = outboxProcessor.processPendingBatch(10);
        assertThat(processed).isEqualTo(0); // Failed execution

        GovOutboxEventEntity event = outboxRepository.findAll().get(0);
        assertThat(event.getStatus()).isEqualTo(GovOutboxStatus.PENDING); // Scheduled for retry
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastErrorCode()).isEqualTo("TIMEOUT");
        assertThat(event.getLastErrorMessage()).contains("Mock gateway timeout");
        assertThat(event.getAvailableAt()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    @DisplayName("Execution failure transitions to FAILED when max attempts exhausted")
    void testExecutionFailure_ExhaustedAttempts() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType("UNSUPPORTED_UNKNOWN_TYPE")
                .maxAttempts(1)
                .build());

        outboxProcessor.processPendingBatch(10);

        GovOutboxEventEntity event = outboxRepository.findById(dto.getId()).orElseThrow();
        assertThat(event.getStatus()).isEqualTo(GovOutboxStatus.FAILED);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastErrorCode()).isEqualTo("NO_HANDLER");
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Stale lock recovery resets stuck processing event back to PENDING")
    void testStaleLockRecovery_ResetsToPending() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .maxAttempts(3)
                .build());

        GovOutboxEventEntity entity = outboxRepository.findById(dto.getId()).orElseThrow();
        entity.setStatus(GovOutboxStatus.PROCESSING);
        entity.setLockedAt(Instant.now().minus(Duration.ofMinutes(10)));
        outboxRepository.save(entity);

        int recovered = outboxProcessor.recoverStaleLocks(Duration.ofMinutes(5), 10);
        assertThat(recovered).isEqualTo(1);

        GovOutboxEventEntity recoveredEntity = outboxRepository.findById(dto.getId()).orElseThrow();
        assertThat(recoveredEntity.getStatus()).isEqualTo(GovOutboxStatus.PENDING);
        assertThat(recoveredEntity.getLockedAt()).isNull();
    }

    @Test
    @DisplayName("Stale lock recovery marks FAILED when max attempts exhausted")
    void testStaleLockRecovery_ExhaustedAttempts() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .maxAttempts(1)
                .build());

        GovOutboxEventEntity entity = outboxRepository.findById(dto.getId()).orElseThrow();
        entity.setStatus(GovOutboxStatus.PROCESSING);
        entity.setLockedAt(Instant.now().minus(Duration.ofMinutes(10)));
        entity.setAttemptCount(0);
        outboxRepository.save(entity);

        int recovered = outboxProcessor.recoverStaleLocks(Duration.ofMinutes(5), 10);
        assertThat(recovered).isEqualTo(1);

        GovOutboxEventEntity failedEntity = outboxRepository.findById(dto.getId()).orElseThrow();
        assertThat(failedEntity.getStatus()).isEqualTo(GovOutboxStatus.FAILED);
        assertThat(failedEntity.getLastErrorCode()).isEqualTo("STALE_TIMEOUT_EXHAUSTED");
    }

    @Test
    @DisplayName("Tenant isolation guarantees Tenant A cannot view or cancel Tenant B events")
    void testTenantIsolation_CrossTenantAccessForbidden() {
        TenantContext.setTenantId(tenantA);
        GovOutboxEventDto dtoA = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType("TENANT_A_EVENT")
                .build());

        TenantContext.setTenantId(tenantB);
        Optional<GovOutboxEventDto> lookupFromB = outboxService.findById(dtoA.getId());
        assertThat(lookupFromB).isEmpty();

        assertThatThrownBy(() -> outboxService.cancel(dtoA.getId()))
                .isInstanceOf(com.taxoryn.core.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("FindByStatus returns only matching tenant events")
    void testFindByStatus_MatchingTenantOnly() {
        TenantContext.setTenantId(tenantA);
        outboxService.enqueue(GovOutboxEnqueueRequest.builder().eventType("EVT1").build());

        TenantContext.setTenantId(tenantB);
        outboxService.enqueue(GovOutboxEnqueueRequest.builder().eventType("EVT2").build());

        TenantContext.setTenantId(tenantA);
        List<GovOutboxEventDto> pendingA = outboxService.findByStatus(GovOutboxStatus.PENDING);
        assertThat(pendingA).hasSize(1);
        assertThat(pendingA.get(0).getEventType()).isEqualTo("EVT1");
    }

    @Test
    @DisplayName("FindByOperationId returns events linked to specified operation")
    void testFindByOperationId_ReturnsLinkedEvents() {
        UUID op1 = UUID.randomUUID();
        UUID op2 = UUID.randomUUID();

        outboxService.scheduleOperationDispatch(op1, GovProviderType.GST, "corr-op-1", null);
        outboxService.scheduleOperationDispatch(op2, GovProviderType.GST, "corr-op-2", null);

        List<GovOutboxEventDto> eventsOp1 = outboxService.findByOperationId(op1);
        assertThat(eventsOp1).hasSize(1);
        assertThat(eventsOp1.get(0).getOperationId()).isEqualTo(op1);
    }

    @Test
    @DisplayName("Custom delay sets availableAt into the future")
    void testCustomDelay_SetsAvailableAtProperly() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType("DELAYED_EVT")
                .delay(Duration.ofMinutes(15))
                .build());

        assertThat(dto.getAvailableAt()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
    }

    @Test
    @DisplayName("Multiple pending events are processed in bounded batches")
    void testMultipleEvents_ProcessedInBoundedBatch() {
        for (int i = 0; i < 5; i++) {
            outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                    .eventType("BATCH_EVT_" + i)
                    .maxAttempts(1)
                    .build());
        }

        assertThat(outboxService.countPending()).isEqualTo(5);

        // Process batch of 3
        outboxProcessor.processPendingBatch(3);

        List<GovOutboxEventEntity> all = outboxRepository.findAll();
        long failedOrDone = all.stream().filter(e -> e.getStatus() != GovOutboxStatus.PENDING).count();
        long stillPending = all.stream().filter(e -> e.getStatus() == GovOutboxStatus.PENDING).count();

        assertThat(failedOrDone).isEqualTo(3);
        assertThat(stillPending).isEqualTo(2);
    }

    @Test
    @DisplayName("Reconciliation outbox event triggers GovOperationRecoveryService")
    void testReconciliationEvent_TriggersRecoveryService() {
        UUID opId = UUID.randomUUID();
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_RECONCILIATION)
                .operationId(opId)
                .build());

        outboxProcessor.processPendingBatch(10);

        verify(recoveryService, atLeastOnce()).reconcileAmbiguousOutcome(opId);

        GovOutboxEventEntity event = outboxRepository.findById(dto.getId()).orElseThrow();
        assertThat(event.getStatus()).isEqualTo(GovOutboxStatus.COMPLETED);
    }

    @Test
    @DisplayName("Audit logs recorded on enqueue, dispatch, and cancellation")
    void testAuditing_RecordsLogs() {
        GovOutboxEventDto dto = outboxService.enqueue(GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .build());

        verify(auditService, atLeastOnce()).logEvent(
                eq(tenantA),
                any(),
                eq("GOV_OUTBOX_EVENT_ENQUEUED"),
                eq("GovOutboxEvent"),
                eq(dto.getId().toString()),
                any(),
                any()
        );

        outboxService.cancel(dto.getId());

        verify(auditService, atLeastOnce()).logEvent(
                eq(tenantA),
                any(),
                eq("GOV_OUTBOX_EVENT_CANCELLED"),
                eq("GovOutboxEvent"),
                eq(dto.getId().toString()),
                any(),
                any()
        );
    }
}
