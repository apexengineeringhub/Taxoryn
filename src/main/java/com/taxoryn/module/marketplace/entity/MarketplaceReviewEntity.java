package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceReviewEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "reviewer_name", nullable = false)
    private String reviewerName;

    @Column(name = "reviewer_designation")
    private String reviewerDesignation;

    @Column(name = "reviewer_company")
    private String reviewerCompany;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "review_title")
    private String reviewTitle;

    @Column(name = "review_comment", nullable = false, columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "service_taken", length = 100)
    private String serviceTaken;

    @Column(name = "is_verified_client")
    @Builder.Default
    private Boolean isVerifiedClient = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.APPROVED;

    public enum ReviewStatus {
        PENDING,
        APPROVED,
        HIDDEN
    }
}
