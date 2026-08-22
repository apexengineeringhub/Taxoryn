package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public Tax Professional / Firm Marketplace Profile")
public class PublicMarketplaceProfileDto {

    private UUID id;
    private UUID organizationId;
    private String slug;
    private String displayName;
    private String headline;
    private String bio;
    private ProfessionalType professionalType;
    private Integer experienceYears;
    private String city;
    private String state;
    private String pincode;
    private String address;
    private String phone;
    private String email;
    private String websiteUrl;
    private String avatarUrl;
    private String bannerUrl;
    private List<String> specializations;
    private String languagesSpoken;
    private BigDecimal startingFee;
    private BigDecimal hourlyRate;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer totalClientsServed;
    private VerificationStatus verificationStatus;
    private Boolean isPublished;
    private Boolean isFeatured;
    private Boolean consultationEnabled;
    private BigDecimal consultationFee;
    private Integer consultationDurationMinutes;
    private List<MarketplaceServiceDto> services;
    private List<MarketplaceReviewDto> recentReviews;
}
