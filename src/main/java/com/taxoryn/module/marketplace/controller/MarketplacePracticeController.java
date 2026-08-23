package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import com.taxoryn.module.marketplace.service.MarketplaceService;
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
@RequestMapping({"/api/v1/marketplace/practice-profile", "/api/v1/practice/marketplace", "/api/practice/marketplace"})
@RequiredArgsConstructor
@Tag(name = "Practice Marketplace Management", description = "Tax practitioner tools to manage marketplace directory listing, service packages, inbound leads, and client conversion")
@SecurityRequirement(name = "BearerAuth")
public class MarketplacePracticeController {

    private final MarketplaceService marketplaceService;
    private final TaxServiceMasterService taxServiceMasterService;

    // --- Controlled Tax Service Master Selection ---

    @GetMapping("/tax-services")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Practice Selected Controlled Tax Services", description = "Retrieves the practice's selected services from the controlled master catalog.")
    public ResponseEntity<ApiResponse<List<PracticeServiceDto>>> getMyTaxServices() {
        List<PracticeServiceDto> services = taxServiceMasterService.getMyPracticeServices();
        return ResponseEntity.ok(ApiResponse.success("Practice controlled services retrieved successfully", services));
    }

    @PutMapping("/tax-services")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Batch Update Practice Selected Tax Services", description = "Updates the practice's active services selection from the controlled master catalog.")
    public ResponseEntity<ApiResponse<List<PracticeServiceDto>>> updateMyTaxServices(@Valid @RequestBody UpdatePracticeServicesRequest request) {
        List<PracticeServiceDto> services = taxServiceMasterService.updateMyPracticeServices(request);
        return ResponseEntity.ok(ApiResponse.success("Practice controlled services updated successfully", services));
    }

    @PostMapping("/tax-services/{taxServiceId}")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Add Controlled Tax Service to Practice", description = "Selects a single controlled tax service to offer on the marketplace.")
    public ResponseEntity<ApiResponse<PracticeServiceDto>> addTaxServiceToPractice(@PathVariable UUID taxServiceId) {
        PracticeServiceDto service = taxServiceMasterService.addServiceToPractice(taxServiceId);
        return ResponseEntity.ok(ApiResponse.success("Service added to practice successfully", service));
    }

    @DeleteMapping("/tax-services/{taxServiceId}")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_DELETE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Remove Controlled Tax Service from Practice", description = "Removes a controlled tax service from the practice's offerings.")
    public ResponseEntity<ApiResponse<Void>> removeTaxServiceFromPractice(@PathVariable UUID taxServiceId) {
        taxServiceMasterService.removeServiceFromPractice(taxServiceId);
        return ResponseEntity.ok(ApiResponse.success("Service removed from practice successfully", null));
    }

    // --- Profile & Listing ---

    @GetMapping({"", "/profile"})
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Practice Marketplace Profile", description = "Retrieves the practice's current marketplace profile settings and visibility.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> getMyProfile() {
        PublicMarketplaceProfileDto profile = marketplaceService.getMyPracticeProfile();
        return ResponseEntity.ok(ApiResponse.success("Practice marketplace profile retrieved", profile));
    }

    @PostMapping({"", "/profile"})
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Practice Marketplace Profile", description = "Initializes a new practice marketplace profile listing.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> createMyProfile(@Valid @RequestBody CreatePracticeProfileRequest request) {
        PublicMarketplaceProfileDto profile = marketplaceService.createPracticeProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Marketplace profile created successfully", profile));
    }

    @PutMapping({"", "/profile"})
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Practice Marketplace Profile", description = "Updates bio, specializations, pricing, contact info, slug, and publish status.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> updateMyProfile(@Valid @RequestBody UpdateMarketplaceProfileRequest request) {
        PublicMarketplaceProfileDto profile = marketplaceService.updateMyPracticeProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Marketplace profile updated successfully", profile));
    }

    @PatchMapping({"/visibility", "/profile/visibility"})
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Marketplace Visibility Status", description = "Toggles visibility status (PRIVATE, PUBLIC, SUSPENDED). Validates required publishing fields.")
    public ResponseEntity<ApiResponse<PublicMarketplaceProfileDto>> updateVisibility(@Valid @RequestBody UpdateProfileVisibilityRequest request) {
        PublicMarketplaceProfileDto profile = marketplaceService.updateProfileVisibility(request);
        return ResponseEntity.ok(ApiResponse.success("Marketplace profile visibility updated successfully", profile));
    }

    @GetMapping({"/slug/generate", "/profile/slug/generate"})
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Generate Unique Public Slug", description = "Generates an available SEO-friendly public URL slug for the practice profile.")
    public ResponseEntity<ApiResponse<String>> generateSlug(
            @RequestParam(required = false) String baseName,
            @RequestParam(required = false) String city
    ) {
        String slug = marketplaceService.generateUniqueSlug(baseName, city);
        return ResponseEntity.ok(ApiResponse.success("Unique slug generated", slug));
    }

    @GetMapping({"/completeness", "/profile/completeness"})
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Profile Completeness Breakdown", description = "Calculates profile completeness percentage, completed items, and missing items.")
    public ResponseEntity<ApiResponse<ProfileCompletenessDto>> getMyProfileCompleteness() {
        ProfileCompletenessDto completeness = marketplaceService.getMyProfileCompleteness();
        return ResponseEntity.ok(ApiResponse.success("Profile completeness metrics retrieved", completeness));
    }

    // --- Practice Locations ---

    @GetMapping("/locations")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Practice Locations", description = "Retrieves all physical office/branch locations created by the firm.")
    public ResponseEntity<ApiResponse<List<PracticeLocationDto>>> getMyLocations() {
        List<PracticeLocationDto> locations = marketplaceService.getMyPracticeLocations();
        return ResponseEntity.ok(ApiResponse.success("Practice locations retrieved successfully", locations));
    }

    @GetMapping("/locations/{locationId}")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Practice Location by ID", description = "Retrieves details of a specific office location belonging to the firm.")
    public ResponseEntity<ApiResponse<PracticeLocationDto>> getLocationById(@PathVariable UUID locationId) {
        PracticeLocationDto location = marketplaceService.getPracticeLocationById(locationId);
        return ResponseEntity.ok(ApiResponse.success("Practice location retrieved successfully", location));
    }

    @PostMapping("/locations")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Practice Location", description = "Adds a new physical branch or office location to the practice profile.")
    public ResponseEntity<ApiResponse<PracticeLocationDto>> createLocation(@Valid @RequestBody CreatePracticeLocationRequest request) {
        PracticeLocationDto location = marketplaceService.createPracticeLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Practice location created successfully", location));
    }

    @PutMapping("/locations/{locationId}")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Practice Location", description = "Updates address, coordinates, or primary flag for a branch location.")
    public ResponseEntity<ApiResponse<PracticeLocationDto>> updateLocation(
            @PathVariable UUID locationId,
            @Valid @RequestBody UpdatePracticeLocationRequest request) {
        PracticeLocationDto location = marketplaceService.updatePracticeLocation(locationId, request);
        return ResponseEntity.ok(ApiResponse.success("Practice location updated successfully", location));
    }

    @PatchMapping("/locations/{locationId}/primary")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Set Primary Location", description = "Designates this branch as the main headquarter/primary location of the practice.")
    public ResponseEntity<ApiResponse<PracticeLocationDto>> setPrimaryLocation(@PathVariable UUID locationId) {
        PracticeLocationDto location = marketplaceService.setPrimaryPracticeLocation(locationId);
        return ResponseEntity.ok(ApiResponse.success("Primary practice location updated successfully", location));
    }

    @PatchMapping("/locations/{locationId}/activate")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reactivate Location", description = "Restores an inactive branch location to public marketplace visibility.")
    public ResponseEntity<ApiResponse<PracticeLocationDto>> activateLocation(@PathVariable UUID locationId) {
        PracticeLocationDto location = marketplaceService.activatePracticeLocation(locationId);
        return ResponseEntity.ok(ApiResponse.success("Practice location activated successfully", location));
    }

    @DeleteMapping("/locations/{locationId}")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_DELETE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete / Deactivate Practice Location", description = "Deactivates or removes a branch location from the practice profile.")
    public ResponseEntity<ApiResponse<PracticeLocationDto>> deactivateLocation(@PathVariable UUID locationId) {
        PracticeLocationDto location = marketplaceService.deactivatePracticeLocation(locationId);
        return ResponseEntity.ok(ApiResponse.success("Practice location deactivated successfully", location));
    }

    // --- Service Packages ---

    @GetMapping("/services")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Practice Service Packages", description = "Retrieves all offerings created by the firm.")
    public ResponseEntity<ApiResponse<List<MarketplaceServiceDto>>> getMyServices() {
        List<MarketplaceServiceDto> services = marketplaceService.getMyPracticeServices();
        return ResponseEntity.ok(ApiResponse.success("Services retrieved successfully", services));
    }

    @PostMapping("/services")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Service Package", description = "Adds a new fixed-fee or retainer package to the practice's profile.")
    public ResponseEntity<ApiResponse<MarketplaceServiceDto>> createService(@Valid @RequestBody CreateMarketplaceServiceRequest request) {
        MarketplaceServiceDto service = marketplaceService.createPracticeService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Service package created successfully", service));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Service Package", description = "Updates package pricing, deliverables, or active status.")
    public ResponseEntity<ApiResponse<MarketplaceServiceDto>> updateService(@PathVariable UUID id, @Valid @RequestBody CreateMarketplaceServiceRequest request) {
        MarketplaceServiceDto service = marketplaceService.updatePracticeService(id, request);
        return ResponseEntity.ok(ApiResponse.success("Service package updated successfully", service));
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete Service Package", description = "Removes a service package from the practice profile.")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable UUID id) {
        marketplaceService.deletePracticeService(id);
        return ResponseEntity.ok(ApiResponse.success("Service package deleted successfully", null));
    }

    // --- Inbound Leads CRM Pipeline ---

    @GetMapping("/leads")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Inbound Leads", description = "Paginated list of customer inquiries with status filtering.")
    public ResponseEntity<ApiResponse<PagedResponse<MarketplaceLeadDto>>> getMyLeads(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = "desc".equalsIgnoreCase(sortDirection) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        PagedResponse<MarketplaceLeadDto> leads = marketplaceService.getMyLeads(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Inbound leads retrieved", leads));
    }

    @PatchMapping("/leads/{id}/status")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Lead Status", description = "Transitions lead status (CONTACTED, PROPOSAL_SENT, ARCHIVED) with practitioner notes.")
    public ResponseEntity<ApiResponse<MarketplaceLeadDto>> updateLeadStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) UUID assignedEmployeeId
    ) {
        MarketplaceLeadDto updated = marketplaceService.updateLeadStatus(id, status, notes, assignedEmployeeId);
        return ResponseEntity.ok(ApiResponse.success("Lead status updated", updated));
    }

    @PostMapping("/leads/{id}/convert-to-client")
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('MARKETPLACE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Convert Lead to CRM Client (Zero Friction)", description = "Provisions an active Client Master record, initial onboarding task, and assigns account manager.")
    public ResponseEntity<ApiResponse<MarketplaceLeadDto>> convertLeadToClient(
            @PathVariable UUID id,
            @Valid @RequestBody ConvertLeadToClientRequest request
    ) {
        MarketplaceLeadDto converted = marketplaceService.convertLeadToClient(id, request);
        return ResponseEntity.ok(ApiResponse.success("Marketplace lead converted to Practice Client successfully", converted));
    }

    // --- Consultations ---

    @GetMapping("/consultations")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Practice Consultations", description = "Retrieves upcoming and completed customer appointments.")
    public ResponseEntity<ApiResponse<PagedResponse<MarketplaceConsultationDto>>> getMyConsultations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        PagedResponse<MarketplaceConsultationDto> consultations = marketplaceService.getMyConsultations(pageable);
        return ResponseEntity.ok(ApiResponse.success("Consultations retrieved", consultations));
    }

    @PatchMapping("/consultations/{id}/status")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Consultation Status", description = "Updates consultation status (COMPLETED, CANCELLED) and video meeting link.")
    public ResponseEntity<ApiResponse<MarketplaceConsultationDto>> updateConsultationStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(required = false) String meetingLink,
            @RequestParam(required = false) String notes
    ) {
        MarketplaceConsultationDto updated = marketplaceService.updateConsultationStatus(id, status, meetingLink, notes);
        return ResponseEntity.ok(ApiResponse.success("Consultation updated", updated));
    }

    // --- Verification & KYC ---

    @PostMapping("/verification")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Submit KYC Credential Verification", description = "Uploads ICAI COP, ICSI, or Bar Council certificate for verified badge approval.")
    public ResponseEntity<ApiResponse<MarketplaceVerificationDto>> submitVerification(@Valid @RequestBody SubmitVerificationRequest request) {
        MarketplaceVerificationDto verification = marketplaceService.submitVerification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Verification credentials submitted for review", verification));
    }

    @GetMapping("/verification")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get KYC Verification Status", description = "Checks the verification status (PENDING, VERIFIED, REJECTED) of the firm.")
    public ResponseEntity<ApiResponse<MarketplaceVerificationDto>> getVerificationStatus() {
        MarketplaceVerificationDto status = marketplaceService.getMyVerificationStatus();
        return ResponseEntity.ok(ApiResponse.success("Verification status retrieved", status));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Practice Marketplace Analytics", description = "Summary of leads received, conversion rate, and pipeline value.")
    public ResponseEntity<ApiResponse<MarketplaceStatsDto>> getPracticeStats() {
        MarketplaceStatsDto stats = marketplaceService.getMyPracticeMarketplaceStats();
        return ResponseEntity.ok(ApiResponse.success("Practice marketplace KPIs retrieved", stats));
    }
}
