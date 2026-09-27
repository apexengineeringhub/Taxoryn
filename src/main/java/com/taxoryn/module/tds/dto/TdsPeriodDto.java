package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TDS Compliance Period Details")
public class TdsPeriodDto {

    @Schema(description = "Financial Year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @Schema(description = "Compliance Quarter (Q1, Q2, Q3, Q4)", example = "Q1")
    private String quarter;

    @Schema(description = "Period Label (e.g. FY 2026-27 Q1)", example = "FY 2026-27 Q1")
    private String periodLabel;

    @Schema(description = "Applicable Return Form Types", example = "[\"FORM_24Q\", \"FORM_26Q\", \"FORM_27Q\", \"FORM_27EQ\"]")
    private List<String> returnTypes;

    @Schema(description = "Statutory Filing Due Date")
    private LocalDate statutoryDueDate;

    @Schema(description = "Common TDS Sections", example = "[\"192\", \"194A\", \"194C\", \"194H\", \"194I\", \"194J\", \"194Q\", \"195\"]")
    private List<String> commonSections;
}
