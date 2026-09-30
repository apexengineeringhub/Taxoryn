package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkTemplateRequest {

    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    @Size(max = 100, message = "Template code must not exceed 100 characters")
    private String templateCode;

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    private String description;

    private ServiceCategory category;

    @Builder.Default
    private WorkTemplateStatus status = WorkTemplateStatus.ACTIVE;

    @Builder.Default
    private WorkTemplateType templateType = WorkTemplateType.STATUTORY_COMPLIANCE;

    @Builder.Default
    private RecurrenceType recurrenceType = RecurrenceType.MONTHLY;

    @Builder.Default
    private int recurrenceInterval = 1;

    private Integer dayOfMonth;

    private Integer monthOfYear;

    @Builder.Default
    private boolean recurrenceEnabled = true;

    private List<CreateWorkTemplateTaskRequest> initialTasks;
}
