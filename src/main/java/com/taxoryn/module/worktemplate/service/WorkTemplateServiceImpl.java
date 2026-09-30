package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.service.service.ServiceCatalogService;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.ReorderTasksRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateStatusRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateFilterRequest;
import com.taxoryn.module.worktemplate.dto.WorkTemplateTaskDto;
import com.taxoryn.module.worktemplate.entity.WorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateTaskEntity;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import com.taxoryn.module.worktemplate.repository.WorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateTaskRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkTemplateServiceImpl implements WorkTemplateService {

    private final WorkTemplateRepository workTemplateRepository;
    private final WorkTemplateTaskRepository workTemplateTaskRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final AuditService auditService;

    @Override
    @Transactional
    public WorkTemplateDto createTemplate(CreateWorkTemplateRequest request) {
        UUID organizationId = resolveOrganizationId();

        // 1. Service Entitlement & Availability Check
        ServiceEntity service = serviceRepository.findAccessibleServiceById(request.getServiceId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));

        if (!serviceCatalogService.isServiceAvailableForPractice(organizationId, service)) {
            throw new BusinessValidationException(
                    "SERVICE_NOT_AVAILABLE: Service '" + service.getServiceName() + "' is not entitled for your organization."
            );
        }

        // 2. Generate or Validate Unique Template Code
        String templateCode;
        if (StringUtils.hasText(request.getTemplateCode())) {
            templateCode = request.getTemplateCode().trim().toUpperCase();
            if (workTemplateRepository.existsByOrganizationIdAndTemplateCodeIgnoreCase(organizationId, templateCode)) {
                throw new DuplicateResourceException("WorkTemplate", "templateCode", templateCode);
            }
        } else {
            templateCode = generateTemplateCode(organizationId, service.getCategory().name());
        }

        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .organizationId(organizationId)
                .serviceId(service.getId())
                .templateCode(templateCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : service.getCategory())
                .status(request.getStatus() != null ? request.getStatus() : WorkTemplateStatus.ACTIVE)
                .templateType(request.getTemplateType() != null ? request.getTemplateType() : WorkTemplateType.STATUTORY_COMPLIANCE)
                .recurrenceType(request.getRecurrenceType() != null ? request.getRecurrenceType() : com.taxoryn.module.worktemplate.model.RecurrenceType.MONTHLY)
                .recurrenceInterval(Math.max(1, request.getRecurrenceInterval()))
                .dayOfMonth(request.getDayOfMonth())
                .monthOfYear(request.getMonthOfYear())
                .recurrenceEnabled(request.isRecurrenceEnabled())
                .isSystemDefault(false)
                .build();

        template = workTemplateRepository.save(template);
        log.info("Created WorkTemplate: id={}, code={}, name={} for org={}",
                template.getId(), template.getTemplateCode(), template.getName(), organizationId);

        // 3. Process Initial Tasks if provided
        if (request.getInitialTasks() != null && !request.getInitialTasks().isEmpty()) {
            int seq = 1;
            for (CreateWorkTemplateTaskRequest tReq : request.getInitialTasks()) {
                WorkTemplateTaskEntity taskEntity = WorkTemplateTaskEntity.builder()
                        .templateId(template.getId())
                        .name(tReq.getName().trim())
                        .description(tReq.getDescription())
                        .sequenceOrder(tReq.getSequenceOrder() != null ? tReq.getSequenceOrder() : seq++)
                        .defaultAssigneeRole(tReq.getDefaultAssigneeRole())
                        .defaultPriority(tReq.getDefaultPriority() != null ? tReq.getDefaultPriority() : com.taxoryn.module.task.entity.TaskEntity.TaskPriority.MEDIUM)
                        .relativeDueDays(tReq.getRelativeDueDays())
                        .mandatory(tReq.isMandatory())
                        .active(tReq.isActive())
                        .build();
                workTemplateTaskRepository.save(taskEntity);
            }
        }

        // Audit Logging
        auditService.logEvent("WORK_TEMPLATE_CREATED", "WORK_TEMPLATE", template.getId().toString(), null, template);

        return enrichDto(template);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkTemplateDto getTemplateById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        WorkTemplateEntity template = workTemplateRepository.findAccessibleById(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", id));

        return enrichDto(template);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkTemplateDto> getTemplates(WorkTemplateFilterRequest filterRequest) {
        UUID organizationId = resolveOrganizationId();

        Specification<WorkTemplateEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Access Predicate: Own Organization OR System Default
            if (Boolean.TRUE.equals(filterRequest.getIncludeSystemDefaults()) || filterRequest.getIncludeSystemDefaults() == null) {
                predicates.add(cb.or(
                        cb.equal(root.get("organizationId"), organizationId),
                        cb.and(cb.isNull(root.get("organizationId")), cb.isTrue(root.get("isSystemDefault")))
                ));
            } else {
                predicates.add(cb.equal(root.get("organizationId"), organizationId));
            }

            if (filterRequest.getServiceId() != null) {
                predicates.add(cb.equal(root.get("serviceId"), filterRequest.getServiceId()));
            }
            if (filterRequest.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filterRequest.getCategory()));
            }
            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }
            if (filterRequest.getTemplateType() != null) {
                predicates.add(cb.equal(root.get("templateType"), filterRequest.getTemplateType()));
            }
            if (filterRequest.getRecurrenceType() != null) {
                predicates.add(cb.equal(root.get("recurrenceType"), filterRequest.getRecurrenceType()));
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("templateCode")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.getSortDirection()), filterRequest.getSortBy());
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), sort);
        Page<WorkTemplateEntity> page = workTemplateRepository.findAll(spec, pageable);

        List<WorkTemplateDto> content = page.getContent().stream().map(this::enrichDto).collect(Collectors.toList());

        return PagedResponse.<WorkTemplateDto>builder()
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
    public List<WorkTemplateDto> getActiveTemplatesForService(UUID serviceId) {
        UUID organizationId = resolveOrganizationId();
        List<WorkTemplateEntity> list = workTemplateRepository.findActiveTemplatesForService(serviceId, organizationId);
        return list.stream().map(this::enrichDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkTemplateDto updateTemplate(UUID id, UpdateWorkTemplateRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkTemplateEntity template = workTemplateRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", id));

        if (template.getOrganizationId() == null && template.isSystemDefault()) {
            throw new ForbiddenException("Global system templates cannot be modified directly. Please create a practice-specific template.");
        }

        if (request.getServiceId() != null) {
            ServiceEntity service = serviceRepository.findAccessibleServiceById(request.getServiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));
            template.setServiceId(service.getId());
        }

        if (StringUtils.hasText(request.getTemplateCode())) {
            String newCode = request.getTemplateCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(template.getTemplateCode())
                    && workTemplateRepository.existsByOrganizationIdAndTemplateCodeIgnoreCase(organizationId, newCode)) {
                throw new DuplicateResourceException("WorkTemplate", "templateCode", newCode);
            }
            template.setTemplateCode(newCode);
        }

        if (StringUtils.hasText(request.getName())) {
            template.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        if (request.getTemplateType() != null) {
            template.setTemplateType(request.getTemplateType());
        }
        if (request.getRecurrenceType() != null) {
            template.setRecurrenceType(request.getRecurrenceType());
        }
        if (request.getRecurrenceInterval() != null) {
            template.setRecurrenceInterval(Math.max(1, request.getRecurrenceInterval()));
        }
        if (request.getDayOfMonth() != null) {
            template.setDayOfMonth(request.getDayOfMonth());
        }
        if (request.getMonthOfYear() != null) {
            template.setMonthOfYear(request.getMonthOfYear());
        }
        if (request.getRecurrenceEnabled() != null) {
            template.setRecurrenceEnabled(request.getRecurrenceEnabled());
        }

        template = workTemplateRepository.save(template);
        log.info("Updated WorkTemplate: id={}", template.getId());

        auditService.logEvent("WORK_TEMPLATE_UPDATED", "WORK_TEMPLATE", template.getId().toString(), null, template);

        return enrichDto(template);
    }

    @Override
    @Transactional
    public WorkTemplateDto updateTemplateStatus(UUID id, UpdateWorkTemplateStatusRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkTemplateEntity template = workTemplateRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", id));

        WorkTemplateStatus previousStatus = template.getStatus();
        WorkTemplateStatus targetStatus = request.getStatus();

        previousStatus.validateTransition(targetStatus);
        template.setStatus(targetStatus);

        template = workTemplateRepository.save(template);
        log.info("Updated WorkTemplate status: id={}, from={}, to={}", template.getId(), previousStatus, targetStatus);

        String auditAction = switch (targetStatus) {
            case ACTIVE -> "WORK_TEMPLATE_ACTIVATED";
            case INACTIVE -> "WORK_TEMPLATE_DEACTIVATED";
            case ARCHIVED -> "WORK_TEMPLATE_ARCHIVED";
            default -> "WORK_TEMPLATE_STATUS_CHANGED";
        };
        auditService.logEvent(auditAction, "WORK_TEMPLATE", template.getId().toString(), previousStatus, targetStatus);

        return enrichDto(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        UUID organizationId = resolveOrganizationId();
        WorkTemplateEntity template = workTemplateRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", id));

        workTemplateRepository.delete(template);
        log.info("Deleted WorkTemplate: id={}", id);

        auditService.logEvent("WORK_TEMPLATE_DELETED", "WORK_TEMPLATE", id.toString(), null, null);
    }

    // --- Template Tasks ---

    @Override
    @Transactional(readOnly = true)
    public List<WorkTemplateTaskDto> getTemplateTasks(UUID templateId) {
        UUID organizationId = resolveOrganizationId();
        workTemplateRepository.findAccessibleById(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", templateId));

        List<WorkTemplateTaskEntity> tasks = workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(templateId);
        return tasks.stream().map(this::mapTaskDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkTemplateTaskDto addTemplateTask(UUID templateId, CreateWorkTemplateTaskRequest request) {
        UUID organizationId = resolveOrganizationId();
        WorkTemplateEntity template = workTemplateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", templateId));

        int nextSeq = request.getSequenceOrder() != null
                ? request.getSequenceOrder()
                : workTemplateTaskRepository.findMaxSequenceOrderByTemplateId(templateId) + 1;

        WorkTemplateTaskEntity task = WorkTemplateTaskEntity.builder()
                .templateId(template.getId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .sequenceOrder(nextSeq)
                .defaultAssigneeRole(request.getDefaultAssigneeRole())
                .defaultPriority(request.getDefaultPriority() != null ? request.getDefaultPriority() : com.taxoryn.module.task.entity.TaskEntity.TaskPriority.MEDIUM)
                .relativeDueDays(request.getRelativeDueDays())
                .mandatory(request.isMandatory())
                .active(request.isActive())
                .build();

        task = workTemplateTaskRepository.save(task);
        log.info("Added task id={} to template id={}", task.getId(), templateId);

        auditService.logEvent("WORK_TEMPLATE_TASK_ADDED", "WORK_TEMPLATE", templateId.toString(), null, task);

        return mapTaskDto(task);
    }

    @Override
    @Transactional
    public WorkTemplateTaskDto updateTemplateTask(UUID templateId, UUID taskId, UpdateWorkTemplateTaskRequest request) {
        UUID organizationId = resolveOrganizationId();
        workTemplateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", templateId));

        WorkTemplateTaskEntity task = workTemplateTaskRepository.findByIdAndTemplateId(taskId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplateTask", "id", taskId));

        if (StringUtils.hasText(request.getName())) {
            task.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getSequenceOrder() != null) {
            task.setSequenceOrder(request.getSequenceOrder());
        }
        if (request.getDefaultAssigneeRole() != null) {
            task.setDefaultAssigneeRole(request.getDefaultAssigneeRole());
        }
        if (request.getDefaultPriority() != null) {
            task.setDefaultPriority(request.getDefaultPriority());
        }
        if (request.getRelativeDueDays() != null) {
            task.setRelativeDueDays(request.getRelativeDueDays());
        }
        if (request.getMandatory() != null) {
            task.setMandatory(request.getMandatory());
        }
        if (request.getActive() != null) {
            task.setActive(request.getActive());
        }

        task = workTemplateTaskRepository.save(task);
        log.info("Updated task id={} on template id={}", taskId, templateId);

        auditService.logEvent("WORK_TEMPLATE_TASK_UPDATED", "WORK_TEMPLATE", templateId.toString(), null, task);

        return mapTaskDto(task);
    }

    @Override
    @Transactional
    public void deleteTemplateTask(UUID templateId, UUID taskId) {
        UUID organizationId = resolveOrganizationId();
        workTemplateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", templateId));

        WorkTemplateTaskEntity task = workTemplateTaskRepository.findByIdAndTemplateId(taskId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplateTask", "id", taskId));

        workTemplateTaskRepository.delete(task);
        log.info("Deleted task id={} from template id={}", taskId, templateId);

        auditService.logEvent("WORK_TEMPLATE_TASK_DELETED", "WORK_TEMPLATE", templateId.toString(), null, null);
    }

    @Override
    @Transactional
    public List<WorkTemplateTaskDto> reorderTemplateTasks(UUID templateId, ReorderTasksRequest request) {
        UUID organizationId = resolveOrganizationId();
        workTemplateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkTemplate", "id", templateId));

        List<WorkTemplateTaskEntity> tasks = workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(templateId);
        Map<UUID, WorkTemplateTaskEntity> taskMap = tasks.stream()
                .collect(Collectors.toMap(WorkTemplateTaskEntity::getId, t -> t));

        int order = 1;
        for (UUID taskId : request.getOrderedTaskIds()) {
            WorkTemplateTaskEntity task = taskMap.get(taskId);
            if (task != null) {
                task.setSequenceOrder(order++);
                workTemplateTaskRepository.save(task);
            }
        }

        List<WorkTemplateTaskEntity> reordered = workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(templateId);
        auditService.logEvent("WORK_TEMPLATE_TASKS_REORDERED", "WORK_TEMPLATE", templateId.toString(), null, request.getOrderedTaskIds());

        return reordered.stream().map(this::mapTaskDto).collect(Collectors.toList());
    }

    // --- Helpers ---

    private String generateTemplateCode(UUID organizationId, String category) {
        long count = workTemplateRepository.countByOrganizationId(organizationId) + 1;
        String prefix = StringUtils.hasText(category) ? category.toUpperCase() : "TPL";
        return String.format("%s_TPL_%04d", prefix, count);
    }

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }

    private WorkTemplateDto enrichDto(WorkTemplateEntity entity) {
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
        List<WorkTemplateTaskDto> taskDtos = tasks.stream().map(this::mapTaskDto).collect(Collectors.toList());

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

    private WorkTemplateTaskDto mapTaskDto(WorkTemplateTaskEntity task) {
        return WorkTemplateTaskDto.builder()
                .id(task.getId())
                .templateId(task.getTemplateId())
                .name(task.getName())
                .description(task.getDescription())
                .sequenceOrder(task.getSequenceOrder())
                .defaultAssigneeRole(task.getDefaultAssigneeRole())
                .defaultPriority(task.getDefaultPriority())
                .relativeDueDays(task.getRelativeDueDays())
                .mandatory(task.isMandatory())
                .active(task.isActive())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion() != null ? task.getVersion() : 0L)
                .build();
    }
}
