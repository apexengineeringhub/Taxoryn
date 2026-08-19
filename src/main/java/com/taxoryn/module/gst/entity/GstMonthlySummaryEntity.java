package com.taxoryn.module.gst.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
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
import java.util.UUID;

@Entity
@Table(name = "gst_monthly_summaries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstMonthlySummaryEntity extends TenantAuditableEntity {

    @Column(name = "gst_profile_id", nullable = false)
    private UUID gstProfileId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "period", nullable = false, length = 50)
    private String period;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "total_sales_taxable", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesTaxable = BigDecimal.ZERO;

    @Column(name = "igst_sales", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal igstSales = BigDecimal.ZERO;

    @Column(name = "cgst_sales", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cgstSales = BigDecimal.ZERO;

    @Column(name = "sgst_sales", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal sgstSales = BigDecimal.ZERO;

    @Column(name = "cess_sales", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cessSales = BigDecimal.ZERO;

    @Column(name = "total_purchase_taxable", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPurchaseTaxable = BigDecimal.ZERO;

    @Column(name = "igst_purchase", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal igstPurchase = BigDecimal.ZERO;

    @Column(name = "cgst_purchase", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cgstPurchase = BigDecimal.ZERO;

    @Column(name = "sgst_purchase", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal sgstPurchase = BigDecimal.ZERO;

    @Column(name = "cess_purchase", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cessPurchase = BigDecimal.ZERO;

    @Column(name = "itc_eligible", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itcEligible = BigDecimal.ZERO;

    @Column(name = "itc_ineligible", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itcIneligible = BigDecimal.ZERO;

    @Column(name = "itc_reversed", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itcReversed = BigDecimal.ZERO;

    @Column(name = "itc_net_claimed", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itcNetClaimed = BigDecimal.ZERO;

    @Column(name = "net_tax_liability", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netTaxLiability = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "challan_status", nullable = false, length = 50)
    @Builder.Default
    private ChallanStatus challanStatus = ChallanStatus.NOT_GENERATED;

    @Column(name = "challan_cprn", length = 100)
    private String challanCprn;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ChallanStatus {
        NOT_GENERATED,
        GENERATED,
        PAID
    }
}
