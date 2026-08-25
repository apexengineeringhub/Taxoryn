package com.taxoryn.module.feedback.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
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
    @Column(name = "actor_type", nullable = false, length = 30)
    private ApplicationFeedbackActorType actorType;

    /** Practice context is the authenticated user's organization, never client supplied. */
    @Column(name = "practice_id")
    private UUID practiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 30)
    private ApplicationFeedbackContextType contextType;

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

    @Column(name = "duplicate_of_id")
    private UUID duplicateOfId;

    @Column(name = "resolution_note", length = 4000)
    private String resolutionNote;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_team", length = 50)
    private FeedbackTeam assignedTeam;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;
}
