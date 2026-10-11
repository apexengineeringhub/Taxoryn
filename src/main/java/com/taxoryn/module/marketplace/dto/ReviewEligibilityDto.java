package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Review eligibility check result for current authenticated user")
public class ReviewEligibilityDto {

    @Schema(description = "Whether the current user is eligible to submit a review for this practitioner")
    private boolean eligible;

    @Schema(description = "Whether the user has already submitted a review for this practitioner service")
    private boolean alreadyReviewed;

    @Schema(description = "Existing review ID if user has already reviewed")
    private UUID existingReviewId;

    @Schema(description = "Completed lead or enquiry ID that qualifies the user for review")
    private UUID completedLeadId;

    @Schema(description = "Service category from the completed engagement")
    private String serviceCategory;

    @Schema(description = "Human-readable explanation of eligibility or reason for ineligibility")
    private String reason;
}
