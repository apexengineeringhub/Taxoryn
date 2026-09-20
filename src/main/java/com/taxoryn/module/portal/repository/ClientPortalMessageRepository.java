package com.taxoryn.module.portal.repository;

import com.taxoryn.module.portal.entity.ClientPortalMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientPortalMessageRepository extends JpaRepository<ClientPortalMessageEntity, UUID> {

    Optional<ClientPortalMessageEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ClientPortalMessageEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtAsc(UUID organizationId, UUID clientId);

    List<ClientPortalMessageEntity> findAllByClientIdOrderByCreatedAtAsc(UUID clientId);

    long countByClientIdAndReadByClientFalse(UUID clientId);

    long countByOrganizationIdAndClientIdAndReadByPracticeFalse(UUID organizationId, UUID clientId);

    @Modifying
    @Query("UPDATE ClientPortalMessageEntity m SET m.readByClient = true, m.readAt = :readAt WHERE m.clientId = :clientId AND m.readByClient = false AND m.senderType = 'PRACTICE'")
    int markAllReadByClient(@Param("clientId") UUID clientId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE ClientPortalMessageEntity m SET m.readByPractice = true, m.readAt = :readAt WHERE m.organizationId = :organizationId AND m.clientId = :clientId AND m.readByPractice = false AND m.senderType = 'CLIENT'")
    int markAllReadByPractice(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId, @Param("readAt") Instant readAt);
}
