package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.workflow.model.ServicePeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServicePeriodRequest {

    @NotNull(message = "Client Service ID is required")
    private UUID clientServiceId;

    @NotNull(message = "Period type is required")
    private ServicePeriodType periodType;

    @NotBlank(message = "Period label is required (e.g., 'July 2026', 'Q2 FY 2026-27')")
    private String periodLabel;

    private String financialYear;
    private String assessmentYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate dueDate;
}
