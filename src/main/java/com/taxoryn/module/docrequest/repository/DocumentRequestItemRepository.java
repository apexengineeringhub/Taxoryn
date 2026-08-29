package com.taxoryn.module.docrequest.repository;

import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRequestItemRepository extends JpaRepository<DocumentRequestItemEntity, UUID> {

    Optional<DocumentRequestItemEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<DocumentRequestItemEntity> findByIdAndClientId(UUID id, UUID clientId);

    List<DocumentRequestItemEntity> findAllByRequestIdOrderByCreatedAtAsc(UUID requestId);

    long countByRequestIdAndStatus(UUID requestId, ItemStatus status);

    long countByRequestId(UUID requestId);
}