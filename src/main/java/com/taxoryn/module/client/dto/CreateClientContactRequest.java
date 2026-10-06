package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ContactRole;
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
@Schema(description = "Request to create a new client contact")
public class CreateClientContactRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "First name", example = "Rahul")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "Last name", example = "Sharma")
    private String lastName;

    @Size(max = 200, message = "Display name must not exceed 200 characters")
    @Schema(description = "Display name (optional)", example = "Rahul Sharma")
    private String displayName;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    @Schema(description = "Designation / job title", example = "Director of Finance")
    private String designation;

    @Email(message = "Valid email address is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Email address", example = "rahul.sharma@example.com")
    private String email;

    @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "Phone must be a valid phone number format")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Primary phone number", example = "+91 9876543210")
    private String phone;

    @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "Alternate phone must be a valid phone number format")
    @Size(max = 20, message = "Alternate phone must not exceed 20 characters")
    @Schema(description = "Alternate phone number", example = "+91 9876543211")
    private String altPhone;

    @Schema(description = "Contact role / function", example = "PRIMARY")
    @Builder.Default
    private ContactRole contactRole = ContactRole.OTHER;

    @Schema(description = "Whether to designate as primary contact", example = "true")
    @Builder.Default
    private Boolean primaryContact = false;

    @Schema(description = "Additional notes", example = "Handles tax compliance queries")
    private String notes;
}
