package com.taxoryn.module.worktemplate.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.worktemplate.dto.WorkInventoryFilterRequest;
import com.taxoryn.module.worktemplate.dto.WorkInventoryItemDto;
import com.taxoryn.module.worktemplate.service.WorkInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/work/inventory", "/api/work/inventory"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Work Operating Model & Inventory", description = "Phase 30.1: Unified authoritative work inventory across work instances, obligations, and tasks")
@SecurityRequirement(name = "BearerAuth")
public class WorkInventoryController {

    private final WorkInventoryService workInventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('TASK_VIEW') or hasAuthority('COMPLIANCE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get unified work inventory", description = "Retrieves paginated and filtered work items projected across work instances, compliance obligations, and tasks without duplication.")
    public ResponseEntity<ApiResponse<PagedResponse<WorkInventoryItemDto>>> getWorkInventory(
            @ModelAttribute WorkInventoryFilterRequest filterRequest) {
        PagedResponse<WorkInventoryItemDto> response = workInventoryService.getWorkInventory(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Work inventory retrieved successfully", response));
    }
}
