package com.taxoryn.module.moduleconfig.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.moduleconfig.dto.OrganizationFeatureDto;
import com.taxoryn.module.moduleconfig.dto.ProductFeatureDto;
import com.taxoryn.module.moduleconfig.dto.UpdateOrganizationFeatureRequest;
import com.taxoryn.module.moduleconfig.service.FeatureConfigurationService;
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
@RequestMapping("/api/v1/organization-features")
@RequiredArgsConstructor
@Tag(name = "Organization Feature Configuration", description = "Endpoints for configuring and querying product feature toggles per organization tenant")
@SecurityRequirement(name = "BearerAuth")
public class FeatureConfigurationController {

    private final FeatureConfigurationService featureConfigurationService;

    @GetMapping
    @PreAuthorize("hasAuthority('MODULE_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all feature configurations for active tenant", description = "Retrieves configuration status of all product features for the authenticated organization.")
    public ResponseEntity<ApiResponse<List<OrganizationFeatureDto>>> getCurrentTenantFeatures() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        List<OrganizationFeatureDto> features = featureConfigurationService.getAllOrganizationFeatures(orgId);
        return ResponseEntity.ok(ApiResponse.success("Organization features retrieved successfully", features));
    }

    @GetMapping("/module/{moduleCode}")
    @PreAuthorize("hasAuthority('MODULE_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get features for a specific module", description = "Retrieves configuration status of features belonging to specified module for the authenticated organization.")
    public ResponseEntity<ApiResponse<List<OrganizationFeatureDto>>> getFeaturesByModule(@PathVariable String moduleCode) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        List<OrganizationFeatureDto> features = featureConfigurationService.getOrganizationFeatures(orgId, moduleCode);
        return ResponseEntity.ok(ApiResponse.success("Module features retrieved successfully", features));
    }

    @PutMapping("/module/{moduleCode}/feature/{featureCode}")
    @PreAuthorize("hasAuthority('MODULE_MANAGE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Enable or disable a feature", description = "Updates the enabled/disabled status of a specific feature for the active tenant.")
    public ResponseEntity<ApiResponse<OrganizationFeatureDto>> updateFeatureStatus(
            @PathVariable String moduleCode,
            @PathVariable String featureCode,
            @Valid @RequestBody UpdateOrganizationFeatureRequest request) {

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        OrganizationFeatureDto updated = featureConfigurationService.updateFeatureStatus(
                orgId, moduleCode, featureCode, Boolean.TRUE.equals(request.getEnabled()));
        return ResponseEntity.ok(ApiResponse.success("Feature configuration updated successfully", updated));
    }

    @GetMapping("/catalog/{moduleCode}")
    @Operation(summary = "Get catalog features", description = "Retrieves master list of product features available for a module.")
    public ResponseEntity<ApiResponse<List<ProductFeatureDto>>> getCatalogFeatures(@PathVariable String moduleCode) {
        List<ProductFeatureDto> catalog = featureConfigurationService.getCatalogFeatures(moduleCode);
        return ResponseEntity.ok(ApiResponse.success("Catalog features retrieved successfully", catalog));
    }
}
