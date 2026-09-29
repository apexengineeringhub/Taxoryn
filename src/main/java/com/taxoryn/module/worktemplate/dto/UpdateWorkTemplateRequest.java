package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkTemplateRequest {

    private UUID serviceId;

    @Size(max = 100, message = "Template code must not exceed 100 characters")
    private String templateCode;

    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    private String description;

    private ServiceCategory category;

    private WorkTemplateType templateType;

    private RecurrenceType recurrenceType;

    private Integer recurrenceInterval;

    private Integer dayOfMonth;

    private Integer monthOfYear;

    private Boolean recurrenceEnabled;
}
