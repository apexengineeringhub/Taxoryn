package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform / Practice Marketplace Analytics & KPI Summary")
public class MarketplaceStatsDto {

    private long totalListedPractitioners;
    private long totalVerifiedPractitioners;
    private long totalPendingVerifications;
    private long totalInboundLeads;
    private long totalConvertedClients;
    private double leadConversionRate;
    private long totalConsultationsBooked;
    private BigDecimal estimatedMarketplacePipelineValue;
}
