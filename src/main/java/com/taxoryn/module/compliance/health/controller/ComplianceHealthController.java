package com.taxoryn.module.compliance.health.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.health.dto.ClientComplianceHealthDto;
import com.taxoryn.module.compliance.health.dto.PracticeComplianceHealthSummaryDto;
import com.taxoryn.module.compliance.health.service.ComplianceHealthService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST Controller for Compliance Health & Intelligence (Phase 29.8).
 * Read-only, explainable, deterministic intelligence layer over compliance state.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Compliance Health & Intelligence", description = "Phase 29.8: Explainable compliance health scoring, risk radar signals, and actionable recommendations")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceHealthController {

    private final ComplianceHealthService healthService;

    @GetMapping("/clients/{clientId}/compliance/health")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Get client compliance health and intelligence", description = "Returns deterministic compliance health score (0-100), explainable status, identified risk signals, and next best recommendations.")
    public ResponseEntity<ApiResponse<ClientComplianceHealthDto>> getClientHealth(
            @PathVariable UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        ClientComplianceHealthDto health = healthService.getClientComplianceHealth(clientId, referenceDate);
        return ResponseEntity.ok(ApiResponse.success("Client compliance health evaluated successfully", health));
    }

    @GetMapping("/compliance/health")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Get practice-wide compliance health overview", description = "Returns aggregated tenant compliance health metrics, counts of at-risk clients, and top priorities.")
    public ResponseEntity<ApiResponse<PracticeComplianceHealthSummaryDto>> getPracticeHealth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        PracticeComplianceHealthSummaryDto summary = healthService.getPracticeComplianceHealth(referenceDate);
        return ResponseEntity.ok(ApiResponse.success("Practice compliance health summary evaluated successfully", summary));
    }
}
