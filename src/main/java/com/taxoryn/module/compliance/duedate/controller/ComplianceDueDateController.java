package com.taxoryn.module.compliance.duedate.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.duedate.dto.DueDateCalculationResult;
import com.taxoryn.module.compliance.duedate.dto.ObligationDueDateDto;
import com.taxoryn.module.compliance.duedate.service.ComplianceDueDateService;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Compliance Due Date Engine", description = "Authoritative statutory due date calculation and validation APIs")
public class ComplianceDueDateController {

    private final ComplianceDueDateService dueDateService;

    @GetMapping("/clients/{clientId}/compliance/obligations/{obligationId}/due-date")
    @RequiresModule(ProductModuleCode.CLIENTS)
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_PLATFORM_ADMIN')")
    @Operation(summary = "Get obligation statutory due-date details and explanation")
    public ResponseEntity<ApiResponse<ObligationDueDateDto>> getObligationDueDate(
            @PathVariable UUID clientId,
            @PathVariable UUID obligationId) {
        ObligationDueDateDto dto = dueDateService.getObligationDueDate(clientId, obligationId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/clients/{clientId}/compliance/obligations/{obligationId}/due-date/recalculate")
    @RequiresModule(ProductModuleCode.CLIENTS)
    @PreAuthorize("hasAnyAuthority('CLIENT_EDIT', 'COMPLIANCE_MANAGE', 'ROLE_ORG_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_PLATFORM_ADMIN')")
    @Operation(summary = "Explicitly recalculate and update authoritative statutory due date for an obligation")
    public ResponseEntity<ApiResponse<ComplianceObligationDto>> recalculateObligationDueDate(
            @PathVariable UUID clientId,
            @PathVariable UUID obligationId) {
        ComplianceObligationDto dto = dueDateService.recalculateObligationDueDate(clientId, obligationId);
        return ResponseEntity.ok(ApiResponse.success("Statutory due date recalculated successfully", dto));
    }

    @GetMapping("/compliance/due-dates/preview")
    @RequiresModule(ProductModuleCode.CLIENTS)
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'COMPLIANCE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_PRACTICE_STAFF', 'ROLE_PLATFORM_ADMIN')")
    @Operation(summary = "Preview statutory due date calculation without persistence")
    public ResponseEntity<ApiResponse<DueDateCalculationResult>> previewDueDate(
            @RequestParam String ruleCode,
            @RequestParam CompliancePeriodType periodType,
            @RequestParam String periodKey) {
        DueDateCalculationResult result = dueDateService.previewDueDate(ruleCode, periodType, periodKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
