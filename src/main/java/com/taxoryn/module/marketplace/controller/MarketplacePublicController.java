package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/marketplace", "/api/marketplace"})
@RequiredArgsConstructor
@Tag(name = "Customer Marketplace & Discovery", description = "Public endpoints for clients to discover, search, compare, book consultations, and submit inquiries to verified tax practitioners across India")
public class MarketplacePublicController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/search")
    @Operation(summary = "Search & Filter Tax Professionals", description = "Public geo-search by city, designation (CA/CS/Advocate), specialization (GST/ITR/TDS), and verification status.")
    public ResponseEntity<ApiResponse<PagedResponse<PublicMarketplaceProfileDto>>> searchProfiles(@Valid @ModelAttribute MarketplaceSearchRequest request) {
        PagedResponse<PublicMarketplaceProfileDto> response = marketplaceService.searchProfiles(request);
        return ResponseEntity.ok(ApiResponse.success("Marketplace directory retrieved successfully", response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Featured Top-Rated Tax Professionals", description = "Retrieves prominent high-rated practitioners featured on the marketplace landing page.")
    public ResponseEntity<ApiResponse<List<PublicMarketplaceProfileDto>>> getFeaturedProfiles() {
        List<PublicMarketplaceProfileDto> featured = marketplaceService.getFeaturedProfiles();
        return ResponseEntity.ok(ApiResponse.success("Featured tax professionals retrieved", featured));
    }

    @GetMapping("/profiles/{id}")
    @Operation(summary = "Get Public Profile by ID", description = "Retrieves full firm details, bio, packages, and verified badges by profile UUID.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> getProfileById(@PathVariable UUID id) {
        PublicMarketplaceProfileDto profile = marketplaceService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @GetMapping("/profiles/slug/{slug}")
    @Operation(summary = "Get Public Profile by Vanity Slug", description = "SEO-friendly endpoint for firm profile showcase (e.g., /marketplace/apex-tax-solutions).")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> getProfileBySlug(@PathVariable String slug) {
        PublicMarketplaceProfileDto profile = marketplaceService.getProfileBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @GetMapping("/profiles/{id}/services")
    @Operation(summary = "Get Packages Offered by Firm", description = "Lists active fixed-fee and retainer services for a specific firm.")
    public ResponseEntity<ApiResponse<List<MarketplaceServiceDto>>> getServices(@PathVariable UUID id) {
        List<MarketplaceServiceDto> services = marketplaceService.getPublicServices(id);
        return ResponseEntity.ok(ApiResponse.success("Packages retrieved successfully", services));
    }

    @GetMapping("/profiles/{id}/reviews")
    @Operation(summary = "Get Verified Reviews & Ratings", description = "Retrieves verified client reviews for a firm.")
    public ResponseEntity<ApiResponse<List<MarketplaceReviewDto>>> getReviews(@PathVariable UUID id) {
        List<MarketplaceReviewDto> reviews = marketplaceService.getPublicReviews(id);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", reviews));
    }

    @PostMapping("/leads")
    @Operation(summary = "Submit Inquiry / Request Callback", description = "Allows a prospective client to inquire about tax filing, notice assistance, or corporate formation.")
    public ResponseEntity<ApiResponse<MarketplaceLeadDto>> submitLead(@Valid @RequestBody CreateMarketplaceLeadRequest request) {
        MarketplaceLeadDto lead = marketplaceService.submitPublicLead(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Inquiry submitted successfully. The practitioner will connect with you shortly.", lead));
    }

    @PostMapping("/consultations")
    @Operation(summary = "Book Consultation Slot", description = "Books a 30-minute dedicated consultation with a tax professional.")
    public ResponseEntity<ApiResponse<MarketplaceConsultationDto>> bookConsultation(@Valid @RequestBody BookConsultationRequest request) {
        MarketplaceConsultationDto consultation = marketplaceService.bookPublicConsultation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Consultation booked successfully", consultation));
    }

    @PostMapping("/reviews")
    @Operation(summary = "Submit Client Review", description = "Post a rating and review for a tax professional.")
    public ResponseEntity<ApiResponse<MarketplaceReviewDto>> submitReview(@Valid @RequestBody SubmitMarketplaceReviewRequest request) {
        MarketplaceReviewDto review = marketplaceService.submitPublicReview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review submitted successfully", review));
    }

    @PostMapping("/seed-demo")
    @Operation(summary = "Seed Demo Marketplace Data", description = "Populates marketplace with realistic Chartered Accountants, CS firms, and Tax Advocates.")
    public ResponseEntity<ApiResponse<List<PublicMarketplaceProfileDto>>> seedDemo() {
        List<PublicMarketplaceProfileDto> seeded = marketplaceService.seedDemoMarketplaceData();
        return ResponseEntity.ok(ApiResponse.success("Demo marketplace data seeded successfully", seeded));
    }
}
