package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity.PricingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Marketplace Fixed/Retainer Service Package DTO")
public class MarketplaceServiceDto {

    private UUID id;
    private UUID organizationId;
    private UUID marketplaceProfileId;
    private String title;
    private String category;
    private String description;
    private BigDecimal price;
    private PricingType pricingType;
    private Integer deliveryDays;
    private String deliverables;
    private Boolean isActive;
}
