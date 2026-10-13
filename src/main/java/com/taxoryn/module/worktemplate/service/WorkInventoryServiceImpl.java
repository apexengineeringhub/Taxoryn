package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.dto.WorkInventoryFilterRequest;
import com.taxoryn.module.worktemplate.dto.WorkInventoryItemDto;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkInventoryStatus;
import com.taxoryn.module.worktemplate.model.WorkInventoryType;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of WorkInventoryService.
 * Builds an aggregated, authoritative, read-only projection across:
 * 1. WorkInstanceEntity (Service & compliance work units)
 * 2. ComplianceObligationEntity (Statutory compliance obligations not yet generated as work instances)
 * 3. TaskEntity (Standalone actionable tasks not part of a work instance)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkInventoryServiceImpl implements WorkInventoryService {

    private final WorkInstanceRepository workInstanceRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;
    private final EngagementRepository engagementRepository;
    private final UserRepository userRepository;

    @Override
    public PagedResponse<WorkInventoryItemDto> getWorkInventory(WorkInventoryFilterRequest filter) {
        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new TenantAccessDeniedException("Organization context is required to query work inventory");
        }

        WorkInventoryFilterRequest safeFilter = filter != null ? filter : new WorkInventoryFilterRequest();

        // Normalize date boundaries if inverted
        if (safeFilter.getFromDate() != null && safeFilter.getToDate() != null
                && safeFilter.getFromDate().isAfter(safeFilter.getToDate())) {
            LocalDate temp = safeFilter.getFromDate();
            safeFilter.setFromDate(safeFilter.getToDate());
            safeFilter.setToDate(temp);
        }

        // 1. Fetch Candidates based on Work Type selection
        List<WorkInstanceEntity> workInstances = Collections.emptyList();
        List<ComplianceObligationEntity> obligations = Collections.emptyList();
        List<TaskEntity> standaloneTasks = Collections.emptyList();

        boolean includeWorkInstances = safeFilter.getWorkType() == null || safeFilter.getWorkType() == WorkInventoryType.WORK_INSTANCE;
        boolean includeObligations = safeFilter.getWorkType() == null || safeFilter.getWorkType() == WorkInventoryType.COMPLIANCE_OBLIGATION;
        boolean includeTasks = safeFilter.getWorkType() == null || safeFilter.getWorkType() == WorkInventoryType.TASK;

        if (includeWorkInstances) {
            workInstances = workInstanceRepository.findAll((root, query, cb) -> {
                var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.equal(root.get("organizationId"), orgId));

                if (safeFilter.getAssignedUserId() != null) {
                    predicates.add(cb.equal(root.get("assignedUserId"), safeFilter.getAssignedUserId()));
                }
                if (safeFilter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), safeFilter.getFromDate()));
                }
                if (safeFilter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), safeFilter.getToDate()));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            });
        }

        if (includeObligations) {
            obligations = obligationRepository.findAll((root, query, cb) -> {
                var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.equal(root.get("organizationId"), orgId));

                if (safeFilter.getClientId() != null) {
                    predicates.add(cb.equal(root.get("clientId"), safeFilter.getClientId()));
                }
                if (safeFilter.getAssignedUserId() != null) {
                    predicates.add(cb.equal(root.get("assignedUserId"), safeFilter.getAssignedUserId()));
                }
                if (safeFilter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            cb.coalesce(root.get("statutoryDueDate"), root.get("dueDate")), safeFilter.getFromDate()));
                }
                if (safeFilter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            cb.coalesce(root.get("statutoryDueDate"), root.get("dueDate")), safeFilter.getToDate()));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            });
        }

        if (includeTasks) {
            standaloneTasks = taskRepository.findAll((root, query, cb) -> {
                var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.equal(root.get("organizationId"), orgId));
                // Only standalone tasks — tasks that are NOT subtasks of a work instance or obligation
                predicates.add(cb.isNull(root.get("workInstanceId")));
                predicates.add(cb.isNull(root.get("complianceId")));

                if (safeFilter.getClientId() != null) {
                    predicates.add(cb.equal(root.get("clientId"), safeFilter.getClientId()));
                }
                if (safeFilter.getAssignedUserId() != null) {
                    predicates.add(cb.equal(root.get("assignedTo"), safeFilter.getAssignedUserId()));
                }
                if (safeFilter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), safeFilter.getFromDate()));
                }
                if (safeFilter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), safeFilter.getToDate()));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            });
        }

        // 2. Identify Obligations that are already represented as Work Instances to avoid duplicate work entries
        Set<UUID> coveredObligationIds = workInstances.stream()
                .map(WorkInstanceEntity::getComplianceObligationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. Batch load related reference entities (Clients, Engagements, Users, Task counts)
        Set<UUID> engagementIds = workInstances.stream()
                .map(WorkInstanceEntity::getEngagementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, EngagementEntity> engagementMap = engagementIds.isEmpty() ? Collections.emptyMap() :
                engagementRepository.findAllById(engagementIds).stream()
                        .filter(e -> orgId.equals(e.getOrganizationId()))
                        .collect(Collectors.toMap(EngagementEntity::getId, Function.identity(), (a, b) -> a));

        Set<UUID> clientIds = new HashSet<>();
        // Add clientIds from engagements
        engagementMap.values().forEach(e -> {
            if (e.getClientId() != null) clientIds.add(e.getClientId());
        });
        // Add clientIds from obligations
        obligations.forEach(o -> {
            if (o.getClientId() != null) clientIds.add(o.getClientId());
        });
        // Add clientIds from tasks
        standaloneTasks.forEach(t -> {
            if (t.getClientId() != null) clientIds.add(t.getClientId());
        });

        Map<UUID, ClientEntity> clientMap = clientIds.isEmpty() ? Collections.emptyMap() :
                clientRepository.findAllById(clientIds).stream()
                        .filter(c -> orgId.equals(c.getOrganizationId()))
                        .collect(Collectors.toMap(ClientEntity::getId, Function.identity(), (a, b) -> a));

        Set<UUID> userIds = new HashSet<>();
        workInstances.forEach(w -> {
            if (w.getAssignedUserId() != null) userIds.add(w.getAssignedUserId());
            if (w.getReviewerUserId() != null) userIds.add(w.getReviewerUserId());
        });
        obligations.forEach(o -> {
            if (o.getAssignedUserId() != null) userIds.add(o.getAssignedUserId());
        });
        standaloneTasks.forEach(t -> {
            if (t.getAssignedTo() != null) userIds.add(t.getAssignedTo());
        });

        Map<UUID, UserEntity> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));

        // Subtasks for work instances
        Set<UUID> workInstanceIds = workInstances.stream().map(WorkInstanceEntity::getId).collect(Collectors.toSet());
        Map<UUID, List<TaskEntity>> instanceTasksMap = new HashMap<>();
        if (!workInstanceIds.isEmpty()) {
            List<TaskEntity> instanceTasks = taskRepository.findAll((root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("organizationId"), orgId),
                            root.get("workInstanceId").in(workInstanceIds)
                    ));
            for (TaskEntity task : instanceTasks) {
                instanceTasksMap.computeIfAbsent(task.getWorkInstanceId(), k -> new ArrayList<>()).add(task);
            }
        }

        // 4. Project into unified WorkInventoryItemDto
        List<WorkInventoryItemDto> projectedList = new ArrayList<>();

        // 4A. Project Work Instances
        for (WorkInstanceEntity wi : workInstances) {
            EngagementEntity eng = engagementMap.get(wi.getEngagementId());
            UUID cId = eng != null ? eng.getClientId() : null;
            ClientEntity client = cId != null ? clientMap.get(cId) : null;

            // Client filter check if filter has clientId
            if (safeFilter.getClientId() != null && !safeFilter.getClientId().equals(cId)) {
                continue;
            }

            List<TaskEntity> subtasks = instanceTasksMap.getOrDefault(wi.getId(), Collections.emptyList());
            int totalSubtasks = subtasks.size();
            int completedSubtasks = (int) subtasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

            // Next actionable step: first non-completed subtask or status description
            String nextStep = subtasks.stream()
                    .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED)
                    .map(TaskEntity::getTitle)
                    .findFirst()
                    .orElse(wi.getStatus() == WorkInstanceStatus.COMPLETED ? "Completed" : "Perform work procedures");

            boolean isBlocked = wi.getStatus() == WorkInstanceStatus.ON_HOLD ||
                    subtasks.stream().anyMatch(t -> t.getStatus() == TaskStatus.BLOCKED);

            String blockedReason = wi.getStatus() == WorkInstanceStatus.ON_HOLD ? wi.getNotes() :
                    subtasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED && StringUtils.hasText(t.getBlockedReason()))
                            .map(TaskEntity::getBlockedReason).findFirst().orElse(null);

            WorkInventoryStatus normalizedStatus = mapWorkInstanceStatus(wi.getStatus(), isBlocked);
            String priority = eng != null && eng.getPriority() != null ? eng.getPriority().name() : "MEDIUM";

            projectedList.add(WorkInventoryItemDto.builder()
                    .id(wi.getId())
                    .workType(WorkInventoryType.WORK_INSTANCE)
                    .title(wi.getTitle())
                    .normalizedStatus(normalizedStatus)
                    .rawStatus(wi.getStatus().name())
                    .priority(priority)
                    .clientId(cId)
                    .clientName(client != null ? (StringUtils.hasText(client.getDisplayName()) ? client.getDisplayName() : client.getLegalName()) : null)
                    .clientPan(client != null ? client.getPan() : null)
                    .engagementId(wi.getEngagementId())
                    .engagementName(eng != null ? eng.getName() : null)
                    .complianceObligationId(wi.getComplianceObligationId())
                    .dueDate(wi.getDueDate())
                    .periodStart(wi.getPeriodStart())
                    .periodEnd(wi.getPeriodEnd())
                    .assignedUserId(wi.getAssignedUserId())
                    .assignedUserName(getUserFullName(userMap.get(wi.getAssignedUserId())))
                    .reviewerUserId(wi.getReviewerUserId())
                    .reviewerUserName(getUserFullName(userMap.get(wi.getReviewerUserId())))
                    .blocked(isBlocked)
                    .blockedReason(blockedReason)
                    .nextActionableStep(nextStep)
                    .totalTasks(totalSubtasks)
                    .completedTasks(completedSubtasks)
                    .createdAt(wi.getCreatedAt())
                    .completedAt(wi.getCompletedAt())
                    .build());
        }

        // 4B. Project Compliance Obligations (only standalone ones not yet generated into work instances)
        for (ComplianceObligationEntity ob : obligations) {
            if (coveredObligationIds.contains(ob.getId())) {
                continue; // Suppress duplicate! Covered as work instance
            }

            ClientEntity client = clientMap.get(ob.getClientId());
            LocalDate dueDate = ob.getStatutoryDueDate() != null ? ob.getStatutoryDueDate() : ob.getDueDate();
            WorkInventoryStatus normalizedStatus = mapObligationStatus(ob.getStatus());
            boolean isBlocked = ob.getStatus() == ComplianceObligationStatus.WAITING_FOR_CLIENT;
            String blockedReason = isBlocked ? ob.getApplicabilityReason() : null;

            projectedList.add(WorkInventoryItemDto.builder()
                    .id(ob.getId())
                    .workType(WorkInventoryType.COMPLIANCE_OBLIGATION)
                    .title(StringUtils.hasText(ob.getTitle()) ? ob.getTitle() : ob.getRuleNameSnapshot())
                    .normalizedStatus(normalizedStatus)
                    .rawStatus(ob.getStatus().name())
                    .priority(ob.getPriority() != null ? ob.getPriority().name() : "MEDIUM")
                    .clientId(ob.getClientId())
                    .clientName(client != null ? (StringUtils.hasText(client.getDisplayName()) ? client.getDisplayName() : client.getLegalName()) : null)
                    .clientPan(client != null ? client.getPan() : null)
                    .complianceObligationId(ob.getId())
                    .ruleCode(ob.getRuleCode())
                    .dueDate(dueDate)
                    .assignedUserId(ob.getAssignedUserId())
                    .assignedUserName(getUserFullName(userMap.get(ob.getAssignedUserId())))
                    .blocked(isBlocked)
                    .blockedReason(blockedReason)
                    .nextActionableStep(ob.getStatus() == ComplianceObligationStatus.READY_FOR_FILING ? "File return on portal" : "Prepare statutory return")
                    .totalTasks(0)
                    .completedTasks(0)
                    .createdAt(ob.getCreatedAt())
                    .completedAt(ob.getCompletedAt())
                    .build());
        }

        // 4C. Project Standalone Tasks
        for (TaskEntity task : standaloneTasks) {
            ClientEntity client = clientMap.get(task.getClientId());
            boolean isBlocked = task.getStatus() == TaskStatus.BLOCKED;
            WorkInventoryStatus normalizedStatus = mapTaskStatus(task.getStatus());

            projectedList.add(WorkInventoryItemDto.builder()
                    .id(task.getId())
                    .workType(WorkInventoryType.TASK)
                    .title(task.getTitle())
                    .normalizedStatus(normalizedStatus)
                    .rawStatus(task.getStatus().name())
                    .priority(task.getPriority() != null ? task.getPriority().name() : "MEDIUM")
                    .clientId(task.getClientId())
                    .clientName(client != null ? (StringUtils.hasText(client.getDisplayName()) ? client.getDisplayName() : client.getLegalName()) : null)
                    .clientPan(client != null ? client.getPan() : null)
                    .dueDate(task.getDueDate())
                    .assignedUserId(task.getAssignedTo())
                    .assignedUserName(getUserFullName(userMap.get(task.getAssignedTo())))
                    .blocked(isBlocked)
                    .blockedReason(task.getBlockedReason())
                    .nextActionableStep(isBlocked ? task.getBlockedReason() : "Execute task")
                    .totalTasks(1)
                    .completedTasks(task.getStatus() == TaskStatus.COMPLETED ? 1 : 0)
                    .createdAt(task.getCreatedAt())
                    .completedAt(task.getStatus() == TaskStatus.COMPLETED ? task.getUpdatedAt() : null)
                    .build());
        }

        // 5. Apply In-Memory Filters (Status, Priority, BlockedOnly, Search)
        var stream = projectedList.stream();

        if (safeFilter.getStatus() != null) {
            stream = stream.filter(item -> item.getNormalizedStatus() == safeFilter.getStatus());
        }

        if (safeFilter.getPriority() != null && !safeFilter.getPriority().isBlank()) {
            stream = stream.filter(item -> safeFilter.getPriority().equalsIgnoreCase(item.getPriority()));
        }

        if (Boolean.TRUE.equals(safeFilter.getBlockedOnly())) {
            stream = stream.filter(WorkInventoryItemDto::isBlocked);
        }

        if (StringUtils.hasText(safeFilter.getSearch())) {
            String q = safeFilter.getSearch().trim().toLowerCase();
            stream = stream.filter(item ->
                    (item.getTitle() != null && item.getTitle().toLowerCase().contains(q)) ||
                    (item.getClientName() != null && item.getClientName().toLowerCase().contains(q)) ||
                    (item.getClientPan() != null && item.getClientPan().toLowerCase().contains(q)) ||
                    (item.getRuleCode() != null && item.getRuleCode().toLowerCase().contains(q)) ||
                    (item.getEngagementName() != null && item.getEngagementName().toLowerCase().contains(q))
            );
        }

        // 6. Sort
        Comparator<WorkInventoryItemDto> comparator = Comparator.comparing(
                WorkInventoryItemDto::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())
        );

        if ("title".equalsIgnoreCase(safeFilter.getSortBy())) {
            comparator = Comparator.comparing(WorkInventoryItemDto::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else if ("priority".equalsIgnoreCase(safeFilter.getSortBy())) {
            comparator = Comparator.comparing(WorkInventoryItemDto::getPriority, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else if ("createdAt".equalsIgnoreCase(safeFilter.getSortBy())) {
            comparator = Comparator.comparing(WorkInventoryItemDto::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("DESC".equalsIgnoreCase(safeFilter.getSortDirection())) {
            comparator = comparator.reversed();
        }

        List<WorkInventoryItemDto> filteredAndSorted = stream.sorted(comparator).collect(Collectors.toList());

        // 7. Paginate
        int page = Math.max(0, safeFilter.getPage());
        int size = safeFilter.getSize() > 0 ? safeFilter.getSize() : 20;
        int totalElements = filteredAndSorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<WorkInventoryItemDto> pagedItems = filteredAndSorted.subList(fromIndex, toIndex);

        return PagedResponse.<WorkInventoryItemDto>builder()
                .content(pagedItems)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(page >= totalPages - 1 || totalPages == 0)
                .isFirst(page == 0)
                .build();
    }

    private WorkInventoryStatus mapWorkInstanceStatus(WorkInstanceStatus status, boolean isBlocked) {
        if (isBlocked || status == WorkInstanceStatus.ON_HOLD) {
            return WorkInventoryStatus.BLOCKED;
        }
        return switch (status) {
            case NOT_STARTED -> WorkInventoryStatus.NOT_STARTED;
            case IN_PROGRESS -> WorkInventoryStatus.IN_PROGRESS;
            case COMPLETED -> WorkInventoryStatus.COMPLETED;
            case CANCELLED -> WorkInventoryStatus.CANCELLED;
            case ON_HOLD -> WorkInventoryStatus.BLOCKED;
        };
    }

    private WorkInventoryStatus mapObligationStatus(ComplianceObligationStatus status) {
        return switch (status) {
            case UPCOMING, READY -> WorkInventoryStatus.NOT_STARTED;
            case IN_PROGRESS -> WorkInventoryStatus.IN_PROGRESS;
            case WAITING_FOR_CLIENT -> WorkInventoryStatus.BLOCKED;
            case READY_FOR_FILING -> WorkInventoryStatus.READY_FOR_FILING;
            case FILED -> WorkInventoryStatus.FILED;
            case COMPLETED -> WorkInventoryStatus.COMPLETED;
            case CANCELLED -> WorkInventoryStatus.CANCELLED;
            case OVERDUE -> WorkInventoryStatus.IN_PROGRESS;
        };
    }

    private WorkInventoryStatus mapTaskStatus(TaskStatus status) {
        return switch (status) {
            case TODO -> WorkInventoryStatus.NOT_STARTED;
            case IN_PROGRESS -> WorkInventoryStatus.IN_PROGRESS;
            case BLOCKED -> WorkInventoryStatus.BLOCKED;
            case UNDER_REVIEW -> WorkInventoryStatus.UNDER_REVIEW;
            case COMPLETED -> WorkInventoryStatus.COMPLETED;
            case CANCELLED -> WorkInventoryStatus.CANCELLED;
        };
    }

    private String getUserFullName(UserEntity user) {
        if (user == null) return null;
        if (StringUtils.hasText(user.getFirstName()) && StringUtils.hasText(user.getLastName())) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (StringUtils.hasText(user.getFirstName())) {
            return user.getFirstName();
        }
        return user.getEmail();
    }
}
