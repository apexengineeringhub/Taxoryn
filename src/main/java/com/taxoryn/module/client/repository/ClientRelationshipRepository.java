package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientRelationshipEntity;
import com.taxoryn.module.client.entity.ClientRelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRelationshipRepository extends JpaRepository<ClientRelationshipEntity, UUID> {

    @Query("SELECT r FROM ClientRelationshipEntity r WHERE r.organizationId = :organizationId AND (r.sourceClientId = :clientId OR r.targetClientId = :clientId)")
    List<ClientRelationshipEntity> findAllForClient(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId);

    List<ClientRelationshipEntity> findAllByOrganizationIdAndSourceClientId(UUID organizationId, UUID sourceClientId);

    List<ClientRelationshipEntity> findAllByOrganizationIdAndTargetClientId(UUID organizationId, UUID targetClientId);

    Optional<ClientRelationshipEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

    boolean existsByOrganizationIdAndSourceClientIdAndTargetClientIdAndRelationshipType(
            UUID organizationId, UUID sourceClientId, UUID targetClientId, ClientRelationshipType relationshipType);

    @Query("SELECT COUNT(r) FROM ClientRelationshipEntity r WHERE r.organizationId = :organizationId AND (r.sourceClientId = :clientId OR r.targetClientId = :clientId)")
    long countAllForClient(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId);
}
