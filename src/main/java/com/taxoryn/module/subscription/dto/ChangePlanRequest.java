package com.taxoryn.module.subscription.dto;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Upgrade / Downgrade Subscription Plan Payload")
public class ChangePlanRequest {

    @NotNull(message = "Subscription plan is required")
    @Schema(description = "Target subscription tier (STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE)", example = "PROFESSIONAL")
    private SubscriptionPlan plan;

    @Schema(description = "Billing frequency (MONTHLY or YEARLY)", example = "MONTHLY")
    @Builder.Default
    private BillingInterval billingInterval = BillingInterval.MONTHLY;
}
