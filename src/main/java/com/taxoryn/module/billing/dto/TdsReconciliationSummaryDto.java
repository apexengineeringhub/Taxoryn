package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice TDS Credit Reconciliation Summary & Breakdown")
public class TdsReconciliationSummaryDto {

    // Distinct aggregates separating deduction, cash, and balances
    private BigDecimal totalGrossInvoiced;
    private BigDecimal totalCashReceived;
    private BigDecimal totalReceivableBalance;
    private BigDecimal totalTdsReported;
    private BigDecimal totalTdsMatched;
    private BigDecimal totalTdsUnmatched;
    private BigDecimal totalTdsDiscrepancy;

    // Counts
    private long totalInvoicesCount;
    private long matchedCount;
    private long discrepancyCount;
    private long unmatchedInvoicesCount;
    private long unmatchedCreditsCount;

    // Detailed lines
    private List<TdsReconciliationItemDto> matchedItems;
    private List<TdsReconciliationItemDto> discrepancyItems;
    private List<TdsReconciliationItemDto> unmatchedInvoices;
    private List<TdsCreditDto> unmatchedCredits;
}
