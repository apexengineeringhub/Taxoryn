package com.taxoryn.module.dashboard.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.dashboard.dto.SupportDashboardDto;
import com.taxoryn.module.dashboard.service.SupportDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/support", "/api/admin/support"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Support Workspace & Dashboard", description = "Endpoints for Taxoryn Platform Support operations")
public class SupportDashboardController {

    private final SupportDashboardService supportDashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get platform support overview metrics, attention queues, and recent activity")
    @PreAuthorize("hasRole('TAXORYN_SUPPORT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('SUPPORT_VIEW') or hasAuthority('FEEDBACK_VIEW')")
    public ResponseEntity<ApiResponse<SupportDashboardDto>> getSupportOverview() {
        SupportDashboardDto dashboard = supportDashboardService.getSupportOverview();
        return ResponseEntity.ok(ApiResponse.success("Support dashboard metrics retrieved", dashboard));
    }
}
