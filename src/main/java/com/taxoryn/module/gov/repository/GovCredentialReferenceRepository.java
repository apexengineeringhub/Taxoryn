package com.taxoryn.module.gov.repository;

import com.taxoryn.module.gov.entity.GovCredentialReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovCredentialReferenceRepository extends JpaRepository<GovCredentialReferenceEntity, UUID> {

    Optional<GovCredentialReferenceEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GovCredentialReferenceEntity> findAllByOrganizationIdAndConnectionId(UUID organizationId, UUID connectionId);

    Optional<GovCredentialReferenceEntity> findByConnectionIdAndOrganizationId(UUID connectionId, UUID organizationId);
}
