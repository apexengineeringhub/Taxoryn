package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Indian Income Tax TDS / TCS Section Rate Configuration")
public class TdsSectionRateDto {

    @Schema(description = "Section Code", example = "194C")
    private String sectionCode;

    @Schema(description = "Section Title / Nature of Payment", example = "Payments to Contractors / Sub-contractors")
    private String title;

    @Schema(description = "Applicable Return Form", example = "FORM_26Q")
    private String returnForm;

    @Schema(description = "Individual / HUF Payee Rate (%)", example = "1.00")
    private BigDecimal rateIndividual;

    @Schema(description = "Company / Other Payee Rate (%)", example = "2.00")
    private BigDecimal rateOthers;

    @Schema(description = "Annual Exemption / Threshold Limit (INR)", example = "100000.00")
    private BigDecimal thresholdLimit;

    @Schema(description = "Single Transaction Threshold Limit (INR)", example = "30000.00")
    private BigDecimal singleTransactionLimit;

    @Schema(description = "Applicable Section 206AA rate if PAN missing (%)", example = "20.00")
    private BigDecimal nonPanRate;

    @Schema(description = "Detailed Statutory Notes and Conditions")
    private String statutoryNotes;
}
