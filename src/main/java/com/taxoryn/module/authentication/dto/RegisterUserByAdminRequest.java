package com.taxoryn.module.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Team Member Registration by Organization Admin")
public class RegisterUserByAdminRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Schema(description = "Member first name", example = "Suresh")
    private String firstName;

    @Schema(description = "Member last name", example = "Kumar")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Login email address", example = "suresh@taxpractice.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Schema(description = "Initial member password", example = "TemporaryPass123!")
    private String password;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @Schema(description = "Contact phone", example = "+919876543210")
    private String phone;

    @NotEmpty(message = "At least one role code must be assigned")
    @Schema(description = "Assigned role codes", example = "[\"CA_PARTNER\", \"MANAGER\"]")
    private Set<String> roleCodes;
}
