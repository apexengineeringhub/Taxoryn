package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.workflow.model.ServiceWorkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWorkflowTemplateDto {
    private UUID id;
    private UUID organizationId;
    private ClientServiceType serviceType;
    private String serviceTypeName;
    private String name;
    private String description;
    private boolean isSystemDefault;
    private boolean active;
    private int stepCount;

    @Builder.Default
    private List<ServiceWorkflowStepTemplateDto> stepTemplates = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceWorkflowStepTemplateDto {
        private UUID id;
        private UUID workflowTemplateId;
        private int sequence;
        private ServiceWorkType workType;
        private String workTypeName;
        private String name;
        private String description;
        private Integer defaultDaysBeforeDueDate;
        private boolean mandatory;
        private boolean requiresClientInput;
        private boolean requiresReview;
        private boolean active;
    }
}
