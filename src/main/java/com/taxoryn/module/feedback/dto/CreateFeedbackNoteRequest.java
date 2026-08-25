package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.FeedbackNoteVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedbackNoteRequest {
    @NotBlank(message = "Note content cannot be blank")
    @Size(max = 4000, message = "Note cannot exceed 4000 characters")
    private String note;

    @Builder.Default
    private FeedbackNoteVisibility visibility = FeedbackNoteVisibility.INTERNAL;
}
