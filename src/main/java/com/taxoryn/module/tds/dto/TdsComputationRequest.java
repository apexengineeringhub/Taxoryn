package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.DeducteeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to compute TDS, interest under 201(1A), and late fee under 234E")
public class TdsComputationRequest {

    @NotBlank(message = "Section code is required")
    @Schema(description = "Section Code (e.g., 194C, 194J, 194I, 192)", example = "194J", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sectionCode;

    @NotNull(message = "Amount is required")
    @Schema(description = "Transaction / Payment Amount", example = "100000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Builder.Default
    @Schema(description = "Deductee Type", example = "NON_COMPANY")
    private DeducteeType deducteeType = DeducteeType.NON_COMPANY;

    @Schema(description = "Valid PAN provided flag", example = "true")
    @Builder.Default
    private boolean validPanProvided = true;

    @Schema(description = "Specified non-filer under Section 206AB flag", example = "false")
    @Builder.Default
    private boolean specifiedNonFiler206AB = false;

    @Schema(description = "Lower deduction certificate rate under Sec 197 (%)")
    private BigDecimal lowerDeductionRate;

    @Schema(description = "Cumulative payments in current financial year")
    @Builder.Default
    private BigDecimal cumulativePaidInYear = BigDecimal.ZERO;

    @Schema(description = "Date of Payment / Credit")
    private LocalDate paymentCreditDate;

    @Schema(description = "Date Tax Deducted")
    private LocalDate deductionDate;

    @Schema(description = "Date Challan Deposited")
    private LocalDate depositDate;

    @Schema(description = "Quarterly Statutory Filing Due Date")
    private LocalDate filingDueDate;

    @Schema(description = "Actual Filing Date")
    private LocalDate actualFilingDate;
}
