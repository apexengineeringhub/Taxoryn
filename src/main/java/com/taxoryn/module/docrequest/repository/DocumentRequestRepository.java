package com.taxoryn.module.docrequest.repository;

import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequestEntity, UUID>, JpaSpecificationExecutor<DocumentRequestEntity> {

    Optional<DocumentRequestEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<DocumentRequestEntity> findByIdAndClientId(UUID id, UUID clientId);

    List<DocumentRequestEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<DocumentRequestEntity> findAllByOrganizationIdAndGstFilingId(UUID organizationId, UUID gstFilingId);

    List<DocumentRequestEntity> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);

    Page<DocumentRequestEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    long countByOrganizationIdAndStatus(UUID organizationId, RequestStatus status);

    List<DocumentRequestEntity> findAllByOrganizationIdAndStatusIn(UUID organizationId, java.util.Collection<RequestStatus> statuses);

    List<DocumentRequestEntity> findAllByOrganizationIdAndClientIdAndStatusIn(UUID organizationId, UUID clientId, java.util.Collection<RequestStatus> statuses);
}