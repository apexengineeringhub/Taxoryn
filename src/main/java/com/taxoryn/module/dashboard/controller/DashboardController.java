package com.taxoryn.module.dashboard.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;
import com.taxoryn.module.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/dashboard", "/api/dashboard"})
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Organization-level aggregate dashboard and workload metrics")
public class DashboardController {

    private final DashboardService dashboardService;

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
