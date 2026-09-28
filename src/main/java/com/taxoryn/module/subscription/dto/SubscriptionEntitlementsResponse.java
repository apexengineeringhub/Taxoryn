package com.taxoryn.module.subscription.dto;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntitlementsResponse {

    private UUID organizationId;
    private String organizationName;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private BillingInterval billingInterval;
    private LocalDate startDate;
    private LocalDate renewalDate;
    private boolean anyLimitReached;
    private boolean anyWarning;
    private List<EntitlementResult> entitlements;
}
