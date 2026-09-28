package com.taxoryn.module.gmail.repository;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.entity.GmailSyncHistoryEntity;
import com.taxoryn.module.gmail.entity.GmailSyncStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class GmailEntityAndRepositoryTest {

    @Autowired
    private GmailAccountRepository accountRepository;

    @Autowired
    private GmailConversationRepository conversationRepository;

    @Autowired
    private GmailSyncHistoryRepository syncHistoryRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID orgId;
    private UUID otherOrgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        otherOrgId = UUID.randomUUID();
        TenantContext.setTenantId(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Verify GmailAccount persistence and query operations")
    void testGmailAccountRepository() {
        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("practice@taxfirm.com")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_acc_token_123")
                .encryptedRefreshToken("enc_ref_token_123")
                .tokenExpiresAt(Instant.now().plus(3600, ChronoUnit.SECONDS))
                .lastHistoryId("100234")
                .build();

        GmailAccountEntity saved = accountRepository.save(account);
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrganizationId()).isEqualTo(orgId);

        Optional<GmailAccountEntity> found = accountRepository.findByIdAndOrganizationId(saved.getId(), orgId);
        assertThat(found).isPresent();
        assertThat(found.get().getEmailAddress()).isEqualTo("practice@taxfirm.com");
        assertThat(found.get().getStatus()).isEqualTo(GmailAccountStatus.CONNECTED);

        // Cross-tenant check
        Optional<GmailAccountEntity> crossTenant = accountRepository.findByIdAndOrganizationId(saved.getId(), otherOrgId);
        assertThat(crossTenant).isEmpty();

        List<GmailAccountEntity> activeAccounts = accountRepository.findAllByOrganizationIdAndStatus(orgId, GmailAccountStatus.CONNECTED);
        assertThat(activeAccounts).hasSize(1);
    }

    @Test
    @DisplayName("Verify GmailConversation persistence, queries, and metrics aggregations")
    void testGmailConversationRepository() {
        GmailAccountEntity account = accountRepository.save(GmailAccountEntity.builder()
                .emailAddress("team@taxfirm.com")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .status(GmailAccountStatus.CONNECTED)
                .build());

        UUID clientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Instant now = Instant.now();

        GmailConversationEntity conv1 = GmailConversationEntity.builder()
                .gmailAccountId(account.getId())
                .threadId("thread_abc_123")
                .clientId(clientId)
                .assignedUserId(staffId)
                .status(GmailConversationStatus.OPEN)
                .priority(GmailConversationPriority.HIGH)
                .subject("GST Query for Q2")
                .snippet("Dear Team, please review our GSTR-3B...")
                .senderEmail("client@acme.com")
                .senderName("John Doe")
                .recipientEmails("team@taxfirm.com")
                .messageCount(2)
                .lastMessageAt(now.minus(2, ChronoUnit.HOURS))
                .firstResponseAt(now.minus(1, ChronoUnit.HOURS))
                .isUnread(true)
                .build();

        GmailConversationEntity conv2 = GmailConversationEntity.builder()
                .gmailAccountId(account.getId())
                .threadId("thread_def_456")
                .clientId(null) // unassigned client
                .assignedUserId(null) // unassigned staff
                .status(GmailConversationStatus.PENDING_CLIENT)
                .priority(GmailConversationPriority.NORMAL)
                .subject("ITR Clarification")
                .snippet("Please find the Form 16 attached...")
                .senderEmail("unknown@corp.com")
                .messageCount(1)
                .lastMessageAt(now.minus(5, ChronoUnit.DAYS))
                .isUnread(false)
                .build();

        conversationRepository.save(conv1);
        conversationRepository.save(conv2);
        entityManager.flush();
        entityManager.clear();

        // Query by Thread ID
        Optional<GmailConversationEntity> foundThread = conversationRepository.findByOrganizationIdAndThreadId(orgId, "thread_abc_123");
        assertThat(foundThread).isPresent();
        assertThat(foundThread.get().getSubject()).isEqualTo("GST Query for Q2");
        assertThat(foundThread.get().getClientId()).isEqualTo(clientId);
        assertThat(foundThread.get().getAssignedUserId()).isEqualTo(staffId);

        // Counts
        assertThat(conversationRepository.countByOrganizationId(orgId)).isEqualTo(2);
        assertThat(conversationRepository.countByOrganizationIdAndStatus(orgId, GmailConversationStatus.OPEN)).isEqualTo(1);
        assertThat(conversationRepository.countByOrganizationIdAndAssignedUserIdIsNull(orgId)).isEqualTo(1);
        assertThat(conversationRepository.countByOrganizationIdAndClientIdIsNull(orgId)).isEqualTo(1);
        assertThat(conversationRepository.countByOrganizationIdAndIsUnreadTrue(orgId)).isEqualTo(1);

        // Status grouped aggregation
        List<Object[]> statusCounts = conversationRepository.countByStatusGroupByStatus(orgId);
        assertThat(statusCounts).isNotEmpty();

        // Assignee grouped aggregation
        List<Object[]> assigneeCounts = conversationRepository.countByAssigneeGroupByAssignee(orgId);
        assertThat(assigneeCounts).hasSize(1);
        assertThat(assigneeCounts.get(0)[0]).isEqualTo(staffId);
        assertThat(((Number) assigneeCounts.get(0)[1]).longValue()).isEqualTo(1L);

        // Response time pairs query
        List<Object[]> responsePairs = conversationRepository.findResponseTimePairsByOrganizationId(orgId);
        assertThat(responsePairs).hasSize(1);
        assertThat(responsePairs.get(0)[0]).isNotNull();
        assertThat(responsePairs.get(0)[1]).isNotNull();
    }

    @Test
    @DisplayName("Verify GmailSyncHistory logging and ordering")
    void testGmailSyncHistoryRepository() {
        GmailAccountEntity account = accountRepository.save(GmailAccountEntity.builder()
                .emailAddress("sync@taxfirm.com")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .status(GmailAccountStatus.CONNECTED)
                .build());

        GmailSyncHistoryEntity sync1 = GmailSyncHistoryEntity.builder()
                .gmailAccountId(account.getId())
                .syncType(GmailSyncType.FULL)
                .threadsSynced(25)
                .status(GmailSyncStatus.SUCCESS)
                .startedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .completedAt(Instant.now().minus(9, ChronoUnit.MINUTES))
                .build();

        GmailSyncHistoryEntity sync2 = GmailSyncHistoryEntity.builder()
                .gmailAccountId(account.getId())
                .syncType(GmailSyncType.INCREMENTAL)
                .threadsSynced(3)
                .status(GmailSyncStatus.SUCCESS)
                .startedAt(Instant.now().minus(2, ChronoUnit.MINUTES))
                .completedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        syncHistoryRepository.save(sync1);
        syncHistoryRepository.save(sync2);
        entityManager.flush();
        entityManager.clear();

        List<GmailSyncHistoryEntity> history = syncHistoryRepository.findAllByOrganizationIdAndGmailAccountIdOrderByCreatedAtDesc(orgId, account.getId());
        assertThat(history).hasSize(2);
    }
}
