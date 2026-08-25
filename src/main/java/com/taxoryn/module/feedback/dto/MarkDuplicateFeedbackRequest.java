package com.taxoryn.module.feedback.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkDuplicateFeedbackRequest {
    @NotNull(message = "Original feedback ID (duplicate of) is required")
    private UUID duplicateOfId;

    private String reason;
}
