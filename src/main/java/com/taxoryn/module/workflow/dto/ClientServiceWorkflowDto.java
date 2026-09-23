package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceWorkflowDto {
    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private String clientPan;
    private UUID clientServiceId;
    private String serviceType;
    private String serviceName;
    private UUID periodId;
    private String periodLabel;
    private String financialYear;
    private String assessmentYear;
    private UUID templateId;
    private String templateName;
    private String title;
    private ServiceWorkflowStatus status;
    private TaskPriority priority;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private String assignedEmployeeEmail;
    private int currentStepSequence;
    private String currentStepName;
    private int totalSteps;
    private int completedSteps;
    private double progressPercentage;
    private LocalDate dueDate;
    private LocalDate internalTargetDate;
    private boolean waitingForClient;
    private String pendingClientActionSummary;
    private boolean isOverdue;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    @Builder.Default
    private List<ClientServiceWorkflowStepDto> steps = new ArrayList<>();
}
