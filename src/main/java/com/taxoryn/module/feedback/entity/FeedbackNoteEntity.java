package com.taxoryn.module.feedback.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "feedback_notes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackNoteEntity extends AuditableEntity {

    @Column(name = "feedback_id", nullable = false)
    private UUID feedbackId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_name", length = 150)
    private String authorName;

    @Column(name = "note", nullable = false, length = 4000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private FeedbackNoteVisibility visibility = FeedbackNoteVisibility.INTERNAL;
}
