package com.taxoryn.module.tds.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
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
@Table(name = "tds_deductee_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsDeducteeEntryEntity extends TenantAuditableEntity {

    @Column(name = "tds_profile_id", nullable = false)
    private UUID tdsProfileId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    @Column(name = "challan_id")
    private UUID challanId;

    @Column(name = "deductee_pan", nullable = false, length = 10)
    private String deducteePan;

    @Column(name = "deductee_name", nullable = false)
    private String deducteeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "deductee_type", nullable = false, length = 50)
    @Builder.Default
    private DeducteeType deducteeType = DeducteeType.NON_COMPANY;

    @Column(name = "section_code", nullable = false, length = 20)
    private String sectionCode;

    @Column(name = "payment_credit_date", nullable = false)
    private LocalDate paymentCreditDate;

    @Column(name = "invoice_ref_number", length = 100)
    private String invoiceRefNumber;

    @Column(name = "amount_paid_credited", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaidCredited = BigDecimal.ZERO;

    @Column(name = "tds_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tdsRate = BigDecimal.ZERO;

    @Column(name = "tds_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(name = "surcharge_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Column(name = "cess_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "total_tax_deducted", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxDeducted = BigDecimal.ZERO;

    @Column(name = "deduction_date", nullable = false)
    private LocalDate deductionDate;

    @Column(name = "certificate_number_197", length = 50)
    private String certificateNumber197;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 50)
    @Builder.Default
    private ReasonCode reasonCode = ReasonCode.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "quarter", nullable = false, length = 10)
    private TdsQuarter quarter;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DeducteeEntryStatus status = DeducteeEntryStatus.ACTIVE;

    public enum DeducteeType {
        COMPANY,
        NON_COMPANY
    }

    public enum ReasonCode {
        STANDARD,               // Normal deduction
        LOWER_RATE_197,         // Lower deduction cert under sec 197
        NIL_RATE_197,           // Nil deduction cert under sec 197
        FORM_15G_15H,           // Form 15G / 15H submitted
        TRANSPORTER_194C,       // Transporter declaration with PAN
        THRESHOLD_EXEMPTION,    // Amount below threshold
        HIGHER_RATE_206AA,      // Non-availability of PAN (20%)
        HIGHER_RATE_206AB       // Non-filer of ITR (Sec 206AB/206CCA)
    }

    public enum DeducteeEntryStatus {
        ACTIVE,
        REVERSED
    }
}
