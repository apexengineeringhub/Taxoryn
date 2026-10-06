package com.taxoryn.module.compliance.profile.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.profile.dto.ComplianceProfileDto;
import com.taxoryn.module.compliance.profile.dto.UpdateComplianceProfileRequest;
import com.taxoryn.module.compliance.profile.service.ComplianceProfileService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/compliance-profile")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Client Compliance Profile", description = "Client statutory and compliance profile facts configuration (Phase 29.1)")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceProfileController {

    private final ComplianceProfileService complianceProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('COMPLIANCE_VIEW') or hasAuthority('COMPLIANCE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client compliance profile", description = "Retrieves authoritative compliance facts (GST, TDS, ITR, statutory flags) and readiness score for a client.")
    public ResponseEntity<ApiResponse<ComplianceProfileDto>> getComplianceProfile(@PathVariable UUID clientId) {
        ComplianceProfileDto profile = complianceProfileService.getComplianceProfile(clientId);
        return ResponseEntity.ok(ApiResponse.success("Compliance profile retrieved successfully", profile));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('COMPLIANCE_WRITE') or hasAuthority('COMPLIANCE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client compliance profile", description = "Configures or updates compliance facts for a client within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ComplianceProfileDto>> updateComplianceProfile(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateComplianceProfileRequest request) {
        ComplianceProfileDto updated = complianceProfileService.updateComplianceProfile(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance profile updated successfully", updated));
    }
}
