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
@Schema(description = "Compliance Calendar Executive & Practice Dashboard Statistics")
public class ComplianceDashboardStatsDto {

    @Schema(description = "Count of compliance obligations due today", example = "5")
    private long dueTodayCount;

    @Schema(description = "Count of compliance obligations due this week", example = "18")
    private long dueThisWeekCount;

    @Schema(description = "Count of overdue unfulfilled compliance obligations", example = "4")
    private long overdueCount;

    @Schema(description = "Count of completed compliance obligations", example = "42")
    private long completedCount;

    @Schema(description = "Total active compliance obligations tracked", example = "69")
    private long totalActiveCount;

    @Schema(description = "Breakdown of active obligations by compliance domain (GST, ITR, TDS, OTHER)")
    private Map<String, Long> countByType;

    @Schema(description = "Obligations due today")
    private List<ComplianceObligationDto> dueTodayList;

    @Schema(description = "Upcoming obligations for the next 7 days")
    private List<ComplianceObligationDto> upcomingList;

    @Schema(description = "Overdue obligations requiring immediate action")
    private List<ComplianceObligationDto> overdueList;
}
