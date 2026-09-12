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

    @com.taxoryn.core.security.validation.StrongPassword(optional = true)
    @Schema(description = "Optional password setup for first-time onboarding", example = "Tx9#SecureP@ss2026!")
    private String password;

    @com.taxoryn.core.security.validation.StrongPassword(optional = true)
    @Schema(description = "Alternative alias for password setup", example = "Tx9#SecureP@ss2026!")
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
