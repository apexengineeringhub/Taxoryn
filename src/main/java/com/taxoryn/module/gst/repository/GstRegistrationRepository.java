package com.taxoryn.module.gst.repository;

import com.taxoryn.module.gst.entity.GstRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GstRegistrationRepository extends JpaRepository<GstRegistrationEntity, UUID>, JpaSpecificationExecutor<GstRegistrationEntity> {

    Optional<GstRegistrationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GstRegistrationEntity> findAllByOrganizationId(UUID organizationId);

    List<GstRegistrationEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<GstRegistrationEntity> findAllByOrganizationIdAndLocationId(UUID organizationId, UUID locationId);

    Optional<GstRegistrationEntity> findByOrganizationIdAndGstin(UUID organizationId, String gstin);

    boolean existsByOrganizationIdAndGstin(UUID organizationId, String gstin);

    long countByOrganizationIdAndClientId(UUID organizationId, UUID clientId);
}
