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
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).*$",
        message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
    )
    @Schema(description = "New password conforming to security complexity standards", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}