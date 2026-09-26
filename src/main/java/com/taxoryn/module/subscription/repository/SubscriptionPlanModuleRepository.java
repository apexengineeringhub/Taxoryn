package com.taxoryn.module.subscription.repository;

import com.taxoryn.module.subscription.entity.SubscriptionPlanModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanModuleRepository extends JpaRepository<SubscriptionPlanModuleEntity, UUID> {

    List<SubscriptionPlanModuleEntity> findByPlanCode(String planCode);

    List<SubscriptionPlanModuleEntity> findByPlanCodeAndIsIncludedTrue(String planCode);

    Optional<SubscriptionPlanModuleEntity> findByPlanCodeAndModuleCode(String planCode, String moduleCode);

    boolean existsByPlanCodeAndModuleCodeAndIsIncludedTrue(String planCode, String moduleCode);
}
