package com.taxoryn.module.gov.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.gov.exception.GovStateTransitionException;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
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
 * Multi-tenant persisted record for government integration operations.
 * Tracks lifecycle, idempotency key, retry attempts, and outcome metadata.
 */
@Entity
@Table(name = "gov_integration_operations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovIntegrationOperationEntity extends TenantAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private GovProviderType providerType;

    @Column(name = "operation_type", nullable = false, length = 100)
    private String operationType;

    @Column(name = "business_entity_type", length = 100)
    private String businessEntityType;

    @Column(name = "business_entity_id")
    private UUID businessEntityId;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GovOperationStatus status = GovOperationStatus.CREATED;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code", length = 50)
    private GovErrorCode errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "provider_reference_id", length = 255)
    private String providerReferenceId;

    @Column(name = "request_metadata", columnDefinition = "TEXT")
    private String requestMetadata;

    @Column(name = "response_metadata", columnDefinition = "TEXT")
    private String responseMetadata;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_reconciled_at")
    private Instant lastReconciledAt;

    @Column(name = "next_reconciliation_at")
    private Instant nextReconciliationAt;

    @Column(name = "reconciliation_attempt_count", nullable = false)
    @Builder.Default
    private int reconciliationAttemptCount = 0;

    /**
     * Validates and applies a state transition on the operation.
     */
    public void transitionTo(GovOperationStatus targetStatus) {
        if (this.status == targetStatus) {
            return;
        }

        boolean valid = switch (this.status) {
            case CREATED -> targetStatus == GovOperationStatus.IN_PROGRESS
                    || targetStatus == GovOperationStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == GovOperationStatus.SUCCEEDED
                    || targetStatus == GovOperationStatus.FAILED
                    || targetStatus == GovOperationStatus.RETRYING
                    || targetStatus == GovOperationStatus.CANCELLED;
            case RETRYING -> targetStatus == GovOperationStatus.IN_PROGRESS
                    || targetStatus == GovOperationStatus.FAILED
                    || targetStatus == GovOperationStatus.CANCELLED;
            case FAILED -> targetStatus == GovOperationStatus.IN_PROGRESS
                    || targetStatus == GovOperationStatus.RETRYING;
            case SUCCEEDED, CANCELLED -> false; // Terminal states cannot be transitioned
        };

        if (!valid) {
            throw new GovStateTransitionException(this.status, targetStatus);
        }

        this.status = targetStatus;
        if (targetStatus == GovOperationStatus.SUCCEEDED || targetStatus == GovOperationStatus.FAILED || targetStatus == GovOperationStatus.CANCELLED) {
            this.completedAt = Instant.now();
        }
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public void recordReconciliationAttempt(Instant nextReconcileAt) {
        this.reconciliationAttemptCount++;
        this.lastReconciledAt = Instant.now();
        this.nextReconciliationAt = nextReconcileAt;
    }
}
