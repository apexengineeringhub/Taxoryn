package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsChallanEntity.ChallanStatus;
import com.taxoryn.module.tds.entity.TdsChallanEntity.MajorHead;
import com.taxoryn.module.tds.entity.TdsChallanEntity.MinorHead;
import com.taxoryn.module.tds.entity.TdsChallanEntity.PaymentMode;
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
@Schema(description = "TDS Challan ITNS 281 Record & Reconciliation")
public class TdsChallanDto {

    @Schema(description = "Challan ID")
    private UUID id;

    @Schema(description = "TDS Profile ID")
    private UUID tdsProfileId;

    @Schema(description = "TAN Number", example = "BLRP12345A")
    private String tan;

    @Schema(description = "Client Display Name")
    private String clientName;

    @Schema(description = "Linked TDS Return ID")
    private UUID tdsReturnId;

    @Schema(description = "BSR Code (7 digits)", example = "0510304")
    private String bsrCode;

    @Schema(description = "Challan Tender Date")
    private LocalDate challanDate;

    @Schema(description = "Challan Serial Number (5 digits)", example = "00125")
    private String challanSerialNo;

    @Schema(description = "Challan Identification Number (CIN)", example = "05103042026070700125")
    private String cin;

    @Schema(description = "Major Head", example = "HEAD_0021_NON_COMPANY")
    private MajorHead majorHead;

    @Schema(description = "Minor Head", example = "HEAD_200_PAYABLE_BY_TAXPAYER")
    private MinorHead minorHead;

    @Schema(description = "TDS Section Code", example = "194C")
    private String sectionCode;

    @Schema(description = "Income Tax / TDS Amount")
    private BigDecimal tdsAmount;

    @Schema(description = "Surcharge Amount")
    private BigDecimal surchargeAmount;

    @Schema(description = "Health & Education Cess (4%)")
    private BigDecimal cessAmount;

    @Schema(description = "Interest Amount under Sec 201(1A)")
    private BigDecimal interestAmount;

    @Schema(description = "Late Fee under Sec 234E")
    private BigDecimal feeAmount;

    @Schema(description = "Penalty Amount")
    private BigDecimal penaltyAmount;

    @Schema(description = "Total Challan Paid Amount")
    private BigDecimal totalAmount;

    @Schema(description = "Amount Utilized against Deductee entries")
    private BigDecimal utilizedAmount;

    @Schema(description = "Remaining Unutilized Balance")
    private BigDecimal balanceAmount;

    @Schema(description = "Challan Utilization Status", example = "UNUTILIZED")
    private ChallanStatus challanStatus;

    @Schema(description = "Quarter", example = "Q1")
    private TdsQuarter quarter;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Payment Mode", example = "NET_BANKING")
    private PaymentMode paymentMode;

    @Schema(description = "Bank Name")
    private String bankName;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Created Timestamp")
    private Instant createdAt;

    @Schema(description = "Updated Timestamp")
    private Instant updatedAt;
}
