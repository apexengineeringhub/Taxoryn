package com.taxoryn.module.compliance.rule.service;

import com.taxoryn.module.compliance.rule.dto.ComplianceRuleCatalogSummaryDto;
import com.taxoryn.module.compliance.rule.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.rule.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.dto.UpdateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;

import java.util.List;
import java.util.UUID;

public interface ComplianceRuleCatalogService {

    List<ComplianceRuleDto> getRules(
            ComplianceRuleDomain domain,
            ComplianceRuleFrequency frequency,
            ComplianceRuleStatus status,
            String search,
            Boolean includeSystem
    );

    ComplianceRuleDto getRuleById(UUID ruleId);

    ComplianceRuleDto getRuleByCode(String ruleCode);

    ComplianceRuleDto createCustomRule(CreateComplianceRuleRequest request);

    ComplianceRuleDto updateRule(UUID ruleId, UpdateComplianceRuleRequest request);

    void deleteRule(UUID ruleId);

    ComplianceRuleCatalogSummaryDto getCatalogSummary();
}
