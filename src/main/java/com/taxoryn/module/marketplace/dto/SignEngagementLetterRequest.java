package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer sign-off on Engagement Letter & Fee Terms")
public class SignEngagementLetterRequest {

    @NotNull(message = "Engagement letter consent is required")
    private Boolean signedConsent;

    @NotNull(message = "Fee agreement consent is required")
    private Boolean agreedToFees;

    private String signatureName;
    private String signatureIpAddress;
}
