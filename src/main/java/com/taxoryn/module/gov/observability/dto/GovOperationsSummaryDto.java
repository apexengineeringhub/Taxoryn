package com.taxoryn.module.gov.observability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Tenant-scoped operational summary for government integration operations and background queues.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Operational summary of government integration operations and queue depths")
public class GovOperationsSummaryDto {

    private UUID organizationId;
    private long totalOperations;
    private long createdOperations;
    private long inProgressOperations;
    private long retryingOperations;
    private long succeededOperations;
    private long failedOperations;
    private long cancelledOperations;

    // Outbox stats
    private long pendingOutboxEvents;
    private long processingOutboxEvents;
    private long completedOutboxEvents;
    private long failedOutboxEvents;
    private long cancelledOutboxEvents;

    // Provider status
    private Map<String, Boolean> providerAvailability;

    private Instant generatedAt;
}
