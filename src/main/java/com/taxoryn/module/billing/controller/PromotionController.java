package com.taxoryn.module.billing.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.billing.dto.CreatePromotionRequest;
import com.taxoryn.module.billing.dto.PriceResolutionResultDto;
import com.taxoryn.module.billing.dto.PromotionDto;
import com.taxoryn.module.billing.dto.UpdatePromotionRequest;
import com.taxoryn.module.billing.dto.UpdatePromotionStatusRequest;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.service.PromotionService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/billing/promotions", "/api/billing/promotions", "/api/v1/promotions", "/api/promotions"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.BILLING)
@Tag(name = "Practice Billing - Promotional Pricing", description = "Endpoints for managing practice promotional pricing rules, discount campaigns, and price resolution")
@SecurityRequirement(name = "BearerAuth")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Create a new promotion", description = "Configure a practice-controlled promotional pricing campaign or discount code")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        PromotionDto result = promotionService.createPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Promotion created successfully", result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "List practice promotions", description = "Retrieve all promotions configured for the practice organization")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getPromotions(
            @RequestParam(required = false) Boolean activeOnly) {
        List<PromotionDto> list = promotionService.getPromotions(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Get promotion details by ID", description = "Retrieve single promotional rule by its UUID")
    public ResponseEntity<ApiResponse<PromotionDto>> getPromotionById(@PathVariable UUID id) {
        PromotionDto result = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Update promotion", description = "Update configuration, validity period, targeting, or discount values")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePromotionRequest request) {
        PromotionDto result = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion updated successfully", result));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Activate or deactivate promotion", description = "Toggle active status of promotion campaign")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePromotionStatusRequest request) {
        PromotionDto result = promotionService.updatePromotionStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion status updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Delete promotion", description = "Remove promotional rule definition")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resolve-price")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Resolve service price", description = "Preview standard, customer-specific, or promotional price for a client and service")
    public ResponseEntity<ApiResponse<PriceResolutionResultDto>> resolvePrice(
            @RequestParam UUID clientId,
            @RequestParam BillingServiceType service,
            @RequestParam(required = false) BigDecimal manualPrice,
            @RequestParam(required = false) String promoCode) {
        UUID organizationId = com.taxoryn.core.security.SecurityUtils.getCurrentOrganizationId();
        PriceResolutionResultDto result = promotionService.resolvePrice(
                organizationId, clientId, service, manualPrice, promoCode, LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
