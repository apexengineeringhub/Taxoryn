package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsReturnEntity.FvuValidationStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TDS / TCS Return Filing Record")
public class TdsReturnDto {

    @Schema(description = "Return Filing ID")
    private UUID id;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client Display Name")
    private String clientName;

    @Schema(description = "TDS Profile ID")
    private UUID tdsProfileId;

    @Schema(description = "TAN Number", example = "BLRP12345A")
    private String tan;

    @Schema(description = "TDS Return Form Type", example = "FORM_26Q")
    private TdsFormType formType;

    @Schema(description = "Quarter", example = "Q1")
    private TdsQuarter quarter;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Assessment Year", example = "2027-28")
    private String assessmentYear;

    @Schema(description = "Statutory Due Date")
    private LocalDate dueDate;

    @Schema(description = "Filing Workflow Status", example = "PENDING")
    private TdsFilingStatus filingStatus;

    @Schema(description = "Date Return Filed on Portal")
    private LocalDate filingDate;

    @Schema(description = "Provisional Receipt Number (Token / PRN)", example = "010020304050607")
    private String tokenNumber;

    @Schema(description = "Portal Acknowledgement Receipt Reference")
    private String receiptNumber;

    @Schema(description = "Total Amount Paid / Credited across deductees")
    private BigDecimal totalAmountPaid;

    @Schema(description = "Total TDS Deducted across deductees")
    private BigDecimal totalTaxDeducted;

    @Schema(description = "Total Tax Deposited via Challans")
    private BigDecimal totalTaxDeposited;

    @Schema(description = "Interest under Section 201(1A)")
    private BigDecimal totalInterest;

    @Schema(description = "Late filing fee under Section 234E")
    private BigDecimal totalLateFee;

    @Schema(description = "Penalty amount")
    private BigDecimal totalPenalty;

    @Schema(description = "Assigned Employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned Employee Name")
    private String assignedEmployeeName;

    @Schema(description = "Linked Statutory Compliance Obligation ID")
    private UUID complianceId;

    @Schema(description = "Linked Practice Task ID")
    private UUID taskId;

    @Schema(description = "Linked Multi-Item Document Request ID")
    private UUID documentRequestId;

    @Schema(description = "FVU Validation Status", example = "NOT_VALIDATED")
    private FvuValidationStatus fvuValidationStatus;

    @Schema(description = "Practitioner Notes")
    private String notes;

    @Schema(description = "Created Timestamp")
    private Instant createdAt;

    @Schema(description = "Updated Timestamp")
    private Instant updatedAt;
}
