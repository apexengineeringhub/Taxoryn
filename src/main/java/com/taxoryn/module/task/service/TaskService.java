package com.taxoryn.module.task.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateTaskRequest;

import java.util.UUID;

public interface TaskService {

    PagedResponse<TaskDto> getTasks(PageRequestDto pageRequest);

    TaskDto getTaskById(UUID taskId);

    TaskDto createTask(CreateTaskRequest request);

    TaskDto updateTask(UUID taskId, UpdateTaskRequest request);

    void deleteTask(UUID taskId);
}
