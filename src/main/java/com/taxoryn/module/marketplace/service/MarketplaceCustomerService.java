package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.dto.*;

import java.util.List;

public interface MarketplaceCustomerService {

    CustomerAuthResponseDto registerCustomer(RegisterCustomerRequest request);

    CustomerProfileDto getCurrentCustomerProfile();

    CustomerProfileDto updateCurrentCustomerProfile(UpdateCustomerProfileRequest request);

    CustomerDashboardDto getCustomerDashboard();

    List<MarketplaceLeadDto> getCustomerLeads();

    List<MarketplaceConsultationDto> getCustomerConsultations();

    List<MarketplaceProposalDto> getCustomerProposals();

    List<MarketplaceReviewDto> getCustomerReviews();
}
