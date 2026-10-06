package com.taxoryn.module.gov.reliability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Snapshot of provider-neutral reliability metrics, outbox events, reconciliation cycles, and failure counters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Government Integration reliability metrics and counter snapshot")
public class GovReliabilityMetricsSnapshot {

    private long operationsTotal;
    private long operationsSuccess;
    private long operationsFailed;
    private long retriesTotal;
    private long timeoutsTotal;
    private long rateLimitsTotal;
    private long providerUnavailableTotal;
    private long authFailuresTotal;

    // Outbox metrics
    private long outboxEnqueuedTotal;
    private long outboxCompletedTotal;
    private long outboxFailedTotal;
    private long outboxStaleRecoveredTotal;

    // Reconciliation metrics
    private long reconciliationsAttemptedTotal;
    private long reconciliationsSucceededTotal;
    private long reconciliationsStatusChangedTotal;
    private long reconciliationsFailedTotal;

    private Instant snapshotTimestamp;
}
