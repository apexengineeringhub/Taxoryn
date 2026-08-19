package com.taxoryn.module.subscription.dto;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SaaS Subscription Tier Plan Definition & Features")
public class SubscriptionPlanDto {

    private SubscriptionPlan plan;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private int maxUsers;
    private int maxClients;
    private long maxStorageBytes;
    private String formattedStorage;
    private List<String> features;
    private boolean isPopular;
}
