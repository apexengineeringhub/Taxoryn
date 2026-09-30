package com.taxoryn.module.worktemplate.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.worktemplate.dto.UpdateWorkInstanceStatusRequest;
import com.taxoryn.module.worktemplate.dto.WorkInstanceDto;
import com.taxoryn.module.worktemplate.service.EngagementWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/work-instances", "/api/work-instances"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Work Instance Management", description = "Endpoints for viewing and updating generated practice work occurrences")
@SecurityRequirement(name = "BearerAuth")
public class WorkInstanceController {

    private final EngagementWorkService engagementWorkService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get work instance by ID", description = "Retrieves work instance details and all instantiated task items.")
    public ResponseEntity<ApiResponse<WorkInstanceDto>> getWorkInstanceById(@PathVariable UUID id) {
        WorkInstanceDto dto = engagementWorkService.getWorkInstanceById(id);
        return ResponseEntity.ok(ApiResponse.success("Work instance retrieved successfully", dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('TASK_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Update work instance status", description = "Updates lifecycle status of a generated work instance.")
    public ResponseEntity<ApiResponse<WorkInstanceDto>> updateWorkInstanceStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkInstanceStatusRequest request) {
        WorkInstanceDto updated = engagementWorkService.updateWorkInstanceStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work instance status updated successfully", updated));
    }
}
