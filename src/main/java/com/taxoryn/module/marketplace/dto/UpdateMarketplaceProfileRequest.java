package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
    @Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    @Schema(description = "Public Firm / Practitioner Name", example = "Apex Corporate & Tax Advisors")
    private String displayName;

    @Size(max = 100, message = "Slug cannot exceed 100 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and single hyphens")
    @Schema(description = "Unique public URL slug (e.g. apex-tax-advisors-mumbai)", example = "apex-tax-advisors-mumbai")
    private String slug;

    @Size(max = 255, message = "Headline cannot exceed 255 characters")
    @Schema(description = "Catchy Profile Headline", example = "Ex-Big4 Senior CAs specializing in Direct & Indirect Tax Optimization")
    private String headline;

    @Size(max = 5000, message = "Bio cannot exceed 5000 characters")
    @Schema(description = "Full Firm Bio / Value Proposition")
    private String bio;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    @Schema(description = "Full Firm Bio / Description")
    private String description;

    @Schema(description = "Professional category")
    private ProfessionalType professionalType;

    @Min(value = 0, message = "Experience years must be non-negative")
    @Max(value = 100, message = "Experience years cannot exceed 100")
    @Schema(description = "Years in Practice", example = "10")
    private Integer experienceYears;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Pattern(regexp = "^$|^[0-9]{5,10}$", message = "Invalid postal pincode format")
    @Schema(description = "Postal Pincode", example = "400051")
    private String pincode;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "Office Address")
    private String address;

    @Pattern(regexp = "^$|^\\+?[0-9\\-\\s()]{7,20}$", message = "Invalid phone number format")
    @Schema(description = "Contact Phone", example = "+919876543210")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(description = "Public Inquiry Email", example = "contact@apexadvisors.com")
    private String email;

    @Size(max = 500, message = "Website URL cannot exceed 500 characters")
    @Pattern(regexp = "^$|^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$", message = "Invalid website URL format")
    @Schema(description = "Official Website URL", example = "https://apexadvisors.com")
    private String websiteUrl;

    @Size(max = 500, message = "Website URL cannot exceed 500 characters")
    @Pattern(regexp = "^$|^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$", message = "Invalid website URL format")
    @Schema(description = "Website URL (alias)")
    private String website;

    @Size(max = 1000, message = "Avatar URL cannot exceed 1000 characters")
    @Schema(description = "Profile Avatar Logo URL")
    private String avatarUrl;

    @Size(max = 1000, message = "Banner URL cannot exceed 1000 characters")
    @Schema(description = "Cover Banner Image URL")
    private String bannerUrl;

    @Schema(description = "Specializations list")
    private List<String> specializations;

    @Size(max = 255, message = "Languages spoken cannot exceed 255 characters")
    @Schema(description = "Languages spoken by practitioners")
    private String languagesSpoken;

    @DecimalMin(value = "0.0", inclusive = true, message = "Starting fee must be non-negative")
    @Schema(description = "Minimum starting service fee")
    private BigDecimal startingFee;

    @DecimalMin(value = "0.0", inclusive = true, message = "Hourly rate must be non-negative")
    @Schema(description = "Standard hourly advisory rate")
    private BigDecimal hourlyRate;

    @Schema(description = "Publish toggle to show listing in public directory")
    private Boolean isPublished;

    @Schema(description = "Marketplace visibility lifecycle status (PRIVATE, PUBLIC, SUSPENDED)")
    private VisibilityStatus visibilityStatus;

    @Schema(description = "Visibility status (alias)")
    private VisibilityStatus visibility;

    @Schema(description = "Enable direct paid consultation bookings")
    private Boolean consultationEnabled;

    @DecimalMin(value = "0.0", inclusive = true, message = "Consultation fee must be non-negative")
    @Schema(description = "Fixed fee for introductory consultation")
    private BigDecimal consultationFee;

    @Min(value = 5, message = "Consultation duration must be at least 5 minutes")
    @Max(value = 480, message = "Consultation duration cannot exceed 480 minutes")
    @Schema(description = "Consultation duration in minutes")
    private Integer consultationDurationMinutes;

    public String resolveBio() {
        if (description != null && !description.isBlank()) return description.trim();
        if (bio != null && !bio.isBlank()) return bio.trim();
        return null;
    }

    public String resolveWebsite() {
        if (websiteUrl != null && !websiteUrl.isBlank()) return websiteUrl.trim();
        if (website != null && !website.isBlank()) return website.trim();
        return null;
    }

    public VisibilityStatus resolveVisibility() {
        if (visibility != null) return visibility;
        if (visibilityStatus != null) return visibilityStatus;
        return null;
    }
}
