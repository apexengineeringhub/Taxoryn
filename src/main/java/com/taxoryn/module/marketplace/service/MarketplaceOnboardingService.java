package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.DocumentType;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingEntity.OnboardingStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MarketplaceOnboardingService {

    // =========================================================================
    // 1. Practice Operations (Authenticated Practice Portal)
    // =========================================================================

    MarketplaceProposalDto sendProposal(CreateProposalRequest request);

    PagedResponse<MarketplaceProposalDto> getPracticeProposals(Pageable pageable);

    MarketplaceOnboardingDto initiateOnboarding(InitiateOnboardingRequest request);

    PagedResponse<MarketplaceOnboardingDto> getPracticeOnboardings(OnboardingStatus status, String search, Pageable pageable);

    MarketplaceOnboardingDto getPracticeOnboardingById(UUID onboardingId);

    OnboardingDocumentDto verifyDocument(UUID onboardingId, UUID documentId, VerifyOnboardingDocumentRequest request);

    MarketplaceOnboardingDto approveAndPromoteToClient(UUID onboardingId, ApproveAndPromoteClientRequest request);

    // =========================================================================
    // 2. Public Self-Serve Customer Operations (Secured by accessToken)
    // =========================================================================

    MarketplaceProposalDto getPublicProposalByToken(String token);

    MarketplaceProposalDto acceptOrRejectProposal(String token, AcceptProposalRequest request);

    MarketplaceOnboardingDto getPublicOnboardingByToken(String token);

    MarketplaceOnboardingDto updatePublicOnboardingDetails(String token, UpdateOnboardingDetailsRequest request);

    MarketplaceOnboardingDto signPublicEngagementLetter(String token, SignEngagementLetterRequest request);

    OnboardingDocumentDto uploadPublicOnboardingDocument(
            String token,
            DocumentType docType,
            String docName,
            String filePath,
            Long fileSizeBytes,
            String contentType
    );
}
