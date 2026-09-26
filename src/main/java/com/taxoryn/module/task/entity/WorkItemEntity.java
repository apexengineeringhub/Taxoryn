package com.taxoryn.module.task.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
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
 * Generic reusable Work Item entity representing a core unit of work across compliance, advisory, and tax workspaces.
 */
@Entity
@Table(name = "work_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemEntity extends TenantAuditableEntity {

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "client_service_id")
    private UUID clientServiceId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private WorkItemStatus status = WorkItemStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private WorkItemPriority priority = WorkItemPriority.MEDIUM;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
