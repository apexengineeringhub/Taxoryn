package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.service.service.ServiceCatalogService;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.dto.EnableEngagementTemplateRequest;
import com.taxoryn.module.worktemplate.dto.EngagementWorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.GenerateWorkInstanceRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkInstanceStatusRequest;
import com.taxoryn.module.worktemplate.dto.WorkInstanceDto;
import com.taxoryn.module.worktemplate.dto.WorkInstanceTaskDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateTaskDto;
import com.taxoryn.module.worktemplate.entity.EngagementWorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateTaskEntity;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.repository.EngagementWorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngagementWorkServiceImpl implements EngagementWorkService {

    private final EngagementRepository engagementRepository;
    private final WorkTemplateRepository workTemplateRepository;
    private final WorkTemplateTaskRepository workTemplateTaskRepository;
    private final EngagementWorkTemplateRepository engagementWorkTemplateRepository;
    private final WorkInstanceRepository workInstanceRepository;
    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkTemplateDto> getAvailableTemplatesForEngagement(UUID engagementId) {
        UUID organizationId = resolveOrganizationId();
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", engagementId));

        if (engagement.getServiceId() == null) {
            return List.of();
        }

        List<WorkTemplateEntity> activeTemplates = workTemplateRepository.findActiveTemplatesForService(engagement.getServiceId(), organizationId);
        return activeTemplates.stream().map(this::mapTemplateDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EngagementWorkTemplateDto> getEnabledTemplatesForEngagement(UUID engagementId) {
        UUID organizationId = resolveOrganizationId();
        engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", engagementId));

        List<EngagementWorkTemplateEntity> list = engagementWorkTemplateRepository.findAllByOrganizationIdAndEngagementId(organizationId, engagementId);
        return list.stream().map(this::mapEngagementTemplateDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EngagementWorkTemplateDto enableTemplateForEngagement(UUID engagementId, UUID templateId, EnableEngagementTemplateRequest request) {
        UUID organizationId = resolveOrganizationId();

        // 1. Engagement Ownership Validation
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", engagementId));

        // 2. Template Access & Status Validation
        WorkTemplateEntity template = workTemplateRepository.findAccessibleById(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", templateId));

        if (template.getStatus() != WorkTemplateStatus.ACTIVE) {
            throw new BusinessValidationException("Cannot attach inactive or archived work template to an engagement.");
        }

        // 3. CRITICAL BUSINESS RULE (Requirement 22): Template Service MUST match Engagement Service
        if (engagement.getServiceId() != null && !template.getServiceId().equals(engagement.getServiceId())) {
            ServiceEntity tplService = serviceRepository.findById(template.getServiceId()).orElse(null);
            ServiceEntity engService = serviceRepository.findById(engagement.getServiceId()).orElse(null);
            String tplServiceName = tplService != null ? tplService.getServiceName() : "Unknown Service";
            String engServiceName = engService != null ? engService.getServiceName() : "Custom Service";

            throw new BusinessValidationException(
                    String.format(
                            "TEMPLATE_SERVICE_MISMATCH: Template '%s' belongs to service '%s' and cannot be attached to engagement '%s' which is configured for service '%s'.",
                            template.getName(), tplServiceName, engagement.getName(), engServiceName
                    )
            );
        }

        // 4. Service Entitlement Verification
        ServiceEntity service = serviceRepository.findById(template.getServiceId()).orElse(null);
        if (service != null && !serviceCatalogService.isServiceAvailableForPractice(organizationId, service)) {
            throw new BusinessValidationException(
                    "SERVICE_NOT_AVAILABLE: The service associated with this template is not entitled under your practice plan."
            );
        }

        // 5. Create or Update Association
        EngagementWorkTemplateEntity association = engagementWorkTemplateRepository
                .findByOrganizationIdAndEngagementIdAndTemplateId(organizationId, engagementId, templateId)
                .orElse(EngagementWorkTemplateEntity.builder()
                        .engagementId(engagement.getId())
                        .templateId(template.getId())
                        .build());
        association.setOrganizationId(organizationId);
        association.setActive(true);

        if (request != null) {
            association.setRecurrenceType(request.getRecurrenceType() != null ? request.getRecurrenceType() : template.getRecurrenceType());
            association.setRecurrenceInterval(request.getRecurrenceInterval() != null ? Math.max(1, request.getRecurrenceInterval()) : template.getRecurrenceInterval());
            association.setDayOfMonth(request.getDayOfMonth() != null ? request.getDayOfMonth() : template.getDayOfMonth());
            association.setMonthOfYear(request.getMonthOfYear() != null ? request.getMonthOfYear() : template.getMonthOfYear());
            association.setStartDate(request.getStartDate() != null ? request.getStartDate() : engagement.getStartDate());
            association.setEndDate(request.getEndDate() != null ? request.getEndDate() : engagement.getEndDate());
        } else {
            association.setRecurrenceType(template.getRecurrenceType());
            association.setRecurrenceInterval(template.getRecurrenceInterval());
            association.setDayOfMonth(template.getDayOfMonth());
            association.setMonthOfYear(template.getMonthOfYear());
            association.setStartDate(engagement.getStartDate());
            association.setEndDate(engagement.getEndDate());
        }

        association = engagementWorkTemplateRepository.save(association);
        log.info("Enabled WorkTemplate id={} for Engagement id={}", templateId, engagementId);

        // Audit Logging
        auditService.logEvent("TEMPLATE_ASSIGNED_TO_ENGAGEMENT", "ENGAGEMENT", engagementId.toString(), null, association);

        return mapEngagementTemplateDto(association);
    }

    @Override
    @Transactional
    public void disableTemplateForEngagement(UUID engagementId, UUID templateId) {
        UUID organizationId = resolveOrganizationId();
        EngagementWorkTemplateEntity association = engagementWorkTemplateRepository
                .findByOrganizationIdAndEngagementIdAndTemplateId(organizationId, engagementId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("EngagementWorkTemplate", "templateId", templateId));

        association.setActive(false);
        engagementWorkTemplateRepository.save(association);
        log.info("Disabled WorkTemplate id={} for Engagement id={}", templateId, engagementId);

        auditService.logEvent("TEMPLATE_DISABLED_FOR_ENGAGEMENT", "ENGAGEMENT", engagementId.toString(), null, association);
    }

    @Override
    @Transactional
    public WorkInstanceDto generateWorkInstance(UUID engagementId, GenerateWorkInstanceRequest request) {
        UUID organizationId = resolveOrganizationId();

        // 1. Engagement & Client Validation
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", engagementId));

        // 2. Template Resolution
        WorkTemplateEntity template = workTemplateRepository.findAccessibleById(request.getTemplateId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", request.getTemplateId()));

        if (template.getStatus() != WorkTemplateStatus.ACTIVE) {
            throw new BusinessValidationException("Cannot generate work from an inactive or archived template.");
        }

        // 3. Service Match Validation
        if (engagement.getServiceId() != null && !template.getServiceId().equals(engagement.getServiceId())) {
            throw new BusinessValidationException("TEMPLATE_SERVICE_MISMATCH: Template service does not match engagement service.");
        }

        // 4. Calculate Period Start, Period End, and Due Date
        LocalDate periodStart = request.getPeriodStart();
        if (periodStart == null) {
            EngagementWorkTemplateEntity association = engagementWorkTemplateRepository
                    .findByOrganizationIdAndEngagementIdAndTemplateId(organizationId, engagementId, template.getId())
                    .orElse(null);

            if (association != null && association.getLastGeneratedPeriodStart() != null) {
                periodStart = association.getRecurrenceType().calculateNextPeriodStart(
                        association.getLastGeneratedPeriodStart(), association.getRecurrenceInterval()
                );
            } else {
                periodStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            }
        }

        LocalDate periodEnd = request.getPeriodEnd();
        if (periodEnd == null) {
            periodEnd = template.getRecurrenceType().calculatePeriodEnd(periodStart, template.getRecurrenceInterval());
        }

        if (periodStart.isAfter(periodEnd)) {
            throw new BusinessValidationException("Period start date cannot be after period end date.");
        }

        LocalDate dueDate = request.getDueDate();
        if (dueDate == null) {
            dueDate = calculateWorkInstanceDueDate(periodStart, periodEnd, template);
        }

        // 5. CRITICAL DUPLICATE WORK PROTECTION (Requirement 11)
        if (workInstanceRepository.existsByOrganizationIdAndEngagementIdAndTemplateIdAndPeriodStartAndPeriodEnd(
                organizationId, engagementId, template.getId(), periodStart, periodEnd)) {
            throw new DuplicateResourceException(
                    "WorkInstance",
                    "period",
                    String.format("Work instance for period %s to %s already exists for this engagement.", periodStart, periodEnd)
            );
        }

        // 6. Resolve Work Title & Personnel
        String title = StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim()
                : generateWorkInstanceTitle(engagement, template, periodStart, periodEnd);

        UUID assignedUserId = request.getAssignedUserId() != null
                ? request.getAssignedUserId()
                : engagement.getAssignedUserId();

        UUID reviewerUserId = request.getReviewerUserId() != null
                ? request.getReviewerUserId()
                : engagement.getReviewerUserId();

        // 7. Create Work Instance
        WorkInstanceEntity instance = WorkInstanceEntity.builder()
                .engagementId(engagement.getId())
                .templateId(template.getId())
                .title(title)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(dueDate)
                .status(WorkInstanceStatus.NOT_STARTED)
                .assignedUserId(assignedUserId)
                .reviewerUserId(reviewerUserId)
                .generatedAt(Instant.now())
                .notes(request.getNotes())
                .build();
        instance.setOrganizationId(organizationId);

        WorkInstanceEntity savedInstance = workInstanceRepository.save(instance);
        log.info("Generated WorkInstance: id={}, title={}, org={}", savedInstance.getId(), savedInstance.getTitle(), organizationId);

        // 8. Instantiate Unified TaskEntity records from Template Tasks
        List<WorkTemplateTaskEntity> templateTasks = workTemplateTaskRepository
                .findAllByTemplateIdOrderBySequenceOrderAsc(template.getId());

        TaskCategory taskCategory = mapServiceCategoryToTaskCategory(template.getCategory());
        String periodFormatted = formatPeriodHeader(periodStart);

        for (WorkTemplateTaskEntity tplTask : templateTasks) {
            if (!tplTask.isActive()) {
                continue;
            }

            LocalDate taskDueDate = periodStart.plusDays(tplTask.getRelativeDueDays());

            TaskEntity task = TaskEntity.builder()
                    .clientId(engagement.getClientId())
                    .engagementId(engagement.getId())
                    .assignedTo(assignedUserId)
                    .workInstanceId(savedInstance.getId())
                    .workTemplateTaskId(tplTask.getId())
                    .locationId(engagement.getLocationId())
                    .title(tplTask.getName() + " — " + periodFormatted)
                    .description(tplTask.getDescription())
                    .taskCategory(taskCategory)
                    .status(TaskStatus.TODO)
                    .priority(tplTask.getDefaultPriority())
                    .startDate(periodStart)
                    .dueDate(taskDueDate)
                    .notes("Generated from Work Template: " + template.getName())
                    .build();
            task.setOrganizationId(organizationId);
            taskRepository.save(task);
        }

        // 9. Update Engagement Work Template tracking dates
        engagementWorkTemplateRepository.findByOrganizationIdAndEngagementIdAndTemplateId(organizationId, engagementId, template.getId())
                .ifPresent(assoc -> {
                    assoc.setLastGeneratedPeriodStart(savedInstance.getPeriodStart());
                    assoc.setLastGeneratedPeriodEnd(savedInstance.getPeriodEnd());
                    engagementWorkTemplateRepository.save(assoc);
                });

        // 10. Audit Logging
        auditService.logEvent("WORK_INSTANCE_CREATED", "WORK_INSTANCE", savedInstance.getId().toString(), null, savedInstance);

        return enrichWorkInstanceDto(savedInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkInstanceDto> getWorkInstancesForEngagement(UUID engagementId, PageRequestDto pageRequest) {
        UUID organizationId = resolveOrganizationId();
        engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", engagementId));

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequest.getSortDirection()), pageRequest.getSortBy());
        Pageable pageable = PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
        Page<WorkInstanceEntity> page = workInstanceRepository.findAllByOrganizationIdAndEngagementId(organizationId, engagementId, pageable);

        List<WorkInstanceDto> content = page.getContent().stream().map(this::enrichWorkInstanceDto).collect(Collectors.toList());

        return PagedResponse.<WorkInstanceDto>builder()
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
    public WorkInstanceDto getWorkInstanceById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        WorkInstanceEntity instance = workInstanceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkInstance", "id", id));

        return enrichWorkInstanceDto(instance);
    }

    @Override
    @Transactional
    public WorkInstanceDto updateWorkInstanceStatus(UUID id, UpdateWorkInstanceStatusRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkInstanceEntity instance = workInstanceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkInstance", "id", id));

        WorkInstanceStatus previousStatus = instance.getStatus();
        WorkInstanceStatus targetStatus = request.getStatus();

        previousStatus.validateTransition(targetStatus);
        instance.setStatus(targetStatus);

        if (targetStatus == WorkInstanceStatus.COMPLETED) {
            instance.setCompletedAt(Instant.now());
        }
        if (request.getNotes() != null) {
            instance.setNotes(request.getNotes());
        }

        instance = workInstanceRepository.save(instance);
        log.info("Updated WorkInstance id={} status to {}", id, targetStatus);

        String auditAction = switch (targetStatus) {
            case COMPLETED -> "WORK_INSTANCE_COMPLETED";
            case CANCELLED -> "WORK_INSTANCE_CANCELLED";
            default -> "WORK_INSTANCE_STATUS_CHANGED";
        };
        auditService.logEvent(auditAction, "WORK_INSTANCE", id.toString(), previousStatus, targetStatus);

        return enrichWorkInstanceDto(instance);
    }

    // --- Helpers ---

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }

    private String generateWorkInstanceTitle(EngagementEntity engagement, WorkTemplateEntity template, LocalDate start, LocalDate end) {
        String periodText = formatPeriodHeader(start);
        return String.format("%s — %s", template.getName(), periodText);
    }

    private LocalDate calculateWorkInstanceDueDate(LocalDate start, LocalDate end, WorkTemplateEntity template) {
        if (template.getCategory() == ServiceCategory.GST) {
            // Standard GSTR-3B due date is 20th of next month
            return end.plusMonths(1).withDayOfMonth(Math.min(20, end.plusMonths(1).lengthOfMonth()));
        } else if (template.getCategory() == ServiceCategory.TDS) {
            // Quarterly TDS due date is last day of following month
            return end.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        }
        return end.plusDays(15);
    }

    private String formatPeriodHeader(LocalDate date) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        return date.format(fmt);
    }

    private TaskCategory mapServiceCategoryToTaskCategory(ServiceCategory category) {
        if (category == null) {
            return TaskCategory.OTHER;
        }
        return switch (category) {
            case GST -> TaskCategory.GST;
            case TDS -> TaskCategory.TDS;
            case ITR -> TaskCategory.ITR;
            case AUDIT -> TaskCategory.AUDIT;
            case NOTICE -> TaskCategory.NOTICE;
            default -> TaskCategory.COMPLIANCE;
        };
    }

    private WorkTemplateDto mapTemplateDto(WorkTemplateEntity entity) {
        String serviceName = null;
        String serviceCode = null;
        if (entity.getServiceId() != null) {
            ServiceEntity service = serviceRepository.findById(entity.getServiceId()).orElse(null);
            if (service != null) {
                serviceName = service.getServiceName();
                serviceCode = service.getServiceCode();
            }
        }

        List<WorkTemplateTaskEntity> tasks = workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(entity.getId());
        List<WorkTemplateTaskDto> taskDtos = tasks.stream().map(t -> WorkTemplateTaskDto.builder()
                .id(t.getId())
                .templateId(t.getTemplateId())
                .name(t.getName())
                .description(t.getDescription())
                .sequenceOrder(t.getSequenceOrder())
                .defaultAssigneeRole(t.getDefaultAssigneeRole())
                .defaultPriority(t.getDefaultPriority())
                .relativeDueDays(t.getRelativeDueDays())
                .mandatory(t.isMandatory())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .version(t.getVersion() != null ? t.getVersion() : 0L)
                .build()).collect(Collectors.toList());

        return WorkTemplateDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .serviceId(entity.getServiceId())
                .serviceName(serviceName)
                .serviceCode(serviceCode)
                .templateCode(entity.getTemplateCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .status(entity.getStatus())
                .templateType(entity.getTemplateType())
                .recurrenceType(entity.getRecurrenceType())
                .recurrenceInterval(entity.getRecurrenceInterval())
                .dayOfMonth(entity.getDayOfMonth())
                .monthOfYear(entity.getMonthOfYear())
                .recurrenceEnabled(entity.isRecurrenceEnabled())
                .isSystemDefault(entity.isSystemDefault())
                .taskCount(taskDtos.size())
                .tasks(taskDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion() != null ? entity.getVersion() : 0L)
                .build();
    }

    private EngagementWorkTemplateDto mapEngagementTemplateDto(EngagementWorkTemplateEntity entity) {
        WorkTemplateEntity template = workTemplateRepository.findById(entity.getTemplateId()).orElse(null);
        String tplCode = template != null ? template.getTemplateCode() : null;
        String tplName = template != null ? template.getName() : null;
        String tplDesc = template != null ? template.getDescription() : null;
        ServiceCategory category = template != null ? template.getCategory() : null;
        int taskCount = template != null ? workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(template.getId()).size() : 0;

        return EngagementWorkTemplateDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .engagementId(entity.getEngagementId())
                .templateId(entity.getTemplateId())
                .templateCode(tplCode)
                .templateName(tplName)
                .templateDescription(tplDesc)
                .category(category)
                .active(entity.isActive())
                .recurrenceType(entity.getRecurrenceType())
                .recurrenceInterval(entity.getRecurrenceInterval())
                .dayOfMonth(entity.getDayOfMonth())
                .monthOfYear(entity.getMonthOfYear())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .lastGeneratedPeriodStart(entity.getLastGeneratedPeriodStart())
                .lastGeneratedPeriodEnd(entity.getLastGeneratedPeriodEnd())
                .taskCount(taskCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WorkInstanceDto enrichWorkInstanceDto(WorkInstanceEntity entity) {
        UUID orgId = entity.getOrganizationId();
        EngagementEntity engagement = engagementRepository.findById(entity.getEngagementId()).orElse(null);
        String engCode = engagement != null ? engagement.getEngagementCode() : null;
        String engName = engagement != null ? engagement.getName() : null;
        UUID clientId = engagement != null ? engagement.getClientId() : null;
        String clientName = null;
        if (clientId != null) {
            clientName = clientRepository.findById(clientId).map(ClientEntity::getDisplayName).orElse(null);
        }

        WorkTemplateEntity template = entity.getTemplateId() != null
                ? workTemplateRepository.findById(entity.getTemplateId()).orElse(null)
                : null;
        String tplName = template != null ? template.getName() : null;
        String tplCode = template != null ? template.getTemplateCode() : null;

        String assignedUserName = null;
        if (entity.getAssignedUserId() != null) {
            assignedUserName = userRepository.findById(entity.getAssignedUserId()).map(UserEntity::getFullName).orElse(null);
        }

        String reviewerUserName = null;
        if (entity.getReviewerUserId() != null) {
            reviewerUserName = userRepository.findById(entity.getReviewerUserId()).map(UserEntity::getFullName).orElse(null);
        }

        // Fetch tasks instantiated for this work instance
        List<TaskEntity> tasks = taskRepository.findAllByOrganizationIdAndWorkInstanceId(orgId, entity.getId());

        List<WorkInstanceTaskDto> taskDtos = tasks.stream().map(t -> {
            String taskAssignedName = t.getAssignedUserId() != null
                    ? userRepository.findById(t.getAssignedUserId()).map(UserEntity::getFullName).orElse(null)
                    : null;
            String taskCompletedByName = t.getCompletedBy() != null
                    ? userRepository.findById(t.getCompletedBy()).map(UserEntity::getFullName).orElse(null)
                    : null;

            return WorkInstanceTaskDto.builder()
                    .id(t.getId())
                    .engagementId(t.getEngagementId())
                    .workInstanceId(t.getWorkInstanceId())
                    .workTemplateTaskId(t.getWorkTemplateTaskId())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .taskCategory(t.getTaskCategory())
                    .status(t.getStatus())
                    .priority(t.getPriority())
                    .dueDate(t.getDueDate())
                    .assignedUserId(t.getAssignedUserId())
                    .assignedUserName(taskAssignedName)
                    .completedAt(t.getCompletedAt())
                    .completedBy(t.getCompletedBy())
                    .completedByName(taskCompletedByName)
                    .notes(t.getNotes())
                    .build();
        }).collect(Collectors.toList());

        int totalTasks = taskDtos.size();
        int completedTasks = (int) taskDtos.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

        return WorkInstanceDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .engagementId(entity.getEngagementId())
                .engagementCode(engCode)
                .engagementName(engName)
                .clientId(clientId)
                .clientName(clientName)
                .templateId(entity.getTemplateId())
                .templateName(tplName)
                .templateCode(tplCode)
                .title(entity.getTitle())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .assignedUserId(entity.getAssignedUserId())
                .assignedUserName(assignedUserName)
                .reviewerUserId(entity.getReviewerUserId())
                .reviewerUserName(reviewerUserName)
                .generatedAt(entity.getGeneratedAt())
                .completedAt(entity.getCompletedAt())
                .notes(entity.getNotes())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .tasks(taskDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion() != null ? entity.getVersion() : 0L)
                .build();
    }
}
