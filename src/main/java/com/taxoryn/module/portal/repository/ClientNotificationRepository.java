package com.taxoryn.module.portal.repository;

import com.taxoryn.module.portal.entity.ClientNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientNotificationRepository extends JpaRepository<ClientNotificationEntity, UUID> {

    Optional<ClientNotificationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ClientNotificationEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<ClientNotificationEntity> findTop10ByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientIdAndReadFalse(UUID organizationId, UUID clientId);
}
