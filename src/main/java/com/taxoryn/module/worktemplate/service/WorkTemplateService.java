package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.ReorderTasksRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateStatusRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateFilterRequest;
import com.taxoryn.module.worktemplate.dto.WorkTemplateTaskDto;

import java.util.List;
import java.util.UUID;

/**
 * Service managing reusable practice Work Templates and their Template Tasks.
 */
public interface WorkTemplateService {

    WorkTemplateDto createTemplate(CreateWorkTemplateRequest request);

    WorkTemplateDto getTemplateById(UUID id);

    PagedResponse<WorkTemplateDto> getTemplates(WorkTemplateFilterRequest filterRequest);

    List<WorkTemplateDto> getActiveTemplatesForService(UUID serviceId);

    WorkTemplateDto updateTemplate(UUID id, UpdateWorkTemplateRequest request);

    WorkTemplateDto updateTemplateStatus(UUID id, UpdateWorkTemplateStatusRequest request);

    void deleteTemplate(UUID id);

    // Template Tasks Management
    List<WorkTemplateTaskDto> getTemplateTasks(UUID templateId);

    WorkTemplateTaskDto addTemplateTask(UUID templateId, CreateWorkTemplateTaskRequest request);

    WorkTemplateTaskDto updateTemplateTask(UUID templateId, UUID taskId, UpdateWorkTemplateTaskRequest request);

    void deleteTemplateTask(UUID templateId, UUID taskId);

    List<WorkTemplateTaskDto> reorderTemplateTasks(UUID templateId, ReorderTasksRequest request);
}
