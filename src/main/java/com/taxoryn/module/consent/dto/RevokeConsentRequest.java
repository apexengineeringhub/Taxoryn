package com.taxoryn.module.consent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to revoke an active or pending taxpayer consent")
public class RevokeConsentRequest {

    @NotBlank(message = "Revocation reason is required")
    @Schema(description = "Mandatory audit reason for revoking the consent delegation")
    private String reason;
}
