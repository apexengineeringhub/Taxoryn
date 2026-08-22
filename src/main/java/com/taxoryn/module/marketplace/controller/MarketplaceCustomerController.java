package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.service.MarketplaceCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/marketplace/customer", "/api/marketplace/customer"})
@RequiredArgsConstructor
@Tag(name = "Marketplace Customer Profile & Account", description = "Endpoints for Customer Registration, Profile, and Self-Service Marketplace Dashboard")
public class MarketplaceCustomerController {

    private final MarketplaceCustomerService customerService;

    @PostMapping("/register")
    @Operation(summary = "Register Marketplace Customer", description = "Creates a new customer identity and customer profile for marketplace interactions")
    public ResponseEntity<ApiResponse<CustomerAuthResponseDto>> register(
            @Valid @RequestBody RegisterCustomerRequest request
    ) {
        CustomerAuthResponseDto response = customerService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Marketplace customer account created successfully", response));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Profile", description = "Retrieves authenticated customer's own profile and completeness status")
    public ResponseEntity<ApiResponse<CustomerProfileDto>> getProfile() {
        CustomerProfileDto profile = customerService.getCurrentCustomerProfile();
        return ResponseEntity.ok(ApiResponse.success("Customer profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Customer Profile", description = "Updates authenticated customer's own profile details")
    public ResponseEntity<ApiResponse<CustomerProfileDto>> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        CustomerProfileDto updated = customerService.updateCurrentCustomerProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Customer profile updated successfully", updated));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Dashboard", description = "Aggregates customer marketplace requests, upcoming consultations, and proposals")
    public ResponseEntity<ApiResponse<CustomerDashboardDto>> getDashboard() {
        CustomerDashboardDto dashboard = customerService.getCustomerDashboard();
        return ResponseEntity.ok(ApiResponse.success("Customer dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/leads")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Leads", description = "Lists enquiries submitted by the authenticated customer")
    public ResponseEntity<ApiResponse<List<MarketplaceLeadDto>>> getLeads() {
        List<MarketplaceLeadDto> leads = customerService.getCustomerLeads();
        return ResponseEntity.ok(ApiResponse.success("Customer enquiries retrieved successfully", leads));
    }

    @GetMapping("/consultations")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Consultations", description = "Lists consultations booked by the authenticated customer")
    public ResponseEntity<ApiResponse<List<MarketplaceConsultationDto>>> getConsultations() {
        List<MarketplaceConsultationDto> consultations = customerService.getCustomerConsultations();
        return ResponseEntity.ok(ApiResponse.success("Customer consultations retrieved successfully", consultations));
    }

    @GetMapping("/proposals")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Proposals", description = "Lists engagement proposals received by the authenticated customer")
    public ResponseEntity<ApiResponse<List<MarketplaceProposalDto>>> getProposals() {
        List<MarketplaceProposalDto> proposals = customerService.getCustomerProposals();
        return ResponseEntity.ok(ApiResponse.success("Customer proposals retrieved successfully", proposals));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Reviews", description = "Lists reviews submitted by the authenticated customer")
    public ResponseEntity<ApiResponse<List<MarketplaceReviewDto>>> getReviews() {
        List<MarketplaceReviewDto> reviews = customerService.getCustomerReviews();
        return ResponseEntity.ok(ApiResponse.success("Customer reviews retrieved successfully", reviews));
    }
}
