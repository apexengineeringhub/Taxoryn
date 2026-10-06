package com.taxoryn.module.compliance.obligation.dto;

import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for listing compliance obligations")
public class ComplianceObligationFilterParams {

    @Schema(description = "Filter by obligation status", example = "UPCOMING")
    private ComplianceObligationStatus status;

    @Schema(description = "Filter by statutory domain", example = "GST")
    private ComplianceRuleDomain domain;

    @Schema(description = "Filter by period type", example = "MONTH")
    private CompliancePeriodType periodType;

    @Schema(description = "Filter by period key", example = "2026-09")
    private String periodKey;

    @Schema(description = "Filter by rule code", example = "GST_GSTR3B_MONTHLY")
    private String ruleCode;
}
