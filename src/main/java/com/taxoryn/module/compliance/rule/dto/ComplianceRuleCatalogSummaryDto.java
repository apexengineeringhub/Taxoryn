package com.taxoryn.module.compliance.rule.dto;

import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Summary metrics of the Compliance Rule Catalog for a practice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRuleCatalogSummaryDto {

    private long totalRules;
    private long activeRules;
    private long systemRules;
    private long customRules;
    private Map<ComplianceRuleDomain, Long> rulesByDomain;
}
