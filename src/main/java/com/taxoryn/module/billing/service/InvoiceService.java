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

    InvoiceDto issueInvoice(UUID id);

    InvoiceDto cancelInvoice(UUID id);

    InvoicePaymentDto recordPayment(UUID invoiceId, RecordPaymentRequest request);

    List<InvoicePaymentDto> getInvoicePayments(UUID invoiceId);

    ClientBillingHistoryDto getClientBillingHistory(UUID clientId);

    BillingDashboardStatsDto getBillingDashboardStats();
}
