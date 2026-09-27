package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID>, JpaSpecificationExecutor<PromotionEntity> {

    Optional<PromotionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<PromotionEntity> findAllByOrganizationId(UUID organizationId);

    List<PromotionEntity> findAllByOrganizationIdAndActiveTrue(UUID organizationId);

    List<PromotionEntity> findAllByOrganizationIdAndActiveTrueOrderByPriorityDesc(UUID organizationId);

    Optional<PromotionEntity> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
