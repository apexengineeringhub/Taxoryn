package com.taxoryn.module.tds.repository;

import com.taxoryn.module.tds.entity.TdsProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TdsProfileRepository extends JpaRepository<TdsProfileEntity, UUID> {

    Optional<TdsProfileEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<TdsProfileEntity> findByOrganizationIdAndTan(UUID organizationId, String tan);

    Optional<TdsProfileEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<TdsProfileEntity> findAllByOrganizationId(UUID organizationId);

    List<TdsProfileEntity> findAllByOrganizationIdAndStatus(UUID organizationId, TdsProfileEntity.TdsProfileStatus status);

    @Query("SELECT p FROM TdsProfileEntity p WHERE p.organizationId = :organizationId " +
           "AND (:clientId IS NULL OR p.clientId = :clientId) " +
           "AND (:deductorType IS NULL OR p.deductorType = :deductorType) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:search IS NULL OR LOWER(p.tan) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.responsiblePersonName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TdsProfileEntity> searchProfiles(
            @Param("organizationId") UUID organizationId,
            @Param("clientId") UUID clientId,
            @Param("deductorType") TdsProfileEntity.DeductorType deductorType,
            @Param("status") TdsProfileEntity.TdsProfileStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, TdsProfileEntity.TdsProfileStatus status);

    @Query("SELECT COUNT(DISTINCT p.clientId) FROM TdsProfileEntity p WHERE p.organizationId = :organizationId")
    long countDistinctClientsByOrganizationId(@Param("organizationId") UUID organizationId);
}
