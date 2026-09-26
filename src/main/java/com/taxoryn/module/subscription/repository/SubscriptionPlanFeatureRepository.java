package com.taxoryn.module.subscription.repository;

import com.taxoryn.module.subscription.entity.SubscriptionPlanFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanFeatureRepository extends JpaRepository<SubscriptionPlanFeatureEntity, UUID> {

    List<SubscriptionPlanFeatureEntity> findByPlanCode(String planCode);

    List<SubscriptionPlanFeatureEntity> findByPlanCodeAndModuleCode(String planCode, String moduleCode);

    List<SubscriptionPlanFeatureEntity> findByPlanCodeAndModuleCodeAndIsIncludedTrue(String planCode, String moduleCode);

    Optional<SubscriptionPlanFeatureEntity> findByPlanCodeAndModuleCodeAndFeatureCode(String planCode, String moduleCode, String featureCode);

    boolean existsByPlanCodeAndModuleCodeAndFeatureCodeAndIsIncludedTrue(String planCode, String moduleCode, String featureCode);
}
