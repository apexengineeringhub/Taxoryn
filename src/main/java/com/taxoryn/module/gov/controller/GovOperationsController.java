package com.taxoryn.module.gov.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.observability.dto.GovOperationsDiagnosticsDto;
import com.taxoryn.module.gov.observability.dto.GovOperationsSummaryDto;
import com.taxoryn.module.gov.observability.service.GovOperationsObservabilityService;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller exposing endpoints for government integration operational observability,
 * diagnostics, and system reliability metrics.
 */
@RestController
@RequestMapping("/api/v1/gov/operations")
@RequiredArgsConstructor
@Tag(name = "Government Operations & Observability", description = "Endpoints for operational summaries, diagnostics, and reliability metrics")
@SecurityRequirement(name = "BearerAuth")
public class GovOperationsController {

    private final GovOperationsObservabilityService observabilityService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get operational summary", description = "Retrieves tenant-scoped operations, outbox queue depths, and provider health summary.")
    public ResponseEntity<ApiResponse<GovOperationsSummaryDto>> getOperationsSummary(
            @RequestParam(required = false) UUID organizationId) {
        UUID effectiveOrgId = organizationId != null ? organizationId : TenantContext.getTenantId();
        GovOperationsSummaryDto summary = observabilityService.getOperationsSummary(effectiveOrgId);
        return ResponseEntity.ok(ApiResponse.success("Operations summary retrieved successfully", summary));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get reliability metrics", description = "Retrieves live snapshot of provider-neutral reliability metrics, retry counters, outbox stats, and reconciliation events.")
    public ResponseEntity<ApiResponse<GovReliabilityMetricsSnapshot>> getMetrics() {
        GovReliabilityMetricsSnapshot snapshot = observabilityService.getMetricsSnapshot();
        return ResponseEntity.ok(ApiResponse.success("Reliability metrics snapshot retrieved successfully", snapshot));
    }

    @GetMapping("/{id}/diagnostics")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get operation diagnostics", description = "Retrieves sanitized diagnostic details for a specific operation, including related outbox events and error classification.")
    public ResponseEntity<ApiResponse<GovOperationsDiagnosticsDto>> getOperationDiagnostics(@PathVariable UUID id) {
        GovOperationsDiagnosticsDto diagnostics = observabilityService.getOperationDiagnostics(id);
        return ResponseEntity.ok(ApiResponse.success("Operation diagnostics retrieved successfully", diagnostics));
    }
}
