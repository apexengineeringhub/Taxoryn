package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Marketplace Client Review & Rating DTO")
public class MarketplaceReviewDto {

    private UUID id;
    private UUID marketplaceProfileId;
    private String reviewerName;
    private String reviewerDesignation;
    private String reviewerCompany;
    private Integer rating;
    private String reviewTitle;
    private String reviewComment;
    private String serviceTaken;
    private Boolean isVerifiedClient;
    private Instant createdAt;
}
