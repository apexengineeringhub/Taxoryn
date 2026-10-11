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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tds_credits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsCreditEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "deductor_pan", nullable = false, length = 10)
    private String deductorPan;

    @Column(name = "deductor_tan", length = 10)
    private String deductorTan;

    @Column(name = "deductor_name", nullable = false)
    private String deductorName;

    @Column(name = "section_code", nullable = false, length = 20)
    @Builder.Default
    private String sectionCode = "194J";

    @Column(name = "assessment_year", nullable = false, length = 20)
    private String assessmentYear;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "quarter", length = 10)
    private String quarter;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "tds_deducted", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tdsDeducted = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    @Builder.Default
    private TdsCreditSourceType sourceType = TdsCreditSourceType.MANUAL_ENTRY;

    @Column(name = "source_document_ref", length = 100)
    private String sourceDocumentRef;

    @Column(name = "certificate_number", length = 100)
    private String certificateNumber;

    @Column(name = "certificate_date")
    private LocalDate certificateDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_status", nullable = false, length = 50)
    @Builder.Default
    private TdsCreditStatus creditStatus = TdsCreditStatus.UNRECONCILED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum TdsCreditSourceType {
        FORM_26AS_IMPORT,
        AIS_IMPORT,
        FORM_16A_UPLOAD,
        MANUAL_ENTRY,
        SIMULATED_DEMO
    }

    public enum TdsCreditStatus {
        UNRECONCILED,
        PARTIALLY_RECONCILED,
        RECONCILED,
        DISCREPANCY
    }
}
