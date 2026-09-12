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

    List<DocumentRequestEntity> findAllByOrganizationIdAndNoticeId(UUID organizationId, UUID noticeId);

    long countByOrganizationIdAndNoticeIdAndStatusIn(UUID organizationId, UUID noticeId, java.util.Collection<RequestStatus> statuses);

    List<DocumentRequestEntity> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);

    Page<DocumentRequestEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, RequestStatus status);
 
    long countByOrganizationIdAndStatusIn(UUID organizationId, java.util.Collection<RequestStatus> statuses);

    long countByOrganizationIdAndStatusNotIn(UUID organizationId, java.util.Collection<RequestStatus> statuses);

    long countByOrganizationIdAndDueDateAndStatusNot(UUID organizationId, java.time.LocalDate dueDate, RequestStatus status);

    long countByOrganizationIdAndDueDateBeforeAndStatusNot(UUID organizationId, java.time.LocalDate dueDate, RequestStatus status);

    @Query("SELECT DISTINCT d.clientId FROM DocumentRequestEntity d WHERE d.organizationId = :organizationId AND d.status IN :statuses AND d.clientId IS NOT NULL")
    List<UUID> findDistinctClientIdsByOrganizationIdAndStatusIn(@Param("organizationId") UUID organizationId, @Param("statuses") java.util.Collection<RequestStatus> statuses);

    List<DocumentRequestEntity> findAllByOrganizationIdAndStatusIn(UUID organizationId, java.util.Collection<RequestStatus> statuses);

    List<DocumentRequestEntity> findAllByOrganizationIdAndClientIdAndStatusIn(UUID organizationId, UUID clientId, java.util.Collection<RequestStatus> statuses);

    List<DocumentRequestEntity> findAllByOrganizationIdAndClientIdIn(UUID organizationId, java.util.Collection<UUID> clientIds);
}