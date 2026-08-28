package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.dto.*;

import java.util.List;

public interface MarketplaceCustomerService {

    CustomerAuthResponseDto registerCustomer(RegisterCustomerRequest request);

    CustomerProfileDto getCurrentCustomerProfile();

    CustomerProfileDto updateCurrentCustomerProfile(UpdateCustomerProfileRequest request);

    CustomerDashboardDto getCustomerDashboard();

    List<MarketplaceLeadDto> getCustomerLeads();

    com.taxoryn.core.response.PagedResponse<EnquiryDetailDto> getCustomerEnquiries(com.taxoryn.module.marketplace.entity.EnquiryStatus status, org.springframework.data.domain.Pageable pageable);

    EnquiryDetailDto getCustomerEnquiryDetail(java.util.UUID enquiryId);

    EnquiryDetailDto cancelCustomerEnquiry(java.util.UUID enquiryId, CancelEnquiryRequest request);

    MarketplaceReviewDto submitVerifiedEnquiryReview(java.util.UUID enquiryId, SubmitEnquiryReviewRequest request);

    EnquiryMessageThreadDto getCustomerEnquiryMessages(java.util.UUID enquiryId);

    EnquiryMessageDto sendCustomerMessage(java.util.UUID enquiryId, SendEnquiryMessageRequest request);

    void markMessagesReadByCustomer(java.util.UUID enquiryId);

    List<MarketplaceConsultationDto> getCustomerConsultations();

    List<MarketplaceProposalDto> getCustomerProposals();

    List<MarketplaceReviewDto> getCustomerReviews();
}
