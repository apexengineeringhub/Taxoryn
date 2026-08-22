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
@Table(name = "tds_challans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsChallanEntity extends TenantAuditableEntity {

    @Column(name = "tds_profile_id", nullable = false)
    private UUID tdsProfileId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    @Column(name = "bsr_code", nullable = false, length = 10)
    private String bsrCode;

    @Column(name = "challan_date", nullable = false)
    private LocalDate challanDate;

    @Column(name = "challan_serial_no", nullable = false, length = 10)
    private String challanSerialNo;

    @Column(name = "cin", length = 50)
    private String cin;

    @Enumerated(EnumType.STRING)
    @Column(name = "major_head", nullable = false, length = 50)
    @Builder.Default
    private MajorHead majorHead = MajorHead.HEAD_0021_NON_COMPANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "minor_head", nullable = false, length = 50)
    @Builder.Default
    private MinorHead minorHead = MinorHead.HEAD_200_PAYABLE_BY_TAXPAYER;

    @Column(name = "section_code", nullable = false, length = 20)
    private String sectionCode;

    @Column(name = "tds_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(name = "surcharge_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Column(name = "cess_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "utilized_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "challan_status", nullable = false, length = 50)
    @Builder.Default
    private ChallanStatus challanStatus = ChallanStatus.UNUTILIZED;

    @Enumerated(EnumType.STRING)
    @Column(name = "quarter", nullable = false, length = 10)
    private TdsQuarter quarter;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 50)
    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.NET_BANKING;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum MajorHead {
        HEAD_0020_COMPANY,      // Corporation Tax (Companies)
        HEAD_0021_NON_COMPANY   // Income-Tax Other Than Companies
    }

    public enum MinorHead {
        HEAD_200_PAYABLE_BY_TAXPAYER,   // TDS/TCS Regular self-deposit
        HEAD_400_REGULAR_ASSESSMENT     // TDS/TCS on demand / assessment
    }

    public enum ChallanStatus {
        UNUTILIZED,
        PARTIALLY_UTILIZED,
        FULLY_UTILIZED,
        OVERUTILIZED
    }

    public enum PaymentMode {
        NET_BANKING,
        DEBIT_CARD,
        OVER_THE_COUNTER,
        NEFT_RTGS
    }
}
