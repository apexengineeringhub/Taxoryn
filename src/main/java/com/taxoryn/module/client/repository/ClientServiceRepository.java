package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientServiceRepository extends JpaRepository<ClientServiceEntity, UUID> {

    List<ClientServiceEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    Optional<ClientServiceEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ClientServiceEntity> findByIdAndOrganizationIdAndClientId(UUID id, UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndClientIdAndServiceTypeAndStatus(UUID organizationId, UUID clientId, ClientServiceType serviceType, ClientServiceStatus status);

    List<ClientServiceEntity> findAllByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, ClientServiceStatus status);

    long countByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, ClientServiceStatus status);
}
