package com.taxoryn.module.gov.outbox.service.impl;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.exception.GovValidationException;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEnqueueRequest;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEventDto;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxEventType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.outbox.service.GovOutboxService;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of GovOutboxService handling multi-tenant event enqueueing,
 * querying, and cancellation with payload sanitization and auditing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovOutboxServiceImpl implements GovOutboxService {

    private final GovOutboxEventRepository outboxRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public GovOutboxEventDto enqueue(GovOutboxEnqueueRequest request) {
        if (request == null) {
            throw new GovValidationException("GovOutboxEnqueueRequest must not be null");
        }
        if (request.getEventType() == null || request.getEventType().trim().isEmpty()) {
            throw new GovValidationException("eventType is mandatory for outbox enqueue");
        }

        UUID orgId = resolveTenant(request.getOrganizationId());

        String correlationId = request.getCorrelationId();
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Sanitize payload to guarantee zero secret leakage in database
        String sanitizedPayload = request.getPayload() != null
                ? GovReliabilitySanitizer.sanitizeString(request.getPayload())
                : null;

        Instant availableAt = Instant.now();
        if (request.getDelay() != null && !request.getDelay().isNegative()) {
            availableAt = availableAt.plus(request.getDelay());
        }

        int maxAttempts = (request.getMaxAttempts() != null && request.getMaxAttempts() > 0)
                ? request.getMaxAttempts()
                : 3;

        GovOutboxEventEntity entity = GovOutboxEventEntity.builder()
                .eventType(request.getEventType().trim())
                .aggregateType(request.getAggregateType())
                .aggregateId(request.getAggregateId())
                .operationId(request.getOperationId())
                .providerType(request.getProviderType())
                .correlationId(correlationId)
                .status(GovOutboxStatus.PENDING)
                .payload(sanitizedPayload)
                .attemptCount(0)
                .maxAttempts(maxAttempts)
                .availableAt(availableAt)
                .build();

        entity.setOrganizationId(orgId);
        GovOutboxEventEntity saved = outboxRepository.save(entity);

        auditService.logEvent(
                orgId,
                null,
                "GOV_OUTBOX_EVENT_ENQUEUED",
                "GovOutboxEvent",
                saved.getId().toString(),
                null,
                "Enqueued event " + saved.getEventType() + " correlation=" + saved.getCorrelationId()
        );

        log.debug("[GOV_OUTBOX_ENQUEUE] Enqueued outbox event: id={}, type={}, orgId={}, correlationId={}",
                saved.getId(), saved.getEventType(), saved.getOrganizationId(), saved.getCorrelationId());

        return toDto(saved);
    }

    @Override
    @Transactional
    public GovOutboxEventDto scheduleOperationDispatch(UUID operationId, GovProviderType providerType, String correlationId, String payloadJson) {
        if (operationId == null) {
            throw new GovValidationException("operationId is mandatory for scheduling operation dispatch");
        }

        GovOutboxEnqueueRequest request = GovOutboxEnqueueRequest.builder()
                .eventType(GovOutboxEventType.GOV_OPERATION_DISPATCH)
                .operationId(operationId)
                .aggregateType("GovIntegrationOperation")
                .aggregateId(operationId)
                .providerType(providerType)
                .correlationId(correlationId)
                .payload(payloadJson)
                .build();

        return enqueue(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GovOutboxEventDto> findById(UUID id) {
        UUID orgId = requireActiveTenant();
        return outboxRepository.findByIdAndOrganizationId(id, orgId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovOutboxEventDto> findByStatus(GovOutboxStatus status) {
        UUID orgId = requireActiveTenant();
        return outboxRepository.findByOrganizationIdAndStatus(orgId, status)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovOutboxEventDto> findByOperationId(UUID operationId) {
        UUID orgId = requireActiveTenant();
        return outboxRepository.findByOrganizationIdAndOperationId(orgId, operationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public GovOutboxEventDto cancel(UUID id) {
        UUID orgId = requireActiveTenant();
        GovOutboxEventEntity entity = outboxRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("GovOutboxEvent not found with ID: " + id));

        if (entity.getStatus() == GovOutboxStatus.COMPLETED || entity.getStatus() == GovOutboxStatus.FAILED) {
            throw new BadRequestException("Cannot cancel outbox event in terminal status: " + entity.getStatus());
        }

        entity.markCancelled();
        GovOutboxEventEntity updated = outboxRepository.save(entity);

        auditService.logEvent(
                orgId,
                null,
                "GOV_OUTBOX_EVENT_CANCELLED",
                "GovOutboxEvent",
                updated.getId().toString(),
                null,
                "Cancelled outbox event " + updated.getId()
        );

        return toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        UUID orgId = requireActiveTenant();
        return outboxRepository.countByOrganizationIdAndStatus(orgId, GovOutboxStatus.PENDING);
    }

    private UUID resolveTenant(UUID requestOrgId) {
        if (requestOrgId != null) {
            return requestOrgId;
        }
        UUID contextOrgId = TenantContext.getTenantId();
        if (contextOrgId != null) {
            return contextOrgId;
        }
        throw new UnauthorizedException("Organization context is mandatory for GovOutbox operations");
    }

    private UUID requireActiveTenant() {
        UUID contextOrgId = TenantContext.getTenantId();
        if (contextOrgId == null) {
            throw new UnauthorizedException("Tenant context is required for this operation");
        }
        return contextOrgId;
    }

    private GovOutboxEventDto toDto(GovOutboxEventEntity entity) {
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
                .payload(entity.getPayload())
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
