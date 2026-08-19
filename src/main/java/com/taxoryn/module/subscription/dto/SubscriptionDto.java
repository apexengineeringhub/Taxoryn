package com.taxoryn.module.subscription.dto;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization Subscription Details")
public class SubscriptionDto {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private BillingInterval billingInterval;
    private LocalDate startDate;
    private LocalDate renewalDate;
    private int maxUsers;
    private int maxClients;
    private long maxStorageBytes;
    private BigDecimal price;
    private boolean autoRenew;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;
}
