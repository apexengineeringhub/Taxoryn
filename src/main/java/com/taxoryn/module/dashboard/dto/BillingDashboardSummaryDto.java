package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Client Billing & Receivables Dashboard Metrics")
public class BillingDashboardSummaryDto {

    @Schema(description = "Total invoices created", example = "50")
    @Builder.Default
    private long totalInvoices = 0;

    @Schema(description = "Invoices in DRAFT status", example = "5")
    @Builder.Default
    private long draftInvoices = 0;

    @Schema(description = "Invoices ISSUED and awaiting payment", example = "15")
    @Builder.Default
    private long issuedInvoices = 0;

    @Schema(description = "Invoices PARTIALLY_PAID", example = "3")
    @Builder.Default
    private long partiallyPaidInvoices = 0;

    @Schema(description = "Invoices fully PAID", example = "25")
    @Builder.Default
    private long paidInvoices = 0;

    @Schema(description = "Invoices past due date with remaining balance", example = "4")
    @Builder.Default
    private long overdueInvoices = 0;

    @Schema(description = "Invoices CANCELLED", example = "2")
    @Builder.Default
    private long cancelledInvoices = 0;

    @Schema(description = "Total cumulative invoiced amount (INR)", example = "750000.00")
    @Builder.Default
    private BigDecimal totalInvoicedAmount = BigDecimal.ZERO;

    @Schema(description = "Total collected payments received (INR)", example = "550000.00")
    @Builder.Default
    private BigDecimal totalCollectedAmount = BigDecimal.ZERO;

    @Schema(description = "Total outstanding receivables balance (INR)", example = "200000.00")
    @Builder.Default
    private BigDecimal totalOutstandingAmount = BigDecimal.ZERO;

    @Schema(description = "Total overdue amount past due date (INR)", example = "45000.00")
    @Builder.Default
    private BigDecimal totalOverdueAmount = BigDecimal.ZERO;

    @Schema(description = "Invoiced amount in selected filter period (INR)", example = "125000.00")
    @Builder.Default
    private BigDecimal periodInvoicedAmount = BigDecimal.ZERO;

    @Schema(description = "Collected amount in selected filter period (INR)", example = "95000.00")
    @Builder.Default
    private BigDecimal periodCollectedAmount = BigDecimal.ZERO;

    @Schema(description = "Revenue distribution by professional service category")
    @Builder.Default
    private Map<String, BigDecimal> revenueByService = new HashMap<>();

    @Schema(description = "Timestamp when this summary was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
