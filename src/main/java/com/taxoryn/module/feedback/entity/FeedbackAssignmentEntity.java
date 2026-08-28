package com.taxoryn.module.feedback.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback_assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAssignmentEntity extends AuditableEntity {

    @Column(name = "feedback_id", nullable = false)
    private UUID feedbackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team", nullable = false, length = 50)
    private FeedbackTeam team;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "unassigned_at")
    private Instant unassignedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
