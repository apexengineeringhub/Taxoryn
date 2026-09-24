package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.workflow.model.ServicePeriodType;
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
public class GenerateWorkflowRequest {

    @NotNull(message = "Client Service ID is required")
    private UUID clientServiceId;

    // Either existing periodId OR inline period parameters:
    private UUID periodId;
    private ServicePeriodType periodType;
    private String periodLabel;
    private String financialYear;
    private String assessmentYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate dueDate;

    private UUID templateId;
    private String customTitle;
    private TaskPriority priority;
    private UUID assignedEmployeeId;
    private LocalDate internalTargetDate;
}
