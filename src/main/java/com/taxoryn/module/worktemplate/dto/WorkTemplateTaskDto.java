package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTemplateTaskDto {

    private UUID id;
    private UUID templateId;
    private String name;
    private String description;
    private int sequenceOrder;
    private String defaultAssigneeRole;
    private TaskPriority defaultPriority;
    private int relativeDueDays;
    private boolean mandatory;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
}
