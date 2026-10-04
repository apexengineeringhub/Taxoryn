package com.taxoryn.module.tds.dto;

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
@Schema(description = "Challan Reconciliation Status DTO")
public class TdsChallanReconciliationDto {

    @Schema(description = "Internal Challan Record ID")
    private UUID challanId;

    @Schema(description = "BSR Code of the bank branch", example = "0210001")
    private String bsrCode;

    @Schema(description = "Challan Serial Number", example = "10023")
    private String challanSerialNo;

    @Schema(description = "Challan Identification Number (CIN)", example = "02100011504202610023")
    private String cin;

    @Schema(description = "Challan payment date")
    private LocalDate challanDate;

    @Schema(description = "Challan Total Tax Amount Deposited")
    private BigDecimal amount;

    @Schema(description = "Authoritative Reconciliation Status (MATCHED, UNMATCHED, PENDING, RECONCILED)", example = "MATCHED")
    private String status;

    @Schema(description = "Reconciliation remarks or discrepancy details")
    private String remarks;
}
