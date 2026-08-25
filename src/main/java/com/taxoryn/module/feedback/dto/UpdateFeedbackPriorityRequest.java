package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.ApplicationFeedbackPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeedbackPriorityRequest {
    @NotNull(message = "Priority is required")
    private ApplicationFeedbackPriority priority;

    private String reason;
}
