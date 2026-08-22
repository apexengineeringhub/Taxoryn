package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsChallanEntity.MajorHead;
import com.taxoryn.module.tds.entity.TdsChallanEntity.MinorHead;
import com.taxoryn.module.tds.entity.TdsChallanEntity.PaymentMode;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to record a TDS Challan ITNS 281 deposit")
public class CreateTdsChallanRequest {

    @NotNull(message = "TDS Profile ID is required")
    @Schema(description = "TDS Profile ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID tdsProfileId;

    @Schema(description = "Linked TDS Return ID (optional)")
    private UUID tdsReturnId;

    @NotBlank(message = "BSR code is required")
    @Schema(description = "7-digit BSR Code of Bank Branch", example = "0510304", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bsrCode;

    @NotNull(message = "Challan tender date is required")
    @Schema(description = "Challan Payment Date", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate challanDate;

    @NotBlank(message = "Challan serial number is required")
    @Schema(description = "5-digit Challan Serial Number", example = "00125", requiredMode = Schema.RequiredMode.REQUIRED)
    private String challanSerialNo;

    @Schema(description = "CIN (auto-computed if omitted)")
    private String cin;

    @Builder.Default
    @Schema(description = "Major Head", example = "HEAD_0021_NON_COMPANY")
    private MajorHead majorHead = MajorHead.HEAD_0021_NON_COMPANY;

    @Builder.Default
    @Schema(description = "Minor Head", example = "HEAD_200_PAYABLE_BY_TAXPAYER")
    private MinorHead minorHead = MinorHead.HEAD_200_PAYABLE_BY_TAXPAYER;

    @NotBlank(message = "Section code is required")
    @Schema(description = "TDS Section (e.g., 194C, 194J, 192)", example = "194C", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sectionCode;

    @NotNull(message = "TDS Amount is required")
    @Schema(description = "Income Tax / TDS Amount", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal tdsAmount;

    @Builder.Default
    @Schema(description = "Surcharge Amount")
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Health & Education Cess (4%)")
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Interest under Sec 201(1A)")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Late Fee under Sec 234E")
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Penalty")
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @NotNull(message = "Quarter is required")
    @Schema(description = "Quarter (Q1, Q2, Q3, Q4)", requiredMode = Schema.RequiredMode.REQUIRED)
    private TdsQuarter quarter;

    @NotBlank(message = "Financial Year is required")
    @Schema(description = "Financial Year", example = "2026-27", requiredMode = Schema.RequiredMode.REQUIRED)
    private String financialYear;

    @Builder.Default
    @Schema(description = "Payment Mode", example = "NET_BANKING")
    private PaymentMode paymentMode = PaymentMode.NET_BANKING;

    @Schema(description = "Bank Name")
    private String bankName;

    @Schema(description = "Notes")
    private String notes;
}
