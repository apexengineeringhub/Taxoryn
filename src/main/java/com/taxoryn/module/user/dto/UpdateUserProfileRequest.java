package com.taxoryn.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Self-Service User Profile Update Payload")
public class UpdateUserProfileRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "First name", example = "Pooja")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "Last name", example = "Joshi")
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Contact phone number", example = "+919876543210")
    private String phone;

    @Size(max = 1000, message = "Avatar URL must not exceed 1000 characters")
    @Schema(description = "Profile photo URL or avatar storage key")
    private String avatarUrl;
}
