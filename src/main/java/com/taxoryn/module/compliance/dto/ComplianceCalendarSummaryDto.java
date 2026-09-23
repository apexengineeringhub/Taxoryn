package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance Calendar Summary Metrics")
public class ComplianceCalendarSummaryDto {

    @Schema(description = "Count of active obligations due today", example = "4")
    private long dueTodayCount;

    @Schema(description = "Due today count alias", example = "4")
    private long dueToday;

    @Schema(description = "Count of active obligations due this week", example = "12")
    private long dueThisWeekCount;

    @Schema(description = "Due this week count alias", example = "12")
    private long dueThisWeek;

    @Schema(description = "Count of active obligations due this month", example = "45")
    private long dueThisMonthCount;

    @Schema(description = "Due this month count alias", example = "45")
    private long dueThisMonth;

    @Schema(description = "Upcoming count alias", example = "5")
    private long upcoming;

    @Schema(description = "Count of overdue obligations", example = "3")
    private long overdueCount;

    @Schema(description = "Overdue count alias", example = "3")
    private long overdue;

    @Schema(description = "Count of obligations waiting for client action", example = "7")
    private long waitingForClientCount;

    @Schema(description = "Count of obligations ready for statutory filing", example = "5")
    private long readyForFilingCount;

    @Schema(description = "Count of completed obligations in current period", example = "28")
    private long completedCount;

    @Schema(description = "Completed count alias", example = "28")
    private long completed;

    @Schema(description = "Total active tracked obligations", example = "71")
    private long totalActiveCount;

    @Schema(description = "Total obligations count alias", example = "99")
    private long totalObligations;

    @Schema(description = "Breakdown of active obligations by obligation type")
    private Map<String, Long> countByType;

    @Schema(description = "Top obligations due today")
    private List<ComplianceObligationDto> dueTodayList;

    @Schema(description = "Top upcoming obligations")
    private List<ComplianceObligationDto> upcomingList;

    @Schema(description = "Top overdue obligations")
    private List<ComplianceObligationDto> overdueList;
}
