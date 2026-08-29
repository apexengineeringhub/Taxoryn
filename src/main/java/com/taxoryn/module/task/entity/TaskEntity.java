package com.taxoryn.module.task.entity;

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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity extends TenantAuditableEntity {

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_category", nullable = false, length = 100)
    @Builder.Default
    private TaskCategory taskCategory = TaskCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "compliance_id")
    private UUID complianceId;

    @Column(name = "document_request_id")
    private UUID documentRequestId;

    @Column(name = "gst_filing_id")
    private UUID gstFilingId;

    @Column(name = "itr_return_id")
    private UUID itrReturnId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    @Column(name = "blocked_reason", columnDefinition = "TEXT")
    private String blockedReason;

    @Column(name = "completed_at")
    private java.time.Instant completedAt;

    public enum TaskCategory {
        GST,
        ITR,
        TDS,
        AUDIT,
        COMPLIANCE,
        BILLING,
        OTHER
    }

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        UNDER_REVIEW,
        BLOCKED,
        COMPLETED,
        CANCELLED
    }

    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
