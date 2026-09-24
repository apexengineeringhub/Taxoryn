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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a concrete, period-specific compliance deliverable associated with a client service engagement.
 */
@Entity
@Table(name = "compliance_work_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceWorkItemEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_service_id", nullable = false)
    private UUID clientServiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 64)
    private ComplianceWorkType workType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "financial_year", length = 50)
    private String financialYear;

    @Column(name = "assessment_year", length = 50)
    private String assessmentYear;

    @Column(name = "compliance_period", length = 50)
    private String compliancePeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplianceWorkStatus status = ComplianceWorkStatus.NOT_STARTED;

    @Column(name = "statutory_due_date")
    private LocalDate statutoryDueDate;

    @Column(name = "internal_target_date")
    private LocalDate internalTargetDate;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "reviewer_employee_id")
    private UUID reviewerEmployeeId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
