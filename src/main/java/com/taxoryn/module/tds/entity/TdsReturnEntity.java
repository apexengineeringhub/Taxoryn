package com.taxoryn.module.tds.entity;

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
@Table(name = "tds_returns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsReturnEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "tds_profile_id", nullable = false)
    private UUID tdsProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_type", nullable = false, length = 50)
    private TdsFormType formType;

    @Enumerated(EnumType.STRING)
    @Column(name = "quarter", nullable = false, length = 10)
    private TdsQuarter quarter;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "assessment_year", nullable = false, length = 20)
    private String assessmentYear;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", nullable = false, length = 50)
    @Builder.Default
    private TdsFilingStatus filingStatus = TdsFilingStatus.PENDING;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "token_number", length = 20)
    private String tokenNumber;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "total_amount_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountPaid = BigDecimal.ZERO;

    @Column(name = "total_tax_deducted", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxDeducted = BigDecimal.ZERO;

    @Column(name = "total_tax_deposited", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxDeposited = BigDecimal.ZERO;

    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Column(name = "total_late_fee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLateFee = BigDecimal.ZERO;

    @Column(name = "total_penalty", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPenalty = BigDecimal.ZERO;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "compliance_id")
    private UUID complianceId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "document_request_id")
    private UUID documentRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fvu_validation_status", nullable = false, length = 50)
    @Builder.Default
    private FvuValidationStatus fvuValidationStatus = FvuValidationStatus.NOT_VALIDATED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum TdsFormType {
        FORM_24Q,   // TDS on Salaries (Sec 192)
        FORM_26Q,   // Non-Salary Resident TDS (Sec 193 to 194S)
        FORM_27Q,   // TDS on Non-Residents (Sec 195, etc.)
        FORM_27EQ,  // TCS (Sec 206C)
        FORM_26QB,  // TDS on Property Purchase (Sec 194-IA)
        FORM_26QC,  // TDS on Rent by Individual/HUF (Sec 194-IB)
        FORM_26QD,  // TDS on Contractor/Prof payments by Ind/HUF (Sec 194M)
        FORM_26QE   // TDS on VDA / Crypto (Sec 194S)
    }

    public enum TdsQuarter {
        Q1, // Apr - Jun
        Q2, // Jul - Sep
        Q3, // Oct - Dec
        Q4  // Jan - Mar
    }

    public enum TdsFilingStatus {
        PENDING,
        DRAFT,
        CHALLANS_ATTACHED,
        UNDER_REVIEW,
        READY_TO_FILE,
        FILED,
        OVERDUE,
        CANCELLED
    }

    public enum FvuValidationStatus {
        NOT_VALIDATED,
        VALIDATED,
        FAILED
    }
}
