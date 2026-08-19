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
@Schema(description = "Client Complete Billing & Outstanding Summary")
public class ClientBillingHistoryDto {

    private UUID clientId;
    private String clientName;
    private String clientGstin;
    private String clientPan;

    private BigDecimal totalBilled;
    private BigDecimal totalPaid;
    private BigDecimal totalOutstanding;
    private long totalInvoicesCount;
    private long overdueInvoicesCount;
    private long paidInvoicesCount;

    private List<InvoiceDto> invoices;
    private List<InvoicePaymentDto> recentPayments;
}
