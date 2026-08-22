package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to submit a rating and review for a Tax Professional")
public class SubmitMarketplaceReviewRequest {

    @NotNull(message = "Marketplace profile ID is required")
    private UUID marketplaceProfileId;

    @NotBlank(message = "Reviewer name is required")
    private String reviewerName;

    private String reviewerDesignation;
    private String reviewerCompany;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    private Integer rating;

    private String reviewTitle;

    @NotBlank(message = "Review comment is required")
    private String reviewComment;

    private String serviceTaken;
}
