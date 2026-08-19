package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compliance_obligations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceObligationEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type", nullable = false, length = 50)
    private ComplianceType complianceType;

    @Column(name = "period", nullable = false, length = 50)
    private String period;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplianceStatus status = ComplianceStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private CompliancePriority priority = CompliancePriority.MEDIUM;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ComplianceStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        OVERDUE,
        WAIVED,
        CANCELLED
    }

    public enum CompliancePriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
