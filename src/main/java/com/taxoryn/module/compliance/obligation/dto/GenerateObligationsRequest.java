package com.taxoryn.module.compliance.obligation.dto;

import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate compliance obligations for a client and period")
public class GenerateObligationsRequest {

    @NotNull(message = "Period type is required")
    @Schema(description = "Compliance period type", example = "MONTH")
    private CompliancePeriodType periodType;

    @NotBlank(message = "Period key is required")
    @Schema(description = "Deterministic period key (e.g. '2026-09', '2026-Q2', '2026-27')", example = "2026-09")
    private String periodKey;

    @Schema(description = "Optional filter to generate only rules in this domain", example = "GST")
    private ComplianceRuleDomain domain;

    @Schema(description = "Optional filter to generate only a specific rule code", example = "GST_GSTR3B_MONTHLY")
    private String ruleCode;
}
