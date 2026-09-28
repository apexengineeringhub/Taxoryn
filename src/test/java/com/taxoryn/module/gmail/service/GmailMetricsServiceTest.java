package com.taxoryn.module.gmail.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.dto.GmailMetricsDto;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GmailMetricsServiceTest {

    @Mock
    private GmailConversationRepository conversationRepository;

    @Mock
    private GmailProperties properties;

    private GmailMetricsServiceImpl metricsService;
    private UUID orgId;
    private PracticeSecurityScope scope;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        scope = PracticeSecurityScope.builder()
                .organizationId(orgId)
                .isFirmAdmin(true)
                .build();

        when(properties.getDefaultSlaHours()).thenReturn(24);

        metricsService = new GmailMetricsServiceImpl(conversationRepository, properties);
    }

    @Test
    @DisplayName("Verify getMetrics computes counts, SLA breaches, and average response times")
    void testGetMetrics() {
        when(conversationRepository.countByOrganizationId(orgId)).thenReturn(20L);
        when(conversationRepository.countByOrganizationIdAndStatus(orgId, GmailConversationStatus.OPEN)).thenReturn(5L);
        when(conversationRepository.countByOrganizationIdAndStatus(orgId, GmailConversationStatus.PENDING_CLIENT)).thenReturn(3L);
        when(conversationRepository.countByOrganizationIdAndStatus(orgId, GmailConversationStatus.IN_PROGRESS)).thenReturn(4L);
        when(conversationRepository.countByOrganizationIdAndStatus(orgId, GmailConversationStatus.RESOLVED)).thenReturn(6L);
        when(conversationRepository.countByOrganizationIdAndStatus(orgId, GmailConversationStatus.CLOSED)).thenReturn(2L);
        when(conversationRepository.countByOrganizationIdAndAssignedUserIdIsNull(orgId)).thenReturn(2L);
        when(conversationRepository.countByOrganizationIdAndClientIdIsNull(orgId)).thenReturn(1L);
        when(conversationRepository.countByOrganizationIdAndIsUnreadTrue(orgId)).thenReturn(4L);

        when(conversationRepository.countByOrganizationIdAndStatusInAndLastMessageAtBefore(eq(orgId), any(), any()))
                .thenReturn(2L);

        Instant t0 = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant t1 = t0.plus(30, ChronoUnit.MINUTES); // 30 min response time
        List<Object[]> responsePairs = new java.util.ArrayList<>();
        responsePairs.add(new Object[]{t0, t1});
        when(conversationRepository.findResponseTimePairsByOrganizationId(orgId)).thenReturn(responsePairs);

        GmailMetricsDto metrics = metricsService.getMetrics(scope);

        assertThat(metrics.getTotalConversations()).isEqualTo(20L);
        assertThat(metrics.getOpenConversations()).isEqualTo(5L);
        assertThat(metrics.getResolvedConversations()).isEqualTo(6L);
        assertThat(metrics.getUnassignedConversations()).isEqualTo(2L);
        assertThat(metrics.getUnlinkedClientConversations()).isEqualTo(1L);
        assertThat(metrics.getUnreadConversations()).isEqualTo(4L);
        assertThat(metrics.getSlaBreachedCount()).isEqualTo(2L);
        assertThat(metrics.getAvgResponseTimeMinutes()).isEqualTo(30.0);
    }
}
