package com.taxoryn.module.dashboard.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.dashboard.dto.BillingDashboardSummaryDto;
import com.taxoryn.module.dashboard.dto.ClientSummaryDashboardDto;
import com.taxoryn.module.dashboard.dto.ComplianceDashboardDto;
import com.taxoryn.module.dashboard.dto.DashboardFilterRequest;
import com.taxoryn.module.dashboard.dto.DocumentDashboardDto;
import com.taxoryn.module.dashboard.dto.NoticeDashboardDto;
import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;
import com.taxoryn.module.dashboard.dto.PracticeDashboardOverviewDto;
import com.taxoryn.module.dashboard.dto.WorkDashboardDto;
import com.taxoryn.module.dashboard.service.DashboardService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/api/v1/dashboard", "/api/dashboard"})
@RequiredArgsConstructor
@Tag(name = "Practice Dashboard", description = "Practice-wide aggregate dashboards, operational KPIs, and workload analytics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('CLIENT_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Practice Dashboard Overview", description = "Consolidated operational KPIs covering clients, engagements, compliance obligations, work items, tasks, documents, notices, and billing.")
    public ResponseEntity<ApiResponse<PracticeDashboardOverviewDto>> getOverview(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        PracticeDashboardOverviewDto overview = dashboardService.getOverview(filter);
        return ResponseEntity.ok(ApiResponse.success("Overview dashboard retrieved successfully", overview));
    }

    @GetMapping("/compliance")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('COMPLIANCE_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Compliance Dashboard", description = "Compliance obligations overview categorized by GST, ITR, TDS, and Notices with status breakdowns and upcoming deadlines.")
    public ResponseEntity<ApiResponse<ComplianceDashboardDto>> getComplianceDashboard(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        ComplianceDashboardDto compliance = dashboardService.getComplianceDashboard(filter);
        return ResponseEntity.ok(ApiResponse.success("Compliance dashboard retrieved successfully", compliance));
    }

    @GetMapping("/work")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('TASK_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Work Management Dashboard", description = "Work items and tasks status distribution, overdue tracking, and individual user workload breakdown.")
    public ResponseEntity<ApiResponse<WorkDashboardDto>> getWorkDashboard(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        WorkDashboardDto work = dashboardService.getWorkDashboard(filter);
        return ResponseEntity.ok(ApiResponse.success("Work dashboard retrieved successfully", work));
    }

    @GetMapping("/documents")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('DOCUMENT_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Document & Request Dashboard", description = "Total documents, active workflow documents, document requests status, fulfilled and overdue tracking.")
    public ResponseEntity<ApiResponse<DocumentDashboardDto>> getDocumentDashboard(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        DocumentDashboardDto documents = dashboardService.getDocumentDashboard(filter);
        return ResponseEntity.ok(ApiResponse.success("Document dashboard retrieved successfully", documents));
    }

    @GetMapping("/notices")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('NOTICE_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Tax Notice Dashboard", description = "Tax notices pipeline, responses required, hearings scheduled, overdue deadlines, and total disputed demand amount.")
    public ResponseEntity<ApiResponse<NoticeDashboardDto>> getNoticeDashboard(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        NoticeDashboardDto notices = dashboardService.getNoticeDashboard(filter);
        return ResponseEntity.ok(ApiResponse.success("Notice dashboard retrieved successfully", notices));
    }

    @GetMapping("/billing")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('BILLING_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Billing Dashboard Summary", description = "Invoice counts by status, invoiced, collected, outstanding, and overdue amounts with revenue by service category.")
    public ResponseEntity<ApiResponse<BillingDashboardSummaryDto>> getBillingDashboard(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        BillingDashboardSummaryDto billing = dashboardService.getBillingDashboard(filter);
        return ResponseEntity.ok(ApiResponse.success("Billing dashboard retrieved successfully", billing));
    }

    @GetMapping("/clients")
    @RequiresModule(ProductModuleCode.REPORTS)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('CLIENT_VIEW') or hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN', 'PRACTICE_ADMIN')")
    @Operation(summary = "Get Client Operational Summary & Attention List", description = "Client statistics and operational attention list highlighting clients with pending obligations, overdue work, open notices, or outstanding bills.")
    public ResponseEntity<ApiResponse<ClientSummaryDashboardDto>> getClientDashboard(
            @Valid @ModelAttribute DashboardFilterRequest filter
    ) {
        ClientSummaryDashboardDto clients = dashboardService.getClientDashboard(filter);
        return ResponseEntity.ok(ApiResponse.success("Client dashboard retrieved successfully", clients));
    }

    @GetMapping({"", "/organization"})
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
    @Operation(
            summary = "Get Organization Dashboard",
            description = "Retrieves high-level organization statistics for clients, employees, tasks, GST, ITR, billing, and employee workloads with optimized aggregation queries."
    )
    public ResponseEntity<ApiResponse<OrganizationDashboardDto>> getOrganizationDashboard() {
        OrganizationDashboardDto dashboard = dashboardService.getOrganizationDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", dashboard));
    }
}
