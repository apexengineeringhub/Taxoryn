package com.taxoryn.module.authentication.dto;

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
@Schema(description = "Request payload for authenticated user password change")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password of the authenticated user", example = "OldPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @com.taxoryn.core.security.validation.StrongPassword
    @Schema(description = "New password meeting complexity standards (min 12 chars, upper, lower, digit, special)", example = "Tx9#SecureP@ss2026!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Confirmation of new password", example = "NewSecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;
}