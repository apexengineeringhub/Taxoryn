package com.taxoryn.module.client.businesscontext.provider;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.dto.WorkSummaryContextDto;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.service.TaskService;
import com.taxoryn.module.task.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkSummaryContextProvider implements BusinessContextProvider<WorkSummaryContextDto> {

    public static final String PROVIDER_KEY = "WORK_CONTEXT";

    private final TaskService taskService;
    private final WorkItemService workItemService;

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public WorkSummaryContextDto resolve(BusinessContextRequest request, UUID organizationId, UUID resolvedClientId) {
        if (request.getTaskId() == null && request.getWorkInstanceId() == null) {
            return null;
        }

        if (request.getTaskId() != null) {
            TaskDto task;
            try {
                task = taskService.getTaskById(request.getTaskId());
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Failed to retrieve task context for ID: {}", request.getTaskId(), e);
                throw new ResourceNotFoundException("Task not found with id: " + request.getTaskId());
            }

            if (task == null) {
                throw new ResourceNotFoundException("Task not found with id: " + request.getTaskId());
            }

            if (task.getOrganizationId() != null && organizationId != null
                    && !task.getOrganizationId().equals(organizationId)) {
                throw new ResourceNotFoundException("Task not found with id: " + request.getTaskId());
            }

            if (task.getClientId() != null && resolvedClientId != null && !task.getClientId().equals(resolvedClientId)) {
                throw new BusinessValidationException("Task " + request.getTaskId() + " does not belong to client " + resolvedClientId);
            }

            if (request.getEngagementId() != null && task.getEngagementId() != null
                    && !task.getEngagementId().equals(request.getEngagementId())) {
                throw new BusinessValidationException("Task engagementId does not match requested engagementId");
            }

            if (request.getWorkInstanceId() != null && task.getWorkInstanceId() != null
                    && !task.getWorkInstanceId().equals(request.getWorkInstanceId())) {
                throw new BusinessValidationException("Task workInstanceId does not match requested workInstanceId");
            }

            LocalDate today = LocalDate.now();
            boolean isOverdue = Boolean.TRUE.equals(task.getIsOverdue()) || (task.getDueDate() != null && task.getDueDate().isBefore(today) && task.getStatus() != TaskStatus.COMPLETED);
            boolean isDueToday = Boolean.TRUE.equals(task.getIsDueToday()) || (task.getDueDate() != null && task.getDueDate().isEqual(today));
            boolean isBlocked = task.getBlockedReason() != null && !task.getBlockedReason().isBlank();

            return WorkSummaryContextDto.builder()
                    .workInstanceId(task.getWorkInstanceId())
                    .taskId(task.getId())
                    .taskTitle(task.getTitle())
                    .taskStatus(task.getStatus() != null ? task.getStatus().name() : null)
                    .taskPriority(task.getPriority() != null ? task.getPriority().name() : null)
                    .taskCategory(task.getTaskCategory() != null ? task.getTaskCategory().name() : null)
                    .assignedUserId(task.getAssignedUserId() != null ? task.getAssignedUserId() : task.getAssignedTo())
                    .assigneeName(task.getAssigneeName())
                    .assigneeEmail(task.getAssigneeEmail())
                    .startDate(task.getStartDate())
                    .dueDate(task.getDueDate())
                    .statutoryDueDate(task.getStatutoryDueDate())
                    .overdue(isOverdue)
                    .dueToday(isDueToday)
                    .dueThisWeek(Boolean.TRUE.equals(task.getIsDueThisWeek()))
                    .blocked(isBlocked)
                    .blockedReason(task.getBlockedReason())
                    .build();
        }

        // Only workInstanceId provided
        WorkItemDto workItem;
        try {
            workItem = workItemService.getWorkItemById(request.getWorkInstanceId());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to retrieve work item context for ID: {}", request.getWorkInstanceId(), e);
            throw new ResourceNotFoundException("Work item not found with id: " + request.getWorkInstanceId());
        }

        if (workItem == null) {
            throw new ResourceNotFoundException("Work item not found with id: " + request.getWorkInstanceId());
        }

        if (workItem.getClientId() != null && resolvedClientId != null && !workItem.getClientId().equals(resolvedClientId)) {
            throw new BusinessValidationException("Work item " + request.getWorkInstanceId() + " does not belong to client " + resolvedClientId);
        }

        return WorkSummaryContextDto.builder()
                .workInstanceId(workItem.getId())
                .taskTitle(workItem.getTitle())
                .taskStatus(workItem.getStatus() != null ? workItem.getStatus().name() : null)
                .taskPriority(workItem.getPriority() != null ? workItem.getPriority().name() : null)
                .assignedUserId(workItem.getAssignedUserId())
                .assigneeName(workItem.getAssignedUserName())
                .dueDate(workItem.getDueDate())
                .overdue(workItem.getDueDate() != null && workItem.getDueDate().isBefore(LocalDate.now()))
                .dueToday(workItem.getDueDate() != null && workItem.getDueDate().isEqual(LocalDate.now()))
                .build();
    }
}
