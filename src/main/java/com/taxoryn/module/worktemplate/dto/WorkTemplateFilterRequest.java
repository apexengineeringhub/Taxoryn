package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkTemplateFilterRequest extends PageRequestDto {

    private UUID serviceId;
    private ServiceCategory category;
    private WorkTemplateStatus status;
    private WorkTemplateType templateType;
    private RecurrenceType recurrenceType;
    private String search;
    private Boolean includeSystemDefaults;
}
