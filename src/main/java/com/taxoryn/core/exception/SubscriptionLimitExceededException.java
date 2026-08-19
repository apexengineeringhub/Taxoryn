package com.taxoryn.core.exception;

public class SubscriptionLimitExceededException extends AppException {

    public SubscriptionLimitExceededException(String message) {
        super(ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED, message);
    }

    public SubscriptionLimitExceededException(String limitType, long currentUsage, long maxLimit, String planName) {
        super(ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED,
                String.format("Subscription limit exceeded for %s: Currently using %d of %d allowed on the %s plan. Please upgrade your subscription to continue.",
                        limitType, currentUsage, maxLimit, planName));
    }
}
