package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "High-Level Practice Operations Overview KPIs")
public class PracticeDashboardOverviewDto {

    @Schema(description = "Total number of clients visible within scope", example = "45")
    @Builder.Default
    private long totalClients = 0;

    @Schema(description = "Total active clients", example = "42")
    @Builder.Default
    private long activeClients = 0;

    @Schema(description = "Active client engagements", example = "38")
    @Builder.Default
    private long activeEngagements = 0;

    @Schema(description = "Open compliance obligations across GST, ITR, TDS, and other services", example = "65")
    @Builder.Default
    private long openComplianceObligations = 0;

    @Schema(description = "Overdue compliance obligations past statutory deadline", example = "4")
    @Builder.Default
    private long overdueComplianceObligations = 0;

    @Schema(description = "Open work items (TODO, IN_PROGRESS, BLOCKED)", example = "28")
    @Builder.Default
    private long openWorkItems = 0;

    @Schema(description = "Overdue work items", example = "3")
    @Builder.Default
    private long overdueWorkItems = 0;

    @Schema(description = "Pending actionable tasks", example = "52")
    @Builder.Default
    private long pendingTasks = 0;

    @Schema(description = "Overdue tasks", example = "6")
    @Builder.Default
    private long overdueTasks = 0;

    @Schema(description = "Pending document requests awaiting client action", example = "12")
    @Builder.Default
    private long pendingDocumentRequests = 0;

    @Schema(description = "Open tax notices requiring practice intervention", example = "5")
    @Builder.Default
    private long openTaxNotices = 0;

    @Schema(description = "Total outstanding practice billing amount across clients (INR)", example = "145000.00")
    @Builder.Default
    private BigDecimal outstandingBillingAmount = BigDecimal.ZERO;

    @Schema(description = "Total invoiced amount in the selected period (INR)", example = "350000.00")
    @Builder.Default
    private BigDecimal periodInvoicedAmount = BigDecimal.ZERO;

    @Schema(description = "Total collected amount in the selected period (INR)", example = "205000.00")
    @Builder.Default
    private BigDecimal periodCollectedAmount = BigDecimal.ZERO;

    @Schema(description = "Timestamp when this overview was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
