package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Accounts Receivables & Aging Summary")
public class ReceivablesSummaryDto {

    private BigDecimal totalInvoiced;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private BigDecimal totalOverdue;

    private long totalInvoicesCount;
    private long draftInvoicesCount;
    private long issuedInvoicesCount;
    private long partiallyPaidInvoicesCount;
    private long paidInvoicesCount;
    private long overdueInvoicesCount;

    // Aging breakdown of outstanding balance
    private BigDecimal currentOutstanding;      // Not yet due (due date >= today)
    private BigDecimal overdue1To30Days;        // Overdue 1 - 30 days
    private BigDecimal overdue31To60Days;       // Overdue 31 - 60 days
    private BigDecimal overdue60PlusDays;       // Overdue > 60 days

    private List<ClientAgingSummaryDto> clientBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientAgingSummaryDto {
        private UUID clientId;
        private String clientName;
        private BigDecimal totalInvoiced;
        private BigDecimal totalCollected;
        private BigDecimal totalOutstanding;
        private BigDecimal overdueAmount;
        private long invoiceCount;
        private long overdueCount;
    }
}
