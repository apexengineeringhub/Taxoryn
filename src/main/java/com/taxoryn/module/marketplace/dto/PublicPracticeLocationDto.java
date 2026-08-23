package com.taxoryn.module.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Public Practice Location DTO for client exploration")
public class PublicPracticeLocationDto {

    @Schema(description = "Location Unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Branch / Location Name", example = "Bengaluru Head Office")
    private String locationName;

    @Schema(description = "Primary street address", example = "Suite 402, Prestige Meridian II, MG Road")
    private String addressLine1;

    @Schema(description = "Secondary address line", example = "4th Floor")
    private String addressLine2;

    @Schema(description = "Prominent landmark", example = "Opposite Trinity Metro Station")
    private String landmark;

    @Schema(description = "City", example = "Bengaluru")
    private String city;

    @Schema(description = "District", example = "Bengaluru Urban")
    private String district;

    @Schema(description = "State / Province", example = "Karnataka")
    private String state;

    @Schema(description = "State Code", example = "KA")
    private String stateCode;

    @Schema(description = "Country", example = "India")
    private String country;

    @Schema(description = "Country Code", example = "IN")
    private String countryCode;

    @Schema(description = "Postal Pincode", example = "560001")
    private String pincode;

    @Schema(description = "Geographic Latitude (-90 to 90)", example = "12.971600")
    private BigDecimal latitude;

    @Schema(description = "Geographic Longitude (-180 to 180)", example = "77.594600")
    private BigDecimal longitude;

    @Schema(description = "Flag indicating if this is the primary headquarter/office", example = "true")
    private Boolean isPrimary;
}
