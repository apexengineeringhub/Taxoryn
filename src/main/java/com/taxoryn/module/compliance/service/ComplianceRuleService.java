package com.taxoryn.module.compliance.service;

import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.compliance.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ComplianceRuleService {

    ComplianceRuleDto createCustomRule(CreateComplianceRuleRequest request);

    List<ComplianceRuleDto> getActiveRules();

    ComplianceRuleDto getRuleById(UUID id);

    LocalDate calculateDueDate(ComplianceRuleEntity rule, String period);

    String formatTitle(ComplianceRuleEntity rule, String period);

    String formatDescription(ComplianceRuleEntity rule, String period);

    boolean isRuleApplicableToClient(ComplianceRuleEntity rule, ClientEntity client);
}
