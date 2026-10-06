package com.taxoryn.module.compliance.rule.dto;

import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Full Data Transfer Object for a Compliance Rule in the Catalog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRuleDto {

    private UUID id;
    private UUID organizationId;
    private String ruleCode;
    private String ruleName;
    private ComplianceRuleDomain domain;
    private String domainDisplayName;
    private ComplianceRuleFrequency frequency;
    private CompliancePeriodType periodType;
    private ComplianceRuleStatus status;
    private boolean systemRule;
    private String description;
    private String statutoryAct;
    private String statutorySection;
    private String statutoryFormCode;
    private String penaltyDetails;

    // Due Date Metadata
    private DueDateRuleType dueDateRuleType;
    private Integer dueDayOffset;
    private Integer dueMonthOffset;
    private Integer fixedMonth;
    private Integer fixedDay;
    private int statutoryGraceDays;
    private String dueDateDescription;

    // Applicability Criteria Metadata
    private String requiredModule;
    private String applicableEntityTypes;
    private String applicableGstRegistrationTypes;
    private String applicableFilingFrequencies;
    private Boolean requiresTaxAudit;
    private Boolean requiresTransferPricing;
    private Boolean requiresTdsDeductor;
    private Boolean requiresMcaFiling;

    // Linking references
    private String defaultWorkTemplateCode;

    // Validity
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // Audit Metadata
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
