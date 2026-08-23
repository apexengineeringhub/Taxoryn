package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MarketplaceService {

    // 1. Public Customer Discovery APIs
    PagedResponse<PublicMarketplaceProfileDto> searchProfiles(MarketplaceSearchRequest request);

    PublicMarketplaceProfileDto getProfileBySlug(String slug);

    PublicMarketplaceProfileDto getProfileById(UUID id);

    List<PublicMarketplaceProfileDto> getFeaturedProfiles();

    MarketplaceLeadDto submitPublicLead(CreateMarketplaceLeadRequest request);

    MarketplaceConsultationDto bookPublicConsultation(BookConsultationRequest request);

    MarketplaceReviewDto submitPublicReview(SubmitMarketplaceReviewRequest request);

    List<MarketplaceServiceDto> getPublicServices(UUID marketplaceProfileId);

    List<MarketplaceReviewDto> getPublicReviews(UUID marketplaceProfileId);

    // 2. Practice Private Portal APIs
    PublicMarketplaceProfileDto getMyPracticeProfile();

    PublicMarketplaceProfileDto createPracticeProfile(CreatePracticeProfileRequest request);

    PublicMarketplaceProfileDto updateMyPracticeProfile(UpdateMarketplaceProfileRequest request);

    PublicMarketplaceProfileDto updateProfileVisibility(UpdateProfileVisibilityRequest request);

    List<MarketplaceServiceDto> getMyPracticeServices();

    MarketplaceServiceDto createPracticeService(CreateMarketplaceServiceRequest request);

    MarketplaceServiceDto updatePracticeService(UUID serviceId, CreateMarketplaceServiceRequest request);

    void deletePracticeService(UUID serviceId);

    PagedResponse<MarketplaceLeadDto> getMyLeads(LeadStatus status, String search, Pageable pageable);

    PagedResponse<EarlyEnquiryViewDto> getMyEarlyEnquiries(LeadStatus status, String search, Pageable pageable);

    EarlyEnquiryViewDto getEarlyEnquiryById(UUID enquiryId);

    MarketplaceLeadDto updateLeadStatus(UUID leadId, LeadStatus status, String notes, UUID assignedEmployeeId);

    MarketplaceLeadDto convertLeadToClient(UUID leadId, ConvertLeadToClientRequest request);

    PagedResponse<MarketplaceConsultationDto> getMyConsultations(Pageable pageable);

    MarketplaceConsultationDto updateConsultationStatus(UUID consultationId, ConsultationStatus status, String meetingLink, String notes);

    MarketplaceVerificationDto submitVerification(SubmitVerificationRequest request);

    MarketplaceVerificationDto getMyVerificationStatus();

    MarketplaceStatsDto getMyPracticeMarketplaceStats();

    String generateUniqueSlug(String baseName, String city);

    ProfileCompletenessDto getMyProfileCompleteness();

    // Practice Locations Management
    List<PracticeLocationDto> getMyPracticeLocations();

    PracticeLocationDto getPracticeLocationById(UUID locationId);

    PracticeLocationDto createPracticeLocation(CreatePracticeLocationRequest request);

    PracticeLocationDto updatePracticeLocation(UUID locationId, UpdatePracticeLocationRequest request);

    PracticeLocationDto setPrimaryPracticeLocation(UUID locationId);

    PracticeLocationDto deactivatePracticeLocation(UUID locationId);

    PracticeLocationDto activatePracticeLocation(UUID locationId);

    void deletePracticeLocation(UUID locationId);

    // 3. Platform Super Admin APIs
    PagedResponse<MarketplaceVerificationDto> getPendingVerifications(Pageable pageable);

    MarketplaceVerificationDto processVerification(UUID verificationId, VerifyPractitionerRequest request);

    PublicMarketplaceProfileDto toggleFeaturedStatus(UUID profileId, boolean isFeatured);

    PublicMarketplaceProfileDto togglePublishStatus(UUID profileId, boolean isPublished);

    MarketplaceStatsDto getPlatformMarketplaceStats();

    List<PublicMarketplaceProfileDto> seedDemoMarketplaceData();
}
