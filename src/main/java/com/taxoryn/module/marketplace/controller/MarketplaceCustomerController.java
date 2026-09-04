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
import java.util.UUID;

import com.taxoryn.core.security.AuthCookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping({"/api/v1/marketplace/customer", "/api/marketplace/customer"})
@RequiredArgsConstructor
@Tag(name = "Marketplace Customer Profile & Account", description = "Endpoints for Customer Registration, Profile, and Self-Service Marketplace Dashboard")
public class MarketplaceCustomerController {

    private final MarketplaceCustomerService customerService;
    private final AuthCookieUtil authCookieUtil;

    @PostMapping("/register")
    @Operation(summary = "Register Marketplace Customer", description = "Creates a new customer identity and customer profile for marketplace interactions")
    public ResponseEntity<ApiResponse<CustomerAuthResponseDto>> register(
            @Valid @RequestBody RegisterCustomerRequest request
    ) {
        CustomerAuthResponseDto response = customerService.registerCustomer(request);
        ResponseCookie cookie = authCookieUtil.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
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

    @GetMapping("/enquiries")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Enquiries with Lifecycle Status", description = "Lists paginated enquiries submitted by customer with real-time status and timeline")
    public ResponseEntity<ApiResponse<com.taxoryn.core.response.PagedResponse<EnquiryDetailDto>>> getCustomerEnquiries(
            @RequestParam(required = false) com.taxoryn.module.marketplace.entity.EnquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        org.springframework.data.domain.Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? org.springframework.data.domain.Sort.by(sortBy).descending()
                : org.springframework.data.domain.Sort.by(sortBy).ascending();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        com.taxoryn.core.response.PagedResponse<EnquiryDetailDto> response = customerService.getCustomerEnquiries(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Customer enquiries retrieved successfully", response));
    }

    @GetMapping("/enquiries/{id}")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Enquiry Details", description = "Retrieves individual enquiry details including full event-driven timeline")
    public ResponseEntity<ApiResponse<EnquiryDetailDto>> getCustomerEnquiryDetail(@PathVariable UUID id) {
        EnquiryDetailDto enquiry = customerService.getCustomerEnquiryDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry details retrieved successfully", enquiry));
    }

    @PostMapping("/enquiries/{id}/cancel")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel Customer Enquiry", description = "Allows customer to cancel enquiry before work commences (NEW or RECEIVED status)")
    public ResponseEntity<ApiResponse<EnquiryDetailDto>> cancelCustomerEnquiry(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelEnquiryRequest request
    ) {
        EnquiryDetailDto cancelled = customerService.cancelCustomerEnquiry(id, request);
        return ResponseEntity.ok(ApiResponse.success("Enquiry cancelled successfully", cancelled));
    }

    @PostMapping("/enquiries/{id}/reviews")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Submit Verified Review for Completed Enquiry", description = "Submits a verified client review once an enquiry reaches COMPLETED status")
    public ResponseEntity<ApiResponse<MarketplaceReviewDto>> submitVerifiedReview(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitEnquiryReviewRequest request
    ) {
        MarketplaceReviewDto review = customerService.submitVerifiedEnquiryReview(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Verified review submitted successfully", review));
    }

    @GetMapping("/enquiries/{id}/messages")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Enquiry Messages", description = "Retrieves complete conversation thread for customer's enquiry")
    public ResponseEntity<ApiResponse<EnquiryMessageThreadDto>> getCustomerEnquiryMessages(@PathVariable UUID id) {
        EnquiryMessageThreadDto thread = customerService.getCustomerEnquiryMessages(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry messages retrieved successfully", thread));
    }

    @PostMapping("/enquiries/{id}/messages")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Send Customer Message on Enquiry", description = "Sends a message from the customer to the assigned practice / practitioner")
    public ResponseEntity<ApiResponse<EnquiryMessageDto>> sendCustomerEnquiryMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendEnquiryMessageRequest request
    ) {
        EnquiryMessageDto message = customerService.sendCustomerMessage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", message));
    }

    @PostMapping("/enquiries/{id}/messages/read")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Mark Customer Enquiry Messages Read", description = "Marks unread practice messages as read by customer")
    public ResponseEntity<ApiResponse<Void>> markCustomerMessagesRead(@PathVariable UUID id) {
        customerService.markMessagesReadByCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('MARKETPLACE_CUSTOMER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Customer Reviews", description = "Lists reviews submitted by the authenticated customer")
    public ResponseEntity<ApiResponse<List<MarketplaceReviewDto>>> getReviews() {
        List<MarketplaceReviewDto> reviews = customerService.getCustomerReviews();
        return ResponseEntity.ok(ApiResponse.success("Customer reviews retrieved successfully", reviews));
    }
}
