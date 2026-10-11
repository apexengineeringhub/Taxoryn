package com.taxoryn.module.compliance.health.dto;

import com.taxoryn.module.compliance.health.model.ComplianceActionCode;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSeverity;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSignalCode;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic compliance risk signal with full explanation and context")
public class ComplianceRiskSignalDto {

    @Schema(description = "Standardized signal code")
    private ComplianceRiskSignalCode signalCode;

    @Schema(description = "Deterministic severity level")
    private ComplianceRiskSeverity severity;

    @Schema(description = "Numeric priority for sorting (higher = more urgent)")
    private int priority;

    @Schema(description = "Short human-readable title of the signal")
    private String title;

    @Schema(description = "Detailed explanation of why this risk was flagged")
    private String explanation;

    @Schema(description = "Target client UUID")
    private UUID clientId;

    @Schema(description = "Related obligation UUID, if applicable")
    private UUID obligationId;

    @Schema(description = "Statutory compliance rule code, if applicable")
    private String ruleCode;

    @Schema(description = "Compliance domain (e.g. GST, TDS, INCOME_TAX)")
    private ComplianceRuleDomain domain;

    @Schema(description = "Period key (e.g. 2026-09), if applicable")
    private String periodKey;

    @Schema(description = "Statutory or effective due date, if applicable")
    private LocalDate dueDate;

    @Schema(description = "Work instance UUID, if work exists")
    private UUID workInstanceId;

    @Schema(description = "Recommended action code to mitigate this risk")
    private ComplianceActionCode actionCode;

    @Schema(description = "Recommended action title")
    private String recommendedAction;

    @Schema(description = "Safe metadata without sensitive credentials")
    private Map<String, Object> metadata;
}
