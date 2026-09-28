package com.taxoryn.module.gmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gmail Operational Metrics and Workload Analytics")
public class GmailMetricsDto {

    private long totalConversations;
    private long openConversations;
    private long pendingClientConversations;
    private long inProgressConversations;
    private long resolvedConversations;
    private long closedConversations;
    private long unassignedConversations;
    private long unlinkedClientConversations;
    private long unreadConversations;
    private Double avgResponseTimeMinutes;
    private long slaBreachedCount;
    private Map<String, Long> conversationsByStatus;
    private Map<UUID, Long> workloadByAssignee;
}
