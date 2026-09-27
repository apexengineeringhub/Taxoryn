package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import com.taxoryn.module.billing.model.PromotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Promotion Details")
public class PromotionDto {

    private UUID id;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private PromotionType promotionType;
    private PromotionDiscountType discountType;
    private BigDecimal discountValue;
    private BillingServiceType targetService;
    private UUID targetClientId;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Boolean active;
    private Integer priority;
    private Integer maxUses;
    private Integer currentUses;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
