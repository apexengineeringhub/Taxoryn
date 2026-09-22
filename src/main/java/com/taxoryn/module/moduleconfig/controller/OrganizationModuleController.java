package com.taxoryn.module.moduleconfig.controller;

import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.dto.ProductModuleDto;
import com.taxoryn.module.moduleconfig.dto.UpdateOrganizationModuleRequest;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/organizations/modules", "/api/v1/organizations/current/modules", "/api/v1/modules"})
@RequiredArgsConstructor
@Tag(name = "Organization Module Configuration", description = "Endpoints for inspecting and configuring organization product modules")
@SecurityRequirement(name = "BearerAuth")
public class OrganizationModuleController {

    private final ModuleConfigurationService moduleConfigurationService;

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get Organization Modules", description = "Retrieves all product modules and their active enabled/disabled status for the authenticated tenant.")
    public ResponseEntity<ApiResponse<List<OrganizationModuleDto>>> getOrganizationModules() {
        UUID organizationId = resolveCurrentOrganizationId();
        List<OrganizationModuleDto> modules = moduleConfigurationService.getOrganizationModules(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Organization modules retrieved successfully", modules));
    }

    @GetMapping("/{moduleCode}")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get Single Module Configuration", description = "Retrieves configuration status of a specific product module for the authenticated tenant.")
    public ResponseEntity<ApiResponse<OrganizationModuleDto>> getOrganizationModule(
            @PathVariable ProductModuleCode moduleCode) {
        UUID organizationId = resolveCurrentOrganizationId();
        OrganizationModuleDto module = moduleConfigurationService.getOrganizationModule(organizationId, moduleCode);
        return ResponseEntity.ok(ApiResponse.success("Organization module retrieved successfully", module));
    }

    @PutMapping("/{moduleCode}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Update Module Status", description = "Enables or disables a product module for the authenticated tenant.")
    public ResponseEntity<ApiResponse<OrganizationModuleDto>> updateModuleStatus(
            @PathVariable ProductModuleCode moduleCode,
            @Valid @RequestBody UpdateOrganizationModuleRequest request) {
        UUID organizationId = resolveCurrentOrganizationId();
        OrganizationModuleDto updated = moduleConfigurationService.updateModuleStatus(
                organizationId, moduleCode, request.getEnabled());
        return ResponseEntity.ok(ApiResponse.success(
                "Module " + moduleCode + " updated to " + (request.getEnabled() ? "ENABLED" : "DISABLED"),
                updated));
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get Global Product Module Catalog", description = "Retrieves the master catalog of all product modules available on the platform.")
    public ResponseEntity<ApiResponse<List<ProductModuleDto>>> getProductModuleCatalog() {
        List<ProductModuleDto> catalog = moduleConfigurationService.getAllProductModules();
        return ResponseEntity.ok(ApiResponse.success("Product module catalog retrieved successfully", catalog));
    }

    private UUID resolveCurrentOrganizationId() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedException("Authenticated tenant context is required");
        }
        return orgId;
    }
}
