package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkInstanceDto {

    private UUID id;
    private UUID organizationId;
    private UUID engagementId;
    private String engagementCode;
    private String engagementName;
    private UUID clientId;
    private String clientName;
    private UUID templateId;
    private String templateName;
    private String templateCode;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;
    private WorkInstanceStatus status;
    private UUID assignedUserId;
    private String assignedUserName;
    private UUID reviewerUserId;
    private String reviewerUserName;
    private Instant generatedAt;
    private Instant completedAt;
    private String notes;
    private int totalTasks;
    private int completedTasks;
    private List<WorkInstanceTaskDto> tasks;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private long version;
}
