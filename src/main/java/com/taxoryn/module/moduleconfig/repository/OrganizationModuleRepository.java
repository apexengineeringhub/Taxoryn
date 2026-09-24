package com.taxoryn.module.moduleconfig.repository;

import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationModuleRepository extends JpaRepository<OrganizationModuleEntity, UUID> {

    List<OrganizationModuleEntity> findByOrganizationId(UUID organizationId);

    Optional<OrganizationModuleEntity> findByOrganizationIdAndModuleCode(UUID organizationId, ProductModuleCode moduleCode);

    boolean existsByOrganizationIdAndModuleCodeAndEnabledTrue(UUID organizationId, ProductModuleCode moduleCode);
}
