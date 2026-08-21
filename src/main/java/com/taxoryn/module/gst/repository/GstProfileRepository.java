package com.taxoryn.module.gst.repository;

import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GstProfileRepository extends JpaRepository<GstProfileEntity, UUID>, JpaSpecificationExecutor<GstProfileEntity> {

    Optional<GstProfileEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<GstProfileEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<GstProfileEntity> findAllByOrganizationId(UUID organizationId);

    List<GstProfileEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<GstProfileEntity> findAllByOrganizationIdAndStatus(UUID organizationId, GstProfileStatus status);

    boolean existsByOrganizationIdAndGstin(UUID organizationId, String gstin);

    Optional<GstProfileEntity> findByOrganizationIdAndGstin(UUID organizationId, String gstin);

    @Query("SELECT COUNT(DISTINCT p.clientId) FROM GstProfileEntity p WHERE p.organizationId = :organizationId")
    long countDistinctClientsByOrganizationId(@Param("organizationId") UUID organizationId);
}
