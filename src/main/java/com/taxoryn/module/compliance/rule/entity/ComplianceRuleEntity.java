package com.taxoryn.module.compliance.rule.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Compliance Rule Entity.
 * Represents a reusable statutory or practice compliance requirement in the Compliance Rule Catalog.
 */
@Entity(name = "ComplianceRuleCatalogEntity")
@Table(name = "compliance_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRuleEntity extends AuditableEntity {

    /**
     * Tenant organization ID. NULL indicates a global built-in system rule.
     */
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "name")
    private String name;

    @Column(name = "compliance_type", length = 50)
    private String complianceType;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(name = "is_active")
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 50)
    private ComplianceRuleDomain domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 50)
    private ComplianceRuleFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 50)
    private CompliancePeriodType periodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplianceRuleStatus status = ComplianceRuleStatus.ACTIVE;

    @Column(name = "is_system_rule", nullable = false)
    @Builder.Default
    private boolean isSystemRule = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "statutory_act", length = 255)
    private String statutoryAct;

    @Column(name = "statutory_section", length = 255)
    private String statutorySection;

    @Column(name = "statutory_form_code", length = 100)
    private String statutoryFormCode;

    @Column(name = "penalty_details", columnDefinition = "TEXT")
    private String penaltyDetails;

    // --- Due Date Rule Metadata ---

    @Enumerated(EnumType.STRING)
    @Column(name = "due_date_rule_type", nullable = false, length = 50)
    @Builder.Default
    private DueDateRuleType dueDateRuleType = DueDateRuleType.DAY_OF_FOLLOWING_MONTH;

    @Column(name = "due_day_offset")
    private Integer dueDayOffset;

    @Column(name = "due_month_offset")
    private Integer dueMonthOffset;

    @Column(name = "fixed_month")
    private Integer fixedMonth;

    @Column(name = "fixed_day")
    private Integer fixedDay;

    @Column(name = "statutory_grace_days", nullable = false)
    @Builder.Default
    private int statutoryGraceDays = 0;

    @Column(name = "due_date_description", length = 255)
    private String dueDateDescription;

    // --- Applicability Criteria (Metadata for Future Evaluation) ---

    @Column(name = "required_module", length = 50)
    private String requiredModule;

    @Column(name = "applicable_entity_types", columnDefinition = "TEXT")
    private String applicableEntityTypes;

    @Column(name = "applicable_gst_registration_types", columnDefinition = "TEXT")
    private String applicableGstRegistrationTypes;

    @Column(name = "applicable_filing_frequencies", columnDefinition = "TEXT")
    private String applicableFilingFrequencies;

    @Column(name = "requires_tax_audit")
    private Boolean requiresTaxAudit;

    @Column(name = "requires_transfer_pricing")
    private Boolean requiresTransferPricing;

    @Column(name = "requires_tds_deductor")
    private Boolean requiresTdsDeductor;

    @Column(name = "requires_mca_filing")
    private Boolean requiresMcaFiling;

    // --- Linking references ---

    @Column(name = "default_work_template_code", length = 100)
    private String defaultWorkTemplateCode;

    // --- Statutory Validity Window ---

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void syncLegacyColumns() {
        if (this.dueDay == null) {
            this.dueDay = this.dueDayOffset != null ? this.dueDayOffset : (this.fixedDay != null ? this.fixedDay : 20);
        }
        if (this.dueMonthOffset == null) {
            this.dueMonthOffset = 1;
        }
        if (this.ruleName != null && this.name == null) {
            this.name = this.ruleName;
        }
        if (this.complianceType == null && this.domain != null) {
            switch (this.domain) {
                case GST -> this.complianceType = "GST";
                case TDS -> this.complianceType = "TDS";
                case INCOME_TAX -> this.complianceType = "ITR";
                case MCA_ROC -> this.complianceType = "ROC";
                default -> this.complianceType = "OTHER";
            }
        }
        if (this.isActive == null) {
            this.isActive = (this.status == ComplianceRuleStatus.ACTIVE);
        }
    }
}
