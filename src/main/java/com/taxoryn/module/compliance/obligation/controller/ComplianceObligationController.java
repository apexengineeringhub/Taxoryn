package com.taxoryn.module.compliance.obligation.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.obligation.dto.CancelObligationRequest;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationFilterParams;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationSummaryDto;
import com.taxoryn.module.compliance.obligation.dto.GenerateObligationsRequest;
import com.taxoryn.module.compliance.obligation.dto.GeneratedObligationsResponseDto;
import com.taxoryn.module.compliance.obligation.service.ComplianceObligationService;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto;
import com.taxoryn.module.compliance.work.service.ComplianceWorkOrchestrationService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/compliance/obligations")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Compliance Obligation Engine", description = "Deterministic statutory compliance obligation generation and lifecycle")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceObligationController {

    private final ComplianceObligationService obligationService;
    private final ComplianceWorkOrchestrationService workOrchestrationService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "List compliance obligations for a client with optional filters")
    public ResponseEntity<ApiResponse<List<ComplianceObligationDto>>> listObligations(
            @PathVariable UUID clientId,
            @RequestParam(required = false) ComplianceObligationStatus status,
            @RequestParam(required = false) ComplianceRuleDomain domain,
            @RequestParam(required = false) CompliancePeriodType periodType,
            @RequestParam(required = false) String periodKey,
            @RequestParam(required = false) String ruleCode
    ) {
        ComplianceObligationFilterParams params = ComplianceObligationFilterParams.builder()
                .status(status)
                .domain(domain)
                .periodType(periodType)
                .periodKey(periodKey)
                .ruleCode(ruleCode)
                .build();

        List<ComplianceObligationDto> obligations = obligationService.listClientObligations(clientId, params);
        return ResponseEntity.ok(ApiResponse.success("Retrieved " + obligations.size() + " compliance obligations", obligations));
    }

    @GetMapping("/{obligationId}")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Get single compliance obligation by ID")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> getObligation(
            @PathVariable UUID clientId,
            @PathVariable UUID obligationId
    ) {
        ComplianceObligationDto dto = obligationService.getObligationById(clientId, obligationId);
        return ResponseEntity.ok(ApiResponse.success("Retrieved compliance obligation", dto));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'CLIENT_WRITE', 'COMPLIANCE_WRITE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Generate or retrieve compliance obligations for applicable rules in a period")
    public ResponseEntity<ApiResponse<GeneratedObligationsResponseDto>> generateObligations(
            @PathVariable UUID clientId,
            @Valid @RequestBody GenerateObligationsRequest request
    ) {
        GeneratedObligationsResponseDto result = obligationService.generateObligations(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Processed " + result.getTotalCount() + " obligations (" + result.getCreatedCount() + " created, " + result.getExistingCount() + " existing)",
                result
        ));
    }

    @PutMapping("/{obligationId}/status")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'CLIENT_WRITE', 'COMPLIANCE_WRITE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Update compliance obligation lifecycle status")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> updateStatus(
            @PathVariable UUID clientId,
            @PathVariable UUID obligationId,
            @Valid @RequestBody UpdateObligationStatusRequest request
    ) {
        ComplianceObligationDto dto = obligationService.updateObligationStatus(clientId, obligationId, request);
        return ResponseEntity.ok(ApiResponse.success("Updated compliance obligation status to " + dto.getStatus(), dto));
    }

    @PostMapping("/{obligationId}/cancel")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'CLIENT_WRITE', 'COMPLIANCE_WRITE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Cancel compliance obligation with audit reason")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> cancelObligation(
            @PathVariable UUID clientId,
            @PathVariable UUID obligationId,
            @Valid @RequestBody CancelObligationRequest request
    ) {
        ComplianceObligationDto dto = obligationService.cancelObligation(clientId, obligationId, request);
        return ResponseEntity.ok(ApiResponse.success("Cancelled compliance obligation", dto));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Get high-level compliance obligation summary metrics for a client")
    public ResponseEntity<ApiResponse<ComplianceObligationSummaryDto>> getObligationSummary(
            @PathVariable UUID clientId
    ) {
        ComplianceObligationSummaryDto summary = obligationService.getClientObligationSummary(clientId);
        return ResponseEntity.ok(ApiResponse.success("Retrieved compliance obligation summary", summary));
    }

    // -------------------------------------------------------------------------
    // Phase 29.7 — Work Generation endpoint
    // -------------------------------------------------------------------------

    @PostMapping("/{obligationId}/work/generate")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'CLIENT_WRITE', 'COMPLIANCE_WRITE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_PRACTICE_ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(
            summary = "Generate work instance for a compliance obligation",
            description = "Idempotently creates a Work Instance (with tasks) linked to the given compliance obligation. " +
                    "Returns CREATED on first call, ALREADY_EXISTS on subsequent calls. " +
                    "Returns TEMPLATE_NOT_CONFIGURED if the rule has no work template code, " +
                    "or ENGAGEMENT_NOT_CONFIGURED if no active engagement is found for the client."
    )
    public ResponseEntity<ApiResponse<ComplianceWorkGenerationResultDto>> generateWorkForObligation(
            @PathVariable UUID clientId,
            @PathVariable UUID obligationId
    ) {
        ComplianceWorkGenerationResultDto result = workOrchestrationService.generateWorkForObligation(clientId, obligationId);
        HttpStatus httpStatus = result.getStatus() == ComplianceWorkGenerationResultDto.GenerationStatus.CREATED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.success(result.getMessage(), result));
    }
}
