package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.FeedbackNoteVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackNoteDto {
    private UUID id;
    private UUID feedbackId;
    private UUID authorId;
    private String authorName;
    private String note;
    private FeedbackNoteVisibility visibility;
    private Instant createdAt;
}
