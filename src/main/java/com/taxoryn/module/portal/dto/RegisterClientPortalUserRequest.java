package com.taxoryn.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Register Client Portal User Request")
public class RegisterClientPortalUserRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Target Client ID")
    private UUID clientId;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @Schema(description = "User login email", example = "contact@clientbusiness.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User initial password", example = "ClientPass123!")
    private String password;

    @NotBlank(message = "First name is required")
    @Schema(description = "First name", example = "Amit")
    private String firstName;

    @Schema(description = "Last name", example = "Sharma")
    private String lastName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Role type: CLIENT_ADMIN or CLIENT_USER", defaultValue = "CLIENT_USER")
    @Builder.Default
    private String role = "CLIENT_USER";
}
