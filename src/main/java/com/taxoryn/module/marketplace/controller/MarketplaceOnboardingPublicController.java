package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.DocumentType;
import com.taxoryn.module.marketplace.service.MarketplaceOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/marketplace/onboarding")
@RequiredArgsConstructor
@Tag(name = "Customer Self-Serve Onboarding Portal", description = "Public endpoints for customers to review proposals, sign engagement letters, and submit KYC documents")
public class MarketplaceOnboardingPublicController {

    private final MarketplaceOnboardingService onboardingService;

    @GetMapping("/proposal/{token}")
    @Operation(summary = "View proposal details by secret access token")
    public ResponseEntity<ApiResponse<MarketplaceProposalDto>> getProposalByToken(@PathVariable String token) {
        MarketplaceProposalDto result = onboardingService.getPublicProposalByToken(token);
        return ResponseEntity.ok(ApiResponse.success("Proposal retrieved", result));
    }

    @PostMapping("/proposal/{token}/respond")
    @Operation(summary = "Accept or reject proposal by access token")
    public ResponseEntity<ApiResponse<MarketplaceProposalDto>> respondToProposal(
            @PathVariable String token,
            @Valid @RequestBody AcceptProposalRequest request
    ) {
        MarketplaceProposalDto result = onboardingService.acceptOrRejectProposal(token, request);
        return ResponseEntity.ok(ApiResponse.success("Proposal response recorded", result));
    }

    @GetMapping("/session/{token}")
    @Operation(summary = "Get onboarding checklist & profile by secret access token")
    public ResponseEntity<ApiResponse<MarketplaceOnboardingDto>> getOnboardingByToken(@PathVariable String token) {
        MarketplaceOnboardingDto result = onboardingService.getPublicOnboardingByToken(token);
        return ResponseEntity.ok(ApiResponse.success("Onboarding session retrieved", result));
    }

    @PutMapping("/session/{token}/details")
    @Operation(summary = "Submit taxpayer entity, PAN, GSTIN, and address information")
    public ResponseEntity<ApiResponse<MarketplaceOnboardingDto>> updateDetails(
            @PathVariable String token,
            @Valid @RequestBody UpdateOnboardingDetailsRequest request
    ) {
        MarketplaceOnboardingDto result = onboardingService.updatePublicOnboardingDetails(token, request);
        return ResponseEntity.ok(ApiResponse.success("Details updated successfully", result));
    }

    @PostMapping("/session/{token}/sign-engagement")
    @Operation(summary = "Sign Engagement Letter and accept professional fee agreement")
    public ResponseEntity<ApiResponse<MarketplaceOnboardingDto>> signEngagementLetter(
            @PathVariable String token,
            @Valid @RequestBody SignEngagementLetterRequest request
    ) {
        MarketplaceOnboardingDto result = onboardingService.signPublicEngagementLetter(token, request);
        return ResponseEntity.ok(ApiResponse.success("Engagement signed successfully", result));
    }

    @PostMapping("/session/{token}/upload-document")
    @Operation(summary = "Submit KYC document file path and metadata")
    public ResponseEntity<ApiResponse<OnboardingDocumentDto>> uploadDocument(
            @PathVariable String token,
            @RequestParam DocumentType documentType,
            @RequestParam String documentName,
            @RequestParam String filePath,
            @RequestParam(required = false) Long fileSizeBytes,
            @RequestParam(required = false) String contentType
    ) {
        OnboardingDocumentDto result = onboardingService.uploadPublicOnboardingDocument(
                token, documentType, documentName, filePath, fileSizeBytes, contentType
        );
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", result));
    }
}
