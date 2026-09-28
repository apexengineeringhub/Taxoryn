package com.taxoryn.module.gmail.repository;

import com.taxoryn.module.gmail.entity.GmailSyncHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GmailSyncHistoryRepository extends JpaRepository<GmailSyncHistoryEntity, UUID> {

    Optional<GmailSyncHistoryEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GmailSyncHistoryEntity> findAllByOrganizationIdAndGmailAccountIdOrderByCreatedAtDesc(UUID organizationId, UUID gmailAccountId);

    Page<GmailSyncHistoryEntity> findAllByOrganizationIdAndGmailAccountId(UUID organizationId, UUID gmailAccountId, Pageable pageable);

    Optional<GmailSyncHistoryEntity> findFirstByOrganizationIdAndGmailAccountIdOrderByCreatedAtDesc(UUID organizationId, UUID gmailAccountId);
}
