package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.FeedbackTeam;
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
public class FeedbackAssignmentDto {
    private UUID id;
    private UUID feedbackId;
    private FeedbackTeam team;
    private UUID assignedUserId;
    private String assignedUserName;
    private String assignedUserEmail;
    private UUID assignedBy;
    private String assignedByName;
    private String reason;
    private Instant assignedAt;
    private Instant unassignedAt;
    private boolean active;
}
