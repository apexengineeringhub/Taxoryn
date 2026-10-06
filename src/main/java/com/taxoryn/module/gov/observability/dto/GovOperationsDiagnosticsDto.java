package com.taxoryn.module.gov.observability.dto;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEventDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed sanitized diagnostic representation of a specific government operation,
 * including related outbox events, lifecycle timestamps, and error classifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sanitized diagnostic information for a government integration operation")
public class GovOperationsDiagnosticsDto {

    private UUID operationId;
    private UUID organizationId;
    private GovProviderType providerType;
    private String operationType;
    private String businessEntityType;
    private UUID businessEntityId;
    private String correlationId;
    private String idempotencyKey;
    private GovOperationStatus status;
    private int attemptCount;
    private int maxAttempts;
    private GovErrorCode errorCode;
    private String errorMessage;
    private String providerReferenceId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant lastReconciledAt;
    private Instant nextReconciliationAt;
    private int reconciliationAttemptCount;

    // Associated outbox events
    private List<GovOutboxEventDto> outboxEvents;
}
