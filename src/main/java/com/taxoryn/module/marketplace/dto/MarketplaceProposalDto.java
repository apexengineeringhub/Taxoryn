package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity.ProposalStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity.PricingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Marketplace Engagement Proposal DTO")
public class MarketplaceProposalDto {

    private UUID id;
    private UUID organizationId;
    private String practiceDisplayName;
    private UUID marketplaceProfileId;
    private UUID leadId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private UUID serviceId;
    private String serviceTitle;
    private String proposalTitle;
    private String scopeOfWork;
    private String deliverables;
    private BigDecimal feeAmount;
    private PricingType pricingType;
    private Integer estimatedTimelineDays;
    private ProposalStatus proposalStatus;
    private String accessToken;
    private LocalDate validUntil;
    private String rejectionReason;
    private Instant acceptedAt;
    private Instant createdAt;
}
