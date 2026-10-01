package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity.PricingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request by practice to send formal engagement proposal to an inbound lead")
public class CreateProposalRequest {

    @NotNull(message = "Lead ID is required")
    private UUID leadId;

    private UUID serviceId;

    @NotBlank(message = "Proposal title is required")
    @Schema(example = "GST Compliance & Tax Advisory Engagement 2026-27")
    private String proposalTitle;

    @NotBlank(message = "Scope of work is required")
    @Schema(example = "Monthly GSTR-1 and GSTR-3B filings, quarterly ITC reconciliation, and departmental notice representations.")
    private String scopeOfWork;

    @Schema(example = "Monthly filing acknowledgements (ARN), ITC credit reports, and representation memo.")
    private String deliverables;

    @DecimalMin(value = "0.00", message = "Fee amount cannot be negative")
    @Schema(example = "4999.00")
    private BigDecimal feeAmount;

    private String serviceCode;

    @Builder.Default
    private PricingType pricingType = PricingType.MONTHLY_RETAINER;

    @Builder.Default
    private Integer estimatedTimelineDays = 7;

    private LocalDate validUntil;
}
