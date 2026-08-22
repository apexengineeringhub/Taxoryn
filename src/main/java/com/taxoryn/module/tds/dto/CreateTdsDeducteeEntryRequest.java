package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.DeducteeType;
import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.ReasonCode;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record a TDS deductee / payee entry")
public class CreateTdsDeducteeEntryRequest {

    @NotNull(message = "TDS Profile ID is required")
    @Schema(description = "TDS Profile ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID tdsProfileId;

    @Schema(description = "TDS Return ID")
    private UUID tdsReturnId;

    @Schema(description = "Challan ID to link")
    private UUID challanId;

    @NotBlank(message = "Deductee PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid 10-character PAN format")
    @Schema(description = "Deductee PAN", example = "ABCPS1234F", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deducteePan;

    @NotBlank(message = "Deductee Name is required")
    @Schema(description = "Deductee Name", example = "Infosys Technologies Ltd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deducteeName;

    @Builder.Default
    @Schema(description = "Deductee Type", example = "NON_COMPANY")
    private DeducteeType deducteeType = DeducteeType.NON_COMPANY;

    @NotBlank(message = "Section Code is required")
    @Schema(description = "TDS Section Code (194C, 194J, 194I, etc.)", example = "194J", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sectionCode;

    @NotNull(message = "Payment/Credit date is required")
    @Schema(description = "Date of Payment or Credit", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate paymentCreditDate;

    @Schema(description = "Invoice / Bill Reference Number")
    private String invoiceRefNumber;

    @NotNull(message = "Amount is required")
    @Schema(description = "Amount Paid or Credited", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amountPaidCredited;

    @NotNull(message = "TDS Rate is required")
    @Schema(description = "TDS Rate applied (%)", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal tdsRate;

    @NotNull(message = "TDS Amount is required")
    @Schema(description = "Calculated TDS Amount", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal tdsAmount;

    @Builder.Default
    @Schema(description = "Surcharge Amount")
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Health & Education Cess Amount (4%)")
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @NotNull(message = "Date of deduction is required")
    @Schema(description = "Date Tax Deducted", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate deductionDate;

    @Schema(description = "Section 197 Lower Deduction Certificate Number")
    private String certificateNumber197;

    @Builder.Default
    @Schema(description = "Reason Code for Lower / Nil deduction", example = "STANDARD")
    private ReasonCode reasonCode = ReasonCode.STANDARD;

    @NotNull(message = "Quarter is required")
    @Schema(description = "Quarter (Q1, Q2, Q3, Q4)", requiredMode = Schema.RequiredMode.REQUIRED)
    private TdsQuarter quarter;

    @NotBlank(message = "Financial Year is required")
    @Schema(description = "Financial Year", example = "2026-27", requiredMode = Schema.RequiredMode.REQUIRED)
    private String financialYear;
}
