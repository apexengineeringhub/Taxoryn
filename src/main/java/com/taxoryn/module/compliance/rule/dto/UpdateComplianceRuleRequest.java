package com.taxoryn.module.compliance.rule.dto;

import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for updating an existing compliance rule in the catalog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplianceRuleRequest {

    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    private String ruleName;

    private ComplianceRuleDomain domain;
    private ComplianceRuleFrequency frequency;
    private CompliancePeriodType periodType;
    private ComplianceRuleStatus status;

    private String description;
    private String statutoryAct;
    private String statutorySection;
    private String statutoryFormCode;
    private String penaltyDetails;

    // Due date metadata
    private DueDateRuleType dueDateRuleType;
    private Integer dueDayOffset;
    private Integer dueMonthOffset;
    private Integer fixedMonth;
    private Integer fixedDay;
    private Integer statutoryGraceDays;
    private String dueDateDescription;

    // Applicability metadata
    private String requiredModule;
    private String applicableEntityTypes;
    private String applicableGstRegistrationTypes;
    private String applicableFilingFrequencies;
    private Boolean requiresTaxAudit;
    private Boolean requiresTransferPricing;
    private Boolean requiresTdsDeductor;
    private Boolean requiresMcaFiling;

    // Linking & validity
    private String defaultWorkTemplateCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
