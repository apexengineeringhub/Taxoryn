package com.taxoryn.module.organization.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.organization.dto.CreateOrganizationRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.dto.OrganizationSettingsDto;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationSettingsRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationStatusRequest;
import com.taxoryn.module.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization & Tenant Management", description = "Endpoints for managing tenants, organization profiles, settings, and lifecycle statuses")
@SecurityRequirement(name = "BearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new organization", description = "Creates and provisions a new tenant organization with default settings (Super Admin only).")
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationDto created = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Organization created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all organizations (Super Admin)", description = "Retrieves paginated list of all tenant organizations on the platform.")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizationDto>>> getOrganizations(@Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<OrganizationDto> response = organizationService.getOrganizations(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Organizations retrieved successfully", response));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get current organization profile", description = "Retrieves profile of the authenticated tenant organization derived from JWT.")
    public ResponseEntity<ApiResponse<OrganizationDto>> getCurrentOrganization() {
        OrganizationDto dto = organizationService.getCurrentOrganization();
        return ResponseEntity.ok(ApiResponse.success("Organization profile retrieved successfully", dto));
    }

    @GetMapping("/{organizationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get organization by ID", description = "Retrieves organization profile with strict tenant isolation verification.")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganizationById(@PathVariable UUID organizationId) {
        OrganizationDto dto = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Organization profile retrieved successfully", dto));
    }

    @PutMapping("/current")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update current organization profile", description = "Updates details of the authenticated tenant.")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateCurrentOrganization(@Valid @RequestBody UpdateOrganizationRequest request) {
        OrganizationDto updated = organizationService.updateCurrentOrganization(request);
        return ResponseEntity.ok(ApiResponse.success("Organization profile updated successfully", updated));
    }

    @PutMapping("/{organizationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update organization by ID", description = "Updates details of specified organization with strict tenant isolation verification.")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(@PathVariable UUID organizationId, @Valid @RequestBody UpdateOrganizationRequest request) {
        OrganizationDto updated = organizationService.updateOrganization(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success("Organization profile updated successfully", updated));
    }

    @PatchMapping("/{organizationId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ORG_ADMIN') and (hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE')))")
    @Operation(summary = "Activate, deactivate, or suspend organization", description = "Updates tenant operational lifecycle status.")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganizationStatus(@PathVariable UUID organizationId, @Valid @RequestBody UpdateOrganizationStatusRequest request) {
        OrganizationDto updated = organizationService.updateOrganizationStatus(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success("Organization status updated successfully to " + updated.getStatus(), updated));
    }

    @GetMapping("/current/settings")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get current organization settings", description = "Retrieves configuration settings (timezone, currency, date format, notifications) for the active tenant.")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> getCurrentOrganizationSettings() {
        OrganizationSettingsDto settings = organizationService.getCurrentOrganizationSettings();
        return ResponseEntity.ok(ApiResponse.success("Organization settings retrieved successfully", settings));
    }

    @GetMapping("/{organizationId}/settings")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get organization settings by ID", description = "Retrieves configuration settings for the specified organization with tenant isolation.")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> getOrganizationSettings(@PathVariable UUID organizationId) {
        OrganizationSettingsDto settings = organizationService.getOrganizationSettings(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Organization settings retrieved successfully", settings));
    }

    @PutMapping("/current/settings")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update current organization settings", description = "Updates configuration settings for the active tenant.")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> updateCurrentOrganizationSettings(@Valid @RequestBody UpdateOrganizationSettingsRequest request) {
        OrganizationSettingsDto updated = organizationService.updateCurrentOrganizationSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Organization settings updated successfully", updated));
    }

    @PutMapping("/{organizationId}/settings")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update organization settings by ID", description = "Updates configuration settings for the specified organization with tenant isolation.")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> updateOrganizationSettings(@PathVariable UUID organizationId, @Valid @RequestBody UpdateOrganizationSettingsRequest request) {
        OrganizationSettingsDto updated = organizationService.updateOrganizationSettings(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success("Organization settings updated successfully", updated));
    }
}
