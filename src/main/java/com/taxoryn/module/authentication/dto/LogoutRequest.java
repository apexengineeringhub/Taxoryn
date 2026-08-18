package com.taxoryn.module.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Logout Request Payload")
public class LogoutRequest {

    @Schema(description = "Optional refresh token to invalidate concurrently", example = "eyJhbGciOi...")
    private String refreshToken;
}
