package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApplicationFeedbackDetailDto {
    private UUID id;
    private String feedbackCode;
    private UUID userId;
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;
    private ApplicationFeedbackActorType actorType;
    private UUID practiceId;
    private String practiceName;
    private String practiceEmail;
    private String practiceSubscriptionPlan;
    private ApplicationFeedbackContextType contextType;
    private ApplicationFeedbackType type;
    private ApplicationFeedbackCategory category;
    private Integer rating;
    private String title;
    private String description;
    private String page;
    private String feature;
    private String source;
    private ApplicationFeedbackStatus status;
    private ApplicationFeedbackPriority priority;

    // Assignment
    private FeedbackTeam assignedTeam;
    private UUID assignedUserId;
    private String assignedUserName;
    private FeedbackAssignmentDto activeAssignment;
    private List<FeedbackAssignmentDto> assignmentHistory;

    // Deduplication
    private UUID duplicateOfId;
    private String duplicateOfTitle;

    // Resolution & Close
    private String resolutionNote;
    private UUID resolvedBy;
    private String resolvedByName;
    private Instant resolvedAt;
    private UUID closedBy;
    private String closedByName;
    private Instant closedAt;

    // Engineering Issue (if escalated)
    private EngineeringIssueDto engineeringIssue;

    // Internal Notes & Status Timeline
    private List<FeedbackNoteDto> notes;
    private List<FeedbackStatusHistoryDto> timeline;

    private Instant createdAt;
    private Instant updatedAt;
}
