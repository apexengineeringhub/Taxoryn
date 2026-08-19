package com.taxoryn.module.subscription.service;

import com.taxoryn.module.subscription.dto.ChangePlanRequest;
import com.taxoryn.module.subscription.dto.SubscriptionDto;
import com.taxoryn.module.subscription.dto.SubscriptionPlanDto;
import com.taxoryn.module.subscription.dto.SubscriptionUsageDto;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    SubscriptionDto getCurrentSubscription();

    SubscriptionDto getOrganizationSubscription(UUID organizationId);

    SubscriptionUsageDto getSubscriptionUsage(UUID organizationId);

    List<SubscriptionPlanDto> getAvailablePlans();

    SubscriptionDto changePlan(UUID organizationId, ChangePlanRequest request);

    SubscriptionDto cancelSubscription(UUID organizationId);

    SubscriptionDto renewSubscription(UUID organizationId);

    SubscriptionEntity createInitialSubscription(UUID organizationId, SubscriptionPlan plan);

    void checkUserLimit(UUID organizationId);

    void checkClientLimit(UUID organizationId);

    void checkStorageLimit(UUID organizationId, long additionalBytes);
}
