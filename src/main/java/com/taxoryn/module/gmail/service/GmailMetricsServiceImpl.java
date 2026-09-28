package com.taxoryn.module.gmail.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.dto.GmailMetricsDto;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailMetricsServiceImpl implements GmailMetricsService {

    private final GmailConversationRepository conversationRepository;
    private final GmailProperties properties;

    @Override
    @Transactional(readOnly = true)
    public GmailMetricsDto getMetrics(PracticeSecurityScope scope) {
        UUID organizationId = scope.getOrganizationId();

        long total = conversationRepository.countByOrganizationId(organizationId);
        long open = conversationRepository.countByOrganizationIdAndStatus(organizationId, GmailConversationStatus.OPEN);
        long pendingClient = conversationRepository.countByOrganizationIdAndStatus(organizationId, GmailConversationStatus.PENDING_CLIENT);
        long inProgress = conversationRepository.countByOrganizationIdAndStatus(organizationId, GmailConversationStatus.IN_PROGRESS);
        long resolved = conversationRepository.countByOrganizationIdAndStatus(organizationId, GmailConversationStatus.RESOLVED);
        long closed = conversationRepository.countByOrganizationIdAndStatus(organizationId, GmailConversationStatus.CLOSED);
        long unassigned = conversationRepository.countByOrganizationIdAndAssignedUserIdIsNull(organizationId);
        long unlinkedClient = conversationRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
        long unread = conversationRepository.countByOrganizationIdAndIsUnreadTrue(organizationId);

        // SLA Breached Count (open conversations with no first response exceeding default SLA hours)
        Instant slaCutoff = Instant.now().minus(properties.getDefaultSlaHours(), ChronoUnit.HOURS);
        long slaBreached = conversationRepository.countByOrganizationIdAndStatusInAndLastMessageAtBefore(
                organizationId,
                Set.of(GmailConversationStatus.OPEN, GmailConversationStatus.IN_PROGRESS),
                slaCutoff
        );

        // Average Response Time in Minutes
        List<Object[]> responsePairs = conversationRepository.findResponseTimePairsByOrganizationId(organizationId);
        Double avgResponseTimeMinutes = null;
        if (responsePairs != null && !responsePairs.isEmpty()) {
            long totalSeconds = 0;
            long count = 0;
            for (Object[] pair : responsePairs) {
                if (pair[0] instanceof Instant created && pair[1] instanceof Instant responded) {
                    long sec = Math.max(0, Duration.between(created, responded).toSeconds());
                    totalSeconds += sec;
                    count++;
                }
            }
            if (count > 0) {
                avgResponseTimeMinutes = ((double) totalSeconds / count) / 60.0;
            }
        }

        // Status Map
        Map<String, Long> statusMap = new HashMap<>();
        List<Object[]> statusGroups = conversationRepository.countByStatusGroupByStatus(organizationId);
        if (statusGroups != null) {
            for (Object[] row : statusGroups) {
                if (row[0] != null && row[1] instanceof Number num) {
                    statusMap.put(row[0].toString(), num.longValue());
                }
            }
        }

        // Assignee Map
        Map<UUID, Long> assigneeMap = new HashMap<>();
        List<Object[]> assigneeGroups = conversationRepository.countByAssigneeGroupByAssignee(organizationId);
        if (assigneeGroups != null) {
            for (Object[] row : assigneeGroups) {
                if (row[0] instanceof UUID uid && row[1] instanceof Number num) {
                    assigneeMap.put(uid, num.longValue());
                }
            }
        }

        return GmailMetricsDto.builder()
                .totalConversations(total)
                .openConversations(open)
                .pendingClientConversations(pendingClient)
                .inProgressConversations(inProgress)
                .resolvedConversations(resolved)
                .closedConversations(closed)
                .unassignedConversations(unassigned)
                .unlinkedClientConversations(unlinkedClient)
                .unreadConversations(unread)
                .avgResponseTimeMinutes(avgResponseTimeMinutes)
                .slaBreachedCount(slaBreached)
                .conversationsByStatus(statusMap)
                .workloadByAssignee(assigneeMap)
                .build();
    }
}
