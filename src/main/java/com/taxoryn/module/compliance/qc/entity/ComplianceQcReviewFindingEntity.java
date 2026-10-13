package com.taxoryn.module.compliance.qc.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.qc.model.ComplianceQcFindingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted structured finding recorded by a reviewer during compliance workflow review.
 */
@Entity
@Table(name = "compliance_qc_review_findings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceQcReviewFindingEntity extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ComplianceWorkflowEntity workflow;

    @Column(name = "review_cycle", nullable = false)
    @Builder.Default
    private int reviewCycle = 1;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private String severity = "BLOCKING";

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "corrective_guidance", columnDefinition = "TEXT")
    private String correctiveGuidance;

    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ComplianceQcFindingStatus status = ComplianceQcFindingStatus.OPEN;

    @Column(name = "preparer_response", columnDefinition = "TEXT")
    private String preparerResponse;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "responded_by_user_id")
    private UUID respondedByUserId;

    @Column(name = "resolution_decision", length = 30)
    private String resolutionDecision;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;
}
