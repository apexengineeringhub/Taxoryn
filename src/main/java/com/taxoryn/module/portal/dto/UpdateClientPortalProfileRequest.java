package com.taxoryn.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Client Contact Information")
public class UpdateClientPortalProfileRequest {

    @Schema(description = "Primary contact email")
    private String email;

    @Schema(description = "Primary contact phone")
    private String phone;

    @Schema(description = "Street address line 1")
    private String addressLine1;

    @Schema(description = "Street address line 2")
    private String addressLine2;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "Postal PIN Code")
    private String pincode;
}
