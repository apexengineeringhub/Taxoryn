package com.taxoryn.module.compliance.applicability.dto;

import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Result DTO of an individual compliance rule applicability evaluation for a client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evaluated compliance rule applicability result with explainability reason")
public class EvaluatedRuleApplicabilityDto {

    @Schema(description = "Compliance Rule ID")
    private UUID ruleId;

    @Schema(description = "Unique Statutory or Custom Rule Code", example = "GST_GSTR3B_MONTHLY")
    private String ruleCode;

    @Schema(description = "Human-readable Rule Name", example = "GSTR-3B Monthly Return & Tax Settlement")
    private String ruleName;

    @Schema(description = "Compliance Domain", example = "GST")
    private ComplianceRuleDomain domain;

    @Schema(description = "Filing / Compliance Frequency", example = "MONTHLY")
    private ComplianceRuleFrequency frequency;

    @Schema(description = "Statutory Period Type", example = "MONTH")
    private CompliancePeriodType periodType;

    @Schema(description = "Whether this is a standard system rule or custom practice rule")
    private boolean isSystemRule;

    @Schema(description = "Applicability Result State", example = "APPLICABLE")
    private ApplicabilityResultState result;

    @Schema(description = "Explainable Reason for the result", example = "GST compliance is enabled with REGULAR registration and MONTHLY filing frequency.")
    private String reason;

    @Schema(description = "Statutory Act", example = "Central Goods and Services Tax Act, 2017")
    private String statutoryAct;

    @Schema(description = "Statutory Section", example = "Section 39")
    private String statutorySection;

    @Schema(description = "Statutory Form Code", example = "GSTR-3B")
    private String statutoryFormCode;

    @Schema(description = "Due date description template", example = "20th of following month")
    private String dueDateDescription;

    @Schema(description = "Rule version evaluated", example = "0")
    private long ruleVersion;

    @Schema(description = "Timestamp when evaluation occurred")
    private Instant evaluatedAt;
}
