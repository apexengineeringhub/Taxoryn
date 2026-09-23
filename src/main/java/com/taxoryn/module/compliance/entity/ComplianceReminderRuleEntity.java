package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceReminderType;
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
@Table(name = "compliance_reminder_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReminderRuleEntity extends AuditableEntity {

    @Column(name = "organization_id")
    private UUID organizationId; // null indicates system default

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 50)
    private ComplianceObligationType obligationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50)
    private ComplianceReminderType reminderType;

    @Column(name = "days_offset", nullable = false)
    private Integer daysOffset; // negative for before due date, 0 on due date, positive for overdue

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
