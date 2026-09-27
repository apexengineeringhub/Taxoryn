package com.taxoryn.module.gst.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.gst.dto.GstWorkspaceSummaryDto;
import com.taxoryn.module.gst.service.GstWorkspaceService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/gst", "/api/gst"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.GST)
@Tag(name = "GST Compliance Workspace", description = "Consolidated GST compliance workspace view, active obligations, and workflow aggregation for clients")
@SecurityRequirement(name = "BearerAuth")
public class GstWorkspaceController {

    private final GstWorkspaceService gstWorkspaceService;

    @GetMapping("/clients/{clientId}/workspace")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client GST workspace", description = "Retrieves unified GST compliance aggregation including registrations, active obligations, workflows, tasks, documents, and time entries.")
    public ResponseEntity<ApiResponse<GstWorkspaceSummaryDto>> getClientGstWorkspace(@PathVariable UUID clientId) {
        GstWorkspaceSummaryDto summary = gstWorkspaceService.getClientGstWorkspace(clientId);
        return ResponseEntity.ok(ApiResponse.success("GST workspace retrieved successfully", summary));
    }

    @GetMapping("/clients/{clientId}/obligations")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client GST obligations", description = "Retrieves active statutory GST compliance obligations (GSTR-1, GSTR-3B, GSTR-9, GSTR-9C) for a client.")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> getClientGstObligations(@PathVariable UUID clientId) {
        List<ComplianceObligationDto> list = gstWorkspaceService.getClientGstObligations(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client GST obligations retrieved successfully", list));
    }

    @GetMapping("/clients/{clientId}/workflows")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client GST workflows", description = "Retrieves operational GST return workflows and execution status for a client.")
    public ResponseEntity<ApiResponse<List<ComplianceWorkflowDto>>> getClientGstWorkflows(@PathVariable UUID clientId) {
        List<ComplianceWorkflowDto> list = gstWorkspaceService.getClientGstWorkflows(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client GST workflows retrieved successfully", list));
    }
}
