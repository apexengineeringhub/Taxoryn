package com.taxoryn.module.gov.outbox.processor;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.handler.GovOutboxHandler;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Executes a single claimed outbox event in an isolated transaction with tenant context propagation,
 * retry backoff calculation, and sanitized outcome recording.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovOutboxEventExecutor {

    private final GovOutboxEventRepository outboxRepository;
    private final List<GovOutboxHandler> handlers;
    private final AuditService auditService;
    private final com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics reliabilityMetrics;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean executeClaimedEvent(UUID eventId) {
        Optional<GovOutboxEventEntity> eventOpt = outboxRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return false;
        }

        GovOutboxEventEntity event = eventOpt.get();
        if (event.getStatus() != GovOutboxStatus.PROCESSING) {
            return false;
        }

        UUID previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(event.getOrganizationId());

            GovOutboxHandler handler = handlers.stream()
                    .filter(h -> h.supports(event.getEventType()))
                    .findFirst()
                    .orElse(null);

            if (handler == null) {
                String error = "No registered handler for outbox event type: " + event.getEventType();
                log.error("[GOV_OUTBOX_NO_HANDLER] eventId={}, type={}", event.getId(), event.getEventType());
                event.recordFailure("NO_HANDLER", error, false, null);
                outboxRepository.save(event);
                reliabilityMetrics.recordOutboxFailed();
                return false;
            }

            log.info("[GOV_OUTBOX_DISPATCH_START] eventId={}, type={}, attempt={}/{}",
                    event.getId(), event.getEventType(), event.getAttemptCount() + 1, event.getMaxAttempts());

            handler.handle(event);

            event.markCompleted();
            outboxRepository.save(event);
            reliabilityMetrics.recordOutboxCompleted();

            auditService.logEvent(
                    event.getOrganizationId(),
                    null,
                    "GOV_OUTBOX_EVENT_COMPLETED",
                    "GovOutboxEvent",
                    event.getId().toString(),
                    "PROCESSING",
                    "COMPLETED"
            );

            log.info("[GOV_OUTBOX_DISPATCH_SUCCESS] eventId={}, type={}", event.getId(), event.getEventType());
            return true;
        } catch (Exception ex) {
            log.warn("[GOV_OUTBOX_DISPATCH_FAILED] eventId={}, type={}, error={}",
                    event.getId(), event.getEventType(), ex.getMessage());

            String errorCode = "EXECUTION_ERROR";
            if (ex instanceof GovIntegrationException gie && gie.getErrorCode() != null) {
                errorCode = gie.getErrorCode().name();
            }

            String sanitizedMsg = GovReliabilitySanitizer.sanitizeString(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());

            int nextAttempt = event.getAttemptCount() + 1;
            boolean willRetry = nextAttempt < event.getMaxAttempts();
            Instant nextAvailableAt = null;

            if (willRetry) {
                // Exponential backoff: 2^attempt * 2 seconds (e.g., attempt 0 -> 2s, attempt 1 -> 4s, attempt 2 -> 8s)
                long backoffSec = (long) Math.min(300, Math.pow(2, event.getAttemptCount()) * 2);
                nextAvailableAt = Instant.now().plus(Duration.ofSeconds(backoffSec));
            }

            event.recordFailure(errorCode, sanitizedMsg, willRetry, nextAvailableAt);
            outboxRepository.save(event);
            reliabilityMetrics.recordOutboxFailed();

            auditService.logEvent(
                    event.getOrganizationId(),
                    null,
                    willRetry ? "GOV_OUTBOX_EVENT_RETRY_SCHEDULED" : "GOV_OUTBOX_EVENT_FAILED",
                    "GovOutboxEvent",
                    event.getId().toString(),
                    "PROCESSING",
                    event.getStatus().name()
            );

            return false;
        } finally {
            if (previousTenant != null) {
                TenantContext.setTenantId(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean resetStaleEvent(UUID eventId) {
        Optional<GovOutboxEventEntity> eventOpt = outboxRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return false;
        }

        GovOutboxEventEntity event = eventOpt.get();
        if (event.getStatus() != GovOutboxStatus.PROCESSING) {
            return false;
        }

        log.warn("[GOV_OUTBOX_STALE_RESET] Resetting stale processing event: id={}, lockedAt={}",
                event.getId(), event.getLockedAt());

        int nextAttempt = event.getAttemptCount() + 1;
        if (nextAttempt >= event.getMaxAttempts()) {
            event.recordFailure("STALE_TIMEOUT_EXHAUSTED", "Operation lock timed out and exhausted max attempts", false, null);
        } else {
            event.resetStaleLock();
        }
        outboxRepository.save(event);
        return true;
    }
}
