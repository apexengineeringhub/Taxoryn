package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID>, JpaSpecificationExecutor<ClientEntity> {

    Optional<ClientEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndPan(UUID organizationId, String pan);

    boolean existsByOrganizationIdAndGstin(UUID organizationId, String gstin);

    Page<ClientEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    java.util.List<ClientEntity> findAllByOrganizationId(UUID organizationId);

    java.util.List<ClientEntity> findAllByOrganizationIdAndStatus(UUID organizationId, ClientEntity.ClientStatus status);

    long countByOrganizationId(UUID organizationId);
}
