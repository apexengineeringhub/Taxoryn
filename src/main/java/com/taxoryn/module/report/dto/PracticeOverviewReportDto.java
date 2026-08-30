package com.taxoryn.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Overview Central Report Summary")
public class PracticeOverviewReportDto {

    @Schema(description = "Total registered clients in practice")
    private long totalClients;

    @Schema(description = "Active clients")
    private long activeClients;

    @Schema(description = "Inactive / prospect clients")
    private long inactiveClients;

    @Schema(description = "Total active tax jobs across GST, ITR, and TDS")
    private long activeTaxJobs;

    // Task Summary
    @Schema(description = "Open tasks (TODO / IN_PROGRESS)")
    private long openTasks;

    @Schema(description = "Tasks currently under review")
    private long reviewTasks;

    @Schema(description = "Overdue tasks")
    private long overdueTasks;

    @Schema(description = "Completed tasks")
    private long completedTasks;

    // Compliance Summary
    @Schema(description = "Compliance obligations due today")
    private long complianceDueToday;

    @Schema(description = "Compliance obligations due this week")
    private long complianceDueThisWeek;

    @Schema(description = "Overdue compliance obligations")
    private long complianceOverdue;

    @Schema(description = "Completed compliance obligations")
    private long complianceCompleted;

    // Documents Summary
    @Schema(description = "Document requests pending client upload")
    private long documentRequestsPending;

    @Schema(description = "Open document requests count")
    private long documentRequestsOpen;

    // Financial Summary (Null if user lacks billing permissions)
    @Schema(description = "Total invoiced amount")
    private BigDecimal totalInvoiced;

    @Schema(description = "Total collected amount")
    private BigDecimal totalCollected;

    @Schema(description = "Total outstanding amount")
    private BigDecimal totalOutstanding;

    @Schema(description = "Whether caller has permission to view financial metrics")
    private boolean hasBillingAccess;
}
