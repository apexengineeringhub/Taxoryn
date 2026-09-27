package com.taxoryn.module.task.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.task.dto.AssignWorkItemRequest;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateWorkItemStatusRequest;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.dto.WorkItemFilterRequest;

import java.util.List;
import java.util.UUID;

public interface WorkItemService {

    WorkItemDto createWorkItem(CreateWorkItemRequest request);

    WorkItemDto getWorkItemById(UUID id);

    WorkItemDto updateWorkItem(UUID id, UpdateWorkItemRequest request);

    WorkItemDto updateWorkItemStatus(UUID id, UpdateWorkItemStatusRequest request);

    WorkItemDto assignWorkItem(UUID id, AssignWorkItemRequest request);

    PagedResponse<WorkItemDto> getWorkItems(WorkItemFilterRequest filterRequest);

    List<WorkItemDto> getWorkItemsByClientId(UUID clientId);

    List<WorkItemDto> getWorkItemsByWorkflowId(UUID workflowId);

    TaskDto createTaskForWorkItem(UUID workItemId, CreateTaskRequest request);

    List<TaskDto> getTasksForWorkItem(UUID workItemId);

    void deleteWorkItem(UUID id);
}
