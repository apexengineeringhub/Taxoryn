package com.taxoryn.module.compliance.calendar.dto;

import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice-wide Compliance Deadline Radar Overview")
public class ComplianceDeadlineRadarDto {

    @Schema(description = "Reference date against which deadlines were classified", example = "2026-10-06")
    private LocalDate asOfDate;

    @Schema(description = "Overall aggregate metrics for practice deadlines")
    private ComplianceDeadlineSummaryDto summary;

    @Schema(description = "Domain-by-domain deadline breakdown (e.g. GST, TDS, INCOME_TAX, MCA_ROC)")
    private Map<ComplianceRuleDomain, ComplianceDeadlineSummaryDto> domainBreakdown;

    @Schema(description = "Top prioritized active deadlines requiring immediate attention")
    private List<ComplianceDeadlineDto> topDeadlines;
}
