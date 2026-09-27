package com.taxoryn.module.itr.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.itr.dto.ItrWorkspaceSummaryDto;
import com.taxoryn.module.itr.service.ItrWorkspaceService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
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
@RequestMapping({"/api/v1/itr", "/api/itr"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.ITR_COMPLIANCE)
@Tag(name = "ITR Compliance Workspace", description = "Consolidated ITR compliance workspace view, active obligations, workflows, and assessment year aggregation for clients")
@SecurityRequirement(name = "BearerAuth")
public class ItrWorkspaceController {

    private final ItrWorkspaceService itrWorkspaceService;

    @GetMapping("/clients/{clientId}/workspace")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client ITR workspace", description = "Retrieves unified ITR compliance aggregation including profile, active obligations, workflows, tasks, documents, and time entries.")
    public ResponseEntity<ApiResponse<ItrWorkspaceSummaryDto>> getClientItrWorkspace(@PathVariable UUID clientId) {
        ItrWorkspaceSummaryDto summary = itrWorkspaceService.getClientItrWorkspace(clientId);
        return ResponseEntity.ok(ApiResponse.success("ITR workspace retrieved successfully", summary));
    }

    @GetMapping("/clients/{clientId}/obligations")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client ITR obligations", description = "Retrieves active statutory ITR compliance obligations for a client.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getClientItrObligations(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String assessmentYear) {
        List<ComplianceObligationDto> list = itrWorkspaceService.getClientItrObligations(clientId, assessmentYear);
        return ResponseEntity.ok(ApiResponse.success("Client ITR obligations retrieved successfully", list));
    }

    @GetMapping("/clients/{clientId}/workflows")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client ITR workflows", description = "Retrieves operational ITR return workflows and execution status for a client.")
    public ResponseEntity<ApiResponse<List<ComplianceWorkflowDto>>> getClientItrWorkflows(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String assessmentYear) {
        List<ComplianceWorkflowDto> list = itrWorkspaceService.getClientItrWorkflows(clientId, assessmentYear);
        return ResponseEntity.ok(ApiResponse.success("Client ITR workflows retrieved successfully", list));
    }

    @GetMapping("/clients/{clientId}/assessment-years")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client Assessment Years", description = "Retrieves distinct Assessment Years available for client ITR filings.")
    public ResponseEntity<ApiResponse<List<String>>> getAssessmentYears(@PathVariable UUID clientId) {
        List<String> list = itrWorkspaceService.getAssessmentYears(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client assessment years retrieved successfully", list));
    }
}
