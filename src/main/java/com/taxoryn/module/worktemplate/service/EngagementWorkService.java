package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.worktemplate.dto.EnableEngagementTemplateRequest;
import com.taxoryn.module.worktemplate.dto.EngagementWorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.GenerateWorkInstanceRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkInstanceStatusRequest;
import com.taxoryn.module.worktemplate.dto.WorkInstanceDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;

import java.util.List;
import java.util.UUID;

/**
 * Service managing Work Template attachment to Engagements and Work Instance generation.
 */
public interface EngagementWorkService {

    List<WorkTemplateDto> getAvailableTemplatesForEngagement(UUID engagementId);

    List<EngagementWorkTemplateDto> getEnabledTemplatesForEngagement(UUID engagementId);

    EngagementWorkTemplateDto enableTemplateForEngagement(UUID engagementId, UUID templateId, EnableEngagementTemplateRequest request);

    void disableTemplateForEngagement(UUID engagementId, UUID templateId);

    WorkInstanceDto generateWorkInstance(UUID engagementId, GenerateWorkInstanceRequest request);

    PagedResponse<WorkInstanceDto> getWorkInstancesForEngagement(UUID engagementId, PageRequestDto pageRequest);

    WorkInstanceDto getWorkInstanceById(UUID id);

    WorkInstanceDto updateWorkInstanceStatus(UUID id, UpdateWorkInstanceStatusRequest request);
}
