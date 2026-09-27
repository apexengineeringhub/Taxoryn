package com.taxoryn.module.billing.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import com.taxoryn.module.billing.model.PromotionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionEntity extends TenantAuditableEntity {

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 50)
    private PromotionType promotionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 50)
    private PromotionDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_service", length = 50)
    private BillingServiceType targetService;

    @Column(name = "target_client_id")
    private UUID targetClientId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Builder.Default
    private Integer currentUses = 0;

    public boolean isCurrentlyValid(LocalDate date) {
        if (Boolean.FALSE.equals(active)) {
            return false;
        }
        if (date != null) {
            if (validFrom != null && date.isBefore(validFrom)) {
                return false;
            }
            if (validUntil != null && date.isAfter(validUntil)) {
                return false;
            }
        }
        if (maxUses != null && currentUses >= maxUses) {
            return false;
        }
        return true;
    }
}
