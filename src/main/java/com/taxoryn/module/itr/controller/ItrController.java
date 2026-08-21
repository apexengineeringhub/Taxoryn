package com.taxoryn.module.itr.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.itr.dto.AssignItrEmployeeRequest;
import com.taxoryn.module.itr.dto.BatchGenerateItrReturnsRequest;
import com.taxoryn.module.itr.dto.BulkItrImportResultDto;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrFilterRequest;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrWorkloadDashboardDto;
import com.taxoryn.module.itr.dto.RecordItrFilingRequest;
import com.taxoryn.module.itr.dto.UpdateItrProfileRequest;
import com.taxoryn.module.itr.dto.UpdateItrReturnRequest;
import com.taxoryn.module.itr.dto.UpdateItrStatusRequest;
import com.taxoryn.module.itr.service.ItrService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/itr", "/api/itr"})
@RequiredArgsConstructor
@Tag(name = "ITR Management", description = "Income Tax Returns compliance lifecycle: Client ITR Profiles, ITR-1 to ITR-7 Filings, Status Workflow Progression, Overdue Monitoring, and Workload Analytics")
@SecurityRequirement(name = "BearerAuth")
public class ItrController {

    private final ItrService itrService;

    // =========================================================================
    // 1. ITR Profiles
    // =========================================================================

    @PostMapping("/profiles")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create ITR profile for client", description = "Registers a client's PAN, default ITR form type, taxpayer constitution, and assigned practitioner.")
    public ResponseEntity<ApiResponse<ItrProfileDto>> createProfile(@Valid @RequestBody CreateItrProfileRequest request) {
        ItrProfileDto profile = itrService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("ITR Profile created successfully", profile));
    }

    @PostMapping("/profiles/bulk")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk import ITR profiles", description = "Migrates multiple client PANs & ITR default forms from CSV/Excel in a single batch.")
    public ResponseEntity<ApiResponse<BulkItrImportResultDto>> bulkCreateProfiles(@RequestBody List<CreateItrProfileRequest> requests) {
        BulkItrImportResultDto result = itrService.bulkCreateProfiles(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk ITR profiles migration completed", result));
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('ITR_UPDATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update ITR profile", description = "Updates PAN, default ITR form type, residential status, or assigned practitioner.")
    public ResponseEntity<ApiResponse<ItrProfileDto>> updateProfile(@PathVariable UUID id, @Valid @RequestBody UpdateItrProfileRequest request) {
        ItrProfileDto profile = itrService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("ITR Profile updated successfully", profile));
    }

    @GetMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get ITR profile by ID", description = "Retrieves complete ITR profile details by ID.")
    public ResponseEntity<ApiResponse<ItrProfileDto>> getProfileById(@PathVariable UUID id) {
        ItrProfileDto profile = itrService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("ITR profile retrieved successfully", profile));
    }

    @GetMapping("/profiles/clients/{clientId}")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get ITR profile by client ID", description = "Retrieves the active ITR profile associated with a specific client.")
    public ResponseEntity<ApiResponse<ItrProfileDto>> getProfileByClientId(@PathVariable UUID clientId) {
        ItrProfileDto profile = itrService.getProfileByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client ITR profile retrieved successfully", profile));
    }

    // =========================================================================
    // 2. ITR Returns Lifecycle
    // =========================================================================

    @PostMapping("/returns")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create ITR return record", description = "Initializes an ITR return filing record for a client and assessment year.")
    public ResponseEntity<ApiResponse<ItrReturnDto>> createReturn(@Valid @RequestBody CreateItrReturnRequest request) {
        ItrReturnDto itrReturn = itrService.createReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("ITR return created successfully", itrReturn));
    }

    @PostMapping("/returns/bulk")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk import ITR returns", description = "Migrates historical ITR return filing records with Ack numbers and dates.")
    public ResponseEntity<ApiResponse<BulkItrImportResultDto>> bulkCreateReturns(@RequestBody List<CreateItrReturnRequest> requests) {
        BulkItrImportResultDto result = itrService.bulkCreateReturns(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk ITR returns migration completed", result));
    }

    @PostMapping("/returns/batch-generate")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Batch generate ITR returns firm-wide", description = "Auto-creates ITR return records for all active ITR practice clients for target Assessment Year.")
    public ResponseEntity<ApiResponse<List<ItrReturnDto>>> batchGenerateReturns(@Valid @RequestBody BatchGenerateItrReturnsRequest request) {
        List<ItrReturnDto> returns = itrService.batchGenerateReturns(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Batch generated " + returns.size() + " ITR returns successfully", returns));
    }

    @PostMapping("/seed-demo")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Seed demo practice taxpayers & returns", description = "Initializes 8 realistic practice clients, ITR profiles and AY 2026-27 returns for testing.")
    public ResponseEntity<ApiResponse<List<ItrReturnDto>>> seedDemoData() {
        List<ItrReturnDto> returns = itrService.seedDemoData();
        return ResponseEntity.ok(ApiResponse.success("Seeded " + returns.size() + " demo practice taxpayers and ITR returns", returns));
    }

    @GetMapping("/returns")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search ITR returns with filters", description = "Retrieves paginated ITR returns with filters for AY, FY, form type, workflow status, or assigned staff.")
    public ResponseEntity<ApiResponse<PagedResponse<ItrReturnDto>>> getReturns(@Valid @ModelAttribute ItrFilterRequest filterRequest) {
        PagedResponse<ItrReturnDto> response = itrService.getReturns(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("ITR returns retrieved successfully", response));
    }

    @GetMapping("/returns/{id}")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get ITR return by ID", description = "Retrieves ITR return details, statutory dates, and filing status.")
    public ResponseEntity<ApiResponse<ItrReturnDto>> getReturnById(@PathVariable UUID id) {
        ItrReturnDto itrReturn = itrService.getReturnById(id);
        return ResponseEntity.ok(ApiResponse.success("ITR return retrieved successfully", itrReturn));
    }

    @PutMapping("/returns/{id}")
    @PreAuthorize("hasAuthority('ITR_UPDATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update ITR return", description = "Updates form type, taxpayer type, due date, status, notes, or assigned employee.")
    public ResponseEntity<ApiResponse<ItrReturnDto>> updateReturn(@PathVariable UUID id, @Valid @RequestBody UpdateItrReturnRequest request) {
        ItrReturnDto itrReturn = itrService.updateReturn(id, request);
        return ResponseEntity.ok(ApiResponse.success("ITR return updated successfully", itrReturn));
    }

    @PatchMapping("/returns/{id}/status")
    @PreAuthorize("hasAuthority('ITR_UPDATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update ITR status workflow transition", description = "Transitions return through workflow statuses (DOCUMENTS_PENDING -> DATA_ENTRY -> UNDER_REVIEW -> READY_TO_FILE -> FILED -> VERIFICATION_PENDING -> COMPLETED).")
    public ResponseEntity<ApiResponse<ItrReturnDto>> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateItrStatusRequest request) {
        ItrReturnDto itrReturn = itrService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("ITR status updated successfully to " + itrReturn.getStatus(), itrReturn));
    }

    @PostMapping({"/returns/{id}/filing-details", "/returns/{id}/file"})
    @PreAuthorize("hasAuthority('ITR_UPDATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Record e-Filing submission details", description = "Records the date of e-filing and the e-Filing Acknowledgement Number / ITR-V Ack.")
    public ResponseEntity<ApiResponse<ItrReturnDto>> recordFilingDetails(@PathVariable UUID id, @Valid @RequestBody RecordItrFilingRequest request) {
        ItrReturnDto itrReturn = itrService.recordFilingDetails(id, request);
        return ResponseEntity.ok(ApiResponse.success("ITR filing submission details recorded successfully", itrReturn));
    }

    @PutMapping("/returns/{id}/assigned-employee")
    @PreAuthorize("hasAuthority('ITR_UPDATE') or hasAuthority('ITR_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign employee to ITR return", description = "Assigns or reassigns a practice staff member to the ITR return.")
    public ResponseEntity<ApiResponse<ItrReturnDto>> assignEmployee(@PathVariable UUID id, @Valid @RequestBody AssignItrEmployeeRequest request) {
        ItrReturnDto itrReturn = itrService.assignEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to ITR return successfully", itrReturn));
    }

    // =========================================================================
    // 3. Upcoming, Overdue, History & Workload Dashboard
    // =========================================================================

    @GetMapping("/returns/upcoming")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List upcoming ITR returns", description = "Retrieves unfiled ITR returns with due dates approaching within the specified number of days (default 30 days).")
    public ResponseEntity<ApiResponse<List<ItrReturnDto>>> getUpcomingReturns(@RequestParam(defaultValue = "30") int daysAhead) {
        List<ItrReturnDto> returns = itrService.getUpcomingReturns(daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Upcoming ITR returns retrieved successfully", returns));
    }

    @GetMapping("/returns/overdue")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List overdue ITR returns", description = "Retrieves all unfiled ITR returns past their statutory due date.")
    public ResponseEntity<ApiResponse<List<ItrReturnDto>>> getOverdueReturns() {
        List<ItrReturnDto> returns = itrService.getOverdueReturns();
        return ResponseEntity.ok(ApiResponse.success("Overdue ITR returns retrieved successfully", returns));
    }

    @GetMapping("/clients/{clientId}/history")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Client ITR filing history", description = "Retrieves all historical ITR return records for a client across assessment years.")
    public ResponseEntity<ApiResponse<List<ItrReturnDto>>> getClientItrHistory(@PathVariable UUID clientId) {
        List<ItrReturnDto> history = itrService.getClientItrHistory(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client ITR history retrieved successfully", history));
    }

    @GetMapping("/dashboard/workload")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "ITR Practice & Employee Workload Dashboard", description = "Returns real-time ITR return breakdown by workflow status, upcoming/overdue counts, and client cards for an Assessment Year.")
    public ResponseEntity<ApiResponse<ItrWorkloadDashboardDto>> getWorkloadDashboard(
            @RequestParam(required = false) String assessmentYear,
            @RequestParam(required = false) UUID assignedEmployeeId) {
        ItrWorkloadDashboardDto dashboard = itrService.getWorkloadDashboard(assessmentYear, assignedEmployeeId);
        return ResponseEntity.ok(ApiResponse.success("ITR workload dashboard retrieved successfully", dashboard));
    }
}
