package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.TaxRequirementStatus;
import com.taxoryn.module.marketplace.service.CustomerTaxRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/customer/tax-requirements", "/api/v1/marketplace/customer/tax-requirements", "/api/marketplace/customer/tax-requirements"})
@RequiredArgsConstructor
@Tag(name = "Customer Tax Requirements", description = "Customer Tax Need & Requirement Capture for Marketplace Discovery")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
public class CustomerTaxRequirementController {

    private final CustomerTaxRequirementService requirementService;

    @PostMapping
    @Operation(summary = "Create Tax Requirement (Draft)", description = "Creates a new structured customer tax requirement in DRAFT state")
    public ResponseEntity<ApiResponse<CustomerTaxRequirementDto>> createRequirement(
            @Valid @RequestBody CreateTaxRequirementRequest request
    ) {
        CustomerTaxRequirementDto created = requirementService.createRequirement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax requirement created in draft state", created));
    }

    @GetMapping
    @Operation(summary = "List My Tax Requirements", description = "Retrieves paginated list of tax requirements created by authenticated customer")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerTaxRequirementSummaryDto>>> getMyRequirements(
            @RequestParam(required = false) TaxRequirementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<CustomerTaxRequirementSummaryDto> response = requirementService.getMyRequirements(pageRequest, status);
        return ResponseEntity.ok(ApiResponse.success("Customer tax requirements retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Tax Requirement Details", description = "Retrieves complete details of a single tax requirement owned by authenticated customer")
    public ResponseEntity<ApiResponse<CustomerTaxRequirementDto>> getRequirementById(
            @PathVariable UUID id
    ) {
        CustomerTaxRequirementDto requirement = requirementService.getRequirementById(id);
        return ResponseEntity.ok(ApiResponse.success("Tax requirement retrieved successfully", requirement));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Draft Tax Requirement", description = "Updates a draft tax requirement before submission")
    public ResponseEntity<ApiResponse<CustomerTaxRequirementDto>> updateRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaxRequirementRequest request
    ) {
        CustomerTaxRequirementDto updated = requirementService.updateRequirement(id, request);
        return ResponseEntity.ok(ApiResponse.success("Draft tax requirement updated successfully", updated));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit Tax Requirement", description = "Explicitly submits a draft requirement, transitioning status from DRAFT to SUBMITTED")
    public ResponseEntity<ApiResponse<CustomerTaxRequirementDto>> submitRequirement(
            @PathVariable UUID id
    ) {
        CustomerTaxRequirementDto submitted = requirementService.submitRequirement(id);
        return ResponseEntity.ok(ApiResponse.success("Your tax requirement has been submitted successfully", submitted));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel Tax Requirement", description = "Cancels a draft or submitted tax requirement")
    public ResponseEntity<ApiResponse<CustomerTaxRequirementDto>> cancelRequirement(
            @PathVariable UUID id
    ) {
        CustomerTaxRequirementDto cancelled = requirementService.cancelRequirement(id);
        return ResponseEntity.ok(ApiResponse.success("Tax requirement has been cancelled", cancelled));
    }

    @GetMapping("/financial-years")
    @Operation(summary = "Get Available Financial Years", description = "Returns active standard Indian financial years for requirement selection")
    public ResponseEntity<ApiResponse<List<FinancialYearOptionDto>>> getFinancialYears() {
        List<FinancialYearOptionDto> years = requirementService.getAvailableFinancialYears();
        return ResponseEntity.ok(ApiResponse.success("Financial years retrieved successfully", years));
    }
}
