package com.taxoryn.module.dashboard.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.dashboard.dto.PlatformDashboardSummaryDto;
import com.taxoryn.module.dashboard.service.PlatformDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/platform/dashboard", "/api/admin/platform/dashboard"})
@RequiredArgsConstructor
@Tag(name = "Platform Operations Dashboard", description = "Consolidated, privacy-safe platform metrics for Taxoryn SuperAdmin")
@SecurityRequirement(name = "BearerAuth")
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get Platform Overview Dashboard",
            description = "Retrieves executive platform-level metrics including growth, practice ecosystem, user breakdown, marketplace funnel, subscription MRR, feedback triage, and system health. Contains ZERO sensitive tax filings or client invoices."
    )
    public ResponseEntity<ApiResponse<PlatformDashboardSummaryDto>> getPlatformDashboard() {
        PlatformDashboardSummaryDto summary = platformDashboardService.getPlatformDashboard();
        return ResponseEntity.ok(ApiResponse.success("Platform dashboard metrics retrieved successfully", summary));
    }
}
