package com.taxoryn.module.portal.dto;

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
@Schema(description = "Payload for revoking a pending approval request")
public class RevokeClientApprovalRequest {

    @NotBlank(message = "Revocation reason is required")
    private String reason;
}
