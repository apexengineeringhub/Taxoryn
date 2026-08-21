package com.taxoryn.module.itr.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Batch Generate ITR Returns across Firm Payload")
public class BatchGenerateItrReturnsRequest {

    @NotBlank(message = "Assessment year is required")
    @JsonAlias({"ay", "assessment_year"})
    @Schema(description = "Target Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @NotBlank(message = "Financial year is required")
    @JsonAlias({"fy", "financial_year"})
    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @JsonAlias({"itrTypes", "forms", "returnTypes"})
    @Schema(description = "Filter by specific ITR Forms (optional, generates for all if omitted)")
    private List<ItrType> itrTypes;

    @JsonAlias({"dueDate", "statutoryDueDate"})
    @Schema(description = "Statutory Due Date for Non-Audit (e.g. 2026-07-31)")
    private LocalDate nonAuditDueDate;

    @JsonAlias({"auditDueDate"})
    @Schema(description = "Statutory Due Date for Tax Audit / Corporate (e.g. 2026-10-31)")
    private LocalDate auditDueDate;
}
