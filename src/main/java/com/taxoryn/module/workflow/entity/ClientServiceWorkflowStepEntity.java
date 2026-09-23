package com.taxoryn.module.workflow.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.workflow.model.ServiceWorkType;
import com.taxoryn.module.workflow.model.StepStatus;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Instantiated workflow step belonging to a ClientServiceWorkflow instance.
 */
@Entity
@Table(
        name = "client_service_workflow_steps",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_csws_workflow_sequence", columnNames = {"workflow_id", "sequence"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceWorkflowStepEntity extends TenantAuditableEntity {

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", insertable = false, updatable = false)
    private ClientServiceWorkflowEntity workflow;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 50)
    private ServiceWorkType workType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    private boolean mandatory = true;

    @Column(name = "requires_client_input", nullable = false)
    @Builder.Default
    private boolean requiresClientInput = false;

    @Column(name = "requires_review", nullable = false)
    @Builder.Default
    private boolean requiresReview = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
