package com.taxoryn.module.review.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_requests", indexes = {
        @Index(name = "idx_review_org_status", columnList = "organization_id,status"),
        @Index(name = "idx_review_org_resource", columnList = "organization_id,resource_type,resource_id"),
        @Index(name = "idx_review_org_reviewer", columnList = "organization_id,assigned_reviewer_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ReviewRequestEntity extends TenantAuditableEntity {
    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;
    @Column(name = "review_type", nullable = false, length = 40)
    private String reviewType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ReviewStatus status = ReviewStatus.SUBMITTED;
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;
    @Column(name = "assigned_reviewer_id", nullable = false)
    private UUID assignedReviewerId;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
