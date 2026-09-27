package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Billing Profile Model")
public class BillingProfileDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private UUID engagementId;
    private String engagementName;
    private String billingFrequency;
    private String currency;
    private BigDecimal defaultRate;
    private Boolean taxApplicable;
    private Boolean active;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
