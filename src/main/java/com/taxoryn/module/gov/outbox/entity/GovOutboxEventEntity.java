package com.taxoryn.module.gov.outbox.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted transactional outbox record for asynchronous government operations.
 * Guaranteed to commit in the same transaction as domain state mutations.
 */
@Entity
@Table(name = "gov_outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovOutboxEventEntity extends TenantAuditableEntity {

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", length = 50)
    private GovProviderType providerType;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GovOutboxStatus status = GovOutboxStatus.PENDING;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 3;

    @Column(name = "available_at", nullable = false)
    @Builder.Default
    private Instant availableAt = Instant.now();

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    /**
     * Marks the outbox event as currently being processed and acquires a lock timestamp.
     */
    public void markProcessing() {
        this.status = GovOutboxStatus.PROCESSING;
        this.lockedAt = Instant.now();
    }

    /**
     * Marks the outbox event as completed successfully and releases lock.
     */
    public void markCompleted() {
        this.status = GovOutboxStatus.COMPLETED;
        this.processedAt = Instant.now();
        this.lockedAt = null;
    }

    /**
     * Records an execution failure, incrementing attempt count and determining retry eligibility.
     */
    public void recordFailure(String errorCode, String errorMessage, boolean willRetry, Instant nextAvailableAt) {
        this.attemptCount++;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage;
        this.lockedAt = null;

        if (willRetry && this.attemptCount < this.maxAttempts) {
            this.status = GovOutboxStatus.PENDING;
            this.availableAt = nextAvailableAt != null ? nextAvailableAt : Instant.now();
        } else {
            this.status = GovOutboxStatus.FAILED;
            this.processedAt = Instant.now();
        }
    }

    /**
     * Resets a stale lock back to PENDING.
     */
    public void resetStaleLock() {
        this.status = GovOutboxStatus.PENDING;
        this.lockedAt = null;
        this.availableAt = Instant.now();
    }

    /**
     * Explicitly cancels the event.
     */
    public void markCancelled() {
        this.status = GovOutboxStatus.CANCELLED;
        this.processedAt = Instant.now();
        this.lockedAt = null;
    }
}
