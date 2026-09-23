package com.taxoryn.module.workflow.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a concrete operational workflow instance executed for a client service engagement in a given period.
 */
@Entity
@Table(
        name = "client_service_workflows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_csw_tenant_service_period", columnNames = {"organization_id", "client_service_id", "period_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceWorkflowEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_service_id", nullable = false)
    private UUID clientServiceId;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ServiceWorkflowStatus status = ServiceWorkflowStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "current_step_sequence", nullable = false)
    @Builder.Default
    private int currentStepSequence = 1;

    @Column(name = "total_steps", nullable = false)
    @Builder.Default
    private int totalSteps = 0;

    @Column(name = "completed_steps", nullable = false)
    @Builder.Default
    private int completedSteps = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "internal_target_date")
    private LocalDate internalTargetDate;

    @Column(name = "waiting_for_client", nullable = false)
    @Builder.Default
    private boolean waitingForClient = false;

    @Column(name = "pending_client_action_summary", length = 500)
    private String pendingClientActionSummary;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<ClientServiceWorkflowStepEntity> steps = new ArrayList<>();
}
