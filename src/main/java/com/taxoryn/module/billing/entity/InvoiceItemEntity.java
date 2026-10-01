package com.taxoryn.module.billing.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "work_item_id")
    private java.util.UUID workItemId;

    @Column(name = "time_entry_id")
    private java.util.UUID timeEntryId;

    @Column(name = "service_id")
    private java.util.UUID serviceId;

    @Column(name = "catalog_service_code", length = 100)
    private String catalogServiceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "service", nullable = false, length = 50)
    @Builder.Default
    private BillingServiceType service = BillingServiceType.CONSULTING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("18.00");

    @Column(name = "tax", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "promotion_id")
    private java.util.UUID promotionId;

    @Column(name = "promotion_name")
    private String promotionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", length = 50)
    @Builder.Default
    private com.taxoryn.module.billing.model.PricingType pricingType = com.taxoryn.module.billing.model.PricingType.STANDARD;

    @Column(name = "standard_unit_price", precision = 15, scale = 2)
    private BigDecimal standardUnitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 50)
    private com.taxoryn.module.billing.model.PromotionDiscountType discountType;

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    public enum BillingServiceType {
        GST_FILING,
        ITR_FILING,
        TDS,
        ACCOUNTING,
        CONSULTING,
        AUDIT,
        ROC_COMPLIANCE,
        OTHER
    }
}
