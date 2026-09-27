package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Client Overview & Attention Summary Dashboard Metrics")
public class ClientSummaryDashboardDto {

    @Schema(description = "Total clients within user's portfolio and location scope", example = "60")
    @Builder.Default
    private long totalClients = 0;

    @Schema(description = "Total active clients", example = "55")
    @Builder.Default
    private long activeClients = 0;

    @Schema(description = "Total inactive/archived clients", example = "5")
    @Builder.Default
    private long inactiveClients = 0;

    @Schema(description = "Number of clients with pending compliance obligations", example = "35")
    @Builder.Default
    private long clientsWithPendingCompliance = 0;

    @Schema(description = "Number of clients with overdue work items or tasks", example = "8")
    @Builder.Default
    private long clientsWithOverdueWork = 0;

    @Schema(description = "Number of clients with pending document requests", example = "12")
    @Builder.Default
    private long clientsWithPendingDocRequests = 0;

    @Schema(description = "Number of clients with open tax notices", example = "4")
    @Builder.Default
    private long clientsWithOpenNotices = 0;

    @Schema(description = "Number of clients with outstanding billing dues", example = "18")
    @Builder.Default
    private long clientsWithOutstandingBilling = 0;

    @Schema(description = "Prioritized list of clients requiring operational practice attention")
    @Builder.Default
    private List<ClientAttentionItemDto> attentionList = new ArrayList<>();

    @Schema(description = "Timestamp when this summary was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Client operational attention summary item")
    public static class ClientAttentionItemDto {
        private UUID clientId;
        private String clientName;
        private String pan;
        private String gstin;
        @Builder.Default
        private long pendingComplianceCount = 0;
        @Builder.Default
        private long overdueWorkCount = 0;
        @Builder.Default
        private long pendingDocRequestsCount = 0;
        @Builder.Default
        private long openNoticesCount = 0;
        @Builder.Default
        private BigDecimal outstandingBalance = BigDecimal.ZERO;
    }
}
