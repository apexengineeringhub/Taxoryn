package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsChallanEntity.ChallanStatus;
import com.taxoryn.module.tds.entity.TdsChallanEntity.MajorHead;
import com.taxoryn.module.tds.entity.TdsChallanEntity.MinorHead;
import com.taxoryn.module.tds.entity.TdsChallanEntity.PaymentMode;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update an existing TDS Challan record")
public class UpdateTdsChallanRequest {

    @Schema(description = "Linked TDS Return ID")
    private UUID tdsReturnId;

    @Schema(description = "BSR Code")
    private String bsrCode;

    @Schema(description = "Challan Tender Date")
    private LocalDate challanDate;

    @Schema(description = "Challan Serial Number")
    private String challanSerialNo;

    @Schema(description = "Major Head")
    private MajorHead majorHead;

    @Schema(description = "Minor Head")
    private MinorHead minorHead;

    @Schema(description = "TDS Section Code")
    private String sectionCode;

    @Schema(description = "TDS Amount")
    private BigDecimal tdsAmount;

    @Schema(description = "Surcharge Amount")
    private BigDecimal surchargeAmount;

    @Schema(description = "Health & Education Cess")
    private BigDecimal cessAmount;

    @Schema(description = "Interest Amount")
    private BigDecimal interestAmount;

    @Schema(description = "Fee Amount")
    private BigDecimal feeAmount;

    @Schema(description = "Penalty Amount")
    private BigDecimal penaltyAmount;

    @Schema(description = "Utilized Amount")
    private BigDecimal utilizedAmount;

    @Schema(description = "Challan Utilization Status")
    private ChallanStatus challanStatus;

    @Schema(description = "Payment Mode")
    private PaymentMode paymentMode;

    @Schema(description = "Bank Name")
    private String bankName;

    @Schema(description = "Notes")
    private String notes;
}
