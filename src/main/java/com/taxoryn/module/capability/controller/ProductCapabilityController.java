package com.taxoryn.module.capability.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.capability.dto.OrganizationCapabilitiesDto;
import com.taxoryn.module.capability.service.ProductCapabilityService;
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
@RequestMapping({"/api/v1/capabilities", "/api/v1/organizations/capabilities"})
@RequiredArgsConstructor
@Tag(name = "Product Capabilities & Experience", description = "Endpoints for inspecting organization capability configuration, feature discovery, and default dashboard experience")
@SecurityRequirement(name = "BearerAuth")
public class ProductCapabilityController {

    private final ProductCapabilityService capabilityService;

    @GetMapping
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('ORGANIZATION_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get Organization Capabilities", description = "Retrieves active product capabilities, recommended workflows, and default dashboard views based on the organization's configuration and subscription tier.")
    public ResponseEntity<ApiResponse<OrganizationCapabilitiesDto>> getCapabilities() {
        OrganizationCapabilitiesDto capabilities = capabilityService.getCurrentOrganizationCapabilities();
        return ResponseEntity.ok(ApiResponse.success("Organization capabilities resolved successfully", capabilities));
    }
}
