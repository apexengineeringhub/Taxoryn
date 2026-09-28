package com.taxoryn.module.gmail.service;

import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.entity.GmailSyncHistoryEntity;
import com.taxoryn.module.gmail.entity.GmailSyncStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.repository.GmailSyncHistoryRepository;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailSyncServiceImpl implements GmailSyncService {

    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");

    private final GmailOAuthService oAuthService;
    private final GmailApiClient apiClient;
    private final TokenEncryptionService encryptionService;
    private final GmailAccountRepository accountRepository;
    private final GmailConversationRepository conversationRepository;
    private final GmailSyncHistoryRepository syncHistoryRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public GmailSyncResult syncAccount(UUID organizationId, UUID accountId, GmailSyncType syncType) {
        Instant startedAt = Instant.now();
        log.info("Starting Gmail sync for orgId={}, accountId={}, type={}", organizationId, accountId, syncType);

        GmailAccountEntity account;
        try {
            account = oAuthService.getValidAuthenticatedAccount(organizationId, accountId);
        } catch (Exception ex) {
            log.error("Sync aborted: Failed to resolve authenticated Gmail account: {}", ex.getMessage());
            recordSyncHistory(organizationId, accountId, syncType, 0, GmailSyncStatus.FAILED, startedAt, ex.getMessage());
            return GmailSyncResult.builder()
                    .accountId(accountId)
                    .syncType(syncType)
                    .threadsSynced(0)
                    .status(GmailSyncStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .build();
        }

        String accessToken = encryptionService.decrypt(account.getEncryptedAccessToken());
        int threadsSynced = 0;
        String latestHistoryId = account.getLastHistoryId();

        try {
            Set<String> threadIdsToProcess = new HashSet<>();

            // Incremental sync via History API if lastHistoryId is available and type is not FULL
            if (syncType != GmailSyncType.FULL && StringUtils.hasText(account.getLastHistoryId())) {
                GmailThreadModels.HistoryResponse history = apiClient.listHistory(accessToken, account.getLastHistoryId(), null);
                if (history != null && history.getHistory() != null && !history.getHistory().isEmpty()) {
                    for (GmailThreadModels.HistoryRecord record : history.getHistory()) {
                        if (record.getMessagesAdded() != null) {
                            for (GmailThreadModels.MessageAdded added : record.getMessagesAdded()) {
                                if (added.getMessage() != null && StringUtils.hasText(added.getMessage().getThreadId())) {
                                    threadIdsToProcess.add(added.getMessage().getThreadId());
                                }
                            }
                        }
                    }
                    if (StringUtils.hasText(history.getHistoryId())) {
                        latestHistoryId = history.getHistoryId();
                    }
                } else if (history == null) {
                    // HistoryId expired or out of date, fallback to standard listing
                    log.info("History ID {} out of date, falling back to recent threads query", account.getLastHistoryId());
                    fetchRecentThreadIds(accessToken, threadIdsToProcess);
                }
            } else {
                fetchRecentThreadIds(accessToken, threadIdsToProcess);
            }

            log.info("Processing {} threads for Gmail sync on mailbox {}", threadIdsToProcess.size(), account.getEmailAddress());

            for (String threadId : threadIdsToProcess) {
                try {
                    GmailThreadModels.ThreadDetail detail = apiClient.getThreadMetadata(accessToken, threadId);
                    if (detail != null && detail.getMessages() != null && !detail.getMessages().isEmpty()) {
                        indexThreadMetadata(account, detail);
                        threadsSynced++;
                        if (StringUtils.hasText(detail.getHistoryId())) {
                            latestHistoryId = detail.getHistoryId();
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to index metadata for threadId={}: {}", threadId, ex.getMessage());
                }
            }

            // Update account sync watermark
            account.setLastHistoryId(latestHistoryId);
            account.setLastSyncedAt(Instant.now());
            account.setSyncErrorMessage(null);
            accountRepository.save(account);

            recordSyncHistory(organizationId, accountId, syncType, threadsSynced, GmailSyncStatus.SUCCESS, startedAt, null);

            return GmailSyncResult.builder()
                    .accountId(accountId)
                    .syncType(syncType)
                    .threadsSynced(threadsSynced)
                    .status(GmailSyncStatus.SUCCESS)
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .build();

        } catch (Exception ex) {
            log.error("Error during Gmail sync execution for accountId={}: {}", accountId, ex.getMessage(), ex);
            account.setSyncErrorMessage("Sync failed: " + ex.getMessage());
            accountRepository.save(account);

            recordSyncHistory(organizationId, accountId, syncType, threadsSynced, GmailSyncStatus.FAILED, startedAt, ex.getMessage());

            return GmailSyncResult.builder()
                    .accountId(accountId)
                    .syncType(syncType)
                    .threadsSynced(threadsSynced)
                    .status(GmailSyncStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public int syncAllActiveAccounts() {
        List<GmailAccountEntity> activeAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getStatus() == GmailAccountStatus.CONNECTED)
                .toList();

        int totalSynced = 0;
        for (GmailAccountEntity acc : activeAccounts) {
            try {
                GmailSyncResult result = syncAccount(acc.getOrganizationId(), acc.getId(), GmailSyncType.INCREMENTAL);
                totalSynced += result.getThreadsSynced();
            } catch (Exception ex) {
                log.error("Failed batch sync for account {}: {}", acc.getEmailAddress(), ex.getMessage());
            }
        }
        return totalSynced;
    }

    private void fetchRecentThreadIds(String accessToken, Set<String> target) {
        GmailThreadModels.ThreadListResponse listResponse = apiClient.listThreads(accessToken, null, null, 50);
        if (listResponse != null && listResponse.getThreads() != null) {
            for (GmailThreadModels.ThreadSummary summary : listResponse.getThreads()) {
                if (StringUtils.hasText(summary.getId())) {
                    target.add(summary.getId());
                }
            }
        }
    }

    private void indexThreadMetadata(GmailAccountEntity account, GmailThreadModels.ThreadDetail detail) {
        UUID organizationId = account.getOrganizationId();
        String threadId = detail.getId();

        List<GmailThreadModels.MessageMetadata> messages = detail.getMessages();
        GmailThreadModels.MessageMetadata firstMsg = messages.get(0);
        GmailThreadModels.MessageMetadata lastMsg = messages.get(messages.size() - 1);

        String subject = extractHeader(firstMsg, "Subject");
        String from = extractHeader(firstMsg, "From");
        String to = extractHeader(firstMsg, "To");

        String senderEmail = extractCleanEmail(from);
        String senderName = extractSenderName(from);
        String recipientEmails = to;

        Instant lastMessageAt = lastMsg.getInternalDate() != null
                ? Instant.ofEpochMilli(lastMsg.getInternalDate())
                : Instant.now();

        // Check if any message in the thread has UNREAD or STARRED label
        boolean isUnread = messages.stream().anyMatch(m -> m.getLabelIds() != null && m.getLabelIds().contains("UNREAD"));
        boolean isStarred = messages.stream().anyMatch(m -> m.getLabelIds() != null && m.getLabelIds().contains("STARRED"));

        List<String> combinedLabels = new ArrayList<>();
        messages.forEach(m -> {
            if (m.getLabelIds() != null) combinedLabels.addAll(m.getLabelIds());
        });
        String labelStr = String.join(",", new HashSet<>(combinedLabels));

        // Check for first outgoing response from the firm
        Instant firstResponseAt = null;
        for (int i = 1; i < messages.size(); i++) {
            GmailThreadModels.MessageMetadata msg = messages.get(i);
            String msgFrom = extractHeader(msg, "From");
            if (msgFrom != null && msgFrom.toLowerCase().contains(account.getEmailAddress().toLowerCase())) {
                firstResponseAt = msg.getInternalDate() != null
                        ? Instant.ofEpochMilli(msg.getInternalDate())
                        : null;
                break;
            }
        }

        String webLink = String.format("https://mail.google.com/mail/u/%s/#inbox/%s",
                account.getEmailAddress(),
                threadId
        );

        Optional<GmailConversationEntity> existingOpt = conversationRepository.findByOrganizationIdAndThreadId(organizationId, threadId);

        if (existingOpt.isPresent()) {
            GmailConversationEntity existing = existingOpt.get();
            existing.setSubject(subject);
            existing.setSnippet(lastMsg.getSnippet());
            existing.setMessageCount(messages.size());
            existing.setLastMessageAt(lastMessageAt);
            if (existing.getFirstResponseAt() == null && firstResponseAt != null) {
                existing.setFirstResponseAt(firstResponseAt);
            }
            existing.setIsUnread(isUnread);
            existing.setIsStarred(isStarred);
            existing.setGmailLabels(labelStr);
            existing.setWebLink(webLink);
            conversationRepository.save(existing);
        } else {
            // New conversation: Auto-match Client
            UUID matchedClientId = null;
            UUID matchedLocationId = null;
            UUID matchedAssigneeId = account.getUserId(); // default to account owner if practitioner account

            Optional<ClientEntity> clientOpt = autoMatchClient(organizationId, senderEmail, recipientEmails);
            if (clientOpt.isPresent()) {
                ClientEntity client = clientOpt.get();
                matchedClientId = client.getId();
                matchedLocationId = client.getLocationId();

                if (matchedAssigneeId == null && client.getAssignedEmployeeId() != null) {
                    // Resolve user ID from assigned employee
                    matchedAssigneeId = employeeRepository.findById(client.getAssignedEmployeeId())
                            .map(EmployeeEntity::getUserId)
                            .orElse(null);
                }
            }

            GmailConversationEntity newConv = GmailConversationEntity.builder()
                    .gmailAccountId(account.getId())
                    .threadId(threadId)
                    .clientId(matchedClientId)
                    .locationId(matchedLocationId)
                    .assignedUserId(matchedAssigneeId)
                    .status(GmailConversationStatus.OPEN)
                    .priority(GmailConversationPriority.NORMAL)
                    .subject(subject)
                    .snippet(lastMsg.getSnippet())
                    .senderEmail(senderEmail)
                    .senderName(senderName)
                    .recipientEmails(recipientEmails)
                    .messageCount(messages.size())
                    .lastMessageAt(lastMessageAt)
                    .firstResponseAt(firstResponseAt)
                    .isUnread(isUnread)
                    .isStarred(isStarred)
                    .gmailLabels(labelStr)
                    .webLink(webLink)
                    .build();
            newConv.setOrganizationId(organizationId);

            conversationRepository.save(newConv);
        }
    }

    private Optional<ClientEntity> autoMatchClient(UUID organizationId, String senderEmail, String recipientEmails) {
        if (StringUtils.hasText(senderEmail)) {
            List<ClientEntity> match = clientRepository.findAllByOrganizationId(organizationId).stream()
                    .filter(c -> senderEmail.equalsIgnoreCase(c.getEmail()))
                    .toList();
            if (!match.isEmpty()) {
                return Optional.of(match.get(0));
            }
        }

        if (StringUtils.hasText(recipientEmails)) {
            Matcher matcher = EMAIL_ADDRESS_PATTERN.matcher(recipientEmails);
            while (matcher.find()) {
                String candidate = matcher.group();
                List<ClientEntity> match = clientRepository.findAllByOrganizationId(organizationId).stream()
                        .filter(c -> candidate.equalsIgnoreCase(c.getEmail()))
                        .toList();
                if (!match.isEmpty()) {
                    return Optional.of(match.get(0));
                }
            }
        }

        return Optional.empty();
    }

    private String extractHeader(GmailThreadModels.MessageMetadata msg, String headerName) {
        if (msg.getPayload() != null && msg.getPayload().getHeaders() != null) {
            for (GmailThreadModels.HeaderEntry header : msg.getPayload().getHeaders()) {
                if (headerName.equalsIgnoreCase(header.getName())) {
                    return header.getValue();
                }
            }
        }
        return null;
    }

    private String extractCleanEmail(String rawFrom) {
        if (!StringUtils.hasText(rawFrom)) return null;
        Matcher matcher = EMAIL_ADDRESS_PATTERN.matcher(rawFrom);
        if (matcher.find()) {
            return matcher.group().toLowerCase();
        }
        return rawFrom.trim();
    }

    private String extractSenderName(String rawFrom) {
        if (!StringUtils.hasText(rawFrom)) return null;
        int angleIdx = rawFrom.indexOf('<');
        if (angleIdx > 0) {
            return rawFrom.substring(0, angleIdx).replace("\"", "").trim();
        }
        return rawFrom.trim();
    }

    private void recordSyncHistory(
            UUID organizationId,
            UUID accountId,
            GmailSyncType type,
            int count,
            GmailSyncStatus status,
            Instant startedAt,
            String error
    ) {
        GmailSyncHistoryEntity history = GmailSyncHistoryEntity.builder()
                .gmailAccountId(accountId)
                .syncType(type)
                .threadsSynced(count)
                .status(status)
                .startedAt(startedAt)
                .completedAt(Instant.now())
                .errorDetails(error)
                .build();
        history.setOrganizationId(organizationId);
        syncHistoryRepository.save(history);
    }
}
