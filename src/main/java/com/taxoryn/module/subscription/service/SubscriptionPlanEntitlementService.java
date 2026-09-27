package com.taxoryn.module.subscription.service;

import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionPlanEntity;

import java.util.List;
import java.util.Set;

public interface SubscriptionPlanEntitlementService {

    boolean isModuleEntitled(SubscriptionPlan plan, String moduleCode);

    boolean isModuleEntitled(String planCode, String moduleCode);

    boolean isFeatureEntitled(SubscriptionPlan plan, String moduleCode, String featureCode);

    boolean isFeatureEntitled(String planCode, String moduleCode, String featureCode);

    Set<String> getEntitledModules(SubscriptionPlan plan);

    Set<String> getEntitledModules(String planCode);

    Set<String> getEntitledFeatures(String planCode, String moduleCode);

    boolean isMultiLocationEnabled(SubscriptionPlan plan);

    boolean isMultiLocationEnabled(String planCode);

    int getMaxLocations(SubscriptionPlan plan);

    int getMaxLocations(String planCode);

    SubscriptionPlanEntity getPlanMaster(String planCode);

    List<SubscriptionPlanEntity> getAllPlans();
}
