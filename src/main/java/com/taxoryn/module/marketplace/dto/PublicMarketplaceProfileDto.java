package com.taxoryn.module.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Public & Practice Marketplace Profile Response")
public class PublicMarketplaceProfileDto {

    @Schema(description = "Marketplace Profile ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Public Firm / Practitioner Display Name", example = "ABC Tax Consultants")
    private String displayName;

    @Schema(description = "SEO-friendly public URL slug", example = "abc-tax-consultants")
    private String publicSlug;

    @Schema(description = "SEO-friendly URL slug (legacy alias)")
    private String slug;

    @Schema(description = "Full practice description / bio")
    private String description;

    @Schema(description = "Bio (legacy alias)")
    private String bio;

    @Schema(description = "Catchy Profile Headline", example = "Ex-Big4 Senior CAs specializing in Direct & Indirect Tax Optimization")
    private String headline;

    @Schema(description = "Contact Phone", example = "+91 98200 11223")
    private String phone;

    @Schema(description = "Public Contact Email", example = "contact@abctax.com")
    private String email;

    @Schema(description = "Official Website URL", example = "https://abctax.com")
    private String website;

    @Schema(description = "Website URL (legacy alias)")
    private String websiteUrl;

    @Schema(description = "Years of professional practice", example = "10")
    private Integer experienceYears;

    @Schema(description = "Professional category (CHARTERED_ACCOUNTANT, etc.)")
    private ProfessionalType professionalType;

    @Schema(description = "Marketplace Visibility Status (PRIVATE, PUBLIC, SUSPENDED)", example = "PRIVATE")
    private VisibilityStatus visibilityStatus;

    @Schema(description = "KYC Verification Status (NOT_SUBMITTED, PENDING, VERIFIED, REJECTED)", example = "NOT_SUBMITTED")
    private VerificationStatus verificationStatus;

    @Schema(description = "City location", example = "Mumbai")
    private String city;

    @Schema(description = "State location", example = "Maharashtra")
    private String state;

    @Schema(description = "Postal Pincode", example = "400051")
    private String pincode;

    @Schema(description = "Office Address")
    private String address;

    @Schema(description = "Profile Avatar Logo URL")
    private String avatarUrl;

    @Schema(description = "Cover Banner Image URL")
    private String bannerUrl;

    @Schema(description = "Specializations list")
    private List<String> specializations;

    @Schema(description = "Languages spoken by practitioners")
    private String languagesSpoken;

    @Schema(description = "Minimum starting service fee")
    private BigDecimal startingFee;

    @Schema(description = "Standard hourly advisory rate")
    private BigDecimal hourlyRate;

    @Schema(description = "Average rating score (0.00 - 5.00)")
    private BigDecimal averageRating;

    @Schema(description = "Total customer reviews count")
    private Integer totalReviews;

    @Schema(description = "Total clients served count")
    private Integer totalClientsServed;

    @Schema(description = "Publish toggle flag")
    private Boolean isPublished;

    @Schema(description = "Featured firm status")
    private Boolean isFeatured;

    @Schema(description = "Enable direct paid consultation bookings")
    private Boolean consultationEnabled;

    @Schema(description = "Fixed fee for introductory consultation")
    private BigDecimal consultationFee;

    @Schema(description = "Consultation duration in minutes")
    private Integer consultationDurationMinutes;

    @Schema(description = "Active Service Offerings (Legacy custom packages)")
    private List<MarketplaceServiceDto> services;

    @Schema(description = "Controlled Tax Services offered by the practice from master catalogue")
    private List<PublicTaxServiceDto> offeredServices;

    @Schema(description = "Recent Approved Reviews")
    private List<MarketplaceReviewDto> recentReviews;

    @Schema(description = "Profile Completeness breakdown (percentage, completedItems, missingItems)")
    private ProfileCompletenessDto profileCompleteness;

    @Schema(description = "Profile Completeness (legacy alias)")
    private ProfileCompletenessDto completeness;

    @Schema(description = "Profile Completeness percentage score (0-100)")
    private Integer completenessScore;

    @Schema(description = "Missing recommended items checklist")
    private List<String> missingCompletenessFields;

    @Schema(description = "Active physical / service locations for the practice")
    private List<PublicPracticeLocationDto> locations;

    @Schema(description = "Primary headquarter / main location of the practice")
    private PublicPracticeLocationDto primaryLocation;

    @Schema(description = "Distance in kilometers from customer search coordinates", example = "2.4")
    private Double distanceKm;

    @Schema(description = "Nearest active branch location matching geographic search criteria")
    private PublicPracticeLocationDto nearestLocation;

    public String getPublicSlug() {
        return publicSlug != null ? publicSlug : slug;
    }

    public String getDescription() {
        return description != null ? description : bio;
    }

    public String getWebsite() {
        return website != null ? website : websiteUrl;
    }

    public ProfileCompletenessDto getProfileCompleteness() {
        return profileCompleteness != null ? profileCompleteness : completeness;
    }
}
