package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientUserAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientUserAssignmentRepository extends JpaRepository<ClientUserAssignmentEntity, UUID> {

    List<ClientUserAssignmentEntity> findAllByClientIdAndOrganizationIdAndActiveTrue(UUID clientId, UUID organizationId);

    List<ClientUserAssignmentEntity> findAllByClientIdAndOrganizationId(UUID clientId, UUID organizationId);

    List<ClientUserAssignmentEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    Optional<ClientUserAssignmentEntity> findByClientIdAndUserIdAndOrganizationId(UUID clientId, UUID userId, UUID organizationId);

    Optional<ClientUserAssignmentEntity> findByOrganizationIdAndClientIdAndUserId(UUID organizationId, UUID clientId, UUID userId);

    Optional<ClientUserAssignmentEntity> findByClientIdAndUserIdAndOrganizationIdAndActiveTrue(UUID clientId, UUID userId, UUID organizationId);

    Optional<ClientUserAssignmentEntity> findByClientIdAndOrganizationIdAndPrimaryResponsibleTrueAndActiveTrue(UUID clientId, UUID organizationId);

    @Query("SELECT cua.clientId FROM ClientUserAssignmentEntity cua WHERE cua.userId = :userId AND cua.organizationId = :organizationId AND cua.active = true")
    List<UUID> findActiveClientIdsByUserIdAndOrganizationId(@Param("userId") UUID userId, @Param("organizationId") UUID organizationId);

    @Query("SELECT cua.userId FROM ClientUserAssignmentEntity cua WHERE cua.clientId = :clientId AND cua.organizationId = :organizationId AND cua.active = true")
    List<UUID> findActiveUserIdsByClientIdAndOrganizationId(@Param("clientId") UUID clientId, @Param("organizationId") UUID organizationId);

    long countByClientIdAndOrganizationIdAndActiveTrue(UUID clientId, UUID organizationId);

    @Modifying
    @Query("UPDATE ClientUserAssignmentEntity cua SET cua.primaryResponsible = false WHERE cua.clientId = :clientId AND cua.organizationId = :organizationId")
    void unsetPrimaryResponsibleForClient(@Param("clientId") UUID clientId, @Param("organizationId") UUID organizationId);

    @Modifying
    @Query("UPDATE ClientUserAssignmentEntity cua SET cua.primaryResponsible = false WHERE cua.clientId = :clientId AND cua.organizationId = :organizationId")
    void clearPrimaryByOrganizationIdAndClientId(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId);
}
