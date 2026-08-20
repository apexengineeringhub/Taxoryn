package com.taxoryn.module.task.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaskDto> getTasks(PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<TaskEntity> page = taskRepository.findAllByOrganizationId(organizationId, pageRequest.toPageable());
        return PagedResponse.of(page, taskMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(UUID taskId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaskEntity entity = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        return taskMapper.toDto(entity);
    }

    @Override
    @Transactional
    public TaskDto createTask(CreateTaskRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        TaskEntity task = TaskEntity.builder()
                .clientId(request.getClientId())
                .assignedTo(request.getAssignedTo())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .taskCategory(request.getTaskCategory())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .build();
        task.setOrganizationId(organizationId);

        TaskEntity saved = taskRepository.save(task);
        log.info("Created task {} for organization {}", saved.getId(), organizationId);

        if (saved.getAssignedTo() != null) {
            notifyTaskAssigned(organizationId, saved);
        }

        return taskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTask(UUID taskId, UpdateTaskRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaskEntity task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        UUID previousAssignee = task.getAssignedTo();

        if (request.getClientId() != null) task.setClientId(request.getClientId());
        if (request.getAssignedTo() != null) task.setAssignedTo(request.getAssignedTo());
        if (request.getTitle() != null) task.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getTaskCategory() != null) task.setTaskCategory(request.getTaskCategory());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());

        TaskEntity saved = taskRepository.save(task);
        log.info("Updated task {} for organization {}", saved.getId(), organizationId);

        if (saved.getAssignedTo() != null && !Objects.equals(previousAssignee, saved.getAssignedTo())) {
            notifyTaskAssigned(organizationId, saved);
        }

        return taskMapper.toDto(saved);
    }

    private void notifyTaskAssigned(UUID organizationId, TaskEntity task) {
        try {
            notificationService.notify(
                    organizationId,
                    task.getAssignedTo(),
                    null,
                    NotificationType.TASK_ASSIGNED,
                    "New Task Assigned: " + task.getTitle(),
                    "You have been assigned the task \"" + task.getTitle() + "\"" +
                            (task.getDueDate() != null ? ", due on " + task.getDueDate() + "." : "."),
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/tasks/" + task.getId(),
                    "{\"taskId\":\"" + task.getId() + "\"}"
            );
        } catch (Exception ex) {
            // Notification failures must never break the primary task workflow.
            log.error("Failed to raise TASK_ASSIGNED notification for task {}: {}", task.getId(), ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void deleteTask(UUID taskId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaskEntity task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        task.setStatus(TaskStatus.CANCELLED);
        taskRepository.save(task);
        log.info("Cancelled task {} for organization {}", taskId, organizationId);
    }

    @Override
    @Transactional
    public com.taxoryn.module.task.dto.BulkTaskImportResultDto generateBulkTasks(com.taxoryn.module.task.dto.BulkTaskCreateRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        com.taxoryn.module.task.dto.BulkTaskImportResultDto result = com.taxoryn.module.task.dto.BulkTaskImportResultDto.builder()
                .totalProcessed(request.getClientIds() != null ? request.getClientIds().size() : 0)
                .build();

        if (request.getClientIds() == null || request.getClientIds().isEmpty()) {
            return result;
        }

        for (UUID clientId : request.getClientIds()) {
            try {
                TaskEntity task = TaskEntity.builder()
                        .clientId(clientId)
                        .assignedTo(request.getAssignedTo())
                        .title(request.getTitle().trim())
                        .description(request.getDescription())
                        .taskCategory(request.getTaskCategory())
                        .status(TaskStatus.TODO)
                        .priority(request.getPriority())
                        .dueDate(request.getDueDate())
                        .build();
                task.setOrganizationId(organizationId);

                TaskEntity saved = taskRepository.save(task);
                result.getCreatedTasks().add(taskMapper.toDto(saved));
                result.setTotalCreated(result.getTotalCreated() + 1);

                if (saved.getAssignedTo() != null) {
                    notifyTaskAssigned(organizationId, saved);
                }
            } catch (Exception ex) {
                result.getErrors().add("Client " + clientId + ": " + ex.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        log.info("Generated bulk tasks for orgId={}: {} created, {} failed",
                organizationId, result.getTotalCreated(), result.getTotalFailed());

        return result;
    }

    @Override
    @Transactional
    public com.taxoryn.module.task.dto.BulkTaskImportResultDto bulkCreateTasks(java.util.List<CreateTaskRequest> requests) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        com.taxoryn.module.task.dto.BulkTaskImportResultDto result = com.taxoryn.module.task.dto.BulkTaskImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) {
            return result;
        }

        int row = 1;
        for (CreateTaskRequest req : requests) {
            row++;
            try {
                TaskEntity task = TaskEntity.builder()
                        .clientId(req.getClientId())
                        .assignedTo(req.getAssignedTo())
                        .title(req.getTitle().trim())
                        .description(req.getDescription())
                        .taskCategory(req.getTaskCategory() != null ? req.getTaskCategory() : TaskEntity.TaskCategory.OTHER)
                        .status(TaskStatus.TODO)
                        .priority(req.getPriority() != null ? req.getPriority() : TaskEntity.TaskPriority.MEDIUM)
                        .dueDate(req.getDueDate())
                        .build();
                task.setOrganizationId(organizationId);

                TaskEntity saved = taskRepository.save(task);
                result.getCreatedTasks().add(taskMapper.toDto(saved));
                result.setTotalCreated(result.getTotalCreated() + 1);

                if (saved.getAssignedTo() != null) {
                    notifyTaskAssigned(organizationId, saved);
                }
            } catch (Exception ex) {
                result.getErrors().add("Row " + row + " (" + req.getTitle() + "): " + ex.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        log.info("Bulk imported tasks for orgId={}: {} created, {} failed",
                organizationId, result.getTotalCreated(), result.getTotalFailed());

        return result;
    }
}
