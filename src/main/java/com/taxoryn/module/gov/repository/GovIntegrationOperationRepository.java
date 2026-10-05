package com.taxoryn.module.gov.repository;

import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovOperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Multi-tenant repository for government integration operations.
 */
@Repository
public interface GovIntegrationOperationRepository extends JpaRepository<GovIntegrationOperationEntity, UUID> {

    Optional<GovIntegrationOperationEntity> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    Optional<GovIntegrationOperationEntity> findByOrganizationIdAndCorrelationId(UUID organizationId, String correlationId);

    Optional<GovIntegrationOperationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GovIntegrationOperationEntity> findByOrganizationIdAndStatus(UUID organizationId, GovOperationStatus status);

    List<GovIntegrationOperationEntity> findByOrganizationIdAndStatusIn(UUID organizationId, List<GovOperationStatus> statuses);

    List<GovIntegrationOperationEntity> findByOrganizationIdAndBusinessEntityTypeAndBusinessEntityId(
            UUID organizationId, String businessEntityType, UUID businessEntityId);
}
