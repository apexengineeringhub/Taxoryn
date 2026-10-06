package com.taxoryn.module.compliance.applicability.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.applicability.dto.ClientComplianceApplicabilityDto;
import com.taxoryn.module.compliance.applicability.dto.ComplianceApplicabilitySummaryDto;
import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.applicability.service.ComplianceApplicabilityService;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/api/v1/clients/{clientId}/compliance/applicability")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Compliance Applicability Engine", description = "Deterministic statutory rule applicability evaluation for clients")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceApplicabilityController {

    private final ComplianceApplicabilityService applicabilityService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Evaluate client compliance rule applicability", description = "Dynamically evaluates statutory and practice compliance rules against the client facts.")
    public ResponseEntity<ApiResponse<ClientComplianceApplicabilityDto>> evaluateClientApplicability(
            @PathVariable UUID clientId,
            @Parameter(description = "Evaluation date (defaults to current date)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate evaluationDate,
            @Parameter(description = "Optional filter by regulatory domain")
            @RequestParam(required = false) ComplianceRuleDomain domain,
            @Parameter(description = "Optional filter by applicability result status (APPLICABLE, NOT_APPLICABLE, INSUFFICIENT_DATA)")
            @RequestParam(required = false) ApplicabilityResultState status) {

        ClientComplianceApplicabilityDto report = applicabilityService.evaluateClientApplicability(
                clientId,
                evaluationDate,
                domain,
                status
        );

        return ResponseEntity.ok(ApiResponse.success("Client compliance applicability evaluated successfully", report));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get client compliance applicability summary", description = "Retrieves compact summary metrics for Client 360 and dashboard widgets.")
    public ResponseEntity<ApiResponse<ComplianceApplicabilitySummaryDto>> getApplicabilitySummary(
            @PathVariable UUID clientId,
            @Parameter(description = "Evaluation date (defaults to current date)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate evaluationDate) {

        ComplianceApplicabilitySummaryDto summary = applicabilityService.getApplicabilitySummary(
                clientId,
                evaluationDate
        );

        return ResponseEntity.ok(ApiResponse.success("Client compliance applicability summary retrieved successfully", summary));
    }

    @GetMapping("/{ruleCode}")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Evaluate single rule applicability", description = "Evaluates whether a specific compliance rule applies to the client.")
    public ResponseEntity<ApiResponse<EvaluatedRuleApplicabilityDto>> evaluateClientRule(
            @PathVariable UUID clientId,
            @PathVariable String ruleCode,
            @Parameter(description = "Evaluation date (defaults to current date)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate evaluationDate) {

        EvaluatedRuleApplicabilityDto result = applicabilityService.evaluateClientRule(
                clientId,
                ruleCode,
                evaluationDate
        );

        return ResponseEntity.ok(ApiResponse.success("Rule applicability evaluated successfully", result));
    }
}
