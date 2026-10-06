package com.taxoryn.module.gov.outbox.repository;

import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Multi-tenant repository for government outbox events with atomic concurrency claiming support.
 */
@Repository
public interface GovOutboxEventRepository extends JpaRepository<GovOutboxEventEntity, UUID> {

    Optional<GovOutboxEventEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GovOutboxEventEntity> findByOrganizationIdAndStatus(UUID organizationId, GovOutboxStatus status);

    List<GovOutboxEventEntity> findByOrganizationIdAndCorrelationId(UUID organizationId, String correlationId);

    List<GovOutboxEventEntity> findByOrganizationIdAndOperationId(UUID organizationId, UUID operationId);

    long countByOrganizationIdAndStatus(UUID organizationId, GovOutboxStatus status);

    long countByStatus(GovOutboxStatus status);

    @Query("SELECT e FROM GovOutboxEventEntity e WHERE e.status = :status AND e.availableAt <= :now ORDER BY e.availableAt ASC")
    List<GovOutboxEventEntity> findPendingEvents(
            @Param("status") GovOutboxStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("SELECT e FROM GovOutboxEventEntity e WHERE e.status = :status AND e.lockedAt <= :staleBefore ORDER BY e.lockedAt ASC")
    List<GovOutboxEventEntity> findStaleLockedEvents(
            @Param("status") GovOutboxStatus status,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable
    );

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("UPDATE GovOutboxEventEntity e SET e.status = :newStatus, e.lockedAt = :lockedAt, e.updatedAt = :now WHERE e.id = :id AND e.status = :expectedStatus")
    int claimEvent(
            @Param("id") UUID id,
            @Param("expectedStatus") GovOutboxStatus expectedStatus,
            @Param("newStatus") GovOutboxStatus newStatus,
            @Param("lockedAt") Instant lockedAt,
            @Param("now") Instant now
    );
}
