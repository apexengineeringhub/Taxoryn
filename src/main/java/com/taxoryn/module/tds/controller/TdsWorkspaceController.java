package com.taxoryn.module.tds.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.tds.dto.TdsPeriodDto;
import com.taxoryn.module.tds.dto.TdsWorkspaceSummaryDto;
import com.taxoryn.module.tds.service.TdsWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/tds", "/api/tds"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.TDS_COMPLIANCE)
@Tag(name = "TDS Compliance Workspace", description = "Consolidated TDS compliance workspace view, active obligations, workflows, and period aggregation for clients")
@SecurityRequirement(name = "BearerAuth")
public class TdsWorkspaceController {

    private final TdsWorkspaceService tdsWorkspaceService;

    @GetMapping("/clients/{clientId}/workspace")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client TDS workspace", description = "Retrieves unified TDS compliance aggregation including profile, active obligations, workflows, tasks, documents, and time entries.")
    public ResponseEntity<ApiResponse<TdsWorkspaceSummaryDto>> getClientTdsWorkspace(@PathVariable UUID clientId) {
        TdsWorkspaceSummaryDto summary = tdsWorkspaceService.getClientTdsWorkspace(clientId);
        return ResponseEntity.ok(ApiResponse.success("TDS workspace retrieved successfully", summary));
    }

    @GetMapping("/clients/{clientId}/obligations")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client TDS obligations", description = "Retrieves active statutory TDS compliance obligations for a client.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getClientTdsObligations(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String financialYear,
            @RequestParam(required = false) String quarter) {
        List<ComplianceObligationDto> list = tdsWorkspaceService.getClientTdsObligations(clientId, financialYear, quarter);
        return ResponseEntity.ok(ApiResponse.success("Client TDS obligations retrieved successfully", list));
    }

    @GetMapping("/clients/{clientId}/workflows")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client TDS workflows", description = "Retrieves operational TDS return workflows and execution status for a client.")
    public ResponseEntity<ApiResponse<List<ComplianceWorkflowDto>>> getClientTdsWorkflows(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String financialYear,
            @RequestParam(required = false) String quarter) {
        List<ComplianceWorkflowDto> list = tdsWorkspaceService.getClientTdsWorkflows(clientId, financialYear, quarter);
        return ResponseEntity.ok(ApiResponse.success("Client TDS workflows retrieved successfully", list));
    }

    @GetMapping("/clients/{clientId}/periods")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client TDS periods", description = "Retrieves distinct TDS quarterly filing periods and statutory metadata for client TDS filings.")
    public ResponseEntity<ApiResponse<List<TdsPeriodDto>>> getPeriods(@PathVariable UUID clientId) {
        List<TdsPeriodDto> list = tdsWorkspaceService.getPeriods(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client TDS periods retrieved successfully", list));
    }
}
