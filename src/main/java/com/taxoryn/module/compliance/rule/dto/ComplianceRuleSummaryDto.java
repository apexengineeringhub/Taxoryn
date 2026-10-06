package com.taxoryn.module.compliance.rule.dto;

import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Compact summary representation of a compliance rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRuleSummaryDto {

    private UUID id;
    private String ruleCode;
    private String ruleName;
    private ComplianceRuleDomain domain;
    private ComplianceRuleFrequency frequency;
    private CompliancePeriodType periodType;
    private ComplianceRuleStatus status;
    private boolean systemRule;
    private String statutoryFormCode;
    private String dueDateDescription;
}
