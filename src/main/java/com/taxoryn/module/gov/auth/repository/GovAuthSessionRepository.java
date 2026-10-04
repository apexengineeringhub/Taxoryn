package com.taxoryn.module.gov.auth.repository;

import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovAuthSessionRepository extends JpaRepository<GovAuthSessionEntity, UUID> {

    Optional<GovAuthSessionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GovAuthSessionEntity> findAllByOrganizationId(UUID organizationId);

    List<GovAuthSessionEntity> findAllByOrganizationIdAndConnectionId(UUID organizationId, UUID connectionId);

    Optional<GovAuthSessionEntity> findFirstByOrganizationIdAndConnectionIdAndStatusInOrderByCreatedAtDesc(
            UUID organizationId,
            UUID connectionId,
            Collection<GovAuthStatus> statuses
    );

    List<GovAuthSessionEntity> findAllByOrganizationIdAndStatus(UUID organizationId, GovAuthStatus status);
}
