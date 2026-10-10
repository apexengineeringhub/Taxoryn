package com.taxoryn.module.billing.entity;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tds_credit_matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsCreditMatchEntity extends TenantAuditableEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "tds_credit_id", nullable = false)
    private UUID tdsCreditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    @Builder.Default
    private TdsMatchStatus matchStatus = TdsMatchStatus.EXACT_MATCH;

    @Column(name = "match_confidence", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal matchConfidence = new BigDecimal("100.00");

    @Column(name = "matched_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal matchedAmount = BigDecimal.ZERO;

    @Column(name = "invoice_subtotal", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal invoiceSubtotal = BigDecimal.ZERO;

    @Column(name = "credit_gross_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal creditGrossAmount = BigDecimal.ZERO;

    @Column(name = "expected_tds_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal expectedTdsRate = new BigDecimal("10.00");

    @Column(name = "expected_tds_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal expectedTdsAmount = BigDecimal.ZERO;

    @Column(name = "actual_tds_deducted", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal actualTdsDeducted = BigDecimal.ZERO;

    @Column(name = "rate_discrepancy", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal rateDiscrepancy = BigDecimal.ZERO;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "reconciled_at", nullable = false)
    @Builder.Default
    private Instant reconciledAt = Instant.now();

    @Column(name = "reconciled_by")
    private String reconciledBy;

    public enum TdsMatchStatus {
        EXACT_MATCH,
        RATE_DISCREPANCY,
        AMOUNT_MISMATCH,
        UNMATCHED_INVOICE,
        UNMATCHED_CREDIT,
        TIMING_DIFFERENCE
    }
}
