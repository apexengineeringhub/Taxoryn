package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.service.TaxServiceMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/tax-services", "/api/admin/tax-services"})
@RequiredArgsConstructor
@Tag(name = "Tax Service Master Administration", description = "Platform Admin governance for controlled Indian tax service catalog, categories, and search aliases")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TaxServiceAdminController {

    private final TaxServiceMasterService masterService;

    // =========================================================================
    // Category Endpoints
    // =========================================================================

    @GetMapping("/categories")
    @Operation(summary = "List All Tax Service Categories", description = "Retrieves all tax service categories ordered by sort order.")
    public ResponseEntity<ApiResponse<List<TaxServiceCategoryDto>>> getAllCategories() {
        List<TaxServiceCategoryDto> categories = masterService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get Category by ID", description = "Retrieves details of a specific tax service category.")
    public ResponseEntity<ApiResponse<TaxServiceCategoryDto>> getCategoryById(@PathVariable UUID id) {
        TaxServiceCategoryDto category = masterService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create Tax Service Category", description = "Creates a new master category (e.g. INCOME_TAX, GST).")
    public ResponseEntity<ApiResponse<TaxServiceCategoryDto>> createCategory(@Valid @RequestBody CreateTaxServiceCategoryRequest request) {
        TaxServiceCategoryDto created = masterService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Category created successfully", created));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update Tax Service Category", description = "Updates category name, description, icon, or sort order.")
    public ResponseEntity<ApiResponse<TaxServiceCategoryDto>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaxServiceCategoryRequest request
    ) {
        TaxServiceCategoryDto updated = masterService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @PatchMapping("/categories/{id}/status")
    @Operation(summary = "Toggle Category Active Status", description = "Activates or deactivates a category (soft status toggle).")
    public ResponseEntity<ApiResponse<TaxServiceCategoryDto>> toggleCategoryStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive
    ) {
        TaxServiceCategoryDto updated = masterService.toggleCategoryStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Category status updated successfully", updated));
    }

    // =========================================================================
    // Service Master Endpoints
    // =========================================================================

    @GetMapping("")
    @Operation(summary = "List Master Tax Services (Paginated)", description = "Queries master tax services with optional category, active status, and search filters.")
    public ResponseEntity<ApiResponse<PagedResponse<TaxServiceDto>>> getTaxServices(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.ASC, "sortOrder", "code"));
        PagedResponse<TaxServiceDto> response = masterService.getTaxServices(categoryId, isActive, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tax services retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Master Tax Service by ID", description = "Retrieves master service details and associated aliases.")
    public ResponseEntity<ApiResponse<TaxServiceDto>> getTaxServiceById(@PathVariable UUID id) {
        TaxServiceDto service = masterService.getTaxServiceById(id);
        return ResponseEntity.ok(ApiResponse.success("Tax service retrieved successfully", service));
    }

    @PostMapping("")
    @Operation(summary = "Create Master Tax Service", description = "Creates a new controlled service entry with unique immutable code.")
    public ResponseEntity<ApiResponse<TaxServiceDto>> createTaxService(@Valid @RequestBody CreateTaxServiceRequest request) {
        TaxServiceDto created = masterService.createTaxService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tax service created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Master Tax Service", description = "Updates title, description, sort order, and category. Service code remains immutable.")
    public ResponseEntity<ApiResponse<TaxServiceDto>> updateTaxService(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaxServiceRequest request
    ) {
        TaxServiceDto updated = masterService.updateTaxService(id, request);
        return ResponseEntity.ok(ApiResponse.success("Tax service updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle Master Tax Service Active Status", description = "Soft activates or deactivates a master tax service.")
    public ResponseEntity<ApiResponse<TaxServiceDto>> toggleTaxServiceStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive
    ) {
        TaxServiceDto updated = masterService.toggleTaxServiceStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Tax service status updated successfully", updated));
    }

    // =========================================================================
    // Alias Endpoints
    // =========================================================================

    @GetMapping("/{id}/aliases")
    @Operation(summary = "List Aliases for Service", description = "Retrieves all search synonyms and abbreviations for a specific service.")
    public ResponseEntity<ApiResponse<List<TaxServiceAliasDto>>> getAliasesForService(@PathVariable UUID id) {
        List<TaxServiceAliasDto> aliases = masterService.getAliasesForService(id);
        return ResponseEntity.ok(ApiResponse.success("Aliases retrieved successfully", aliases));
    }

    @PostMapping("/{id}/aliases")
    @Operation(summary = "Add Search Alias to Service", description = "Registers a new search synonym (e.g. 'ITR Filing') for a service.")
    public ResponseEntity<ApiResponse<TaxServiceAliasDto>> addAlias(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTaxServiceAliasRequest request
    ) {
        TaxServiceAliasDto created = masterService.addAlias(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Alias added successfully", created));
    }

    @DeleteMapping("/{id}/aliases/{aliasId}")
    @Operation(summary = "Delete Search Alias", description = "Deletes a search alias from a service.")
    public ResponseEntity<ApiResponse<Void>> deleteAlias(
            @PathVariable UUID id,
            @PathVariable UUID aliasId
    ) {
        masterService.deleteAlias(id, aliasId);
        return ResponseEntity.ok(ApiResponse.success("Alias deleted successfully", null));
    }
}
