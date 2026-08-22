package com.taxoryn.module.tds.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.tds.dto.*;
import com.taxoryn.module.tds.service.TdsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/tds", "/api/tds"})
@RequiredArgsConstructor
@Tag(name = "TDS & TCS Management", description = "End-to-end Indian Tax Deducted at Source (TDS) and Tax Collected at Source (TCS) compliance: TAN Profiles, Form 24Q/26Q/27Q/27EQ Returns, Challan 281 Reconciliation, Section Rate Engine, and Form 16/16A Certificates")
@SecurityRequirement(name = "BearerAuth")
public class TdsController {

    private final TdsService tdsService;

    // =========================================================================
    // 1. TDS Profiles (TAN Master)
    // =========================================================================

    @PostMapping("/profiles")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register TAN Deductor Profile", description = "Registers a client's 10-character TAN, deductor category, and principal officer details.")
    public ResponseEntity<ApiResponse<TdsProfileDto>> createProfile(@Valid @RequestBody CreateTdsProfileRequest request) {
        TdsProfileDto profile = tdsService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TAN Profile registered successfully", profile));
    }

    @GetMapping("/profiles")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search TAN profiles", description = "Retrieves paginated TAN profiles with filters for deductor type, status, or search keywords.")
    public ResponseEntity<ApiResponse<PagedResponse<TdsProfileDto>>> getProfiles(@Valid @ModelAttribute TdsProfileFilterRequest filterRequest) {
        PagedResponse<TdsProfileDto> response = tdsService.getProfiles(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("TDS profiles retrieved successfully", response));
    }

    @GetMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get TAN profile by ID", description = "Retrieves complete details of a TAN profile registration.")
    public ResponseEntity<ApiResponse<TdsProfileDto>> getProfileById(@PathVariable UUID id) {
        TdsProfileDto profile = tdsService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("TDS profile retrieved successfully", profile));
    }

    @GetMapping("/profiles/clients/{clientId}")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get TAN profile by client ID", description = "Retrieves the active TAN profile associated with a specific client.")
    public ResponseEntity<ApiResponse<TdsProfileDto>> getProfileByClientId(@PathVariable UUID clientId) {
        TdsProfileDto profile = tdsService.getProfileByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client TDS profile retrieved successfully", profile));
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update TAN profile", description = "Updates responsible person details, deductor category, or assigned practitioner.")
    public ResponseEntity<ApiResponse<TdsProfileDto>> updateProfile(@PathVariable UUID id, @Valid @RequestBody UpdateTdsProfileRequest request) {
        TdsProfileDto profile = tdsService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("TDS profile updated successfully", profile));
    }

    @PostMapping("/profiles/bulk")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk import TAN profiles", description = "Migrates multiple client TAN registrations from CSV/Excel in a single batch.")
    public ResponseEntity<ApiResponse<BulkTdsProfileImportResultDto>> bulkCreateProfiles(@RequestBody List<CreateTdsProfileRequest> requests) {
        BulkTdsProfileImportResultDto result = tdsService.bulkCreateProfiles(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk TAN profile migration completed", result));
    }

    // =========================================================================
    // 2. TDS Returns Lifecycle
    // =========================================================================

    @PostMapping("/returns")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create quarterly TDS return filing", description = "Initializes a quarterly statement record (Form 24Q, 26Q, 27Q, 27EQ).")
    public ResponseEntity<ApiResponse<TdsReturnDto>> createReturn(@Valid @RequestBody CreateTdsReturnRequest request) {
        TdsReturnDto tdsReturn = tdsService.createReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TDS return filing created successfully", tdsReturn));
    }

    @GetMapping("/returns")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search TDS returns", description = "Filters quarterly filings by Form (24Q/26Q/27Q/27EQ), Quarter (Q1-Q4), FY, or workflow status.")
    public ResponseEntity<ApiResponse<PagedResponse<TdsReturnDto>>> getReturns(@Valid @ModelAttribute TdsReturnFilterRequest filterRequest) {
        PagedResponse<TdsReturnDto> response = tdsService.getReturns(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("TDS returns retrieved successfully", response));
    }

    @GetMapping("/returns/{id}")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get TDS return by ID", description = "Retrieves quarterly filing record details, token receipt, and financial figures.")
    public ResponseEntity<ApiResponse<TdsReturnDto>> getReturnById(@PathVariable UUID id) {
        TdsReturnDto tdsReturn = tdsService.getReturnById(id);
        return ResponseEntity.ok(ApiResponse.success("TDS return retrieved successfully", tdsReturn));
    }

    @PutMapping("/returns/{id}")
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update TDS return", description = "Updates statutory dates, status, or financial computation totals.")
    public ResponseEntity<ApiResponse<TdsReturnDto>> updateReturn(@PathVariable UUID id, @Valid @RequestBody UpdateTdsReturnRequest request) {
        TdsReturnDto tdsReturn = tdsService.updateReturn(id, request);
        return ResponseEntity.ok(ApiResponse.success("TDS return updated successfully", tdsReturn));
    }

    @PatchMapping("/returns/{id}/status")
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update TDS return status", description = "Transitions return through workflow statuses (PENDING -> DRAFT -> CHALLANS_ATTACHED -> UNDER_REVIEW -> READY_TO_FILE -> FILED).")
    public ResponseEntity<ApiResponse<TdsReturnDto>> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateTdsReturnStatusRequest request) {
        TdsReturnDto tdsReturn = tdsService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("TDS return status updated successfully", tdsReturn));
    }

    @PostMapping({"/returns/{id}/filing-details", "/returns/{id}/file"})
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Record e-Filing Token / PRN", description = "Records the 15-digit Provisional Receipt Number (Token Number) and marks filing status as FILED.")
    public ResponseEntity<ApiResponse<TdsReturnDto>> recordFiling(@PathVariable UUID id, @Valid @RequestBody RecordTdsFilingRequest request) {
        TdsReturnDto tdsReturn = tdsService.recordFiling(id, request);
        return ResponseEntity.ok(ApiResponse.success("TDS return filed successfully", tdsReturn));
    }

    @PutMapping("/returns/{id}/assigned-employee")
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign employee to TDS return", description = "Assigns or reassigns practice staff to prepare or review the TDS return.")
    public ResponseEntity<ApiResponse<TdsReturnDto>> assignEmployee(@PathVariable UUID id, @Valid @RequestBody AssignTdsEmployeeRequest request) {
        TdsReturnDto tdsReturn = tdsService.assignEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to TDS return successfully", tdsReturn));
    }

    @PostMapping("/returns/batch-generate")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Batch generate quarterly returns", description = "Auto-generates Form 24Q and Form 26Q return records for all active TAN clients for a target quarter.")
    public ResponseEntity<ApiResponse<List<TdsReturnDto>>> batchGenerateReturns(@Valid @RequestBody BatchGenerateTdsReturnsRequest request) {
        List<TdsReturnDto> returns = tdsService.batchGenerateReturns(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Batch generated " + returns.size() + " TDS returns successfully", returns));
    }

    @PostMapping("/returns/bulk")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk import historical TDS returns", description = "Imports historical quarterly TDS filings with token numbers and filing dates.")
    public ResponseEntity<ApiResponse<BulkTdsReturnImportResultDto>> bulkCreateReturns(@RequestBody List<CreateTdsReturnRequest> requests) {
        BulkTdsReturnImportResultDto result = tdsService.bulkCreateReturns(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk TDS returns migration completed", result));
    }

    @GetMapping("/returns/upcoming")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List upcoming TDS returns", description = "Retrieves unfiled returns approaching due date within specified days.")
    public ResponseEntity<ApiResponse<List<TdsReturnDto>>> getUpcomingReturns(@RequestParam(defaultValue = "30") int daysAhead) {
        List<TdsReturnDto> returns = tdsService.getUpcomingReturns(daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Upcoming TDS returns retrieved successfully", returns));
    }

    @GetMapping("/returns/overdue")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List overdue TDS returns", description = "Retrieves all unfiled returns past statutory deadline.")
    public ResponseEntity<ApiResponse<List<TdsReturnDto>>> getOverdueReturns() {
        List<TdsReturnDto> returns = tdsService.getOverdueReturns();
        return ResponseEntity.ok(ApiResponse.success("Overdue TDS returns retrieved successfully", returns));
    }

    @GetMapping("/clients/{clientId}/history")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Client TDS filing history", description = "Retrieves historical quarterly returns for a specific client.")
    public ResponseEntity<ApiResponse<List<TdsReturnDto>>> getClientReturnHistory(@PathVariable UUID clientId) {
        List<TdsReturnDto> history = tdsService.getClientReturnHistory(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client TDS filing history retrieved successfully", history));
    }

    @PostMapping("/seed-demo")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Seed demo practice TANs and returns", description = "Populates realistic TAN registrations, ITNS 281 challans, and quarterly return filings for testing.")
    public ResponseEntity<ApiResponse<List<TdsReturnDto>>> seedDemoData() {
        List<TdsReturnDto> returns = tdsService.seedDemoData();
        return ResponseEntity.ok(ApiResponse.success("Seeded " + returns.size() + " demo practice TDS returns", returns));
    }

    // =========================================================================
    // 3. Challans ITNS 281
    // =========================================================================

    @PostMapping("/challans")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Record Challan ITNS 281 deposit", description = "Records BSR code, challan tender date, serial number, and CIN breakdown.")
    public ResponseEntity<ApiResponse<TdsChallanDto>> createChallan(@Valid @RequestBody CreateTdsChallanRequest request) {
        TdsChallanDto challan = tdsService.createChallan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TDS Challan recorded successfully", challan));
    }

    @GetMapping("/challans")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search Challan 281 records", description = "Retrieves paginated challans with filters for quarter, FY, section, or utilization status.")
    public ResponseEntity<ApiResponse<PagedResponse<TdsChallanDto>>> getChallans(@Valid @ModelAttribute TdsChallanFilterRequest filterRequest) {
        PagedResponse<TdsChallanDto> response = tdsService.getChallans(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("TDS Challans retrieved successfully", response));
    }

    @GetMapping("/challans/{id}")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Challan 281 by ID", description = "Retrieves complete challan details and deductee allocation balance.")
    public ResponseEntity<ApiResponse<TdsChallanDto>> getChallanById(@PathVariable UUID id) {
        TdsChallanDto challan = tdsService.getChallanById(id);
        return ResponseEntity.ok(ApiResponse.success("TDS Challan retrieved successfully", challan));
    }

    @PutMapping("/challans/{id}")
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Challan 281", description = "Updates BSR, serial, section code, or utilization figures.")
    public ResponseEntity<ApiResponse<TdsChallanDto>> updateChallan(@PathVariable UUID id, @Valid @RequestBody UpdateTdsChallanRequest request) {
        TdsChallanDto challan = tdsService.updateChallan(id, request);
        return ResponseEntity.ok(ApiResponse.success("TDS Challan updated successfully", challan));
    }

    // =========================================================================
    // 4. Deductee Register
    // =========================================================================

    @PostMapping("/deductees")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Record Deductee entry", description = "Records PAN, payment date, taxable amount, TDS rate, and links to Challan 281.")
    public ResponseEntity<ApiResponse<TdsDeducteeEntryDto>> createDeducteeEntry(@Valid @RequestBody CreateTdsDeducteeEntryRequest request) {
        TdsDeducteeEntryDto entry = tdsService.createDeducteeEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Deductee entry recorded successfully", entry));
    }

    @GetMapping("/profiles/{profileId}/deductees")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Deductees by TAN Profile", description = "Retrieves all deductee transaction entries recorded for a TAN profile.")
    public ResponseEntity<ApiResponse<List<TdsDeducteeEntryDto>>> getDeducteesByProfile(@PathVariable UUID profileId) {
        List<TdsDeducteeEntryDto> entries = tdsService.getDeducteesByProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success("Deductee entries retrieved successfully", entries));
    }

    @GetMapping("/returns/{returnId}/deductees")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Deductees in TDS Return", description = "Retrieves all deductee transaction line items included in a specific quarterly return.")
    public ResponseEntity<ApiResponse<List<TdsDeducteeEntryDto>>> getDeducteesByReturn(@PathVariable UUID returnId) {
        List<TdsDeducteeEntryDto> entries = tdsService.getDeducteesByReturn(returnId);
        return ResponseEntity.ok(ApiResponse.success("Return deductees retrieved successfully", entries));
    }

    // =========================================================================
    // 5. Form 16 / 16A Certificates
    // =========================================================================

    @PostMapping("/certificates")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register TDS Certificate", description = "Registers Form 16 / 16A / 27D certificate details and TRACES request number.")
    public ResponseEntity<ApiResponse<TdsCertificateDto>> createCertificate(@Valid @RequestBody CreateTdsCertificateRequest request) {
        TdsCertificateDto cert = tdsService.createCertificate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TDS certificate registered successfully", cert));
    }

    @GetMapping("/profiles/{profileId}/certificates")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Certificates for TAN", description = "Retrieves all Form 16 / 16A certificates for a TAN profile.")
    public ResponseEntity<ApiResponse<List<TdsCertificateDto>>> getCertificatesByProfile(@PathVariable UUID profileId) {
        List<TdsCertificateDto> certs = tdsService.getCertificatesByProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success("Certificates retrieved successfully", certs));
    }

    @PatchMapping("/certificates/{id}/status")
    @PreAuthorize("hasAuthority('TDS_UPDATE') or hasAuthority('TDS_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Certificate Dispatch Status", description = "Updates lifecycle status (DOWNLOADED, DIGITALLY_SIGNED, SENT_TO_CLIENT, SENT_TO_DEDUCTEE).")
    public ResponseEntity<ApiResponse<TdsCertificateDto>> updateCertificateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateTdsCertificateStatusRequest request) {
        TdsCertificateDto cert = tdsService.updateCertificateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Certificate status updated successfully", cert));
    }

    // =========================================================================
    // 6. Workload Dashboard & Calculator
    // =========================================================================

    @GetMapping("/dashboard/workload")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "TDS Practice Workload Dashboard", description = "Returns summary metrics: active TANs, scheduled quarterly returns, total deducted, challans paid, and return cards for a quarter.")
    public ResponseEntity<ApiResponse<TdsWorkloadDashboardDto>> getWorkloadDashboard(
            @RequestParam(defaultValue = "Q1") String quarter,
            @RequestParam(defaultValue = "2026-27") String financialYear,
            @RequestParam(required = false) UUID assignedEmployeeId) {
        TdsWorkloadDashboardDto dashboard = tdsService.getWorkloadDashboard(quarter, financialYear, assignedEmployeeId);
        return ResponseEntity.ok(ApiResponse.success("TDS workload dashboard retrieved successfully", dashboard));
    }

    @PostMapping("/calculator/compute")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Instant TDS & Statutory Penalties Calculator", description = "Calculates TDS, surcharge, 4% cess, delay interest under Sec 201(1A), and late filing fee under Sec 234E.")
    public ResponseEntity<ApiResponse<TdsComputationResultDto>> computeTds(@Valid @RequestBody TdsComputationRequest request) {
        TdsComputationResultDto result = tdsService.computeTds(request);
        return ResponseEntity.ok(ApiResponse.success("TDS computed successfully", result));
    }

    @GetMapping("/calculator/rates")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "TDS Section Rates Master Catalog", description = "Returns pre-configured statutory rates, exemption threshold limits, and form mappings for all Indian TDS/TCS sections.")
    public ResponseEntity<ApiResponse<List<TdsSectionRateDto>>> getSectionRates() {
        List<TdsSectionRateDto> rates = tdsService.getSectionRates();
        return ResponseEntity.ok(ApiResponse.success("TDS section rates retrieved successfully", rates));
    }
}
