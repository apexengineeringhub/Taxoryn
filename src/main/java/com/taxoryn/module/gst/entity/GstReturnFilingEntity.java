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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "gst_return_filings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstReturnFilingEntity extends TenantAuditableEntity {

    @Column(name = "gst_profile_id", nullable = false)
    private UUID gstProfileId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, length = 50)
    private GstReturnType returnType;

    @Column(name = "return_period", nullable = false, length = 50)
    private String returnPeriod;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", nullable = false, length = 50)
    @Builder.Default
    private GstFilingStatus filingStatus = GstFilingStatus.PENDING;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "acknowledgement_number", length = 100)
    private String acknowledgementNumber;

    @Column(name = "total_taxable_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxableValue = BigDecimal.ZERO;

    @Column(name = "total_tax_liability", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxLiability = BigDecimal.ZERO;

    @Column(name = "total_itc_claimed", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalItcClaimed = BigDecimal.ZERO;

    @Column(name = "tax_paid_cash", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxPaidCash = BigDecimal.ZERO;

    @Column(name = "tax_paid_itc", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxPaidItc = BigDecimal.ZERO;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "compliance_id")
    private UUID complianceId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "document_request_id")
    private UUID documentRequestId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum GstReturnType {
        GSTR1,
        GSTR3B,
        GSTR9,
        GSTR9C,
        CMP08,
        GSTR4,
        GSTR7,
        GSTR8
    }

    public enum GstFilingStatus {
        PENDING,
        PREPARED,
        UNDER_REVIEW,
        FILED,
        OVERDUE,
        CANCELLED
    }
}
