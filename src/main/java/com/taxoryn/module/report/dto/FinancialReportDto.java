package com.taxoryn.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Financial, Invoicing, Collections, and Outstanding Aging Report")
public class FinancialReportDto {

    @Schema(description = "Whether the user has authorized billing access")
    private boolean hasBillingAccess;

    // High level financial metrics
    @Schema(description = "Total invoiced amount (excluding cancelled)")
    private BigDecimal totalInvoiced;

    @Schema(description = "Total collected / paid amount")
    private BigDecimal totalCollected;

    @Schema(description = "Total outstanding balance amount")
    private BigDecimal totalOutstanding;

    @Schema(description = "Outstanding amount due soon (within next 15 days)")
    private BigDecimal outstandingDueSoon;

    @Schema(description = "Outstanding amount already overdue")
    private BigDecimal outstandingOverdue;

    // Invoices breakdown
    @Schema(description = "Total invoice records count")
    private long totalInvoices;

    @Schema(description = "Draft invoices count")
    private long draftInvoices;

    @Schema(description = "Issued (unpaid) invoices count")
    private long issuedInvoices;

    @Schema(description = "Partially paid invoices count")
    private long partiallyPaidInvoices;

    @Schema(description = "Fully paid invoices count")
    private long paidInvoices;

    @Schema(description = "Overdue invoices count")
    private long overdueInvoices;

    @Schema(description = "Cancelled invoices count")
    private long cancelledInvoices;

    @Schema(description = "Invoice counts grouped by status")
    private Map<String, Long> invoicesByStatus;

    // Payments Summary
    @Schema(description = "Total payment receipts recorded count")
    private long totalPaymentsCount;

    @Schema(description = "Amount collected this month")
    private BigDecimal collectedThisMonth;

    @Schema(description = "Amount collected this quarter")
    private BigDecimal collectedThisQuarter;

    // Outstanding Invoices Aging List
    @Schema(description = "List of outstanding invoices with aging days")
    private List<OutstandingInvoiceDto> outstandingInvoices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Single outstanding invoice record with client and aging details")
    public static class OutstandingInvoiceDto {
        private UUID invoiceId;
        private String invoiceNumber;
        private UUID clientId;
        private String clientName;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceDue;
        private String status;
        private long daysDueOrOverdue;
        private boolean isOverdue;
    }
}
