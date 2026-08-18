package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    Page<ClientEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<ClientEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ClientEntity> findByOrganizationIdAndPan(UUID organizationId, String pan);

    Optional<ClientEntity> findByOrganizationIdAndGstin(UUID organizationId, String gstin);
}
