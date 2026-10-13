package com.taxoryn.module.compliance.qc.dto;

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
@Schema(description = "Request to Revoke Filing Authorization")
public class RevokeAuthorizationRequest {

    @NotBlank(message = "Reason for revocation is required")
    @Schema(description = "Explanation for why filing authorization is being revoked", example = "Client provided amended sales invoices requiring recalculation")
    private String reason;
}
