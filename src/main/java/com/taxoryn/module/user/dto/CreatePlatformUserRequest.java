package com.taxoryn.module.user.dto;

import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to provision a new internal Taxoryn platform employee or administrator")
public class CreatePlatformUserRequest {

    @NotBlank(message = "First name is required")
    @Schema(description = "First name of the platform administrator", example = "Anjani")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Last name of the platform administrator", example = "Pathak")
    private String lastName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Valid email format is required")
    @Schema(description = "Corporate Taxoryn email address", example = "anjani.pathak@taxoryn.com")
    private String email;

    @Schema(description = "Contact phone number", example = "+919876543210")
    private String phone;

    @NotBlank(message = "Role code is required")
    @Schema(description = "Controlled Taxoryn Platform Role Code (e.g. TAXORYN_OPERATIONS_ADMIN, TAXORYN_SUPPORT_ADMIN, TAXORYN_MARKETPLACE_ADMIN, TAXORYN_FINANCE_ADMIN, TAXORYN_CONTENT_ADMIN, TAXORYN_SECURITY_ADMIN, TAXORYN_ENGINEERING_ADMIN)", example = "TAXORYN_OPERATIONS_ADMIN")
    private String roleCode;

    @Builder.Default
    @Schema(description = "Initial account status", example = "ACTIVE")
    private UserStatus status = UserStatus.ACTIVE;

    @Schema(description = "Temporary password (optional; if omitted, standard default password will be assigned)", example = "Password123!")
    private String temporaryPassword;
}
