package com.taxoryn.module.task.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.TaskFilterRequest;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaskDto> getTasks(PageRequestDto pageRequest) {
        if (pageRequest instanceof TaskFilterRequest filterRequest) {
            return getTasks(filterRequest);
        }
        TaskFilterRequest fallback = TaskFilterRequest.builder()
                .page(pageRequest.getPage())
                .size(pageRequest.getSize())
                .sortBy(pageRequest.getSortBy())
                .sortDirection(pageRequest.getSortDirection())
                .build();
        return getTasks(fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaskDto> getTasks(TaskFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<TaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Enforce RBAC/ABAC Task Isolation:
            if (scope.isStaff()) {
                // Staff / Article Assistant can ONLY see tasks assigned to them
                Set<UUID> selfIds = scope.getAccessibleAssigneeIds();
                if (selfIds != null && !selfIds.isEmpty()) {
                    predicates.add(root.get("assignedTo").in(selfIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            } else if (scope.isDepartmentManager()) {
                // Manager can see tasks assigned to staff in their department or direct reports
                Set<UUID> deptIds = scope.getAccessibleAssigneeIds();
                if (filterRequest.getAssignedTo() != null) {
                    if (deptIds != null && deptIds.contains(filterRequest.getAssignedTo())) {
                        predicates.add(cb.equal(root.get("assignedTo"), filterRequest.getAssignedTo()));
                    } else {
                        predicates.add(cb.disjunction()); // Outside department boundary
                    }
                } else if (Boolean.TRUE.equals(filterRequest.getMyTasksOnly())) {
                    predicates.add(cb.equal(root.get("assignedTo"), scope.getUserId()));
                } else if (deptIds != null && !deptIds.isEmpty()) {
                    predicates.add(root.get("assignedTo").in(deptIds));
                }
            } else {
                // Firm Admin: unrestricted
                if (Boolean.TRUE.equals(filterRequest.getMyTasksOnly()) && scope.getUserId() != null) {
                    Set<UUID> adminIds = new HashSet<>();
                    adminIds.add(scope.getUserId());
                    if (scope.getEmployeeId() != null) adminIds.add(scope.getEmployeeId());
                    predicates.add(root.get("assignedTo").in(adminIds));
                } else if (filterRequest.getAssignedTo() != null) {
                    predicates.add(cb.equal(root.get("assignedTo"), filterRequest.getAssignedTo()));
                }
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (filterRequest.getTaskCategory() != null) {
                predicates.add(cb.equal(root.get("taskCategory"), filterRequest.getTaskCategory()));
            }

            if (filterRequest.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filterRequest.getPriority()));
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toUpperCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("title")), pattern),
                        cb.like(cb.upper(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TaskEntity> page = taskRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(UUID taskId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaskEntity entity = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (!scope.isFirmAdmin()) {
            Set<UUID> accessibleIds = scope.getAccessibleAssigneeIds();
            if (entity.getAssignedTo() != null && (accessibleIds == null || !accessibleIds.contains(entity.getAssignedTo()))) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission to view tasks outside your department or assigned workload.");
            }
        }

        return enrichDto(entity);
    }

    private UUID resolveAssigneeUserId(UUID assignedTo, UUID organizationId) {
        if (assignedTo == null) return null;
        return employeeRepository.findByIdAndOrganizationId(assignedTo, organizationId)
                .map(emp -> emp.getUserId() != null ? emp.getUserId() : assignedTo)
                .orElse(assignedTo);
    }

    @Override
    @Transactional
    public TaskDto createTask(CreateTaskRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        TaskEntity task = TaskEntity.builder()
                .clientId(request.getClientId())
                .assignedTo(resolveAssigneeUserId(request.getAssignedTo(), organizationId))
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

        return enrichDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTask(UUID taskId, UpdateTaskRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaskEntity task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        UUID previousAssignee = task.getAssignedTo();

        if (request.getClientId() != null) task.setClientId(request.getClientId());
        if (request.getAssignedTo() != null) task.setAssignedTo(resolveAssigneeUserId(request.getAssignedTo(), organizationId));
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

        return enrichDto(saved);
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
                        .assignedTo(resolveAssigneeUserId(request.getAssignedTo(), organizationId))
                        .title(request.getTitle().trim())
                        .description(request.getDescription())
                        .taskCategory(request.getTaskCategory())
                        .status(TaskStatus.TODO)
                        .priority(request.getPriority())
                        .dueDate(request.getDueDate())
                        .build();
                task.setOrganizationId(organizationId);

                TaskEntity saved = taskRepository.save(task);
                result.getCreatedTasks().add(enrichDto(saved));
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
                        .assignedTo(resolveAssigneeUserId(req.getAssignedTo(), organizationId))
                        .title(req.getTitle().trim())
                        .description(req.getDescription())
                        .taskCategory(req.getTaskCategory() != null ? req.getTaskCategory() : TaskEntity.TaskCategory.OTHER)
                        .status(TaskStatus.TODO)
                        .priority(req.getPriority() != null ? req.getPriority() : TaskEntity.TaskPriority.MEDIUM)
                        .dueDate(req.getDueDate())
                        .build();
                task.setOrganizationId(organizationId);

                TaskEntity saved = taskRepository.save(task);
                result.getCreatedTasks().add(enrichDto(saved));
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

    private TaskDto enrichDto(TaskEntity entity) {
        if (entity == null) return null;
        TaskDto dto = taskMapper.toDto(entity);
        if (dto == null) return null;

        if (entity.getClientId() != null) {
            clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                    .ifPresent(client -> dto.setClientName(client.getDisplayName()));
        }

        if (entity.getAssignedTo() != null) {
            // First match by Employee ID
            employeeRepository.findByIdAndOrganizationId(entity.getAssignedTo(), entity.getOrganizationId())
                    .ifPresentOrElse(emp -> {
                        dto.setAssigneeName(emp.getFullName());
                        dto.setAssigneeEmail(emp.getEmail());
                    }, () -> {
                        // Match by Employee UserId
                        employeeRepository.findByOrganizationIdAndUserId(entity.getOrganizationId(), entity.getAssignedTo())
                                .ifPresentOrElse(emp -> {
                                    dto.setAssigneeName(emp.getFullName());
                                    dto.setAssigneeEmail(emp.getEmail());
                                }, () -> {
                                    // Match by User ID
                                    userRepository.findByIdAndOrganizationId(entity.getAssignedTo(), entity.getOrganizationId())
                                            .ifPresent(user -> {
                                                dto.setAssigneeName(user.getFullName());
                                                dto.setAssigneeEmail(user.getEmail());
                                            });
                                });
                    });
        }

        return dto;
    }
}
