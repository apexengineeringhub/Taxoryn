package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
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

@Entity
@Table(name = "compliance_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRuleEntity extends TenantAuditableEntity {

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type", nullable = false, length = 50)
    private ComplianceType complianceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 50)
    @Builder.Default
    private ComplianceFrequency frequency = ComplianceFrequency.MONTHLY;

    @Column(name = "due_day", nullable = false)
    private int dueDay;

    @Column(name = "due_month_offset", nullable = false)
    @Builder.Default
    private int dueMonthOffset = 1;

    @Column(name = "fixed_due_month")
    private Integer fixedDueMonth;

    @Column(name = "description_template", columnDefinition = "TEXT")
    private String descriptionTemplate;

    @Column(name = "applicable_client_types", length = 255)
    private String applicableClientTypes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system_rule", nullable = false)
    @Builder.Default
    private boolean systemRule = false;

    public enum ComplianceType {
        GST,
        ITR,
        TDS,
        OTHER,
        ROC,
        ADVANCE_TAX
    }

    public enum ComplianceFrequency {
        MONTHLY,
        QUARTERLY,
        ANNUALLY,
        ONE_TIME
    }
}
