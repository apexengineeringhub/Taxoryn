package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Billing Profile Payload")
public class CreateBillingProfileRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    private UUID engagementId;

    @Builder.Default
    private String billingFrequency = "MONTHLY";

    @Builder.Default
    private String currency = "INR";

    private BigDecimal defaultRate;

    @Builder.Default
    private Boolean taxApplicable = true;

    @Builder.Default
    private Boolean active = true;

    private String notes;
}
