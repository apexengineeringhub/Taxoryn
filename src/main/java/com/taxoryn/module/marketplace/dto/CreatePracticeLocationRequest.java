package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to create a new practice branch/office location")
public class CreatePracticeLocationRequest {

    @NotBlank(message = "Location / branch name is required")
    @Size(max = 150, message = "Location name must not exceed 150 characters")
    @Schema(description = "Branch / Location name", example = "Whitefield Branch Office")
    private String locationName;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Schema(description = "Street address", example = "Plot 42, ITPL Main Road, Whitefield")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Schema(description = "Suite, building, or unit number", example = "Tech Park Tower B, 3rd Floor")
    private String addressLine2;

    @Size(max = 255, message = "Landmark must not exceed 255 characters")
    @Schema(description = "Prominent landmark", example = "Near Hope Farm Junction")
    private String landmark;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City", example = "Bengaluru")
    private String city;

    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(description = "District", example = "Bengaluru Urban")
    private String district;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State / Province", example = "Karnataka")
    private String state;

    @Size(max = 10, message = "State code must not exceed 10 characters")
    @Schema(description = "State 2-letter Code", example = "KA")
    private String stateCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country", example = "India")
    @Builder.Default
    private String country = "India";

    @Size(max = 10, message = "Country code must not exceed 10 characters")
    @Schema(description = "Country Code", example = "IN")
    @Builder.Default
    private String countryCode = "IN";

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid Indian postal PIN code format (must be 6 digits starting with 1-9)")
    @Schema(description = "Postal PIN code", example = "560066")
    private String pincode;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    @Schema(description = "Geographic latitude (-90 to 90)", example = "12.969800")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    @Schema(description = "Geographic longitude (-180 to 180)", example = "77.750000")
    private BigDecimal longitude;

    @Schema(description = "Set as the primary / headquarter location of the practice", example = "false")
    @Builder.Default
    private Boolean isPrimary = false;

    @AssertTrue(message = "Both latitude and longitude must be provided together")
    private boolean isCoordinatesPairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }
}
