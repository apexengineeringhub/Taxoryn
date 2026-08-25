package com.taxoryn.module.feedback.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Product feedback about Taxoryn itself. This is deliberately separate from
 * MarketplaceReviewEntity, which records verified practice-service reviews.
 */
@Entity
@Table(name = "application_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFeedbackEntity extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private ApplicationFeedbackType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ApplicationFeedbackCategory category;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "page_path", length = 500)
    private String page;

    @Column(name = "feature_name", length = 100)
    private String feature;

    @Column(name = "source", nullable = false, length = 40)
    @Builder.Default
    private String source = "WEB";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ApplicationFeedbackStatus status = ApplicationFeedbackStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private ApplicationFeedbackPriority priority = ApplicationFeedbackPriority.MEDIUM;
}
