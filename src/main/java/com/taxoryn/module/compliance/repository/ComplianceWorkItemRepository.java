package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceWorkItemEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceWorkItemRepository extends JpaRepository<ComplianceWorkItemEntity, UUID>, JpaSpecificationExecutor<ComplianceWorkItemEntity> {

    Optional<ComplianceWorkItemEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ComplianceWorkItemEntity> findAllByOrganizationIdAndClientIdOrderByStatutoryDueDateAsc(UUID organizationId, UUID clientId);

    Page<ComplianceWorkItemEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    Page<ComplianceWorkItemEntity> findAllByOrganizationIdAndClientServiceId(UUID organizationId, UUID clientServiceId, Pageable pageable);

    List<ComplianceWorkItemEntity> findAllByOrganizationIdAndClientServiceId(UUID organizationId, UUID clientServiceId);

    long countByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, ComplianceWorkStatus status);

    @Query("SELECT w FROM ComplianceWorkItemEntity w WHERE w.organizationId = :orgId AND w.clientId IN :clientIds ORDER BY w.statutoryDueDate ASC")
    List<ComplianceWorkItemEntity> findAllByOrganizationIdAndClientIdIn(
            @Param("orgId") UUID organizationId,
            @Param("clientIds") Collection<UUID> clientIds
    );

    @Query("SELECT COUNT(w) FROM ComplianceWorkItemEntity w WHERE w.organizationId = :orgId AND w.status NOT IN ('COMPLETED', 'CANCELLED') AND w.statutoryDueDate < :currentDate")
    long countOverdue(
            @Param("orgId") UUID organizationId,
            @Param("currentDate") LocalDate currentDate
    );
}
