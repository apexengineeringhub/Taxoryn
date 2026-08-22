package com.taxoryn.module.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDashboardDto {

    private CustomerProfileDto profile;

    private long totalRequests;

    private long totalConsultations;

    private long totalProposals;

    private long totalReviews;

    private List<MarketplaceLeadDto> recentLeads;

    private List<MarketplaceConsultationDto> recentConsultations;

    private List<MarketplaceProposalDto> recentProposals;

    private List<MarketplaceReviewDto> recentReviews;
}
