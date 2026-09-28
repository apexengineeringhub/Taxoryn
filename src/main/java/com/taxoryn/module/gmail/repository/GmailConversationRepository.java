package com.taxoryn.module.gmail.repository;

import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GmailConversationRepository extends JpaRepository<GmailConversationEntity, UUID>, JpaSpecificationExecutor<GmailConversationEntity> {

    Optional<GmailConversationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<GmailConversationEntity> findByOrganizationIdAndThreadId(UUID organizationId, String threadId);

    List<GmailConversationEntity> findAllByOrganizationIdAndThreadIdIn(UUID organizationId, Collection<String> threadIds);

    Page<GmailConversationEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<GmailConversationEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    Page<GmailConversationEntity> findAllByOrganizationIdAndAssignedUserId(UUID organizationId, UUID assignedUserId, Pageable pageable);

    Page<GmailConversationEntity> findAllByOrganizationIdAndGmailAccountId(UUID organizationId, UUID gmailAccountId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, GmailConversationStatus status);

    long countByOrganizationIdAndStatusIn(UUID organizationId, Collection<GmailConversationStatus> statuses);

    long countByOrganizationIdAndAssignedUserIdIsNull(UUID organizationId);

    long countByOrganizationIdAndClientIdIsNull(UUID organizationId);

    long countByOrganizationIdAndIsUnreadTrue(UUID organizationId);

    long countByOrganizationIdAndStatusInAndLastMessageAtBefore(
            UUID organizationId,
            Collection<GmailConversationStatus> statuses,
            Instant cutoff
    );

    @Query("SELECT c.status, COUNT(c) FROM GmailConversationEntity c WHERE c.organizationId = :organizationId GROUP BY c.status")
    List<Object[]> countByStatusGroupByStatus(@Param("organizationId") UUID organizationId);

    @Query("SELECT c.assignedUserId, COUNT(c) FROM GmailConversationEntity c WHERE c.organizationId = :organizationId AND c.assignedUserId IS NOT NULL GROUP BY c.assignedUserId")
    List<Object[]> countByAssigneeGroupByAssignee(@Param("organizationId") UUID organizationId);

    @Query("SELECT c.createdAt, c.firstResponseAt FROM GmailConversationEntity c WHERE c.organizationId = :organizationId AND c.firstResponseAt IS NOT NULL")
    List<Object[]> findResponseTimePairsByOrganizationId(@Param("organizationId") UUID organizationId);
}
