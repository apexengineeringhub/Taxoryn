package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice-Wide Billing Executive Summary")
public class BillingDashboardStatsDto {

    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private BigDecimal totalDraft;

    private long totalInvoices;
    private long draftInvoices;
    private long issuedInvoices;
    private long partiallyPaidInvoices;
    private long paidInvoices;
    private long overdueInvoices;
    private long cancelledInvoices;

    private Map<String, BigDecimal> revenueByService;
}
