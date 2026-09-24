package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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
import jakarta.persistence.Version;
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
 * Concrete operational compliance execution workflow representing the practitioner process for one ComplianceObligation.
 */
@Entity
@Table(
        name = "compliance_workflows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_compliance_workflow_obligation", columnNames = {"organization_id", "compliance_obligation_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceWorkflowEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_service_id")
    private UUID clientServiceId;

    @Column(name = "compliance_obligation_id", nullable = false)
    private UUID complianceObligationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false, length = 50)
    @Builder.Default
    private ComplianceWorkflowStatus workflowStatus = ComplianceWorkflowStatus.CREATED;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "reviewer_employee_id")
    private UUID reviewerEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "statutory_due_date")
    private LocalDate statutoryDueDate;

    // --- Client Action / Waiting State Tracking ---
    @Column(name = "waiting_for_client", nullable = false)
    @Builder.Default
    private boolean waitingForClient = false;

    @Column(name = "waiting_reason", columnDefinition = "TEXT")
    private String waitingReason;

    @Column(name = "waiting_requested_at")
    private Instant waitingRequestedAt;

    @Column(name = "waiting_requested_by", length = 255)
    private String waitingRequestedBy;

    @Column(name = "expected_response_date")
    private LocalDate expectedResponseDate;

    // --- Execution & Review Lifecycles ---
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "changes_requested_reason", columnDefinition = "TEXT")
    private String changesRequestedReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    // --- Government Filing Execution Metadata ---
    @Column(name = "filed_date")
    private LocalDate filedDate;

    @Column(name = "filed_at")
    private Instant filedAt;

    @Column(name = "filed_by", length = 255)
    private String filedBy;

    @Column(name = "acknowledgement_number", length = 100)
    private String acknowledgementNumber;

    // --- Completion & Cancellation ---
    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 255)
    private String cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;



    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<ComplianceWorkflowChecklistItemEntity> checklistItems = new ArrayList<>();

    public void addChecklistItem(ComplianceWorkflowChecklistItemEntity item) {
        checklistItems.add(item);
        item.setWorkflow(this);
        item.setOrganizationId(this.getOrganizationId());
    }

    /**
     * Validates that transitioning from the current state to the requested new state is permitted.
     */
    public void validateTransition(ComplianceWorkflowStatus targetStatus) {
        if (targetStatus == null) {
            throw new BusinessValidationException("Target workflow status cannot be null");
        }
        if (this.workflowStatus == targetStatus) {
            return; // Idempotent same-status transition
        }

        boolean valid = switch (this.workflowStatus) {
            case CREATED -> targetStatus == ComplianceWorkflowStatus.READY
                    || targetStatus == ComplianceWorkflowStatus.IN_PROGRESS
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case READY -> targetStatus == ComplianceWorkflowStatus.IN_PROGRESS
                    || targetStatus == ComplianceWorkflowStatus.WAITING_FOR_CLIENT
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == ComplianceWorkflowStatus.WAITING_FOR_CLIENT
                    || targetStatus == ComplianceWorkflowStatus.UNDER_REVIEW
                    || targetStatus == ComplianceWorkflowStatus.READY_FOR_FILING
                    || targetStatus == ComplianceWorkflowStatus.FILED
                    || targetStatus == ComplianceWorkflowStatus.COMPLETED
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case WAITING_FOR_CLIENT -> targetStatus == ComplianceWorkflowStatus.IN_PROGRESS
                    || targetStatus == ComplianceWorkflowStatus.READY
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case UNDER_REVIEW -> targetStatus == ComplianceWorkflowStatus.CHANGES_REQUIRED
                    || targetStatus == ComplianceWorkflowStatus.READY_FOR_FILING
                    || targetStatus == ComplianceWorkflowStatus.IN_PROGRESS
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case CHANGES_REQUIRED -> targetStatus == ComplianceWorkflowStatus.IN_PROGRESS
                    || targetStatus == ComplianceWorkflowStatus.UNDER_REVIEW
                    || targetStatus == ComplianceWorkflowStatus.WAITING_FOR_CLIENT
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case READY_FOR_FILING -> targetStatus == ComplianceWorkflowStatus.FILED
                    || targetStatus == ComplianceWorkflowStatus.ACKNOWLEDGEMENT_PENDING
                    || targetStatus == ComplianceWorkflowStatus.IN_PROGRESS
                    || targetStatus == ComplianceWorkflowStatus.COMPLETED
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case FILED -> targetStatus == ComplianceWorkflowStatus.ACKNOWLEDGEMENT_PENDING
                    || targetStatus == ComplianceWorkflowStatus.COMPLETED
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case ACKNOWLEDGEMENT_PENDING -> targetStatus == ComplianceWorkflowStatus.COMPLETED
                    || targetStatus == ComplianceWorkflowStatus.FILED
                    || targetStatus == ComplianceWorkflowStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };

        if (!valid) {
            throw new BusinessValidationException(String.format(
                    "Invalid workflow transition from '%s' to '%s'", this.workflowStatus, targetStatus));
        }
    }
}
