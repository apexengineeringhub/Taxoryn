package com.taxoryn.module.engagement.repository;

import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EngagementRepository extends JpaRepository<EngagementEntity, UUID>, JpaSpecificationExecutor<EngagementEntity> {

    Optional<EngagementEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<EngagementEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<EngagementEntity> findAllByOrganizationIdAndStatus(UUID organizationId, EngagementStatus status);

    List<EngagementEntity> findAllByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndClientId(UUID organizationId, UUID clientId);
}
