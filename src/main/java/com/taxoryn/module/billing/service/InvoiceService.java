package com.taxoryn.module.billing.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.billing.dto.BillingDashboardStatsDto;
import com.taxoryn.module.billing.dto.ClientBillingHistoryDto;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.dto.InvoiceFilterRequest;
import com.taxoryn.module.billing.dto.InvoicePaymentDto;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceRequest;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    InvoiceDto createInvoice(CreateInvoiceRequest request);

    InvoiceDto getInvoiceById(UUID id);

    PagedResponse<InvoiceDto> getInvoices(InvoiceFilterRequest filterRequest);

    InvoiceDto updateInvoice(UUID id, UpdateInvoiceRequest request);

    InvoiceDto updateInvoiceStatus(UUID id, com.taxoryn.module.billing.dto.UpdateInvoiceStatusRequest request);

    List<InvoiceDto> getInvoicesByClientId(UUID clientId);

    InvoiceDto issueInvoice(UUID id);

    InvoiceDto cancelInvoice(UUID id);

    InvoicePaymentDto recordPayment(UUID invoiceId, RecordPaymentRequest request);

    List<InvoicePaymentDto> getInvoicePayments(UUID invoiceId);

    ClientBillingHistoryDto getClientBillingHistory(UUID clientId);

    BillingDashboardStatsDto getBillingDashboardStats();

    com.taxoryn.module.billing.dto.BulkInvoiceResultDto bulkCreateInvoices(com.taxoryn.module.billing.dto.BulkCreateInvoicesRequest request);

    void sendInvoiceReminder(UUID invoiceId);

    List<InvoiceDto> seedDemoInvoices();

    InvoiceDto generateInvoiceFromTimeEntries(com.taxoryn.module.billing.dto.GenerateInvoiceFromTimeEntriesRequest request);

    List<com.taxoryn.module.billing.dto.UnbilledTimeEntryDto> getUnbilledTimeEntries(UUID clientId, UUID engagementId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    com.taxoryn.module.billing.dto.ReceivablesSummaryDto getReceivablesSummary(UUID locationId, UUID clientId);
}
