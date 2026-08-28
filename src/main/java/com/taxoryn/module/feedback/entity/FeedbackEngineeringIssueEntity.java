package com.taxoryn.module.feedback.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "engineering_issues")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEngineeringIssueEntity extends AuditableEntity {

    @Column(name = "feedback_id", nullable = false, unique = true)
    private UUID feedbackId;

    @Column(name = "issue_code", nullable = false, unique = true, length = 50)
    private String issueCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private ApplicationFeedbackPriority priority = ApplicationFeedbackPriority.HIGH;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EngineeringIssueStatus status = EngineeringIssueStatus.OPEN;

    @Column(name = "assigned_team", nullable = false, length = 50)
    @Builder.Default
    private String assignedTeam = "ENGINEERING";

    @Column(name = "creator_user_id")
    private UUID creatorUserId;

    // Future Jira / GitHub integration fields
    @Column(name = "external_system", length = 50)
    private String externalSystem;

    @Column(name = "external_issue_id", length = 100)
    private String externalIssueId;

    @Column(name = "external_issue_url", length = 500)
    private String externalIssueUrl;

    @Column(name = "external_status", length = 50)
    private String externalStatus;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
