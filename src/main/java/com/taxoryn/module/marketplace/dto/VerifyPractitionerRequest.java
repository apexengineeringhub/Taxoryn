package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform Super Admin action to verify or reject practitioner credentials")
public class VerifyPractitionerRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;

    private String rejectionReason;
}
