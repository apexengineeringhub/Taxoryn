package com.taxoryn.module.subscription.service;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionPlanEntity;
import com.taxoryn.module.subscription.entity.SubscriptionPlanFeatureEntity;
import com.taxoryn.module.subscription.entity.SubscriptionPlanModuleEntity;
import com.taxoryn.module.subscription.repository.SubscriptionPlanFeatureRepository;
import com.taxoryn.module.subscription.repository.SubscriptionPlanModuleRepository;
import com.taxoryn.module.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPlanEntitlementServiceImpl implements SubscriptionPlanEntitlementService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanModuleRepository subscriptionPlanModuleRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEntitled(SubscriptionPlan plan, String moduleCode) {
        if (plan == null || moduleCode == null) {
            return false;
        }
        return isModuleEntitled(plan.name(), moduleCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEntitled(String planCode, String moduleCode) {
        if (planCode == null || moduleCode == null) {
            return false;
        }

        // 1. Check DB-backed plan module entitlement
        Optional<SubscriptionPlanModuleEntity> entitlement = subscriptionPlanModuleRepository
                .findByPlanCodeAndModuleCode(planCode.toUpperCase(), moduleCode.toUpperCase());

        if (entitlement.isPresent()) {
            return entitlement.get().isIncluded();
        }

        // 2. Default fallback: All standard compliance and operations modules are entitled across active tiers
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureEntitled(SubscriptionPlan plan, String moduleCode, String featureCode) {
        if (plan == null || moduleCode == null || featureCode == null) {
            return false;
        }
        return isFeatureEntitled(plan.name(), moduleCode, featureCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureEntitled(String planCode, String moduleCode, String featureCode) {
        if (planCode == null || moduleCode == null || featureCode == null) {
            return false;
        }

        // Check if module itself is entitled
        if (!isModuleEntitled(planCode, moduleCode)) {
            return false;
        }

        Optional<SubscriptionPlanFeatureEntity> entitlement = subscriptionPlanFeatureRepository
                .findByPlanCodeAndModuleCodeAndFeatureCode(planCode.toUpperCase(), moduleCode.toUpperCase(), featureCode.toUpperCase());

        if (entitlement.isPresent()) {
            return entitlement.get().isIncluded();
        }

        // Default: Feature is entitled if not explicitly excluded
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getEntitledModules(SubscriptionPlan plan) {
        if (plan == null) {
            return Set.of();
        }
        return getEntitledModules(plan.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getEntitledModules(String planCode) {
        if (planCode == null) {
            return Set.of();
        }
        List<SubscriptionPlanModuleEntity> entitlements = subscriptionPlanModuleRepository
                .findByPlanCodeAndIsIncludedTrue(planCode.toUpperCase());

        if (!entitlements.isEmpty()) {
            return entitlements.stream()
                    .map(SubscriptionPlanModuleEntity::getModuleCode)
                    .collect(Collectors.toSet());
        }

        // Fallback: standard modules
        return Set.of("CLIENTS", "TASKS", "DOCUMENTS", "DOCUMENT_REQUESTS", "CLIENT_PORTAL", "NOTIFICATIONS", "AUDIT", "GST", "ITR", "TDS", "TAX_NOTICES", "BILLING", "REPORTS");
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getEntitledFeatures(String planCode, String moduleCode) {
        if (planCode == null || moduleCode == null) {
            return Set.of();
        }
        List<SubscriptionPlanFeatureEntity> entitlements = subscriptionPlanFeatureRepository
                .findByPlanCodeAndModuleCodeAndIsIncludedTrue(planCode.toUpperCase(), moduleCode.toUpperCase());

        return entitlements.stream()
                .map(SubscriptionPlanFeatureEntity::getFeatureCode)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMultiLocationEnabled(SubscriptionPlan plan) {
        if (plan == null) {
            return false;
        }
        return isMultiLocationEnabled(plan.name());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMultiLocationEnabled(String planCode) {
        if (planCode == null) {
            return false;
        }
        return subscriptionPlanRepository.findByCode(planCode.toUpperCase())
                .map(SubscriptionPlanEntity::isMultiLocationEnabled)
                .orElseGet(() -> !planCode.equalsIgnoreCase("STARTER"));
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxLocations(SubscriptionPlan plan) {
        if (plan == null) {
            return 1;
        }
        return getMaxLocations(plan.name());
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxLocations(String planCode) {
        if (planCode == null) {
            return 1;
        }
        return subscriptionPlanRepository.findByCode(planCode.toUpperCase())
                .map(SubscriptionPlanEntity::getMaxLocations)
                .orElseGet(() -> switch (planCode.toUpperCase()) {
                    case "STARTER" -> 1;
                    case "PROFESSIONAL" -> 3;
                    case "BUSINESS" -> 10;
                    case "ENTERPRISE" -> 50;
                    default -> 1;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanEntity getPlanMaster(String planCode) {
        if (planCode == null) {
            return null;
        }
        return subscriptionPlanRepository.findByCode(planCode.toUpperCase()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanEntity> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }
}
