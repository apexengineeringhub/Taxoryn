package com.taxoryn.module.subscription.dto;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization Subscription Usage & Limit Metrics")
public class SubscriptionUsageDto {

    private UUID organizationId;
    private String organizationName;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDate renewalDate;

    // Users
    private long currentUsers;
    private int maxUsers;
    private double usersUsagePercentage;
    private boolean userLimitReached;

    // Clients
    private long currentClients;
    private int maxClients;
    private double clientsUsagePercentage;
    private boolean clientLimitReached;

    // Storage
    private long currentStorageBytes;
    private long maxStorageBytes;
    private double storageUsagePercentage;
    private boolean storageLimitReached;
}
