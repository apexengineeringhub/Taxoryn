package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.*;
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
public class AdminApplicationFeedbackSummaryDto {
    private UUID id;
    private String feedbackCode;
    private ApplicationFeedbackActorType actorType;
    private ApplicationFeedbackContextType contextType;
    private ApplicationFeedbackType type;
    private ApplicationFeedbackCategory category;
    private Integer rating;
    private String title;
    private String descriptionExcerpt;
    private String page;
    private String feature;
    private ApplicationFeedbackStatus status;
    private ApplicationFeedbackPriority priority;
    private FeedbackTeam assignedTeam;
    private UUID assignedUserId;
    private String assignedUserName;
    private UUID practiceId;
    private String practiceName;
    private String reporterName;
    private String reporterEmail;
    private boolean hasEngineeringIssue;
    private String engineeringIssueCode;
    private boolean hasDuplicateOf;
    private UUID duplicateOfId;
    private int notesCount;
    private Instant createdAt;
    private Instant updatedAt;
}
