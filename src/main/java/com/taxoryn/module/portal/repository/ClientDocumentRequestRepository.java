package com.taxoryn.module.portal.repository;

import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientDocumentRequestRepository extends JpaRepository<ClientDocumentRequestEntity, UUID> {

    Optional<ClientDocumentRequestEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ClientDocumentRequestEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<ClientDocumentRequestEntity> findAllByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, RequestStatus status);

    long countByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, RequestStatus status);
}
