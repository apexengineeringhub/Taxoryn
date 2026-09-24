package com.taxoryn.module.notice.controller;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.notice.dto.TaxNoticeConfigDto;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeConfigRequest;
import com.taxoryn.module.notice.service.TaxNoticeConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/organizations/tax-notice-config", "/api/v1/tax-notices/config"})
@RequiredArgsConstructor
@Tag(name = "Tax Notice Configuration", description = "Endpoints for retrieving and customizing tenant tax notice operations settings")
@SecurityRequirement(name = "BearerAuth")
public class TaxNoticeConfigController {

    private final TaxNoticeConfigurationService configurationService;
    private final ModuleConfigurationService moduleConfigurationService;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasAuthority('TAX_NOTICE_VIEW') or hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get Effective Tax Notice Configuration", description = "Retrieves the effective tax notice operations configuration for the authenticated organization.")
    public ResponseEntity<ApiResponse<TaxNoticeConfigDto>> getConfiguration() {
        UUID organizationId = resolveCurrentOrganizationId();
        verifyModuleEnabled(organizationId);

        TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Tax notice configuration retrieved successfully", config));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('TAX_NOTICE:CONFIGURE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Update Tax Notice Configuration", description = "Updates and persists custom tax notice operations configuration for the authenticated organization.")
    public ResponseEntity<ApiResponse<TaxNoticeConfigDto>> updateConfiguration(
            @Valid @RequestBody UpdateTaxNoticeConfigRequest request) {
        UUID organizationId = resolveCurrentOrganizationId();
        verifyModuleEnabled(organizationId);

        TaxNoticeConfigDto updated = configurationService.updateConfiguration(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success("Tax notice configuration updated successfully", updated));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('TAX_NOTICE:CONFIGURE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Reset Tax Notice Configuration", description = "Deletes custom organization overrides and reverts to persona defaults.")
    public ResponseEntity<ApiResponse<TaxNoticeConfigDto>> resetToPersonaDefaults() {
        UUID organizationId = resolveCurrentOrganizationId();
        verifyModuleEnabled(organizationId);

        TaxNoticeConfigDto reset = configurationService.resetToPersonaDefaults(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Tax notice configuration reset to persona defaults", reset));
    }

    private UUID resolveCurrentOrganizationId() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        return organizationId;
    }

    private void verifyModuleEnabled(UUID organizationId) {
        if (!moduleConfigurationService.isModuleEnabled(organizationId, ProductModuleCode.TAX_NOTICES)) {
            throw new ForbiddenException("Product module TAX_NOTICES is disabled for this organization");
        }
    }
}
