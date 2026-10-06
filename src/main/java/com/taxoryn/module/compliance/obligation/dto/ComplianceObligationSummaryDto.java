package com.taxoryn.module.compliance.obligation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated compliance obligation summary statistics for a client")
public class ComplianceObligationSummaryDto {

    @Schema(description = "Total number of obligations tracked", example = "12")
    private long totalCount;

    @Schema(description = "Number of open / upcoming obligations", example = "4")
    private long openCount;

    @Schema(description = "Number of in-progress obligations", example = "2")
    private long inProgressCount;

    @Schema(description = "Number of completed obligations", example = "5")
    private long completedCount;

    @Schema(description = "Number of cancelled obligations", example = "1")
    private long cancelledCount;

    @Schema(description = "Counts broken down by statutory domain (e.g. GST, TDS, INCOME_TAX)")
    private Map<String, Long> byDomain;

    @Schema(description = "Counts broken down by obligation lifecycle status")
    private Map<String, Long> byStatus;
}
