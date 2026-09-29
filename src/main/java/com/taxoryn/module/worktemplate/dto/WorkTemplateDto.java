package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTemplateDto {

    private UUID id;
    private UUID organizationId;
    private UUID serviceId;
    private String serviceName;
    private String serviceCode;
    private String templateCode;
    private String name;
    private String description;
    private ServiceCategory category;
    private WorkTemplateStatus status;
    private WorkTemplateType templateType;
    private RecurrenceType recurrenceType;
    private int recurrenceInterval;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    private boolean recurrenceEnabled;
    private boolean isSystemDefault;
    private int taskCount;
    private List<WorkTemplateTaskDto> tasks;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private long version;
}
