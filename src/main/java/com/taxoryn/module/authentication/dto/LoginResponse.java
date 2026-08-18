package com.taxoryn.module.authentication.dto;

import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.user.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication Success Response")
public class LoginResponse {

    @Schema(description = "JWT Access Token")
    private String accessToken;

    @Schema(description = "JWT Refresh Token")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token lifetime in seconds", example = "86400")
    private long expiresIn;

    @Schema(description = "Authenticated user profile")
    private UserDto user;

    @Schema(description = "Tenant organization profile")
    private OrganizationDto organization;
}
