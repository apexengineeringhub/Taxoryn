package com.taxoryn.module.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Organization Self-Registration & Onboarding Payload")
public class RegisterOrganizationRequest {

    // Organization Information
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    @Schema(description = "Organization / Practice Name", example = "Apex & Associates Tax Advisors")
    private String organizationName;

    @NotBlank(message = "Organization email is required")
    @Email(message = "Invalid organization email format")
    @Schema(description = "Official organization email", example = "contact@apexadvisors.com")
    private String organizationEmail;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @Schema(description = "Practice contact phone", example = "+919876543210")
    private String organizationPhone;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (e.g. ABCDE1234F)")
    @Schema(description = "Entity PAN", example = "ABCDE1234F")
    private String pan;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    @Schema(description = "Entity GSTIN", example = "27ABCDE1234F1Z5")
    private String gstin;

    // Administrator Details
    @NotBlank(message = "Admin first name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Schema(description = "Admin first name", example = "Rajesh")
    private String adminFirstName;

    @Schema(description = "Admin last name", example = "Sharma")
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email format")
    @Schema(description = "Admin login email", example = "rajesh@apexadvisors.com")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "Admin password", example = "StrongPassword123!")
    private String adminPassword;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid admin phone number format")
    @Schema(description = "Admin mobile phone", example = "+919876543210")
    private String adminPhone;
}
