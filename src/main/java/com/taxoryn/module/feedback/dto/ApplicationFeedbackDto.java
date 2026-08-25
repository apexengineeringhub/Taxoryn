package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.ApplicationFeedbackCategory;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Customer-safe feedback view. Internal status, priority, and captured context are excluded. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFeedbackDto {
    private UUID id;
    private ApplicationFeedbackType type;
    private ApplicationFeedbackCategory category;
    private Integer rating;
    private String title;
    private String description;
    private Instant createdAt;
}
