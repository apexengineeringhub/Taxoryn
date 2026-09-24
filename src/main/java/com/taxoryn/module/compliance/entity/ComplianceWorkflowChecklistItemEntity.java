package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

/**
 * Represents a single operational readiness checklist checkpoint in a compliance workflow.
 */
@Entity
@Table(
        name = "compliance_workflow_checklist_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cw_checklist_item", columnNames = {"organization_id", "workflow_id", "item_key"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceWorkflowChecklistItemEntity extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ComplianceWorkflowEntity workflow;

    @Column(name = "item_key", nullable = false, length = 100)
    private String itemKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private boolean isCompleted = false;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean isRequired = true;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by_user_id")
    private UUID completedByUserId;

    @Column(name = "completed_by_name", length = 255)
    private String completedByName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
