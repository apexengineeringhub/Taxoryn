package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import com.taxoryn.module.billing.model.PromotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Create Promotion Request")
public class CreatePromotionRequest {

    @Schema(description = "Unique promotional code (optional)", example = "GSTFEST2026")
    private String code;

    @NotBlank(message = "Promotion name is required")
    @Schema(description = "Promotion display name", example = "GST Season Festival 15% Off")
    private String name;

    @Schema(description = "Promotion description", example = "Special 15% discount on GST compliance filings for Q3")
    private String description;

    @NotNull(message = "Promotion type is required")
    @Schema(description = "Promotion type (GENERAL, NEW_CLIENT, SERVICE_SPECIFIC, CUSTOMER_SPECIFIC, SEASONAL, REFERRAL)", example = "SERVICE_SPECIFIC")
    private PromotionType promotionType;

    @NotNull(message = "Discount type is required")
    @Schema(description = "Discount type (PERCENTAGE, FIXED_AMOUNT, FIXED_PRICE)", example = "PERCENTAGE")
    private PromotionDiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.00", message = "Discount value cannot be negative")
    @Schema(description = "Discount value (percentage e.g. 15.00, or flat INR amount)", example = "15.00")
    private BigDecimal discountValue;

    @Schema(description = "Target service if SERVICE_SPECIFIC (e.g. GST_FILING, ITR_FILING)", example = "GST_FILING")
    private BillingServiceType targetService;

    @Schema(description = "Target client ID if CUSTOMER_SPECIFIC")
    private UUID targetClientId;

    @Schema(description = "Start date of promotion validity", example = "2026-09-01")
    private LocalDate validFrom;

    @Schema(description = "End date of promotion validity", example = "2026-10-31")
    private LocalDate validUntil;

    @Schema(description = "Initial active status", example = "true")
    @Builder.Default
    private Boolean active = true;

    @Schema(description = "Priority evaluation order (higher priority evaluated first)", example = "10")
    @Builder.Default
    private Integer priority = 0;

    @Schema(description = "Maximum total allowed uses (optional)", example = "100")
    private Integer maxUses;
}
