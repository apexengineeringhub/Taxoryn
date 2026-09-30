package com.taxoryn.module.marketplace.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.ApproveAndPromoteClientRequest;
import com.taxoryn.module.marketplace.dto.CreateProposalRequest;
import com.taxoryn.module.marketplace.dto.InitiateOnboardingRequest;
import com.taxoryn.module.marketplace.dto.MarketplaceOnboardingDto;
import com.taxoryn.module.marketplace.dto.MarketplaceProposalDto;
import com.taxoryn.module.marketplace.dto.OnboardingDocumentDto;
import com.taxoryn.module.marketplace.dto.VerifyOnboardingDocumentRequest;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingEntity.OnboardingStatus;
import com.taxoryn.module.marketplace.service.MarketplaceOnboardingService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/practice/marketplace/onboarding")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.MARKETPLACE)
@Tag(name = "Practice Marketplace Onboarding Hub", description = "Endpoints for practice to manage proposals, KYC documents, and client promotion")
public class MarketplaceOnboardingPracticeController {

    private final MarketplaceOnboardingService onboardingService;

    // =========================================================================
    // Proposals Management
    // =========================================================================

    @PostMapping("/proposals")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('TAX_ASSOCIATE') or hasRole('SENIOR_TAX_ASSOCIATE') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Send formal engagement proposal to an inbound lead")
    public ResponseEntity<ApiResponse<MarketplaceProposalDto>> sendProposal(
            @Valid @RequestBody CreateProposalRequest request
    ) {
        MarketplaceProposalDto result = onboardingService.sendProposal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Proposal sent to lead successfully", result));
    }

    @GetMapping("/proposals")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('TAX_ASSOCIATE') or hasRole('SENIOR_TAX_ASSOCIATE') or hasRole('STAFF') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Get list of engagement proposals sent by practice")
    public ResponseEntity<ApiResponse<PagedResponse<MarketplaceProposalDto>>> getProposals(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<MarketplaceProposalDto> result = onboardingService.getPracticeProposals(pageable);
        return ResponseEntity.ok(ApiResponse.success("Proposals retrieved successfully", result));
    }

    // =========================================================================
    // Onboardings Pipeline Management
    // =========================================================================

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('TAX_ASSOCIATE') or hasRole('SENIOR_TAX_ASSOCIATE') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Explicitly initiate an onboarding pipeline for an accepted lead")
    public ResponseEntity<ApiResponse<MarketplaceOnboardingDto>> initiateOnboarding(
            @Valid @RequestBody InitiateOnboardingRequest request
    ) {
        MarketplaceOnboardingDto result = onboardingService.initiateOnboarding(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Onboarding pipeline initiated", result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('TAX_ASSOCIATE') or hasRole('SENIOR_TAX_ASSOCIATE') or hasRole('STAFF') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Get practice onboardings pipeline with status and search filters")
    public ResponseEntity<ApiResponse<PagedResponse<MarketplaceOnboardingDto>>> getOnboardings(
            @RequestParam(required = false) OnboardingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<MarketplaceOnboardingDto> result = onboardingService.getPracticeOnboardings(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Onboarding records retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MARKETPLACE_VIEW') or hasAuthority('MARKETPLACE_READ') or hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('TAX_ASSOCIATE') or hasRole('SENIOR_TAX_ASSOCIATE') or hasRole('STAFF') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Get detailed onboarding record by ID with KYC checklist")
    public ResponseEntity<ApiResponse<MarketplaceOnboardingDto>> getOnboardingById(@PathVariable UUID id) {
        MarketplaceOnboardingDto result = onboardingService.getPracticeOnboardingById(id);
        return ResponseEntity.ok(ApiResponse.success("Onboarding record retrieved", result));
    }

    @PutMapping("/{onboardingId}/documents/{documentId}/verify")
    @PreAuthorize("hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('TAX_ASSOCIATE') or hasRole('SENIOR_TAX_ASSOCIATE') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Verify or reject an onboarding KYC document")
    public ResponseEntity<ApiResponse<OnboardingDocumentDto>> verifyDocument(
            @PathVariable UUID onboardingId,
            @PathVariable UUID documentId,
            @Valid @RequestBody VerifyOnboardingDocumentRequest request
    ) {
        OnboardingDocumentDto result = onboardingService.verifyDocument(onboardingId, documentId, request);
        return ResponseEntity.ok(ApiResponse.success("Document verification status updated", result));
    }

    @PostMapping("/{onboardingId}/promote-to-client")
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('MARKETPLACE_WRITE') or hasAuthority('MARKETPLACE_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER') or hasRole('TAX_MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN')")
    @Operation(summary = "Approve onboarding and promote record to Client Master and provision Client Portal credentials")
    public ResponseEntity<ApiResponse<MarketplaceOnboardingDto>> promoteToClientMaster(
            @PathVariable UUID onboardingId,
            @Valid @RequestBody(required = false) ApproveAndPromoteClientRequest request
    ) {
        if (request == null) {
            request = ApproveAndPromoteClientRequest.builder().build();
        }
        MarketplaceOnboardingDto result = onboardingService.approveAndPromoteToClient(onboardingId, request);
        return ResponseEntity.ok(ApiResponse.success("Client successfully promoted to Client Master & Client Portal provisioned", result));
    }
}
