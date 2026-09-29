package com.taxoryn.module.subscription.service;

import com.taxoryn.module.subscription.dto.EntitlementResult;
import com.taxoryn.module.subscription.dto.SubscriptionEntitlementsResponse;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionResourceType;

import java.util.UUID;

public interface SubscriptionEntitlementService {

    EntitlementResult getEntitlement(UUID organizationId, SubscriptionResourceType resourceType);

    SubscriptionEntitlementsResponse getAllEntitlements(UUID organizationId);

    void checkCanCreate(UUID organizationId, SubscriptionResourceType resourceType);

    void checkCanStore(UUID organizationId, long additionalBytes);

    void validateDowngrade(UUID organizationId, SubscriptionPlan targetPlan);
}
