package com.taxoryn.module.worktemplate.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Reusable task definition belonging to a Work Template.
 */
@Entity
@Table(
        name = "work_template_tasks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_work_template_task_seq", columnNames = {"template_id", "sequence_order"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTemplateTaskEntity extends AuditableEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", insertable = false, updatable = false)
    private WorkTemplateEntity template;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "default_assignee_role", length = 100)
    private String defaultAssigneeRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_priority", nullable = false, length = 50)
    @Builder.Default
    private TaskPriority defaultPriority = TaskPriority.MEDIUM;

    @Column(name = "relative_due_days", nullable = false)
    @Builder.Default
    private int relativeDueDays = 0;

    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    private boolean mandatory = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
