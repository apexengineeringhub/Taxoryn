package com.taxoryn.module.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for activating organization or employee account")
public class ActivateOrganizationRequest {

    public ActivateOrganizationRequest(String token) {
        this.token = token;
    }

    @NotBlank(message = "Activation token is required")
    @Schema(description = "Raw activation/invitation token", example = "a1b2c3d4e5f6...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).*$",
        message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
    )
    @Schema(description = "Optional password setup for first-time employee onboarding", example = "SecurePass123!")
    private String password;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).*$",
        message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
    )
    @Schema(description = "Alternative alias for password setup", example = "SecurePass123!")
    private String newPassword;

    public String getEffectivePassword() {
        if (StringUtils.hasText(password)) {
            return password.trim();
        }
        if (StringUtils.hasText(newPassword)) {
            return newPassword.trim();
        }
        return null;
    }
}
