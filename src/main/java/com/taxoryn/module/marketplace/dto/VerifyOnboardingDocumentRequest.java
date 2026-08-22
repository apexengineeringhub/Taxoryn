package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practitioner review decision on a customer onboarding document")
public class VerifyOnboardingDocumentRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;

    private String rejectionReason;
}
