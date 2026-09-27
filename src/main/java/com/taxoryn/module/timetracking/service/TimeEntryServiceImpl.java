package com.taxoryn.module.timetracking.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.timetracking.dto.CreateTimeEntryRequest;
import com.taxoryn.module.timetracking.dto.TimeEntryDto;
import com.taxoryn.module.timetracking.dto.TimeEntryFilterRequest;
import com.taxoryn.module.timetracking.dto.UpdateTimeEntryRequest;
import com.taxoryn.module.timetracking.entity.TimeEntryEntity;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import com.taxoryn.module.timetracking.repository.TimeEntryRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeEntryServiceImpl implements TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final ClientRepository clientRepository;
    private final EngagementRepository engagementRepository;
    private final WorkItemRepository workItemRepository;
    private final TaskRepository taskRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional
    public TimeEntryDto createTimeEntry(CreateTimeEntryRequest request) {
        UUID organizationId = resolveOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        UUID locationId = request.getLocationId() != null ? request.getLocationId() : client.getLocationId();

        validateAccess(client.getId(), locationId);

        if (request.getEngagementId() != null) {
            engagementRepository.findByIdAndOrganizationId(request.getEngagementId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", request.getEngagementId()));
        }
        if (request.getWorkItemId() != null) {
            workItemRepository.findByIdAndOrganizationId(request.getWorkItemId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", request.getWorkItemId()));
        }
        if (request.getTaskId() != null) {
            taskRepository.findByIdAndOrganizationId(request.getTaskId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));
        }

        UUID userId = request.getUserId() != null ? request.getUserId() : SecurityUtils.getCurrentUserId();

        TimeEntryEntity timeEntry = TimeEntryEntity.builder()
                .locationId(locationId)
                .clientId(client.getId())
                .engagementId(request.getEngagementId())
                .workItemId(request.getWorkItemId())
                .taskId(request.getTaskId())
                .userId(userId)
                .entryDate(request.getEntryDate())
                .durationMinutes(request.getDurationMinutes())
                .description(request.getDescription())
                .billable(request.getBillable() != null ? request.getBillable() : true)
                .billingRate(request.getBillingRate())
                .status(request.getStatus() != null ? request.getStatus() : TimeEntryStatus.SUBMITTED)
                .build();
        timeEntry.setOrganizationId(organizationId);

        timeEntry = timeEntryRepository.save(timeEntry);
        log.info("Created Time Entry: id={}, minutes={} for tenant={}", timeEntry.getId(), timeEntry.getDurationMinutes(), organizationId);

        auditService.logEvent("TIME_ENTRY_CREATED", "TIME_ENTRY", timeEntry.getId().toString(), null, timeEntry);

        return enrichDto(timeEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeEntryDto getTimeEntryById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        TimeEntryEntity timeEntry = timeEntryRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeEntry", "id", id));

        validateAccess(timeEntry.getClientId(), timeEntry.getLocationId());

        return enrichDto(timeEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TimeEntryDto> getTimeEntries(TimeEntryFilterRequest filterRequest) {
        UUID organizationId = resolveOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<TimeEntryEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
                if (accessibleLocs != null && !accessibleLocs.isEmpty()) {
                    predicates.add(root.get("locationId").in(accessibleLocs));
                }
                Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (accessibleClients != null) {
                    predicates.add(root.get("clientId").in(accessibleClients));
                }
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }
            if (filterRequest.getLocationId() != null) {
                predicates.add(cb.equal(root.get("locationId"), filterRequest.getLocationId()));
            }
            if (filterRequest.getEngagementId() != null) {
                predicates.add(cb.equal(root.get("engagementId"), filterRequest.getEngagementId()));
            }
            if (filterRequest.getWorkItemId() != null) {
                predicates.add(cb.equal(root.get("workItemId"), filterRequest.getWorkItemId()));
            }
            if (filterRequest.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), filterRequest.getTaskId()));
            }
            if (filterRequest.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filterRequest.getUserId()));
            }
            if (filterRequest.getBillable() != null) {
                predicates.add(cb.equal(root.get("billable"), filterRequest.getBillable()));
            }
            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }
            if (filterRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), filterRequest.getStartDate()));
            }
            if (filterRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), filterRequest.getEndDate()));
            }
            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("description")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.getSortDirection()), filterRequest.getSortBy());
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), sort);
        Page<TimeEntryEntity> page = timeEntryRepository.findAll(spec, pageable);

        List<TimeEntryDto> content = enrichDtoList(page.getContent());
        return PagedResponse.<TimeEntryDto>builder()
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
    public List<TimeEntryDto> getTimeEntriesByClientId(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<TimeEntryEntity> list = timeEntryRepository.findAllByOrganizationIdAndClientIdOrderByEntryDateDesc(organizationId, clientId);
        return enrichDtoList(list);
    }

    @Override
    @Transactional
    public TimeEntryDto updateTimeEntry(UUID id, UpdateTimeEntryRequest request) {
        UUID organizationId = resolveOrganizationId();
        TimeEntryEntity timeEntry = timeEntryRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeEntry", "id", id));

        validateAccess(timeEntry.getClientId(), timeEntry.getLocationId());

        if (request.getLocationId() != null) {
            timeEntry.setLocationId(request.getLocationId());
        }
        if (request.getEngagementId() != null) {
            timeEntry.setEngagementId(request.getEngagementId());
        }
        if (request.getWorkItemId() != null) {
            timeEntry.setWorkItemId(request.getWorkItemId());
        }
        if (request.getTaskId() != null) {
            timeEntry.setTaskId(request.getTaskId());
        }
        if (request.getUserId() != null) {
            timeEntry.setUserId(request.getUserId());
        }
        if (request.getEntryDate() != null) {
            timeEntry.setEntryDate(request.getEntryDate());
        }
        if (request.getDurationMinutes() != null) {
            timeEntry.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getDescription() != null) {
            timeEntry.setDescription(request.getDescription());
        }
        if (request.getBillable() != null) {
            timeEntry.setBillable(request.getBillable());
        }
        if (request.getBillingRate() != null) {
            timeEntry.setBillingRate(request.getBillingRate());
        }
        if (request.getStatus() != null) {
            timeEntry.setStatus(request.getStatus());
        }

        timeEntry = timeEntryRepository.save(timeEntry);
        log.info("Updated Time Entry: id={}", timeEntry.getId());

        auditService.logEvent("TIME_ENTRY_UPDATED", "TIME_ENTRY", timeEntry.getId().toString(), null, timeEntry);

        return enrichDto(timeEntry);
    }

    @Override
    @Transactional
    public void deleteTimeEntry(UUID id) {
        UUID organizationId = resolveOrganizationId();
        TimeEntryEntity timeEntry = timeEntryRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeEntry", "id", id));

        validateAccess(timeEntry.getClientId(), timeEntry.getLocationId());

        timeEntryRepository.delete(timeEntry);
        log.info("Deleted Time Entry: id={}", id);

        auditService.logEvent("TIME_ENTRY_DELETED", "TIME_ENTRY", id.toString(), null, null);
    }

    // --- Helper Methods ---

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }

    private void validateAccess(UUID clientId, UUID locationId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (scope.isFirmAdmin()) {
            return;
        }

        if (locationId != null) {
            Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
            if (accessibleLocs != null && !accessibleLocs.isEmpty() && !accessibleLocs.contains(locationId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this location");
            }
        }

        if (clientId != null) {
            Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClients != null && !accessibleClients.contains(clientId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this client");
            }
        }
    }

    private TimeEntryDto enrichDto(TimeEntryEntity entity) {
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

        String engagementName = null;
        if (entity.getEngagementId() != null) {
            engagementName = engagementRepository.findByIdAndOrganizationId(entity.getEngagementId(), orgId)
                    .map(EngagementEntity::getName)
                    .orElse(null);
        }

        String workItemTitle = null;
        if (entity.getWorkItemId() != null) {
            workItemTitle = workItemRepository.findByIdAndOrganizationId(entity.getWorkItemId(), orgId)
                    .map(WorkItemEntity::getTitle)
                    .orElse(null);
        }

        String taskTitle = null;
        if (entity.getTaskId() != null) {
            taskTitle = taskRepository.findByIdAndOrganizationId(entity.getTaskId(), orgId)
                    .map(TaskEntity::getTitle)
                    .orElse(null);
        }

        String userName = null;
        if (entity.getUserId() != null) {
            userName = userRepository.findByIdAndOrganizationId(entity.getUserId(), orgId)
                    .map(UserEntity::getFullName)
                    .orElse(null);
        }

        return TimeEntryDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .clientId(entity.getClientId())
                .clientName(clientName)
                .engagementId(entity.getEngagementId())
                .engagementName(engagementName)
                .workItemId(entity.getWorkItemId())
                .workItemTitle(workItemTitle)
                .taskId(entity.getTaskId())
                .taskTitle(taskTitle)
                .userId(entity.getUserId())
                .userName(userName)
                .entryDate(entity.getEntryDate())
                .durationMinutes(entity.getDurationMinutes())
                .description(entity.getDescription())
                .billable(entity.getBillable())
                .billingRate(entity.getBillingRate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private List<TimeEntryDto> enrichDtoList(List<TimeEntryEntity> entities) {
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
        Map<UUID, String> engagementNames = engagementRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(EngagementEntity::getId, EngagementEntity::getName, (a, b) -> a));

        return entities.stream().map(e -> TimeEntryDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .locationId(e.getLocationId())
                .locationName(e.getLocationId() != null ? locationNames.get(e.getLocationId()) : null)
                .clientId(e.getClientId())
                .clientName(e.getClientId() != null ? clientNames.get(e.getClientId()) : null)
                .engagementId(e.getEngagementId())
                .engagementName(e.getEngagementId() != null ? engagementNames.get(e.getEngagementId()) : null)
                .workItemId(e.getWorkItemId())
                .taskId(e.getTaskId())
                .userId(e.getUserId())
                .userName(e.getUserId() != null ? userNames.get(e.getUserId()) : null)
                .entryDate(e.getEntryDate())
                .durationMinutes(e.getDurationMinutes())
                .description(e.getDescription())
                .billable(e.getBillable())
                .billingRate(e.getBillingRate())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build()).collect(Collectors.toList());
    }
}
