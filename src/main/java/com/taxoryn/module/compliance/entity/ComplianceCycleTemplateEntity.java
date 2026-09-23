package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceRecurrenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "compliance_cycle_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCycleTemplateEntity extends AuditableEntity {

    @Column(name = "organization_id")
    private UUID organizationId; // null indicates system default

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ClientServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 50)
    private ComplianceObligationType obligationType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 50)
    @Builder.Default
    private ComplianceRecurrenceType recurrenceType = ComplianceRecurrenceType.MONTHLY;

    @Column(name = "default_due_day")
    private Integer defaultDueDay;

    @Column(name = "default_due_month_offset", nullable = false)
    @Builder.Default
    private Integer defaultDueMonthOffset = 1;

    @Column(name = "fixed_due_month")
    private Integer fixedDueMonth;

    @Column(name = "internal_target_offset_days", nullable = false)
    @Builder.Default
    private Integer internalTargetOffsetDays = 3;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;
}
