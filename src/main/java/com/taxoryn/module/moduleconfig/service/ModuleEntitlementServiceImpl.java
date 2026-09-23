package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleEntitlementServiceImpl implements ModuleEntitlementService {

    private final ModuleConfigurationService moduleConfigurationService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    public void verifyModuleAndSubscriptionAccess(ProductModuleCode moduleCode) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        verifyModuleAndSubscriptionAccess(organizationId, moduleCode);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyModuleAndSubscriptionAccess(UUID organizationId, ProductModuleCode moduleCode) {
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        if (moduleCode == null) {
            return;
        }

        // 1. Product Module Gate
        if (!moduleConfigurationService.isModuleEnabled(organizationId, moduleCode)) {
            log.warn("Access denied: Product module {} is disabled for organization {}", moduleCode, organizationId);
            throw new ForbiddenException("Product module " + moduleCode.name() + " is disabled for this organization");
        }

        // 2. Subscription Commercial Status Gate
        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    try {
                        return subscriptionService.createInitialSubscription(organizationId, SubscriptionPlan.STARTER);
                    } catch (Exception e) {
                        return subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
                    }
                });

        if (subscription != null) {
            if (!isSubscriptionStatusValid(subscription.getStatus())) {
                log.warn("Access denied: Subscription inactive ({}) for organization {} accessing module {}",
                        subscription.getStatus(), organizationId, moduleCode);
                throw new ForbiddenException("Active subscription required to access " + moduleCode.name() +
                        ". Current subscription status: " + subscription.getStatus());
            }

            // 3. Subscription Plan Tier Entitlement Gate
            if (!isModuleEntitledForPlan(subscription.getPlan(), moduleCode)) {
                log.warn("Access denied: Plan {} does not entitle module {} for organization {}",
                        subscription.getPlan(), moduleCode, organizationId);
                throw new ForbiddenException("Current subscription plan (" + subscription.getPlan().name() +
                        ") does not include module " + moduleCode.name() + ". Please upgrade your subscription.");
            }
        }
    }

    @Override
    public boolean isSubscriptionStatusValid(SubscriptionStatus status) {
        if (status == null) {
            return false;
        }
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING;
    }

    @Override
    public boolean isModuleEntitledForPlan(SubscriptionPlan plan, ProductModuleCode moduleCode) {
        if (plan == null || moduleCode == null) {
            return false;
        }
        return getEntitledModulesForPlan(plan).contains(moduleCode);
    }

    @Override
    public Set<ProductModuleCode> getEntitledModulesForPlan(SubscriptionPlan plan) {
        if (plan == null) {
            return EnumSet.noneOf(ProductModuleCode.class);
        }

        return switch (plan) {
            case STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE -> EnumSet.allOf(ProductModuleCode.class);
        };
    }
}
