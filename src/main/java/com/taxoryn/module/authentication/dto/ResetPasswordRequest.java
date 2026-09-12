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
@Schema(description = "Request payload for completing password recovery with token")
public class ResetPasswordRequest {

    @NotBlank(message = "Password reset token is required")
    @Schema(description = "Raw password reset token received via email", example = "a1b2c3d4e5f6...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotBlank(message = "New password is required")
    @com.taxoryn.core.security.validation.StrongPassword
    @Schema(description = "New password conforming to security complexity standards (min 12 chars, upper, lower, digit, special)", example = "Tx9#SecureP@ss2026!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}