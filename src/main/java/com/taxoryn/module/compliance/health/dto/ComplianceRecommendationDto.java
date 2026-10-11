package com.taxoryn.module.compliance.health.dto;

import com.taxoryn.module.compliance.health.model.ComplianceActionCode;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSeverity;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Actionable, explainable practitioner recommendation")
public class ComplianceRecommendationDto {

    @Schema(description = "Action code")
    private ComplianceActionCode actionCode;

    @Schema(description = "Action title explaining WHAT to do")
    private String title;

    @Schema(description = "Detailed justification explaining WHY this action is recommended")
    private String explanation;

    @Schema(description = "Severity of the underlying risk")
    private ComplianceRiskSeverity severity;

    @Schema(description = "Sorting priority (higher = more urgent)")
    private int priority;

    @Schema(description = "Target client identifier")
    private UUID clientId;

    @Schema(description = "Target obligation identifier, if applicable")
    private UUID obligationId;

    @Schema(description = "Target rule code, if applicable")
    private String ruleCode;

    @Schema(description = "Domain of the requirement")
    private ComplianceRuleDomain domain;

    @Schema(description = "Target entity type (e.g. COMPLIANCE_OBLIGATION, COMPLIANCE_PROFILE, WORK_INSTANCE)")
    private String targetType;

    @Schema(description = "Target entity ID")
    private UUID targetId;

    @Schema(description = "Safe context metadata")
    private Map<String, Object> metadata;
}
