package com.taxoryn.module.moduleconfig.repository;

import com.taxoryn.module.moduleconfig.entity.OrganizationFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationFeatureRepository extends JpaRepository<OrganizationFeatureEntity, UUID> {

    List<OrganizationFeatureEntity> findByOrganizationId(UUID organizationId);

    List<OrganizationFeatureEntity> findByOrganizationIdAndModuleCode(UUID organizationId, String moduleCode);

    Optional<OrganizationFeatureEntity> findByOrganizationIdAndModuleCodeAndFeatureCode(UUID organizationId, String moduleCode, String featureCode);

    boolean existsByOrganizationIdAndModuleCodeAndFeatureCode(UUID organizationId, String moduleCode, String featureCode);
}
