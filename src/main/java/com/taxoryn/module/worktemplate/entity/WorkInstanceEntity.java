package com.taxoryn.module.worktemplate.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Concrete generated occurrence of practice work for an Engagement.
 */
@Entity
@Table(
        name = "work_instances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_work_instance_period", columnNames = {"organization_id", "engagement_id", "template_id", "period_start", "period_end"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkInstanceEntity extends TenantAuditableEntity {

    @Column(name = "engagement_id", nullable = false)
    private UUID engagementId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private WorkInstanceStatus status = WorkInstanceStatus.NOT_STARTED;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "reviewer_user_id")
    private UUID reviewerUserId;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
