package com.taxoryn.module.gov.observability.service.impl;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.observability.dto.GovOperationsDiagnosticsDto;
import com.taxoryn.module.gov.observability.dto.GovOperationsSummaryDto;
import com.taxoryn.module.gov.observability.service.GovOperationsObservabilityService;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEventDto;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for operational observability, safe diagnostics, and metrics reporting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovOperationsObservabilityServiceImpl implements GovOperationsObservabilityService {

    private final GovIntegrationOperationRepository operationRepository;
    private final GovOutboxEventRepository outboxRepository;
    private final GovernmentProviderRegistry providerRegistry;
    private final GovReliabilityMetrics reliabilityMetrics;

    @Override
    @Transactional(readOnly = true)
    public GovOperationsSummaryDto getOperationsSummary(UUID organizationId) {
        UUID effectiveOrgId = resolveTenant(organizationId);

        long totalOps = operationRepository.countByOrganizationId(effectiveOrgId);
        long createdOps = operationRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOperationStatus.CREATED);
        long inProgressOps = operationRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOperationStatus.IN_PROGRESS);
        long retryingOps = operationRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOperationStatus.RETRYING);
        long succeededOps = operationRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOperationStatus.SUCCEEDED);
        long failedOps = operationRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOperationStatus.FAILED);
        long cancelledOps = operationRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOperationStatus.CANCELLED);

        long pendingOutbox = outboxRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOutboxStatus.PENDING);
        long processingOutbox = outboxRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOutboxStatus.PROCESSING);
        long completedOutbox = outboxRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOutboxStatus.COMPLETED);
        long failedOutbox = outboxRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOutboxStatus.FAILED);
        long cancelledOutbox = outboxRepository.countByOrganizationIdAndStatus(effectiveOrgId, GovOutboxStatus.CANCELLED);

        Map<String, Boolean> providerAvailability = new HashMap<>();
        for (GovProviderType type : GovProviderType.values()) {
            providerAvailability.put(type.name(), providerRegistry.isProviderSupported(type));
        }

        return GovOperationsSummaryDto.builder()
                .organizationId(effectiveOrgId)
                .totalOperations(totalOps)
                .createdOperations(createdOps)
                .inProgressOperations(inProgressOps)
                .retryingOperations(retryingOps)
                .succeededOperations(succeededOps)
                .failedOperations(failedOps)
                .cancelledOperations(cancelledOps)
                .pendingOutboxEvents(pendingOutbox)
                .processingOutboxEvents(processingOutbox)
                .completedOutboxEvents(completedOutbox)
                .failedOutboxEvents(failedOutbox)
                .cancelledOutboxEvents(cancelledOutbox)
                .providerAvailability(providerAvailability)
                .generatedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GovOperationsDiagnosticsDto getOperationDiagnostics(UUID operationId) {
        if (operationId == null) {
            throw new IllegalArgumentException("operationId must not be null");
        }

        UUID effectiveOrgId = requireActiveTenant();

        GovIntegrationOperationEntity op = operationRepository.findByIdAndOrganizationId(operationId, effectiveOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Government operation not found with ID: " + operationId));

        List<GovOutboxEventEntity> outboxEntities = outboxRepository.findByOrganizationIdAndOperationId(effectiveOrgId, operationId);
        List<GovOutboxEventDto> outboxDtos = outboxEntities.stream()
                .map(this::toOutboxDto)
                .toList();

        return GovOperationsDiagnosticsDto.builder()
                .operationId(op.getId())
                .organizationId(op.getOrganizationId())
                .providerType(op.getProviderType())
                .operationType(op.getOperationType())
                .businessEntityType(op.getBusinessEntityType())
                .businessEntityId(op.getBusinessEntityId())
                .correlationId(op.getCorrelationId())
                .idempotencyKey(op.getIdempotencyKey())
                .status(op.getStatus())
                .attemptCount(op.getAttemptCount())
                .maxAttempts(op.getMaxAttempts())
                .errorCode(op.getErrorCode())
                .errorMessage(op.getErrorMessage() != null ? GovReliabilitySanitizer.sanitizeString(op.getErrorMessage()) : null)
                .providerReferenceId(op.getProviderReferenceId())
                .createdAt(op.getCreatedAt())
                .updatedAt(op.getUpdatedAt())
                .completedAt(op.getCompletedAt())
                .lastReconciledAt(op.getLastReconciledAt())
                .nextReconciliationAt(op.getNextReconciliationAt())
                .reconciliationAttemptCount(op.getReconciliationAttemptCount())
                .outboxEvents(outboxDtos)
                .build();
    }

    @Override
    public GovReliabilityMetricsSnapshot getMetricsSnapshot() {
        return reliabilityMetrics.getSnapshot();
    }

    private UUID resolveTenant(UUID requestOrgId) {
        if (requestOrgId != null) {
            return requestOrgId;
        }
        return requireActiveTenant();
    }

    private UUID requireActiveTenant() {
        UUID contextOrgId = TenantContext.getTenantId();
        if (contextOrgId == null) {
            throw new UnauthorizedException("Active tenant context is required for government operations observability");
        }
        return contextOrgId;
    }

    private GovOutboxEventDto toOutboxDto(GovOutboxEventEntity entity) {
        return GovOutboxEventDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .eventType(entity.getEventType())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .operationId(entity.getOperationId())
                .providerType(entity.getProviderType())
                .correlationId(entity.getCorrelationId())
                .status(entity.getStatus())
                .payload(entity.getPayload()) // already sanitized on enqueue
                .attemptCount(entity.getAttemptCount())
                .maxAttempts(entity.getMaxAttempts())
                .availableAt(entity.getAvailableAt())
                .lockedAt(entity.getLockedAt())
                .processedAt(entity.getProcessedAt())
                .lastErrorCode(entity.getLastErrorCode())
                .lastErrorMessage(entity.getLastErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
