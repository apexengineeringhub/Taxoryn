package com.taxoryn.module.service.repository;

import com.taxoryn.module.service.entity.PracticeServicePricingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeServicePricingRepository extends JpaRepository<PracticeServicePricingEntity, UUID> {
    Optional<PracticeServicePricingEntity> findByOrganizationIdAndTaxServiceId(UUID organizationId, UUID taxServiceId);
    List<PracticeServicePricingEntity> findAllByOrganizationId(UUID organizationId);
}
