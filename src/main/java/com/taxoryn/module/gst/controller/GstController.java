package com.taxoryn.module.gst.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.gst.dto.BatchGenerateFilingsRequest;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.GstFilingFilterRequest;
import com.taxoryn.module.gst.dto.GstMonthlySummaryDto;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstProfileFilterRequest;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto;
import com.taxoryn.module.gst.dto.SaveGstMonthlySummaryRequest;
import com.taxoryn.module.gst.dto.UpdateGstFilingStatusRequest;
import com.taxoryn.module.gst.dto.UpdateGstProfileRequest;
import com.taxoryn.module.gst.dto.UpdateGstProfileStatusRequest;
import com.taxoryn.module.gst.service.GstService;
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
@RequestMapping({"/api/v1/gst", "/api/gst"})
@RequiredArgsConstructor
@Tag(name = "GST Management", description = "End-to-end Goods & Services Tax compliance: Registrations, GSTR-1/3B/9/CMP-08 Filings, ITC Tracking, and Workload Analytics")
@SecurityRequirement(name = "BearerAuth")
public class GstController {

    private final GstService gstService;

    // =========================================================================
    // 1. GST Profiles & Registrations
    // =========================================================================

    @PostMapping("/profiles")
    @PreAuthorize("hasAuthority('GST_CREATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register GST profile", description = "Onboards a client's GSTIN registration and sets filing frequency & assigned practitioner.")
    public ResponseEntity<ApiResponse<GstProfileDto>> createProfile(@Valid @RequestBody CreateGstProfileRequest request) {
        GstProfileDto profile = gstService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("GST Profile registered successfully", profile));
    }

    @GetMapping("/profiles")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search GST profiles", description = "Retrieves paginated GST profiles with filters for GSTIN, scheme, and assigned practitioner.")
    public ResponseEntity<ApiResponse<PagedResponse<GstProfileDto>>> getProfiles(@Valid @ModelAttribute GstProfileFilterRequest filterRequest) {
        PagedResponse<GstProfileDto> response = gstService.getProfiles(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("GST profiles retrieved successfully", response));
    }

    @GetMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get GST profile by ID", description = "Retrieves complete details of a GST registration profile.")
    public ResponseEntity<ApiResponse<GstProfileDto>> getProfileById(@PathVariable UUID id) {
        GstProfileDto profile = gstService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("GST profile retrieved successfully", profile));
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('GST_UPDATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update GST profile", description = "Updates business names, scheme type, frequency, or assigned practitioner.")
    public ResponseEntity<ApiResponse<GstProfileDto>> updateProfile(@PathVariable UUID id, @Valid @RequestBody UpdateGstProfileRequest request) {
        GstProfileDto profile = gstService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("GST profile updated successfully", profile));
    }

    @PatchMapping("/profiles/{id}/status")
    @PreAuthorize("hasAuthority('GST_UPDATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update GST profile status", description = "Transitions profile status (ACTIVE, SUSPENDED, CANCELLED, SURRENDERED).")
    public ResponseEntity<ApiResponse<GstProfileDto>> updateProfileStatus(@PathVariable UUID id, @Valid @RequestBody UpdateGstProfileStatusRequest request) {
        GstProfileDto profile = gstService.updateProfileStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("GST profile status updated successfully", profile));
    }

    // =========================================================================
    // 2. GST Return Filings
    // =========================================================================

    @PostMapping("/filings")
    @PreAuthorize("hasAuthority('GST_CREATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create / Schedule return filing", description = "Schedules a specific GST return filing (GSTR-1, GSTR-3B, GSTR-9, CMP-08).")
    public ResponseEntity<ApiResponse<GstReturnFilingDto>> createFiling(@Valid @RequestBody CreateGstReturnFilingRequest request) {
        GstReturnFilingDto filing = gstService.createFiling(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("GST filing created successfully", filing));
    }

    @GetMapping("/filings")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & filter return filings", description = "Filters filings by return type, period, status, or assigned practitioner.")
    public ResponseEntity<ApiResponse<PagedResponse<GstReturnFilingDto>>> getFilings(@Valid @ModelAttribute GstFilingFilterRequest filterRequest) {
        PagedResponse<GstReturnFilingDto> response = gstService.getFilings(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("GST filings retrieved successfully", response));
    }

    @GetMapping("/filings/{id}")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get filing by ID", description = "Retrieves filing record details and filing history.")
    public ResponseEntity<ApiResponse<GstReturnFilingDto>> getFilingById(@PathVariable UUID id) {
        GstReturnFilingDto filing = gstService.getFilingById(id);
        return ResponseEntity.ok(ApiResponse.success("GST filing retrieved successfully", filing));
    }

    @PatchMapping("/filings/{id}/status")
    @PreAuthorize("hasAuthority('GST_UPDATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update filing status & record ARN", description = "Updates filing lifecycle (PREPARED, UNDER_REVIEW, FILED) and records the GST Portal ARN.")
    public ResponseEntity<ApiResponse<GstReturnFilingDto>> updateFilingStatus(@PathVariable UUID id, @Valid @RequestBody UpdateGstFilingStatusRequest request) {
        GstReturnFilingDto filing = gstService.updateFilingStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("GST filing status updated successfully", filing));
    }

    @PostMapping("/filings/batch-generate")
    @PreAuthorize("hasAuthority('GST_CREATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Batch generate filings for practice", description = "Auto-generates monthly/quarterly return filings for all active GST clients in the organization.")
    public ResponseEntity<ApiResponse<List<GstReturnFilingDto>>> batchGenerateFilings(@Valid @RequestBody BatchGenerateFilingsRequest request) {
        List<GstReturnFilingDto> filings = gstService.batchGenerateFilings(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Batch generated " + filings.size() + " filings successfully", filings));
    }

    // =========================================================================
    // 3. Monthly Computation & Summary
    // =========================================================================

    @PostMapping("/summaries")
    @PreAuthorize("hasAuthority('GST_CREATE') or hasAuthority('GST_UPDATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Save monthly computation summary", description = "Records taxable sales, purchases, eligible/ineligible ITC, and net tax liability.")
    public ResponseEntity<ApiResponse<GstMonthlySummaryDto>> saveMonthlySummary(@Valid @RequestBody SaveGstMonthlySummaryRequest request) {
        GstMonthlySummaryDto summary = gstService.saveMonthlySummary(request);
        return ResponseEntity.ok(ApiResponse.success("GST monthly computation saved successfully", summary));
    }

    @GetMapping("/summaries")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get monthly computation summary", description = "Retrieves sales, purchase, ITC, and liability computation for a specific profile and month.")
    public ResponseEntity<ApiResponse<GstMonthlySummaryDto>> getMonthlySummary(
            @RequestParam UUID gstProfileId,
            @RequestParam String period) {
        GstMonthlySummaryDto summary = gstService.getMonthlySummary(gstProfileId, period);
        return ResponseEntity.ok(ApiResponse.success("GST monthly summary retrieved successfully", summary));
    }

    // =========================================================================
    // 4. Workload Dashboard & History
    // =========================================================================

    @GetMapping("/dashboard/workload")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "GST Practice & Employee Workload Dashboard", description = "Returns the real-time compliance workload table for a period: Client, GSTIN, GSTR-1, GSTR-3B, ITC, Tax Liability, Due Date, and Assigned Practitioner.")
    public ResponseEntity<ApiResponse<GstWorkloadDashboardDto>> getWorkloadDashboard(
            @RequestParam String period,
            @RequestParam(required = false) UUID assignedEmployeeId) {
        GstWorkloadDashboardDto dashboard = gstService.getWorkloadDashboard(period, assignedEmployeeId);
        return ResponseEntity.ok(ApiResponse.success("GST workload dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/clients/{clientId}/history")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Client GST filing history", description = "Retrieves historical list of all GST returns filed by a client.")
    public ResponseEntity<ApiResponse<List<GstReturnFilingDto>>> getClientFilingHistory(@PathVariable UUID clientId) {
        List<GstReturnFilingDto> history = gstService.getClientFilingHistory(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client GST filing history retrieved successfully", history));
    }
}
