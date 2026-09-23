package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowStatusRequest {

    @NotNull(message = "Target status is required")
    private ServiceWorkflowStatus status;

    private String notes;
    private String pendingClientActionSummary;
}
