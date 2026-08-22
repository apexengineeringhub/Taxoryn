package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity.PricingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create/update a Marketplace Service Package")
public class CreateMarketplaceServiceRequest {

    @NotBlank(message = "Service title is required")
    @Schema(description = "Package Title", example = "Monthly GST Return Filing & ITC Reconciliation")
    private String title;

    @NotBlank(message = "Category is required")
    @Schema(description = "Category", example = "GST")
    private String category;

    @Schema(description = "Detailed Service Description")
    private String description;

    @NotNull(message = "Price is required")
    @Schema(description = "Fee amount in INR", example = "2499.00")
    private BigDecimal price;

    @Builder.Default
    @Schema(description = "Pricing structure", example = "MONTHLY_RETAINER")
    private PricingType pricingType = PricingType.FIXED;

    @Builder.Default
    @Schema(description = "Estimated turnaround time in days", example = "3")
    private Integer deliveryDays = 3;

    @Schema(description = "Included Deliverables (bullet points or JSON)")
    private String deliverables;

    @Builder.Default
    private Boolean isActive = true;
}
