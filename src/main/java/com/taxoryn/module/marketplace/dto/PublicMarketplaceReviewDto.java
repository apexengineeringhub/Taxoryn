package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public Verified Client Review DTO (Sanitized for unauthenticated and client public views)")
public class PublicMarketplaceReviewDto {

    @Schema(description = "Review ID")
    private UUID id;

    @Schema(description = "Marketplace Profile ID")
    private UUID marketplaceProfileId;

    @Schema(description = "Reviewer Display Name")
    private String reviewerName;

    @Schema(description = "Reviewer Designation / Title")
    private String reviewerDesignation;

    @Schema(description = "Reviewer Company Name")
    private String reviewerCompany;

    @Schema(description = "Rating (1 to 5 stars)")
    private Integer rating;

    @Schema(description = "Review Title / Headline")
    private String reviewTitle;

    @Schema(description = "Detailed Review Feedback")
    private String reviewComment;

    @Schema(description = "Service Package or Category Taken")
    private String serviceTaken;

    @Schema(description = "Verified Client Flag")
    private Boolean isVerifiedClient;

    @Schema(description = "Timestamp when review was submitted")
    private Instant createdAt;
}
