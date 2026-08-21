package com.taxoryn.module.billing.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.billing.dto.BillingDashboardStatsDto;
import com.taxoryn.module.billing.dto.ClientBillingHistoryDto;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.dto.InvoiceFilterRequest;
import com.taxoryn.module.billing.dto.InvoicePaymentDto;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceRequest;
import com.taxoryn.module.billing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/invoices", "/api/invoices", "/api/v1/billing", "/api/billing"})
@RequiredArgsConstructor
@Tag(name = "Client Billing & Invoicing", description = "Professional invoicing, line item pricing, payment receipt recording, outstanding balances, and client billing history")
@SecurityRequirement(name = "BearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // =========================================================================
    // 1. Invoices CRUD & Workflow
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create client invoice", description = "Generates a new tax invoice with professional line items (GST, ITR, TDS, Accounting, Consulting), auto-calculated tax, subtotal, and total.")
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceDto invoice = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Invoice created successfully", invoice));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk generate client invoices", description = "Generates professional tax invoices in batch across multiple or all practice clients.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.billing.dto.BulkInvoiceResultDto>> bulkCreateInvoices(
            @Valid @RequestBody com.taxoryn.module.billing.dto.BulkCreateInvoicesRequest request) {
        com.taxoryn.module.billing.dto.BulkInvoiceResultDto result = invoiceService.bulkCreateInvoices(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Bulk invoices batch generated successfully", result));
    }

    @PostMapping("/seed-demo")
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Seed demo practice invoices", description = "Generates sample practice tax invoices for testing and demo.")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> seedDemoInvoices() {
        List<InvoiceDto> result = invoiceService.seedDemoInvoices();
        return ResponseEntity.ok(ApiResponse.success("Demo invoices seeded successfully", result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search invoices", description = "Retrieves paginated invoices with filters by client, status, and date range.")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> getInvoices(
            @Valid @ModelAttribute InvoiceFilterRequest filterRequest) {
        PagedResponse<InvoiceDto> invoices = invoiceService.getInvoices(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved successfully", invoices));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get invoice by ID", description = "Retrieves full invoice details with line items and recorded payment history.")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(@PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved successfully", invoice));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update draft invoice", description = "Modifies line items, due date, or notes for invoices in DRAFT status.")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        InvoiceDto invoice = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Invoice updated successfully", invoice));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Issue invoice to client", description = "Finalizes invoice and transitions status from DRAFT to ISSUED, generating a client notification.")
    public ResponseEntity<ApiResponse<InvoiceDto>> issueInvoice(@PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.issueInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice issued successfully", invoice));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel invoice", description = "Marks invoice as CANCELLED.")
    public ResponseEntity<ApiResponse<InvoiceDto>> cancelInvoice(@PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice cancelled successfully", invoice));
    }

    // =========================================================================
    // 2. Payments & Receipts
    // =========================================================================

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Record client payment receipt", description = "Records payment amount against an issued invoice and automatically updates invoice status to PAID or PARTIALLY_PAID.")
    public ResponseEntity<ApiResponse<InvoicePaymentDto>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request) {
        InvoicePaymentDto payment = invoiceService.recordPayment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Payment recorded successfully", payment));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List invoice payment receipts", description = "Retrieves all payment receipts recorded against an invoice.")
    public ResponseEntity<ApiResponse<List<InvoicePaymentDto>>> getInvoicePayments(@PathVariable UUID id) {
        List<InvoicePaymentDto> payments = invoiceService.getInvoicePayments(id);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    // =========================================================================
    // 3. Client History & Executive Dashboard
    // =========================================================================

    @GetMapping("/clients/{clientId}/history")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Client billing history & outstanding summary", description = "Retrieves complete invoice ledger, total billed, total paid, and outstanding balance for a specific client.")
    public ResponseEntity<ApiResponse<ClientBillingHistoryDto>> getClientBillingHistory(@PathVariable UUID clientId) {
        ClientBillingHistoryDto history = invoiceService.getClientBillingHistory(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client billing history retrieved successfully", history));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Practice billing dashboard statistics", description = "Retrieves aggregate billing KPIs: Total Billed, Total Collected, Total Outstanding, and Revenue Breakdown by Service.")
    public ResponseEntity<ApiResponse<BillingDashboardStatsDto>> getBillingDashboardStats() {
        BillingDashboardStatsDto stats = invoiceService.getBillingDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Billing dashboard stats retrieved successfully", stats));
    }
}
