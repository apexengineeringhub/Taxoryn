package com.taxoryn.module.organization.repository;

import com.taxoryn.module.organization.entity.OrganizationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettingsEntity, UUID> {

    Optional<OrganizationSettingsEntity> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationId(UUID organizationId);
}
