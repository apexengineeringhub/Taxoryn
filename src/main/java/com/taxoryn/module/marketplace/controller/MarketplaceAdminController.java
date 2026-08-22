package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/marketplace", "/api/admin/marketplace"})
@RequiredArgsConstructor
@Tag(name = "Platform Admin Marketplace Governance", description = "Super Admin platform governance: KYC verification approvals, listing moderation, featured ranks, and platform KPIs")
@SecurityRequirement(name = "BearerAuth")
public class MarketplaceAdminController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/verifications/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Pending KYC Verifications", description = "Lists all practitioner credential verifications awaiting platform review.")
    public ResponseEntity<ApiResponse<PagedResponse<MarketplaceVerificationDto>>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        PagedResponse<MarketplaceVerificationDto> verifications = marketplaceService.getPendingVerifications(pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending verifications retrieved", verifications));
    }

    @PostMapping("/verifications/{id}/process")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve / Reject Practitioner KYC", description = "Approves or rejects a practitioner's verified badge and membership certificate.")
    public ResponseEntity<ApiResponse<MarketplaceVerificationDto>> processVerification(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyPractitionerRequest request
    ) {
        MarketplaceVerificationDto processed = marketplaceService.processVerification(id, request);
        return ResponseEntity.ok(ApiResponse.success("Verification processed successfully", processed));
    }

    @PatchMapping("/profiles/{id}/featured")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Toggle Featured Practitioner Placement", description = "Controls homepage featured showcase for high-performing practices.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> toggleFeatured(
            @PathVariable UUID id,
            @RequestParam boolean isFeatured
    ) {
        PublicMarketplaceProfileDto profile = marketplaceService.toggleFeaturedStatus(id, isFeatured);
        return ResponseEntity.ok(ApiResponse.success("Featured status updated", profile));
    }

    @PatchMapping("/profiles/{id}/publish")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Moderation Toggle for Profile Visibility", description = "Allows Super Admin to suspend or publish any practice listing.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> togglePublish(
            @PathVariable UUID id,
            @RequestParam boolean isPublished
    ) {
        PublicMarketplaceProfileDto profile = marketplaceService.togglePublishStatus(id, isPublished);
        return ResponseEntity.ok(ApiResponse.success("Publish status updated", profile));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Global Platform Marketplace KPIs", description = "Platform-wide metrics for searches, leads, verified firms, and conversion.")
    public ResponseEntity<ApiResponse<MarketplaceStatsDto>> getPlatformStats() {
        MarketplaceStatsDto stats = marketplaceService.getPlatformMarketplaceStats();
        return ResponseEntity.ok(ApiResponse.success("Platform marketplace analytics retrieved", stats));
    }
}
