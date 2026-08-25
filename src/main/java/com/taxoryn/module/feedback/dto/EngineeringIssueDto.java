package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.ApplicationFeedbackPriority;
import com.taxoryn.module.feedback.entity.EngineeringIssueStatus;
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
public class EngineeringIssueDto {
    private UUID id;
    private UUID feedbackId;
    private String issueCode;
    private String title;
    private String description;
    private ApplicationFeedbackPriority priority;
    private EngineeringIssueStatus status;
    private String assignedTeam;
    private UUID createdBy;
    private String createdByName;
    private String externalSystem;
    private String externalIssueId;
    private String externalIssueUrl;
    private String externalStatus;
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
