package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to batch generate TDS returns for all active TAN clients")
public class BatchGenerateTdsReturnsRequest {

    @NotNull(message = "Quarter is required")
    @Schema(description = "Target Quarter (Q1, Q2, Q3, Q4)", requiredMode = Schema.RequiredMode.REQUIRED)
    private TdsQuarter quarter;

    @NotBlank(message = "Financial Year is required")
    @Schema(description = "Target Financial Year (e.g., 2026-27)", example = "2026-27", requiredMode = Schema.RequiredMode.REQUIRED)
    private String financialYear;

    @Schema(description = "Assessment Year (auto-computed if omitted)")
    private String assessmentYear;

    @Schema(description = "Specific form types to generate (defaults to Form 24Q and Form 26Q)")
    private List<TdsFormType> formTypes;

    @Schema(description = "Statutory Due Date")
    private LocalDate dueDate;
}
