package com.taxoryn.core.exception;

import com.taxoryn.module.subscription.entity.SubscriptionResourceType;
import lombok.Getter;

@Getter
public class SubscriptionLimitExceededException extends AppException {

    private String limitType;
    private long currentUsage;
    private long maxLimit;
    private String planName;

    public SubscriptionLimitExceededException(String message) {
        super(ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED, message);
    }

    public SubscriptionLimitExceededException(String limitType, long currentUsage, long maxLimit, String planName) {
        super(ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED,
                String.format("Subscription limit exceeded for %s: Currently using %d of %d allowed on the %s plan. Please upgrade your subscription to continue.",
                        limitType, currentUsage, maxLimit, planName));
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.maxLimit = maxLimit;
        this.planName = planName;
    }

    public SubscriptionLimitExceededException(SubscriptionResourceType resourceType, long currentUsage, long maxLimit, String planName) {
        super(ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED,
                String.format("Subscription limit reached for %s: Currently using %d of %d allowed on the %s plan. Please upgrade your subscription to continue.",
                        resourceType != null ? resourceType.getDisplayName() : "Resource", currentUsage, maxLimit, planName));
        this.limitType = resourceType != null ? resourceType.name() : "RESOURCE";
        this.currentUsage = currentUsage;
        this.maxLimit = maxLimit;
        this.planName = planName;
    }
}
