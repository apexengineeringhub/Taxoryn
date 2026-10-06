package com.taxoryn.module.compliance.rule.dto;

import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for creating a custom compliance rule in the practice catalog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplianceRuleRequest {

    @NotBlank(message = "Rule code is required")
    @Size(max = 100, message = "Rule code must not exceed 100 characters")
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    private String ruleName;

    @NotNull(message = "Domain is required")
    private ComplianceRuleDomain domain;

    @NotNull(message = "Frequency is required")
    private ComplianceRuleFrequency frequency;

    @NotNull(message = "Period type is required")
    private CompliancePeriodType periodType;

    private String description;
    private String statutoryAct;
    private String statutorySection;
    private String statutoryFormCode;
    private String penaltyDetails;

    // Due date metadata
    @Builder.Default
    private DueDateRuleType dueDateRuleType = DueDateRuleType.DAY_OF_FOLLOWING_MONTH;
    private Integer dueDayOffset;
    private Integer dueMonthOffset;
    private Integer fixedMonth;
    private Integer fixedDay;
    private int statutoryGraceDays;
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
