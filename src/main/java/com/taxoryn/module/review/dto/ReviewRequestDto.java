package com.taxoryn.module.review.dto;

import com.taxoryn.module.review.entity.ReviewStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ReviewRequestDto {
    UUID id;
    String resourceType;
    UUID resourceId;
    String reviewType;
    ReviewStatus status;
    UUID requestedBy;
    UUID assignedReviewerId;
    Instant requestedAt;
    Instant reviewedAt;
    UUID reviewedBy;
    String reviewComment;
    String rejectionReason;
    Long version;
    List<ReviewActionDto> history;
}
