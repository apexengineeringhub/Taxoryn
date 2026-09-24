package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.workflow.model.StepStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowStepStatusRequest {

    @NotNull(message = "Step status is required")
    private StepStatus status;

    private String notes;
    private String clientActionSummary;
}
