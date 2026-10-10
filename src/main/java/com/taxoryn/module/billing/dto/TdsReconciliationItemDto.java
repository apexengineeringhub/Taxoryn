package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.TdsCreditEntity.TdsCreditSourceType;
import com.taxoryn.module.billing.entity.TdsCreditMatchEntity.TdsMatchStatus;
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
@Schema(description = "Individual TDS Credit Reconciliation Line Item")
public class TdsReconciliationItemDto {

    private UUID matchId;
    private UUID invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal invoiceSubtotal;
    private BigDecimal invoiceTotal;
    private BigDecimal cashReceived;
    private BigDecimal receivableBalance;
    private String invoiceStatus;

    private UUID clientId;
    private String clientName;
    private String clientPan;

    private UUID tdsCreditId;
    private String deductorPan;
    private String deductorTan;
    private String deductorName;
    private String sectionCode;
    private String financialYear;
    private String quarter;
    private BigDecimal creditGrossAmount;
    private BigDecimal actualTdsDeducted;
    private BigDecimal expectedTdsRate;
    private BigDecimal expectedTdsAmount;
    private BigDecimal rateDiscrepancy;
    private TdsCreditSourceType sourceType;
    private String certificateNumber;

    private TdsMatchStatus matchStatus;
    private BigDecimal matchConfidence;
    private String remarks;
    private Instant reconciledAt;
}
