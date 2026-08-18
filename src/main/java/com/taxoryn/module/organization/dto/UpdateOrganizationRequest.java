package com.taxoryn.module.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Organization Details Request Payload")
public class UpdateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Display name", example = "Apex & Associates Tax Advisors")
    private String name;

    @Size(max = 255, message = "Legal name cannot exceed 255 characters")
    @Schema(description = "Registered legal name", example = "Apex & Associates LLP")
    private String legalName;

    @Size(max = 255, message = "Trade name cannot exceed 255 characters")
    @Schema(description = "Trade name", example = "Apex Advisors")
    private String tradeName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @Schema(description = "Official phone number", example = "+919876543210")
    private String phone;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "Street address", example = "401, Financial Tower, Nariman Point")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Schema(description = "Country", example = "India")
    private String country;

    @Pattern(regexp = "^[0-9]{5,10}$", message = "Invalid pincode format")
    @Schema(description = "Postal pincode", example = "400021")
    private String pincode;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (e.g. ABCDE1234F)")
    @Schema(description = "PAN", example = "ABCDE1234F")
    private String pan;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    @Schema(description = "GSTIN", example = "27ABCDE1234F1Z5")
    private String gstin;

    @Size(max = 50, message = "Tax registration number cannot exceed 50 characters")
    @Schema(description = "Tax registration number", example = "LLPIN-AAO-1234")
    private String taxRegistrationNumber;
}
