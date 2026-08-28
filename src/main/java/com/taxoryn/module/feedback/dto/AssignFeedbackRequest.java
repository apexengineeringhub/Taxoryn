package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.FeedbackTeam;
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
public class AssignFeedbackRequest {
    @NotNull(message = "Team assignment is required")
    private FeedbackTeam team;

    private UUID assignedUserId;

    private String reason;
}
