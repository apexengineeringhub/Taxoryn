package com.taxoryn.module.document.repository;

import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID>, JpaSpecificationExecutor<DocumentEntity> {

    Optional<DocumentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<DocumentEntity> findAllByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, DocumentStatus status);

    List<DocumentEntity> findAllByOrganizationIdAndGstFilingIdAndStatus(UUID organizationId, UUID gstFilingId, DocumentStatus status);

    List<DocumentEntity> findAllByOrganizationIdAndItrReturnIdAndStatus(UUID organizationId, UUID itrReturnId, DocumentStatus status);

    List<DocumentEntity> findAllByOrganizationIdAndTaskIdAndStatus(UUID organizationId, UUID taskId, DocumentStatus status);

    long countByOrganizationIdAndStatus(UUID organizationId, DocumentStatus status);
}
