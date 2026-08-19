package com.taxoryn.module.compliance.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.AssignComplianceEmployeeRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.service.ComplianceRuleService;
import com.taxoryn.module.compliance.service.ComplianceService;
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
@RequestMapping({"/api/v1/compliance", "/api/compliance"})
@RequiredArgsConstructor
@Tag(name = "Compliance Calendar", description = "Statutory deadlines engine: Configurable due-date rules, compliance calendar, automated task generation, upcoming and overdue obligation monitoring")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final ComplianceRuleService ruleService;

    // =========================================================================
    // 1. Calendar & Filter Views
    // =========================================================================

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('GST_VIEW') or hasAuthority('ITR_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get compliance calendar obligations", description = "Retrieves paginated compliance obligations with filters by date range, period, domain, status, or assigned staff.")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceObligationDto>>> getCalendar(
            @Valid @ModelAttribute ComplianceCalendarFilterRequest filterRequest) {
        PagedResponse<ComplianceObligationDto> response = complianceService.getCalendar(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Compliance calendar retrieved successfully", response));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('GST_VIEW') or hasAuthority('ITR_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List upcoming compliance obligations", description = "Retrieves active obligations due within the specified number of days (default 30 days).")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getUpcoming(
            @RequestParam(defaultValue = "30") int daysAhead) {
        List<ComplianceObligationDto> upcoming = complianceService.getUpcoming(daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Upcoming compliance obligations retrieved successfully", upcoming));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('GST_VIEW') or hasAuthority('ITR_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List overdue compliance obligations", description = "Retrieves all unfulfilled compliance obligations past their statutory due date.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getOverdue() {
        List<ComplianceObligationDto> overdue = complianceService.getOverdue();
        return ResponseEntity.ok(ApiResponse.success("Overdue compliance obligations retrieved successfully", overdue));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('GST_VIEW') or hasAuthority('ITR_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List compliance obligations due today", description = "Retrieves all obligations whose statutory due date is today.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getDueToday() {
        List<ComplianceObligationDto> dueToday = complianceService.getDueToday();
        return ResponseEntity.ok(ApiResponse.success("Obligations due today retrieved successfully", dueToday));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('GST_VIEW') or hasAuthority('ITR_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Compliance executive dashboard statistics", description = "Returns summary metrics: due today, due this week, overdue, completed, and domain breakdown.")
    public ResponseEntity<ApiResponse<ComplianceDashboardStatsDto>> getDashboardStats() {
        ComplianceDashboardStatsDto stats = complianceService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Compliance dashboard statistics retrieved successfully", stats));
    }

    // =========================================================================
    // 2. Obligation Management & Assignment
    // =========================================================================

    @PostMapping("/obligations")
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('GST_CREATE') or hasAuthority('ITR_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create custom compliance obligation", description = "Creates a standalone or custom compliance obligation for a client.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> createObligation(
            @Valid @RequestBody CreateComplianceObligationRequest request) {
        ComplianceObligationDto obligation = complianceService.createObligation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Compliance obligation created successfully", obligation));
    }

    @GetMapping("/obligations/{id}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('GST_VIEW') or hasAuthority('ITR_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get compliance obligation by ID", description = "Retrieves details of a specific compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> getObligationById(@PathVariable UUID id) {
        ComplianceObligationDto obligation = complianceService.getObligationById(id);
        return ResponseEntity.ok(ApiResponse.success("Compliance obligation retrieved successfully", obligation));
    }

    @PatchMapping("/obligations/{id}/status")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('GST_UPDATE') or hasAuthority('ITR_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update compliance obligation status", description = "Updates status (e.g. COMPLETED, IN_PROGRESS, WAIVED). If completed, synchronizes linked task.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplianceStatusRequest request) {
        ComplianceObligationDto obligation = complianceService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance status updated successfully", obligation));
    }

    @PutMapping("/obligations/{id}/assigned-employee")
    @PreAuthorize("hasAuthority('TASK_ASSIGN') or hasAuthority('TASK_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign practitioner to obligation", description = "Assigns or reassigns staff member to the compliance obligation and synchronizes linked task.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> assignEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody AssignComplianceEmployeeRequest request) {
        ComplianceObligationDto obligation = complianceService.assignEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to compliance obligation successfully", obligation));
    }

    @PostMapping("/obligations/{id}/create-task")
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Convert obligation to actionable Task", description = "Generates a corresponding task in the Task Management module linked to this compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> createTaskForObligation(@PathVariable UUID id) {
        ComplianceObligationDto obligation = complianceService.createTaskForObligation(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Actionable task created and linked to compliance obligation", obligation));
    }

    // =========================================================================
    // 3. Batch Generation & Rules Engine
    // =========================================================================

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Batch generate compliance obligations for period", description = "Evaluates active compliance rules and generates obligations for active practice clients for a period.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> generateCompliance(
            @Valid @RequestBody GenerateComplianceRequest request) {
        List<ComplianceObligationDto> obligations = complianceService.generateComplianceObligations(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Generated " + obligations.size() + " compliance obligations for period " + request.getPeriod(), obligations));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List active compliance rules", description = "Retrieves all active system and custom configurable compliance rules.")
    public ResponseEntity<ApiResponse<List<ComplianceRuleDto>>> getActiveRules() {
        List<ComplianceRuleDto> rules = ruleService.getActiveRules();
        return ResponseEntity.ok(ApiResponse.success("Active compliance rules retrieved successfully", rules));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create custom compliance rule", description = "Creates a tenant-specific custom compliance due-date rule.")
    public ResponseEntity<ApiResponse<ComplianceRuleDto>> createCustomRule(
            @Valid @RequestBody CreateComplianceRuleRequest request) {
        ComplianceRuleDto rule = ruleService.createCustomRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Custom compliance rule created successfully", rule));
    }
}
