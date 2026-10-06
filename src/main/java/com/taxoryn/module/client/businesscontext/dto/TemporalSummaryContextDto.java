package com.taxoryn.module.client.businesscontext.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Temporal Context Model (When is it relevant or due?)")
public class TemporalSummaryContextDto {

    @Schema(description = "Current server resolution date")
    private LocalDate currentDate;

    @Schema(description = "Relevant period descriptor (e.g. FY 2026-27, Q1, SEP-2026)")
    private String relevantPeriod;

    @Schema(description = "Effective start date across the resolved context hierarchy")
    private LocalDate effectiveStartDate;

    @Schema(description = "Effective operational due date across the resolved context hierarchy")
    private LocalDate effectiveDueDate;

    @Schema(description = "Statutory government filing deadline if applicable")
    private LocalDate statutoryDueDate;

    @Schema(description = "Whether the resolved work or engagement is past its due date")
    private Boolean isOverdue;
}
