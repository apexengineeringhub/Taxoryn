package com.taxoryn.module.subscription.dto;

import com.taxoryn.module.subscription.entity.SubscriptionResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementResult {

    private SubscriptionResourceType resourceType;
    private long currentUsage;
    private long limit;
    private long remaining;
    private double percentageUsed;
    private boolean allowed;
    private boolean warning;
    private boolean unlimited;
    private String formattedUsage;
    private String message;
}
