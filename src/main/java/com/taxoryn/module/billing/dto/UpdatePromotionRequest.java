package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import com.taxoryn.module.billing.model.PromotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Promotion Request")
public class UpdatePromotionRequest {

    @Schema(description = "Unique promotional code (optional)", example = "GSTFEST2026")
    private String code;

    @Schema(description = "Promotion display name", example = "GST Season Festival 15% Off")
    private String name;

    @Schema(description = "Promotion description", example = "Special 15% discount on GST compliance filings for Q3")
    private String description;

    @Schema(description = "Promotion type (GENERAL, NEW_CLIENT, SERVICE_SPECIFIC, CUSTOMER_SPECIFIC, SEASONAL, REFERRAL)")
    private PromotionType promotionType;

    @Schema(description = "Discount type (PERCENTAGE, FIXED_AMOUNT, FIXED_PRICE)")
    private PromotionDiscountType discountType;

    @DecimalMin(value = "0.00", message = "Discount value cannot be negative")
    @Schema(description = "Discount value (percentage e.g. 15.00, or flat INR amount)")
    private BigDecimal discountValue;

    @Schema(description = "Target service if SERVICE_SPECIFIC")
    private BillingServiceType targetService;

    @Schema(description = "Target client ID if CUSTOMER_SPECIFIC")
    private UUID targetClientId;

    @Schema(description = "Start date of promotion validity")
    private LocalDate validFrom;

    @Schema(description = "End date of promotion validity")
    private LocalDate validUntil;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Priority evaluation order")
    private Integer priority;

    @Schema(description = "Maximum total allowed uses")
    private Integer maxUses;
}
