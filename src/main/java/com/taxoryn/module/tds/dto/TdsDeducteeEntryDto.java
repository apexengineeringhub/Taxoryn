package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.DeducteeEntryStatus;
import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.DeducteeType;
import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.ReasonCode;
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
@Schema(description = "TDS Deductee / Payee Entry")
public class TdsDeducteeEntryDto {

    @Schema(description = "Entry ID")
    private UUID id;

    @Schema(description = "TDS Profile ID")
    private UUID tdsProfileId;

    @Schema(description = "TDS Return ID")
    private UUID tdsReturnId;

    @Schema(description = "Linked Challan ID")
    private UUID challanId;

    @Schema(description = "Deductee PAN", example = "ABCPS1234F")
    private String deducteePan;

    @Schema(description = "Deductee Legal / Trade Name")
    private String deducteeName;

    @Schema(description = "Deductee Type", example = "NON_COMPANY")
    private DeducteeType deducteeType;

    @Schema(description = "TDS Section Code", example = "194J")
    private String sectionCode;

    @Schema(description = "Date of Payment / Credit")
    private LocalDate paymentCreditDate;

    @Schema(description = "Invoice / Bill Reference Number")
    private String invoiceRefNumber;

    @Schema(description = "Amount Paid or Credited")
    private BigDecimal amountPaidCredited;

    @Schema(description = "TDS Rate (%)", example = "10.00")
    private BigDecimal tdsRate;

    @Schema(description = "TDS Amount")
    private BigDecimal tdsAmount;

    @Schema(description = "Surcharge Amount")
    private BigDecimal surchargeAmount;

    @Schema(description = "Health & Education Cess (4%)")
    private BigDecimal cessAmount;

    @Schema(description = "Total Tax Deducted")
    private BigDecimal totalTaxDeducted;

    @Schema(description = "Date of Tax Deduction")
    private LocalDate deductionDate;

    @Schema(description = "Lower Deduction Certificate Number (Sec 197)")
    private String certificateNumber197;

    @Schema(description = "Reason for Lower / Nil / Non-Deduction", example = "STANDARD")
    private ReasonCode reasonCode;

    @Schema(description = "Quarter", example = "Q1")
    private TdsQuarter quarter;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Status", example = "ACTIVE")
    private DeducteeEntryStatus status;

    @Schema(description = "Created Timestamp")
    private Instant createdAt;

    @Schema(description = "Updated Timestamp")
    private Instant updatedAt;
}
