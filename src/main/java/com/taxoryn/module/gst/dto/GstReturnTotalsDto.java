package com.taxoryn.module.gst.dto;

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
@Schema(description = "Aggregated monetary and tax totals for a GST return")
public class GstReturnTotalsDto {

    @Schema(description = "Total taxable value of outward / inward supplies", example = "100000.00")
    @Builder.Default
    private BigDecimal taxableValue = BigDecimal.ZERO;

    @Schema(description = "Integrated Goods and Services Tax (IGST)", example = "18000.00")
    @Builder.Default
    private BigDecimal igst = BigDecimal.ZERO;

    @Schema(description = "Central Goods and Services Tax (CGST)", example = "0.00")
    @Builder.Default
    private BigDecimal cgst = BigDecimal.ZERO;

    @Schema(description = "State/UT Goods and Services Tax (SGST)", example = "0.00")
    @Builder.Default
    private BigDecimal sgst = BigDecimal.ZERO;

    @Schema(description = "GST Compensation Cess", example = "0.00")
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    @Schema(description = "Total tax liability (IGST + CGST + SGST + Cess)", example = "18000.00")
    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Schema(description = "Total Input Tax Credit (ITC) eligible / claimed", example = "5000.00")
    @Builder.Default
    private BigDecimal totalItc = BigDecimal.ZERO;

    public static GstReturnTotalsDto of(BigDecimal taxableValue, BigDecimal igst, BigDecimal cgst, BigDecimal sgst, BigDecimal cess, BigDecimal itc) {
        BigDecimal tv = taxableValue != null ? taxableValue : BigDecimal.ZERO;
        BigDecimal i = igst != null ? igst : BigDecimal.ZERO;
        BigDecimal c = cgst != null ? cgst : BigDecimal.ZERO;
        BigDecimal s = sgst != null ? sgst : BigDecimal.ZERO;
        BigDecimal cs = cess != null ? cess : BigDecimal.ZERO;
        BigDecimal it = itc != null ? itc : BigDecimal.ZERO;
        BigDecimal tt = i.add(c).add(s).add(cs);

        return GstReturnTotalsDto.builder()
                .taxableValue(tv)
                .igst(i)
                .cgst(c)
                .sgst(s)
                .cess(cs)
                .totalTax(tt)
                .totalItc(it)
                .build();
    }
}
