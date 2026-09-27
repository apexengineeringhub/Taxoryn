package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientLocationAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientLocationAssignmentRepository extends JpaRepository<ClientLocationAssignmentEntity, UUID> {

    List<ClientLocationAssignmentEntity> findAllByClientIdAndOrganizationIdAndActiveTrue(UUID clientId, UUID organizationId);

    List<ClientLocationAssignmentEntity> findAllByClientIdAndOrganizationId(UUID clientId, UUID organizationId);

    List<ClientLocationAssignmentEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    Optional<ClientLocationAssignmentEntity> findByClientIdAndLocationIdAndOrganizationId(UUID clientId, UUID locationId, UUID organizationId);

    Optional<ClientLocationAssignmentEntity> findByOrganizationIdAndClientIdAndLocationId(UUID organizationId, UUID clientId, UUID locationId);

    Optional<ClientLocationAssignmentEntity> findByClientIdAndLocationIdAndOrganizationIdAndActiveTrue(UUID clientId, UUID locationId, UUID organizationId);

    Optional<ClientLocationAssignmentEntity> findByClientIdAndOrganizationIdAndPrimaryLocationTrueAndActiveTrue(UUID clientId, UUID organizationId);

    @Query("SELECT cla.clientId FROM ClientLocationAssignmentEntity cla WHERE cla.locationId IN :locationIds AND cla.organizationId = :organizationId AND cla.active = true")
    List<UUID> findActiveClientIdsByLocationIdsAndOrganizationId(@Param("locationIds") Collection<UUID> locationIds, @Param("organizationId") UUID organizationId);

    @Query("SELECT cla.locationId FROM ClientLocationAssignmentEntity cla WHERE cla.clientId = :clientId AND cla.organizationId = :organizationId AND cla.active = true")
    List<UUID> findActiveLocationIdsByClientIdAndOrganizationId(@Param("clientId") UUID clientId, @Param("organizationId") UUID organizationId);

    long countByClientIdAndOrganizationIdAndActiveTrue(UUID clientId, UUID organizationId);

    @Modifying
    @Query("UPDATE ClientLocationAssignmentEntity cla SET cla.primaryLocation = false WHERE cla.clientId = :clientId AND cla.organizationId = :organizationId")
    void unsetPrimaryLocationForClient(@Param("clientId") UUID clientId, @Param("organizationId") UUID organizationId);

    @Modifying
    @Query("UPDATE ClientLocationAssignmentEntity cla SET cla.primaryLocation = false WHERE cla.clientId = :clientId AND cla.organizationId = :organizationId")
    void clearPrimaryByOrganizationIdAndClientId(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId);
}
