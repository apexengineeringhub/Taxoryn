package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search and filter criteria for public marketplace discovery")
public class MarketplaceSearchRequest {

    @Schema(description = "City or metropolitan area filter", example = "Bengaluru")
    private String city;

    @Schema(description = "State name or abbreviation", example = "Karnataka")
    private String state;

    @Schema(description = "6-digit PIN Code / Postal Code", example = "560001")
    private String pincode;

    @Schema(description = "Professional category filter", example = "CHARTERED_ACCOUNTANT")
    private ProfessionalType professionalType;

    @Schema(description = "Specialization or service keyword", example = "GST_FILING")
    private String specialization;

    @Schema(description = "Service keyword alias", example = "ITR Filing")
    private String service;

    @Schema(description = "Filter only KYC verified practitioners")
    private Boolean verifiedOnly;

    @Schema(description = "Verification filter alias")
    private Boolean verified;

    @Schema(description = "Minimum average rating threshold (1.0 - 5.0)", example = "4.0")
    @DecimalMin(value = "0.0", message = "Minimum rating cannot be negative")
    @DecimalMax(value = "5.0", message = "Minimum rating cannot exceed 5.0")
    private Double minRating;

    @Schema(description = "Keyword search for firm name, headline, bio, or branch address")
    private String search;

    @Schema(description = "Search keyword alias", example = "Audit")
    private String q;

    // --- Geographic / Radius Search Parameters ---

    @Schema(description = "Customer latitude in decimal degrees (-90.0 to 90.0)", example = "12.971600")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.0")
    private BigDecimal latitude;

    @Schema(description = "Customer longitude in decimal degrees (-180.0 to 180.0)", example = "77.594600")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.0")
    private BigDecimal longitude;

    @Schema(description = "Search radius in kilometers (1.0 to 100.0 km). Defaults to 10 km.", example = "10.0")
    @DecimalMin(value = "1.0", message = "Search radius must be at least 1.0 km")
    @DecimalMax(value = "100.0", message = "Search radius cannot exceed 100.0 km")
    private Double radiusKm;

    // --- Pagination and Sorting ---

    @Schema(description = "Zero-based page index", example = "0")
    @Min(value = 0, message = "Page index cannot be negative")
    @Builder.Default
    private int page = 0;

    @Schema(description = "Page size (1 to 50)", example = "12")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 50, message = "Page size cannot exceed 50")
    @Builder.Default
    private int size = 12;

    @Schema(description = "Sort property (distance, averageRating, experienceYears, startingFee, createdAt)", example = "distance")
    @Builder.Default
    private String sortBy = "averageRating";

    @Schema(description = "Sort property alias")
    private String sort;

    @Schema(description = "Sort direction (asc or desc)", example = "asc")
    @Builder.Default
    private String sortDirection = "desc";

    /**
     * Resolves effective search keyword.
     */
    public String getEffectiveSearch() {
        if (StringUtils.hasText(search)) return search.trim();
        if (StringUtils.hasText(q)) return q.trim();
        return null;
    }

    /**
     * Resolves effective service specialization.
     */
    public String getEffectiveSpecialization() {
        if (StringUtils.hasText(specialization)) return specialization.trim();
        if (StringUtils.hasText(service)) return service.trim();
        return null;
    }

    /**
     * Resolves effective verified filter.
     */
    public Boolean getEffectiveVerified() {
        if (verifiedOnly != null) return verifiedOnly;
        return verified;
    }

    /**
     * Checks if coordinates are supplied.
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Validates that latitude and longitude must both be provided together or neither.
     */
    @AssertTrue(message = "Both latitude and longitude must be provided together")
    public boolean isCoordinatesPairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }

    /**
     * Validates radius bounds when supplied.
     */
    @AssertTrue(message = "Search radius must be between 1.0 km and 100.0 km")
    public boolean isRadiusValid() {
        if (radiusKm == null) return true;
        return radiusKm >= 1.0 && radiusKm <= 100.0;
    }

    public Pageable toPageable() {
        String activeSort = StringUtils.hasText(sort) ? sort.trim() : sortBy;
        if ("distance".equalsIgnoreCase(activeSort)) {
            // Distance sorting is handled via Geo calculation
            return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (activeSort.toLowerCase()) {
            case "rating", "averagerating" -> "averageRating";
            case "experience", "experienceyears" -> "experienceYears";
            case "fee", "startingfee" -> "startingFee";
            case "newest", "createdat" -> "createdAt";
            default -> "averageRating";
        };

        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(direction, property));
    }
}
