package com.taxoryn.module.organization.repository;

import com.taxoryn.module.organization.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {

    List<LocationEntity> findAllByOrganizationId(UUID organizationId);

    List<LocationEntity> findAllByOrganizationIdAndIsActiveTrue(UUID organizationId);

    Optional<LocationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationIdAndIsActiveTrue(UUID organizationId);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}
