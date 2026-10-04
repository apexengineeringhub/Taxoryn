package com.taxoryn.module.dsc.repository;

import com.taxoryn.module.dsc.entity.DscEntity;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DscRepository extends JpaRepository<DscEntity, UUID>, JpaSpecificationExecutor<DscEntity> {

    Optional<DscEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<DscEntity> findByOrganizationId(UUID organizationId);

    List<DscEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, DscStatus status);

    @Query("SELECT COUNT(d) FROM DscEntity d WHERE d.organizationId = :organizationId AND d.status NOT IN ('REVOKED', 'INACTIVE') AND d.expiryDate < :today")
    long countExpiredByOrganizationId(@Param("organizationId") UUID organizationId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(d) FROM DscEntity d WHERE d.organizationId = :organizationId AND d.status NOT IN ('REVOKED', 'INACTIVE') AND d.expiryDate >= :today AND d.expiryDate <= :cutoffDate")
    long countExpiringSoonByOrganizationId(@Param("organizationId") UUID organizationId, @Param("today") LocalDate today, @Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT COUNT(d) FROM DscEntity d WHERE d.organizationId = :organizationId AND d.status NOT IN ('REVOKED', 'INACTIVE') AND d.expiryDate > :cutoffDate")
    long countActiveByOrganizationId(@Param("organizationId") UUID organizationId, @Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT d FROM DscEntity d WHERE d.organizationId = :organizationId AND d.status NOT IN ('REVOKED', 'INACTIVE') AND d.expiryDate >= :today AND d.expiryDate <= :cutoffDate")
    List<DscEntity> findExpiringSoon(@Param("organizationId") UUID organizationId, @Param("today") LocalDate today, @Param("cutoffDate") LocalDate cutoffDate);
}
