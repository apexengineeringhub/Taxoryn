package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;

import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for enforcing product module availability and commercial subscription entitlements.
 */
public interface ModuleEntitlementService {

    /**
     * Verifies that the given module is enabled for the organization AND that the organization
     * has an active subscription entitled to this module.
     * Throws ForbiddenException if disabled, expired/canceled, or not included in the plan.
     */
    void verifyModuleAndSubscriptionAccess(UUID organizationId, ProductModuleCode moduleCode);

    /**
     * Convenience method resolving the current organization context from SecurityUtils.
     */
    void verifyModuleAndSubscriptionAccess(ProductModuleCode moduleCode);

    /**
     * Checks if a module is entitled for a given subscription plan tier.
     */
    boolean isModuleEntitledForPlan(SubscriptionPlan plan, ProductModuleCode moduleCode);

    /**
     * Checks if a subscription status is active/valid for access.
     */
    boolean isSubscriptionStatusValid(SubscriptionStatus status);

    /**
     * Gets all entitled product modules for a given subscription plan tier.
     */
    Set<ProductModuleCode> getEntitledModulesForPlan(SubscriptionPlan plan);
}
