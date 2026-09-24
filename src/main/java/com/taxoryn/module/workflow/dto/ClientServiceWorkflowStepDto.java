package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.workflow.model.ServiceWorkType;
import com.taxoryn.module.workflow.model.StepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceWorkflowStepDto {
    private UUID id;
    private UUID organizationId;
    private UUID workflowId;
    private int sequence;
    private ServiceWorkType workType;
    private String name;
    private String description;
    private StepStatus status;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private String assignedEmployeeEmail;
    private UUID taskId;
    private LocalDate dueDate;
    private boolean mandatory;
    private boolean requiresClientInput;
    private boolean requiresReview;
    private Instant completedAt;
    private UUID completedBy;
    private String completedByName;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
