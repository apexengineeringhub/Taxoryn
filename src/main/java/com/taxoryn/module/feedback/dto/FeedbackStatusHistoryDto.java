package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.ApplicationFeedbackStatus;
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
public class FeedbackStatusHistoryDto {
    private UUID id;
    private UUID feedbackId;
    private ApplicationFeedbackStatus oldStatus;
    private ApplicationFeedbackStatus newStatus;
    private UUID changedBy;
    private String changedByName;
    private String reason;
    private Instant createdAt;
}
