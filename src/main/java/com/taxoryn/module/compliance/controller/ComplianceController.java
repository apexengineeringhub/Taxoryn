package com.taxoryn.module.compliance.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.AssignComplianceEmployeeRequest;
import com.taxoryn.module.compliance.dto.AssignObligationRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarSummaryDto;
import com.taxoryn.module.compliance.dto.ComplianceCycleTemplateDto;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
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
@Tag(name = "Compliance Calendar", description = "Centralized Compliance Calendar & Statutory Obligations Engine")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final ComplianceRuleService ruleService;

    // =========================================================================
    // 1. Calendar & Filter Views
    // =========================================================================

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance calendar obligations", description = "Retrieves paginated compliance obligations with filters by date range, period, domain, status, priority, or assigned staff.")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceObligationDto>>> getCalendar(
            @Valid @ModelAttribute ComplianceCalendarFilterRequest filterRequest) {
        PagedResponse<ComplianceObligationDto> response = complianceService.getCalendar(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Compliance calendar retrieved successfully", response));
    }

    @GetMapping("/obligations")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List compliance obligations", description = "Retrieves paginated compliance obligations matching query parameters.")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceObligationDto>>> getObligations(
            @Valid @ModelAttribute ComplianceCalendarFilterRequest filterRequest) {
        PagedResponse<ComplianceObligationDto> response = complianceService.getCalendar(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Compliance obligations retrieved successfully", response));
    }

    @GetMapping("/calendar/summary")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Compliance calendar summary metrics", description = "Returns summary metrics: due today, due this week, due this month, overdue, waiting for client, ready for filing, and completed.")
    public ResponseEntity<ApiResponse<ComplianceCalendarSummaryDto>> getCalendarSummary() {
        ComplianceCalendarSummaryDto summary = complianceService.getCalendarSummary();
        return ResponseEntity.ok(ApiResponse.success("Compliance calendar summary retrieved successfully", summary));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List upcoming compliance obligations", description = "Retrieves active obligations due within the specified number of days (default 30 days).")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getUpcoming(
            @RequestParam(defaultValue = "30") int daysAhead) {
        List<ComplianceObligationDto> upcoming = complianceService.getUpcoming(daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Upcoming compliance obligations retrieved successfully", upcoming));
    }

    @GetMapping("/calendar/upcoming")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List upcoming compliance obligations (Calendar route)", description = "Retrieves active obligations due within the specified number of days.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getCalendarUpcoming(
            @RequestParam(defaultValue = "30") int daysAhead) {
        List<ComplianceObligationDto> upcoming = complianceService.getUpcoming(daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Upcoming compliance obligations retrieved successfully", upcoming));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List overdue compliance obligations", description = "Retrieves all unfulfilled compliance obligations past their statutory due date.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getOverdue() {
        List<ComplianceObligationDto> overdue = complianceService.getOverdue();
        return ResponseEntity.ok(ApiResponse.success("Overdue compliance obligations retrieved successfully", overdue));
    }

    @GetMapping("/calendar/overdue")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List overdue compliance obligations (Calendar route)", description = "Retrieves all unfulfilled compliance obligations past their statutory due date.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getCalendarOverdue() {
        List<ComplianceObligationDto> overdue = complianceService.getOverdue();
        return ResponseEntity.ok(ApiResponse.success("Overdue compliance obligations retrieved successfully", overdue));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List compliance obligations due today", description = "Retrieves all obligations whose statutory due date is today.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getDueToday() {
        List<ComplianceObligationDto> dueToday = complianceService.getDueToday();
        return ResponseEntity.ok(ApiResponse.success("Obligations due today retrieved successfully", dueToday));
    }

    @GetMapping("/calendar/today")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List compliance obligations due today (Calendar route)", description = "Retrieves all obligations whose statutory due date is today.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getCalendarDueToday() {
        List<ComplianceObligationDto> dueToday = complianceService.getDueToday();
        return ResponseEntity.ok(ApiResponse.success("Obligations due today retrieved successfully", dueToday));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Compliance executive dashboard statistics", description = "Returns summary metrics: due today, due this week, overdue, completed, and domain breakdown.")
    public ResponseEntity<ApiResponse<ComplianceDashboardStatsDto>> getDashboardStats() {
        ComplianceDashboardStatsDto stats = complianceService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Compliance dashboard statistics retrieved successfully", stats));
    }

    // =========================================================================
    // 2. Obligation Management & Assignment
    // =========================================================================

    @PostMapping({"/obligations", "/calendar/obligations"})
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'CLIENT_UPDATE', 'GST_CREATE', 'ITR_CREATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Create custom compliance obligation", description = "Creates a standalone or client-service linked compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> createObligation(
            @Valid @RequestBody CreateComplianceObligationRequest request) {
        ComplianceObligationDto obligation = complianceService.createObligation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Compliance obligation created successfully", obligation));
    }

    @GetMapping({"/obligations/{id}", "/calendar/obligations/{id}"})
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance obligation by ID", description = "Retrieves details of a specific compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> getObligationById(@PathVariable UUID id) {
        ComplianceObligationDto obligation = complianceService.getObligationById(id);
        return ResponseEntity.ok(ApiResponse.success("Compliance obligation retrieved successfully", obligation));
    }

    @PutMapping({"/obligations/{id}", "/calendar/obligations/{id}"})
    @PreAuthorize("hasAnyAuthority('TASK_UPDATE', 'CLIENT_UPDATE', 'GST_UPDATE', 'ITR_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Update compliance obligation details", description = "Updates title, description, target dates, or priority of a compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> updateObligation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplianceObligationRequest request) {
        ComplianceObligationDto obligation = complianceService.updateObligation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance obligation updated successfully", obligation));
    }

    @PatchMapping({"/obligations/{id}/status", "/calendar/obligations/{id}/status"})
    @PreAuthorize("hasAnyAuthority('TASK_UPDATE', 'CLIENT_UPDATE', 'GST_UPDATE', 'ITR_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Update compliance obligation status", description = "Updates status (e.g. IN_PROGRESS, WAITING_FOR_CLIENT, READY_FOR_FILING, COMPLETED).")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateObligationStatusRequest request) {
        ComplianceObligationDto obligation = complianceService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance status updated successfully", obligation));
    }

    @PatchMapping({"/obligations/{id}/assign", "/calendar/obligations/{id}/assign"})
    @PreAuthorize("hasAnyAuthority('TASK_ASSIGN', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Assign practitioner to obligation", description = "Assigns staff member to the compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> assignEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody AssignObligationRequest request) {
        ComplianceObligationDto obligation = complianceService.assignEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to compliance obligation successfully", obligation));
    }

    @PutMapping({"/obligations/{id}/assigned-employee", "/calendar/obligations/{id}/assigned-employee"})
    @PreAuthorize("hasAnyAuthority('TASK_ASSIGN', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Assign practitioner to obligation (Legacy route)", description = "Assigns staff member to the compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> assignEmployeeLegacy(
            @PathVariable UUID id,
            @Valid @RequestBody AssignComplianceEmployeeRequest request) {
        ComplianceObligationDto obligation = complianceService.assignEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to compliance obligation successfully", obligation));
    }

    @org.springframework.web.bind.annotation.DeleteMapping({"/obligations/{id}", "/calendar/obligations/{id}"})
    @PreAuthorize("hasAnyAuthority('TASK_DELETE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN')")
    @Operation(summary = "Delete compliance obligation", description = "Removes a compliance obligation.")
    public ResponseEntity<ApiResponse<Void>> deleteObligation(@PathVariable UUID id) {
        complianceService.deleteObligation(id);
        return ResponseEntity.ok(ApiResponse.success("Compliance obligation deleted successfully", null));
    }

    @GetMapping({"/clients/{clientId}", "/calendar/clients/{clientId}"})
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance obligations for a client", description = "Retrieves all compliance obligations associated with a client.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getObligationsByClient(@PathVariable UUID clientId) {
        List<ComplianceObligationDto> list = complianceService.getObligationsByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client compliance obligations retrieved successfully", list));
    }

    @GetMapping({"/services/{serviceId}", "/calendar/services/{serviceId}"})
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance obligations for a client service", description = "Retrieves all compliance obligations associated with a client service engagement.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getObligationsByService(@PathVariable UUID serviceId) {
        List<ComplianceObligationDto> list = complianceService.getObligationsByServiceId(serviceId);
        return ResponseEntity.ok(ApiResponse.success("Service compliance obligations retrieved successfully", list));
    }

    @PostMapping({"/services/{serviceId}/periods/{periodId}/generate", "/calendar/services/{serviceId}/periods/{periodId}/generate"})
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'CLIENT_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Generate obligation for service period", description = "Idempotently creates a compliance obligation for a client service period.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> generateObligationForServicePeriod(
            @PathVariable UUID serviceId,
            @PathVariable UUID periodId) {
        ComplianceObligationDto obligation = complianceService.generateObligationForServicePeriod(serviceId, periodId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Compliance obligation generated for service period successfully", obligation));
    }

    @PostMapping({"/obligations/{id}/create-task", "/calendar/obligations/{id}/create-task"})
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Convert obligation to actionable Task", description = "Generates a corresponding task in the Task Management module linked to this compliance obligation.")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> createTaskForObligation(@PathVariable UUID id) {
        ComplianceObligationDto obligation = complianceService.createTaskForObligation(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Actionable task created and linked to compliance obligation", obligation));
    }

    // =========================================================================
    // 3. Batch Generation & Recurring Cycle Templates
    // =========================================================================

    @GetMapping({"/cycles", "/cycle-templates", "/calendar/cycle-templates"})
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "List compliance cycle templates", description = "Retrieves all system and practice-specific recurring compliance cycle definitions.")
    public ResponseEntity<ApiResponse<List<ComplianceCycleTemplateDto>>> getCycleTemplates() {
        List<ComplianceCycleTemplateDto> templates = complianceService.getCycleTemplates();
        return ResponseEntity.ok(ApiResponse.success("Compliance cycle templates retrieved successfully", templates));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
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
