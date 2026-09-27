package com.taxoryn.module.task.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.task.dto.AssignWorkItemRequest;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateWorkItemStatusRequest;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.dto.WorkItemFilterRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemServiceImpl implements WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ComplianceWorkflowRepository workflowRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;
    private final TaskService taskService;

    @Override
    @Transactional
    public WorkItemDto createWorkItem(CreateWorkItemRequest request) {
        UUID organizationId = resolveOrganizationId();

        // 1. Resolve & Validate Client
        ClientEntity client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
        }

        // 2. Resolve Location Scope (from request or client)
        UUID locationId = request.getLocationId();
        if (locationId == null && client != null) {
            locationId = client.getLocationId();
        }

        // 3. Validate ABAC Access
        validateAccess(client != null ? client.getId() : null, locationId);

        // 4. Validate Workflow if provided
        if (request.getWorkflowId() != null) {
            workflowRepository.findByIdAndOrganizationId(request.getWorkflowId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkflow", "id", request.getWorkflowId()));
        }

        // 5. Build & Persist Work Item
        WorkItemEntity workItem = WorkItemEntity.builder()
                .clientId(request.getClientId())
                .locationId(locationId)
                .clientServiceId(request.getClientServiceId())
                .workflowId(request.getWorkflowId())
                .noticeId(request.getNoticeId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : WorkItemStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : com.taxoryn.module.task.model.WorkItemPriority.MEDIUM)
                .assignedUserId(request.getAssignedUserId())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .build();
        workItem.setOrganizationId(organizationId);

        if (workItem.getStatus() == WorkItemStatus.COMPLETED) {
            workItem.setCompletedAt(Instant.now());
        }

        WorkItemEntity saved = workItemRepository.save(workItem);
        log.info("Created Work Item: id={}, title={} for tenant={}", saved.getId(), saved.getTitle(), organizationId);

        auditService.logEvent("WORK_ITEM_CREATED", "WORK_ITEM", saved.getId().toString(), null, saved);
        return enrichDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkItemDto getWorkItemById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        validateAccess(workItem.getClientId(), workItem.getLocationId());
        return enrichDto(workItem);
    }

    @Override
    @Transactional
    public WorkItemDto updateWorkItem(UUID id, UpdateWorkItemRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        validateAccess(workItem.getClientId(), workItem.getLocationId());

        if (StringUtils.hasText(request.getTitle())) {
            workItem.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            workItem.setDescription(request.getDescription());
        }
        if (request.getLocationId() != null) {
            validateAccess(workItem.getClientId(), request.getLocationId());
            workItem.setLocationId(request.getLocationId());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == WorkItemStatus.COMPLETED && workItem.getStatus() != WorkItemStatus.COMPLETED) {
                workItem.setCompletedAt(Instant.now());
            } else if (request.getStatus() != WorkItemStatus.COMPLETED && workItem.getStatus() == WorkItemStatus.COMPLETED) {
                workItem.setCompletedAt(null);
            }
            workItem.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            workItem.setPriority(request.getPriority());
        }
        if (request.getAssignedUserId() != null) {
            workItem.setAssignedUserId(request.getAssignedUserId());
        }
        if (request.getNoticeId() != null) {
            workItem.setNoticeId(request.getNoticeId());
        }
        if (request.getDueDate() != null) {
            workItem.setDueDate(request.getDueDate());
        }
        if (request.getNotes() != null) {
            workItem.setNotes(request.getNotes());
        }

        WorkItemEntity updated = workItemRepository.save(workItem);
        auditService.logEvent("WORK_ITEM_UPDATED", "WORK_ITEM", updated.getId().toString(), null, updated);
        return enrichDto(updated);
    }

    @Override
    @Transactional
    public WorkItemDto updateWorkItemStatus(UUID id, UpdateWorkItemStatusRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        validateAccess(workItem.getClientId(), workItem.getLocationId());

        if (request.getStatus() == null) {
            throw new BusinessValidationException("Work Item status cannot be null");
        }

        if (request.getStatus() == WorkItemStatus.COMPLETED && workItem.getStatus() != WorkItemStatus.COMPLETED) {
            workItem.setCompletedAt(Instant.now());
        } else if (request.getStatus() != WorkItemStatus.COMPLETED && workItem.getStatus() == WorkItemStatus.COMPLETED) {
            workItem.setCompletedAt(null);
        }

        workItem.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getNotes())) {
            workItem.setNotes(request.getNotes());
        }

        WorkItemEntity updated = workItemRepository.save(workItem);
        auditService.logEvent("WORK_ITEM_STATUS_UPDATED", "WORK_ITEM", updated.getId().toString(), null, updated);
        return enrichDto(updated);
    }

    @Override
    @Transactional
    public WorkItemDto assignWorkItem(UUID id, AssignWorkItemRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        validateAccess(workItem.getClientId(), workItem.getLocationId());

        if (request.getAssignedUserId() == null) {
            throw new BusinessValidationException("Assigned user ID is required");
        }

        userRepository.findByIdAndOrganizationId(request.getAssignedUserId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedUserId()));

        workItem.setAssignedUserId(request.getAssignedUserId());
        if (StringUtils.hasText(request.getNotes())) {
            workItem.setNotes(request.getNotes());
        }

        WorkItemEntity updated = workItemRepository.save(workItem);
        auditService.logEvent("WORK_ITEM_ASSIGNED", "WORK_ITEM", updated.getId().toString(), null, updated);
        return enrichDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkItemDto> getWorkItems(WorkItemFilterRequest filterRequest) {
        UUID organizationId = resolveOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<WorkItemEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Location Scoping
            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
                Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);

                Predicate locPredicate = null;
                if (accessibleLocs != null && !accessibleLocs.isEmpty()) {
                    locPredicate = root.get("locationId").in(accessibleLocs);
                }

                Predicate clientPredicate = null;
                if (accessibleClients != null && !accessibleClients.isEmpty()) {
                    clientPredicate = root.get("clientId").in(accessibleClients);
                }

                Predicate assigneePredicate = null;
                if (scope.getAccessibleAssigneeIds() != null && !scope.getAccessibleAssigneeIds().isEmpty()) {
                    assigneePredicate = root.get("assignedUserId").in(scope.getAccessibleAssigneeIds());
                }

                if (locPredicate != null && clientPredicate != null) {
                    predicates.add(cb.or(locPredicate, clientPredicate));
                } else if (locPredicate != null) {
                    predicates.add(locPredicate);
                } else if (clientPredicate != null) {
                    predicates.add(clientPredicate);
                } else if (assigneePredicate != null) {
                    predicates.add(assigneePredicate);
                }
            }

            // Filters
            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }
            if (filterRequest.getLocationId() != null) {
                predicates.add(cb.equal(root.get("locationId"), filterRequest.getLocationId()));
            }
            if (filterRequest.getWorkflowId() != null) {
                predicates.add(cb.equal(root.get("workflowId"), filterRequest.getWorkflowId()));
            }
            if (filterRequest.getClientServiceId() != null) {
                predicates.add(cb.equal(root.get("clientServiceId"), filterRequest.getClientServiceId()));
            }
            if (filterRequest.getAssignedUserId() != null) {
                predicates.add(cb.equal(root.get("assignedUserId"), filterRequest.getAssignedUserId()));
            }
            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }
            if (filterRequest.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filterRequest.getPriority()));
            }
            if (filterRequest.getDueBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filterRequest.getDueBefore()));
            }
            if (filterRequest.getDueAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filterRequest.getDueAfter()));
            }
            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.getSortDirection()), filterRequest.getSortBy());
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), sort);
        Page<WorkItemEntity> page = workItemRepository.findAll(spec, pageable);

        List<WorkItemDto> content = enrichDtoList(page.getContent());
        return PagedResponse.<WorkItemDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkItemDto> getWorkItemsByClientId(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<WorkItemEntity> items = workItemRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        return enrichDtoList(items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkItemDto> getWorkItemsByWorkflowId(UUID workflowId) {
        UUID organizationId = resolveOrganizationId();
        workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkflow", "id", workflowId));

        List<WorkItemEntity> items = workItemRepository.findAllByOrganizationIdAndWorkflowIdOrderByCreatedAtDesc(organizationId, workflowId);
        return enrichDtoList(items);
    }

    @Override
    @Transactional
    public TaskDto createTaskForWorkItem(UUID workItemId, CreateTaskRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(workItemId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        validateAccess(workItem.getClientId(), workItem.getLocationId());

        request.setWorkItemId(workItemId);
        if (request.getClientId() == null) {
            request.setClientId(workItem.getClientId());
        }
        if (request.getLocationId() == null) {
            request.setLocationId(workItem.getLocationId());
        }

        return taskService.createTask(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasksForWorkItem(UUID workItemId) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(workItemId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        validateAccess(workItem.getClientId(), workItem.getLocationId());

        List<TaskEntity> tasks = taskRepository.findAllByOrganizationIdAndWorkItemIdOrderByCreatedAtAsc(organizationId, workItemId);
        return tasks.stream().map(t -> taskService.getTaskById(t.getId())).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteWorkItem(UUID id) {
        UUID organizationId = resolveOrganizationId();
        WorkItemEntity workItem = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        validateAccess(workItem.getClientId(), workItem.getLocationId());

        workItem.setStatus(WorkItemStatus.CANCELLED);
        workItemRepository.save(workItem);
        auditService.logEvent("WORK_ITEM_CANCELLED", "WORK_ITEM", workItem.getId().toString(), null, workItem);
    }

    // --- Helper Methods ---

    private void validateAccess(UUID clientId, UUID locationId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (scope.isFirmAdmin()) {
            return; // Firm Admin has unrestricted access
        }

        // 1. Check Location Scope
        if (locationId != null) {
            Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
            if (accessibleLocs != null && !accessibleLocs.isEmpty() && !accessibleLocs.contains(locationId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this location");
            }
        }

        // 2. Check Client Portfolio Scope
        if (clientId != null) {
            Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClients != null && !accessibleClients.contains(clientId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this client");
            }
        }
    }

    private WorkItemDto enrichDto(WorkItemEntity entity) {
        UUID orgId = entity.getOrganizationId();
        String clientName = null;
        if (entity.getClientId() != null) {
            clientName = clientRepository.findByIdAndOrganizationId(entity.getClientId(), orgId)
                    .map(ClientEntity::getDisplayName)
                    .orElse(null);
        }

        String locationName = null;
        if (entity.getLocationId() != null) {
            locationName = locationRepository.findByIdAndOrganizationId(entity.getLocationId(), orgId)
                    .map(LocationEntity::getName)
                    .orElse(null);
        }

        String assignedUserName = null;
        if (entity.getAssignedUserId() != null) {
            assignedUserName = userRepository.findByIdAndOrganizationId(entity.getAssignedUserId(), orgId)
                    .map(UserEntity::getFullName)
                    .orElse(null);
        }

        int totalTasks = (int) taskRepository.countByOrganizationIdAndWorkItemId(orgId, entity.getId());
        int completedTasks = (int) taskRepository.countByOrganizationIdAndWorkItemIdAndStatus(orgId, entity.getId(), TaskStatus.COMPLETED);

        return WorkItemDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .clientId(entity.getClientId())
                .clientName(clientName)
                .clientServiceId(entity.getClientServiceId())
                .workflowId(entity.getWorkflowId())
                .noticeId(entity.getNoticeId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .assignedUserId(entity.getAssignedUserId())
                .assignedUserName(assignedUserName)
                .dueDate(entity.getDueDate())
                .completedAt(entity.getCompletedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .build();
    }

    private List<WorkItemDto> enrichDtoList(List<WorkItemEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        UUID orgId = entities.get(0).getOrganizationId();
        Map<UUID, String> clientNames = clientRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, ClientEntity::getDisplayName, (a, b) -> a));
        Map<UUID, String> locationNames = locationRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(LocationEntity::getId, LocationEntity::getName, (a, b) -> a));
        Map<UUID, String> userNames = userRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName, (a, b) -> a));

        return entities.stream().map(e -> {
            int totalTasks = (int) taskRepository.countByOrganizationIdAndWorkItemId(orgId, e.getId());
            int completedTasks = (int) taskRepository.countByOrganizationIdAndWorkItemIdAndStatus(orgId, e.getId(), TaskStatus.COMPLETED);

            return WorkItemDto.builder()
                    .id(e.getId())
                    .organizationId(e.getOrganizationId())
                    .locationId(e.getLocationId())
                    .locationName(e.getLocationId() != null ? locationNames.get(e.getLocationId()) : null)
                    .clientId(e.getClientId())
                    .clientName(e.getClientId() != null ? clientNames.get(e.getClientId()) : null)
                    .clientServiceId(e.getClientServiceId())
                    .workflowId(e.getWorkflowId())
                    .noticeId(e.getNoticeId())
                    .title(e.getTitle())
                    .description(e.getDescription())
                    .status(e.getStatus())
                    .priority(e.getPriority())
                    .assignedUserId(e.getAssignedUserId())
                    .assignedUserName(e.getAssignedUserId() != null ? userNames.get(e.getAssignedUserId()) : null)
                    .dueDate(e.getDueDate())
                    .completedAt(e.getCompletedAt())
                    .notes(e.getNotes())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .createdBy(e.getCreatedBy())
                    .updatedBy(e.getUpdatedBy())
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .build();
        }).collect(Collectors.toList());
    }

    private UUID resolveOrganizationId() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            orgId = TenantContext.getTenantId();
        }
        if (orgId == null) {
            throw new IllegalStateException("Organization context is missing");
        }
        return orgId;
    }
}
