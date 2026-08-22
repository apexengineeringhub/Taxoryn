package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request by Practice to update its public Marketplace Profile & Settings")
public class UpdateMarketplaceProfileRequest {

    @NotBlank(message = "Display name is required")
    @Schema(description = "Public Firm / Practitioner Name", example = "Apex Corporate & Tax Advisors")
    private String displayName;

    @Schema(description = "Catchy Profile Headline", example = "Ex-Big4 Senior CAs specializing in Direct & Indirect Tax Optimization")
    private String headline;

    @Schema(description = "Full Firm Bio / Value Proposition")
    private String bio;

    @Schema(description = "Professional category")
    private ProfessionalType professionalType;

    @Schema(description = "Years in Practice")
    private Integer experienceYears;

    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Schema(description = "Postal Pincode", example = "400051")
    private String pincode;

    @Schema(description = "Office Address")
    private String address;

    @Schema(description = "Contact Phone")
    private String phone;

    @Schema(description = "Public Inquiry Email")
    private String email;

    @Schema(description = "Official Website URL")
    private String websiteUrl;

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

    @Schema(description = "Publish toggle to show listing in public directory")
    private Boolean isPublished;

    @Schema(description = "Enable direct paid consultation bookings")
    private Boolean consultationEnabled;

    @Schema(description = "Fixed fee for introductory consultation")
    private BigDecimal consultationFee;

    @Schema(description = "Consultation duration in minutes")
    private Integer consultationDurationMinutes;
}
