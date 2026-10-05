package com.taxoryn.module.gov.repository;

import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovOperationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Multi-tenant repository for government integration operations with batch reconciliation queries.
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

    @Query("SELECT op FROM GovIntegrationOperationEntity op WHERE op.status IN :statuses " +
           "AND (op.nextReconciliationAt IS NULL OR op.nextReconciliationAt <= :now) " +
           "ORDER BY COALESCE(op.nextReconciliationAt, op.createdAt) ASC")
    List<GovIntegrationOperationEntity> findEligibleForReconciliation(
            @Param("statuses") List<GovOperationStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("SELECT op FROM GovIntegrationOperationEntity op WHERE op.organizationId = :organizationId " +
           "AND op.status IN :statuses AND (op.nextReconciliationAt IS NULL OR op.nextReconciliationAt <= :now) " +
           "ORDER BY COALESCE(op.nextReconciliationAt, op.createdAt) ASC")
    List<GovIntegrationOperationEntity> findEligibleForTenantReconciliation(
            @Param("organizationId") UUID organizationId,
            @Param("statuses") List<GovOperationStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, GovOperationStatus status);

    @Transactional
    @Modifying
    @Query("UPDATE GovIntegrationOperationEntity op SET op.nextReconciliationAt = :lockUntil, " +
           "op.lastReconciledAt = :now, op.reconciliationAttemptCount = op.reconciliationAttemptCount + 1, " +
           "op.updatedAt = :now WHERE op.id = :id AND op.status = :expectedStatus " +
           "AND (op.nextReconciliationAt IS NULL OR op.nextReconciliationAt <= :now)")
    int claimOperationForReconciliation(
            @Param("id") UUID id,
            @Param("expectedStatus") GovOperationStatus expectedStatus,
            @Param("lockUntil") Instant lockUntil,
            @Param("now") Instant now
    );
}
