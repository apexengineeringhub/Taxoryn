package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.model.PricingType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resolved Pricing Details for Invoice Line")
public class PriceResolutionResultDto {

    @Schema(description = "Final effective unit price to charge", example = "2125.00")
    private BigDecimal unitPrice;

    @Schema(description = "Standard catalog unit price before discount", example = "2500.00")
    private BigDecimal standardUnitPrice;

    @Schema(description = "Resolved pricing category (STANDARD, CUSTOMER_SPECIFIC, PROMOTIONAL, CUSTOM)", example = "PROMOTIONAL")
    private PricingType pricingType;

    @Schema(description = "Applied promotion ID (if promotional)")
    private UUID promotionId;

    @Schema(description = "Applied promotion name (if promotional)", example = "GST Season 15% Off")
    private String promotionName;

    @Schema(description = "Promotion discount type (PERCENTAGE, FIXED_AMOUNT, FIXED_PRICE)")
    private PromotionDiscountType discountType;

    @Schema(description = "Promotion discount configured value", example = "15.00")
    private BigDecimal discountValue;

    @Schema(description = "Calculated discount amount per unit", example = "375.00")
    private BigDecimal discountAmount;
}
