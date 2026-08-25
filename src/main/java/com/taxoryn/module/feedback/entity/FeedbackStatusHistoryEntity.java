package com.taxoryn.module.feedback.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "feedback_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatusHistoryEntity extends AuditableEntity {

    @Column(name = "feedback_id", nullable = false)
    private UUID feedbackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private ApplicationFeedbackStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ApplicationFeedbackStatus newStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_by_name", length = 150)
    private String changedByName;

    @Column(name = "reason", length = 1000)
    private String reason;
}
