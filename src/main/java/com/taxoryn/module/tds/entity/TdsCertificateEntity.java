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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tds_certificates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsCertificateEntity extends TenantAuditableEntity {

    @Column(name = "tds_profile_id", nullable = false)
    private UUID tdsProfileId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 50)
    @Builder.Default
    private CertificateType certificateType = CertificateType.FORM_16A;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "quarter", length = 10)
    private TdsQuarter quarter;

    @Column(name = "deductee_pan", nullable = false, length = 10)
    private String deducteePan;

    @Column(name = "deductee_name", nullable = false)
    private String deducteeName;

    @Column(name = "traces_request_number", length = 50)
    private String tracesRequestNumber;

    @Column(name = "certificate_number", length = 100)
    private String certificateNumber;

    @Column(name = "generation_date")
    private LocalDate generationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 50)
    @Builder.Default
    private DispatchStatus dispatchStatus = DispatchStatus.PENDING;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum CertificateType {
        FORM_16_PART_A, // Annual Salary TDS (TRACES)
        FORM_16_PART_B, // Salary computation Annexure
        FORM_16A,       // Non-salary quarterly TDS
        FORM_27D        // TCS Certificate
    }

    public enum DispatchStatus {
        PENDING,
        REQUESTED_FROM_TRACES,
        DOWNLOADED,
        DIGITALLY_SIGNED,
        SENT_TO_CLIENT,
        SENT_TO_DEDUCTEE
    }
}
