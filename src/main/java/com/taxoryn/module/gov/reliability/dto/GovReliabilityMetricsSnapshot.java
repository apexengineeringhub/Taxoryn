package com.taxoryn.module.gov.reliability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Snapshot of provider-neutral reliability metrics and failure counters.
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
    private Instant snapshotTimestamp;
}
