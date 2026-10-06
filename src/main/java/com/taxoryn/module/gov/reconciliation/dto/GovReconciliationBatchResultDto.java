package com.taxoryn.module.gov.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result summary of a batch reconciliation run across multiple eligible operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovReconciliationBatchResultDto {

    private int totalEligible;
    private int totalProcessed;
    private int statusChangedCount;
    private int succeededCount;
    private int failedCount;
    private int inProgressCount;
    private int skippedCount;
    private long durationMs;

    @Builder.Default
    private List<GovReconciliationResultDto> results = new ArrayList<>();
}
