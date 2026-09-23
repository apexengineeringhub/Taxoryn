package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
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
public class WorkflowFilterRequest {

    private UUID clientId;
    private UUID clientServiceId;
    private ClientServiceType serviceType;
    private ServiceWorkflowStatus status;
    private TaskPriority priority;
    private UUID assignedEmployeeId;
    private String financialYear;
    private String assessmentYear;
    private String periodLabel;
    private LocalDate dueFrom;
    private LocalDate dueTo;
    private Boolean overdue;
    private Boolean dueToday;
    private Boolean dueThisWeek;
    private Boolean myWorkOnly;
    private Boolean teamWorkOnly;
    private Boolean waitingForClient;
    private Boolean readyForFiling;
    private String search;
}
