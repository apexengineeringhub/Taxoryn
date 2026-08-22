package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "marketplace_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceServiceEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 50)
    @Builder.Default
    private PricingType pricingType = PricingType.FIXED;

    @Column(name = "delivery_days")
    @Builder.Default
    private Integer deliveryDays = 3;

    @Column(name = "deliverables", columnDefinition = "TEXT")
    private String deliverables;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public enum PricingType {
        FIXED,
        MONTHLY_RETAINER,
        HOURLY,
        CUSTOM
    }
}
