package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated rating summary and star distribution for a practitioner profile")
public class MarketplaceRatingSummaryDto {

    @Schema(description = "Marketplace profile ID")
    private UUID marketplaceProfileId;

    @Schema(description = "Deterministic average rating rounded to 2 decimal places", example = "4.85")
    private BigDecimal averageRating;

    @Schema(description = "Total number of approved published reviews", example = "42")
    private Integer totalReviews;

    @Schema(description = "Count of 5-star reviews")
    private Long fiveStarCount;

    @Schema(description = "Count of 4-star reviews")
    private Long fourStarCount;

    @Schema(description = "Count of 3-star reviews")
    private Long threeStarCount;

    @Schema(description = "Count of 2-star reviews")
    private Long twoStarCount;

    @Schema(description = "Count of 1-star reviews")
    private Long oneStarCount;
}
