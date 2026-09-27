package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Update Billing Profile Payload")
public class UpdateBillingProfileRequest {

    private UUID engagementId;
    private String billingFrequency;
    private String currency;
    private BigDecimal defaultRate;
    private Boolean taxApplicable;
    private Boolean active;
    private String notes;
}
