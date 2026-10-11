package com.taxoryn.module.billing.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.billing.dto.TdsCreditDto;
import com.taxoryn.module.billing.dto.TdsReconciliationSummaryDto;
import com.taxoryn.module.billing.service.TdsReconciliationService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/billing/tds-reconciliation", "/api/billing/tds-reconciliation"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.BILLING)
@Tag(name = "TDS Receivables Credit Reconciliation", description = "Automated matching of practice receivable invoices with client TDS withholding credits (Form 26AS/AIS/16A)")
@SecurityRequirement(name = "BearerAuth")
public class TdsReconciliationController {

    private final TdsReconciliationService tdsReconciliationService;

    @PostMapping("/credits")
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Record inward TDS withholding credit", description = "Records a client TDS deduction entry (from Form 16A, 26AS, or client remittance advice).")
    public ResponseEntity<ApiResponse<TdsCreditDto>> recordCredit(@Valid @RequestBody TdsCreditDto request) {
        TdsCreditDto result = tdsReconciliationService.recordTdsCredit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TDS credit recorded successfully", result));
    }

    @GetMapping("/credits")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List practice inward TDS credits", description = "Retrieves all recorded inward TDS credits with filters by client or financial year.")
    public ResponseEntity<ApiResponse<List<TdsCreditDto>>> getCredits(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String financialYear) {
        List<TdsCreditDto> list = tdsReconciliationService.getTdsCredits(clientId, financialYear);
        return ResponseEntity.ok(ApiResponse.success("TDS credits retrieved successfully", list));
    }

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Run automatic TDS credit reconciliation", description = "Executes automated reconciliation between invoices and attributable TDS withholding credits.")
    public ResponseEntity<ApiResponse<TdsReconciliationSummaryDto>> runReconciliation(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String financialYear) {
        TdsReconciliationSummaryDto result = tdsReconciliationService.runReconciliation(clientId, financialYear);
        return ResponseEntity.ok(ApiResponse.success("TDS credit reconciliation executed successfully", result));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get TDS credit reconciliation summary", description = "Retrieves summary breakdown of matched, discrepancy, and unmatched invoices/credits.")
    public ResponseEntity<ApiResponse<TdsReconciliationSummaryDto>> getSummary(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String financialYear) {
        TdsReconciliationSummaryDto result = tdsReconciliationService.getReconciliationSummary(clientId, financialYear);
        return ResponseEntity.ok(ApiResponse.success("TDS reconciliation summary retrieved successfully", result));
    }
}
