package com.taxoryn.module.task.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.TaskFilterRequest;
import com.taxoryn.module.task.dto.TaskWorklistFilterRequest;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.dto.WorklistSummaryDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final ComplianceObligationRepository complianceObligationRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestItemRepository documentRequestItemRepository;
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

            // Enforce Assignee Filtering & RBAC/ABAC Task Isolation:
            if (filterRequest.getAssignedTo() != null) {
                Set<UUID> candidateIds = resolveCandidateAssigneeIds(filterRequest.getAssignedTo(), organizationId);
                if (scope.isStaff()) {
                    Set<UUID> selfIds = scope.getAccessibleAssigneeIds();
                    if (selfIds != null && selfIds.stream().anyMatch(candidateIds::contains)) {
                        predicates.add(root.get("assignedTo").in(candidateIds));
                    } else {
                        predicates.add(cb.disjunction());
                    }
                } else if (scope.isDepartmentManager()) {
                    Set<UUID> deptIds = scope.getAccessibleAssigneeIds();
                    if (deptIds != null && deptIds.stream().anyMatch(candidateIds::contains)) {
                        predicates.add(root.get("assignedTo").in(candidateIds));
                    } else {
                        predicates.add(cb.disjunction());
                    }
                } else {
                    // Firm Admin
                    predicates.add(root.get("assignedTo").in(candidateIds));
                }
            } else if (scope.isStaff()) {
                // Staff / Article Assistant can ONLY see tasks assigned to them
                Set<UUID> selfIds = scope.getAccessibleAssigneeIds();
                if (selfIds != null && !selfIds.isEmpty()) {
                    predicates.add(root.get("assignedTo").in(selfIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            } else if (scope.isDepartmentManager()) {
                // Manager can see tasks assigned to staff in their department or direct reports
                if (Boolean.TRUE.equals(filterRequest.getMyTasksOnly())) {
                    Set<UUID> managerSelfIds = resolveCandidateAssigneeIds(scope.getUserId(), organizationId);
                    predicates.add(root.get("assignedTo").in(managerSelfIds));
                } else {
                    Set<UUID> deptIds = scope.getAccessibleAssigneeIds();
                    if (deptIds != null && !deptIds.isEmpty()) {
                        predicates.add(root.get("assignedTo").in(deptIds));
                    }
                }
            } else {
                // Firm Admin: unrestricted
                if (Boolean.TRUE.equals(filterRequest.getMyTasksOnly()) && scope.getUserId() != null) {
                    Set<UUID> adminIds = resolveCandidateAssigneeIds(scope.getUserId(), organizationId);
                    predicates.add(root.get("assignedTo").in(adminIds));
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

    private Set<UUID> resolveCandidateAssigneeIds(UUID targetId, UUID organizationId) {
        if (targetId == null) return java.util.Collections.emptySet();
        Set<UUID> candidateIds = new HashSet<>();
        candidateIds.add(targetId);

        // If targetId is an Employee ID -> also include linked userId and any matching user account
        employeeRepository.findByIdAndOrganizationId(targetId, organizationId)
                .ifPresent(emp -> {
                    if (emp.getUserId() != null) {
                        candidateIds.add(emp.getUserId());
                    }
                    if (emp.getEmail() != null) {
                        userRepository.findByEmailIgnoreCase(emp.getEmail().toLowerCase().trim())
                                .ifPresent(user -> candidateIds.add(user.getId()));
                    }
                });

        // If targetId is a User ID -> also include any Employee record linked to this user or email
        employeeRepository.findByOrganizationIdAndUserId(organizationId, targetId)
                .ifPresent(emp -> candidateIds.add(emp.getId()));

        userRepository.findByIdAndOrganizationId(targetId, organizationId)
                .ifPresent(user -> {
                    if (user.getEmail() != null) {
                        employeeRepository.findByOrganizationIdAndEmail(organizationId, user.getEmail().toLowerCase().trim())
                                .ifPresent(emp -> candidateIds.add(emp.getId()));
                    }
                });

        return candidateIds;
    }

    private UUID resolveAssigneeUserId(UUID assignedTo, UUID organizationId) {
        if (assignedTo == null) return null;
        return employeeRepository.findByIdAndOrganizationId(assignedTo, organizationId)
                .map(emp -> {
                    if (emp.getUserId() != null) {
                        return emp.getUserId();
                    }
                    if (emp.getEmail() != null) {
                        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(emp.getEmail().toLowerCase().trim());
                        if (userOpt.isPresent()) {
                            emp.setUserId(userOpt.get().getId());
                            employeeRepository.save(emp);
                            return userOpt.get().getId();
                        }
                    }
                    return assignedTo;
                })
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
                .complianceId(request.getComplianceId())
                .documentRequestId(request.getDocumentRequestId())
                .blockedReason(request.getBlockedReason())
                .build();
        task.setOrganizationId(organizationId);

        TaskEntity saved = taskRepository.save(task);
        log.info("Created task {} for organization {}", saved.getId(), organizationId);

        if (request.getComplianceId() != null) {
            complianceObligationRepository.findByIdAndOrganizationId(request.getComplianceId(), organizationId)
                    .ifPresent(ob -> {
                        if (ob.getTaskId() == null) {
                            ob.setTaskId(saved.getId());
                            complianceObligationRepository.save(ob);
                        }
                    });
        }

        if (request.getDocumentRequestId() != null) {
            documentRequestRepository.findByIdAndOrganizationId(request.getDocumentRequestId(), organizationId)
                    .ifPresent(docReq -> {
                        if (docReq.getTaskId() == null) {
                            docReq.setTaskId(saved.getId());
                            documentRequestRepository.save(docReq);
                        }
                        if (docReq.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.COMPLETED
                                && (saved.getStatus() == TaskStatus.TODO || saved.getStatus() == TaskStatus.IN_PROGRESS)) {
                            saved.setStatus(TaskStatus.BLOCKED);
                            saved.setBlockedReason("Waiting for client documents: " + docReq.getPurpose());
                            taskRepository.save(saved);
                        }
                    });
        }

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

        // SECURITY: this endpoint is reachable by non-admin STAFF/PRACTITIONER/ARTICLE_ASSISTANT
        // roles (see TaskController @PreAuthorize). Without this check, any such user could
        // modify (reassign, change status/priority, clear blocked reason on) ANY task in the
        // organization, not just tasks within their own department/assigned workload - the
        // same ABAC scope that is already enforced for reads in getTaskById()/getTasks().
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (!scope.isFirmAdmin()) {
            Set<UUID> accessibleIds = scope.getAccessibleAssigneeIds();
            if (task.getAssignedTo() != null && (accessibleIds == null || !accessibleIds.contains(task.getAssignedTo()))) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied: You do not have permission to modify tasks outside your department or assigned workload.");
            }
        }

        UUID previousAssignee = task.getAssignedTo();
        TaskStatus previousStatus = task.getStatus();

        if (request.getClientId() != null) task.setClientId(request.getClientId());
        if (Boolean.TRUE.equals(request.getUnassign())) {
            task.setAssignedTo(null);
        } else if (request.getAssignedTo() != null) {
            task.setAssignedTo(resolveAssigneeUserId(request.getAssignedTo(), organizationId));
        }
        if (request.getTitle() != null) task.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getTaskCategory() != null) task.setTaskCategory(request.getTaskCategory());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getComplianceId() != null) task.setComplianceId(request.getComplianceId());
        if (request.getDocumentRequestId() != null) task.setDocumentRequestId(request.getDocumentRequestId());

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            if (request.getStatus() == TaskStatus.COMPLETED && previousStatus != TaskStatus.COMPLETED) {
                task.setCompletedAt(Instant.now());
            } else if (request.getStatus() != TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
            }
            if (request.getStatus() == TaskStatus.BLOCKED) {
                task.setBlockedReason(request.getBlockedReason() != null ? request.getBlockedReason() : "Waiting for client documents or action");
            }
        }

        if (request.getBlockedReason() != null) {
            task.setBlockedReason(request.getBlockedReason());
        }

        if (Boolean.TRUE.equals(request.getClearBlockedReason())) {
            task.setBlockedReason(null);
        }

        TaskEntity saved = taskRepository.save(task);
        log.info("Updated task {} for organization {}", saved.getId(), organizationId);

        if (saved.getAssignedTo() != null && !Objects.equals(previousAssignee, saved.getAssignedTo())) {
            notifyTaskAssigned(organizationId, saved);
        }

        if (saved.getStatus() == TaskStatus.BLOCKED && previousStatus != TaskStatus.BLOCKED && saved.getAssignedTo() != null) {
            notifyTaskBlocked(organizationId, saved);
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
                    "/tasks",
                    "{\"taskId\":\"" + task.getId() + "\"}"
            );
        } catch (Exception ex) {
            log.error("Failed to raise TASK_ASSIGNED notification for task {}: {}", task.getId(), ex.getMessage(), ex);
        }
    }

    private void notifyTaskBlocked(UUID organizationId, TaskEntity task) {
        try {
            notificationService.notify(
                    organizationId,
                    task.getAssignedTo(),
                    null,
                    NotificationType.TASK_BLOCKED,
                    "Task Blocked: " + task.getTitle(),
                    "Task \"" + task.getTitle() + "\" is blocked: " + (task.getBlockedReason() != null ? task.getBlockedReason() : "Waiting for client documents."),
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/tasks",
                    "{\"taskId\":\"" + task.getId() + "\"}"
            );
        } catch (Exception ex) {
            log.error("Failed to raise TASK_BLOCKED notification for task {}: {}", task.getId(), ex.getMessage(), ex);
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
    @Transactional(readOnly = true)
    public PagedResponse<TaskDto> getWorklist(TaskWorklistFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        LocalDate today = LocalDate.now();

        Specification<TaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Scope Evaluation (MY_WORK vs TEAM_WORK)
            if (scope.isStaff()) {
                // Staff can only ever see their own accessible tasks
                Set<UUID> selfIds = scope.getAccessibleAssigneeIds();
                if (selfIds != null && !selfIds.isEmpty()) {
                    predicates.add(root.get("assignedTo").in(selfIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            } else if (filterRequest.getScope() == TaskWorklistFilterRequest.WorklistScope.MY_WORK) {
                Set<UUID> myCandidateIds = resolveCandidateAssigneeIds(scope.getUserId(), organizationId);
                if (!myCandidateIds.isEmpty()) {
                    predicates.add(root.get("assignedTo").in(myCandidateIds));
                }
            } else if (scope.isDepartmentManager()) {
                Set<UUID> deptIds = scope.getAccessibleAssigneeIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    predicates.add(root.get("assignedTo").in(deptIds));
                }
            }

            // Bucket Predicates
            if (filterRequest.getBucket() != null) {
                switch (filterRequest.getBucket()) {
                    case OVERDUE:
                        predicates.add(root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW, TaskStatus.BLOCKED));
                        predicates.add(cb.isNotNull(root.get("dueDate")));
                        predicates.add(cb.lessThan(root.get("dueDate"), today));
                        break;
                    case DUE_TODAY:
                        predicates.add(root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW, TaskStatus.BLOCKED));
                        predicates.add(cb.equal(root.get("dueDate"), today));
                        break;
                    case DUE_THIS_WEEK:
                        predicates.add(root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW, TaskStatus.BLOCKED));
                        predicates.add(cb.isNotNull(root.get("dueDate")));
                        predicates.add(cb.between(root.get("dueDate"), today, today.plusDays(7)));
                        break;
                    case BLOCKED:
                        predicates.add(cb.equal(root.get("status"), TaskStatus.BLOCKED));
                        break;
                    case COMPLETED:
                        predicates.add(cb.equal(root.get("status"), TaskStatus.COMPLETED));
                        break;
                    case ALL:
                    default:
                        if (filterRequest.getStatus() == null) {
                            predicates.add(cb.notEqual(root.get("status"), TaskStatus.CANCELLED));
                        }
                        break;
                }
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }

            if (filterRequest.getAssignedTo() != null) {
                Set<UUID> candidateIds = resolveCandidateAssigneeIds(filterRequest.getAssignedTo(), organizationId);
                predicates.add(root.get("assignedTo").in(candidateIds));
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

        Pageable pageable = filterRequest.toPageable();
        Page<TaskEntity> page = taskRepository.findAll(spec, pageable);
        return PagedResponse.of(page, this::enrichDto);
    }

    @Override
    @Transactional(readOnly = true)
    public WorklistSummaryDto getWorklistSummary() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        LocalDate today = LocalDate.now();
        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TaskStatus> activeStatuses = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW, TaskStatus.BLOCKED);

        long overdue = taskRepository.countByOrganizationIdAndStatusInAndDueDateBefore(organizationId, activeStatuses, today);
        long dueToday = taskRepository.countByOrganizationIdAndStatusInAndDueDate(organizationId, activeStatuses, today);
        long dueThisWeek = taskRepository.countByOrganizationIdAndStatusInAndDueDateBetween(organizationId, activeStatuses, today, today.plusDays(7));
        long inProgress = taskRepository.countByOrganizationIdAndStatus(organizationId, TaskStatus.IN_PROGRESS);
        long blocked = taskRepository.countByOrganizationIdAndStatus(organizationId, TaskStatus.BLOCKED);
        long completedToday = taskRepository.countByOrganizationIdAndStatusAndCompletedAtGreaterThanEqual(organizationId, TaskStatus.COMPLETED, todayStart);
        long docsWaiting = documentRequestItemRepository.countByOrganizationIdAndStatus(organizationId, ItemStatus.PENDING);

        Set<UUID> userIds = resolveCandidateAssigneeIds(scope.getUserId(), organizationId);
        long myTasks = !userIds.isEmpty()
                ? taskRepository.countByStatuses(organizationId, userIds, activeStatuses)
                : 0L;
        long teamTasks = taskRepository.countByOrganizationIdAndStatusIn(organizationId, activeStatuses);

        return WorklistSummaryDto.builder()
                .overdueCount(overdue)
                .dueTodayCount(dueToday)
                .dueThisWeekCount(dueThisWeek)
                .inProgressCount(inProgress)
                .blockedCount(blocked)
                .completedTodayCount(completedToday)
                .documentsWaitingCount(docsWaiting)
                .myTasksCount(myTasks)
                .teamTasksCount(teamTasks)
                .build();
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

        log.info("Bulk generated {} tasks for orgId={}, failed={}",
                result.getTotalCreated(), organizationId, result.getTotalFailed());

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

        int row = 0;
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
                        .complianceId(req.getComplianceId())
                        .documentRequestId(req.getDocumentRequestId())
                        .blockedReason(req.getBlockedReason())
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

        LocalDate today = LocalDate.now();
        boolean isNotClosed = entity.getStatus() != TaskStatus.COMPLETED && entity.getStatus() != TaskStatus.CANCELLED;

        if (entity.getDueDate() != null && isNotClosed) {
            dto.setIsOverdue(today.isAfter(entity.getDueDate()));
            dto.setIsDueToday(today.isEqual(entity.getDueDate()));
            dto.setIsDueThisWeek(!entity.getDueDate().isBefore(today) && !entity.getDueDate().isAfter(today.plusDays(7)));
        } else {
            dto.setIsOverdue(false);
            dto.setIsDueToday(false);
            dto.setIsDueThisWeek(false);
        }

        if (entity.getClientId() != null) {
            clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                    .ifPresent(client -> dto.setClientName(client.getDisplayName()));
        }

        if (entity.getComplianceId() != null) {
            complianceObligationRepository.findByIdAndOrganizationId(entity.getComplianceId(), entity.getOrganizationId())
                    .ifPresent(ob -> {
                        dto.setComplianceTitle(ob.getTitle());
                        dto.setStatutoryDueDate(ob.getDueDate());
                    });
        }

        if (entity.getDocumentRequestId() != null) {
            documentRequestRepository.findByIdAndOrganizationId(entity.getDocumentRequestId(), entity.getOrganizationId())
                    .ifPresent(docReq -> {
                        dto.setDocumentRequestNumber(docReq.getRequestNumber());
                        dto.setDocumentRequestStatus(docReq.getStatus().name());
                        dto.setDocumentRequestItemsCount((int) documentRequestItemRepository.countByRequestId(docReq.getId()));
                        long receivedCount = documentRequestItemRepository.countByRequestIdAndStatus(docReq.getId(), ItemStatus.ACCEPTED)
                                + documentRequestItemRepository.countByRequestIdAndStatus(docReq.getId(), ItemStatus.UPLOADED);
                        dto.setDocumentRequestReceivedCount((int) receivedCount);
                    });
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
