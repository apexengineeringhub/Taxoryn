package com.taxoryn.module.workflow.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.workflow.dto.AssignWorkflowRequest;
import com.taxoryn.module.workflow.dto.ClientServicePeriodDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowStepDto;
import com.taxoryn.module.workflow.dto.CreateServicePeriodRequest;
import com.taxoryn.module.workflow.dto.GenerateWorkflowRequest;
import com.taxoryn.module.workflow.dto.ServiceWorkflowTemplateDto;
import com.taxoryn.module.workflow.dto.UpdateWorkflowPriorityRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStatusRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStepStatusRequest;
import com.taxoryn.module.workflow.dto.WorkflowFilterRequest;
import com.taxoryn.module.workflow.entity.ClientServicePeriodEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowStepEntity;
import com.taxoryn.module.workflow.entity.ServiceWorkflowStepTemplateEntity;
import com.taxoryn.module.workflow.entity.ServiceWorkflowTemplateEntity;
import com.taxoryn.module.workflow.model.ServicePeriodStatus;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import com.taxoryn.module.workflow.model.StepStatus;
import com.taxoryn.module.workflow.repository.ClientServicePeriodRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowStepRepository;
import com.taxoryn.module.workflow.repository.ServiceWorkflowStepTemplateRepository;
import com.taxoryn.module.workflow.repository.ServiceWorkflowTemplateRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceWorkflowServiceImpl implements ClientServiceWorkflowService {

    private final ClientServiceWorkflowRepository workflowRepository;
    private final ClientServiceWorkflowStepRepository stepRepository;
    private final ClientServicePeriodRepository periodRepository;
    private final ServiceWorkflowTemplateRepository templateRepository;
    private final ServiceWorkflowStepTemplateRepository stepTemplateRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ModuleConfigurationService moduleConfigurationService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional
    public ClientServicePeriodDto createServicePeriod(CreateServicePeriodRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        if (request.getClientServiceId() == null) {
            throw new BusinessValidationException("Client Service ID is required");
        }
        if (request.getPeriodLabel() == null || request.getPeriodLabel().trim().isBlank()) {
            throw new BusinessValidationException("Period label is required");
        }

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(request.getClientServiceId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", request.getClientServiceId()));

        validateClientAccess(service.getClientId());

        String cleanLabel = request.getPeriodLabel().trim();
        if (periodRepository.existsByOrganizationIdAndClientServiceIdAndPeriodLabel(organizationId, service.getId(), cleanLabel)) {
            throw new BusinessValidationException("Period '" + cleanLabel + "' already exists for this client service");
        }

        ClientServicePeriodEntity period = ClientServicePeriodEntity.builder()
                .clientId(service.getClientId())
                .clientServiceId(service.getId())
                .periodType(request.getPeriodType())
                .periodLabel(cleanLabel)
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .dueDate(request.getDueDate())
                .status(ServicePeriodStatus.ACTIVE)
                .build();
        period.setOrganizationId(organizationId);

        ClientServicePeriodEntity saved = periodRepository.save(period);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_PERIOD_CREATED",
                "CLIENT_SERVICE_PERIOD",
                saved.getId().toString(),
                null,
                "Created service period '" + saved.getPeriodLabel() + "' for service " + service.getServiceType()
        );

        ClientEntity client = clientRepository.findByIdAndOrganizationId(service.getClientId(), organizationId).orElse(null);
        return mapToPeriodDto(saved, client, service, null);
    }

    @Override
    public List<ClientServicePeriodDto> getServicePeriods(UUID clientServiceId) {
        UUID organizationId = getRequiredTenantId();
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", clientServiceId));

        validateClientAccess(service.getClientId());

        ClientEntity client = clientRepository.findByIdAndOrganizationId(service.getClientId(), organizationId).orElse(null);
        List<ClientServicePeriodEntity> periods = periodRepository.findAllByOrganizationIdAndClientServiceIdOrderByStartDateDesc(organizationId, clientServiceId);

        List<ClientServiceWorkflowEntity> workflows = workflowRepository.findAllByOrganizationIdAndClientServiceIdOrderByCreatedAtDesc(organizationId, clientServiceId);
        Map<UUID, UUID> periodToWorkflowMap = workflows.stream()
                .filter(w -> w.getStatus() != ServiceWorkflowStatus.CANCELLED)
                .collect(Collectors.toMap(ClientServiceWorkflowEntity::getPeriodId, ClientServiceWorkflowEntity::getId, (a, b) -> a));

        return periods.stream()
                .map(p -> mapToPeriodDto(p, client, service, periodToWorkflowMap.get(p.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientServiceWorkflowDto generateWorkflow(GenerateWorkflowRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        if (request.getClientServiceId() == null) {
            throw new BusinessValidationException("Client Service ID is required");
        }

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(request.getClientServiceId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", request.getClientServiceId()));

        if (service.getStatus() != ClientServiceStatus.ACTIVE) {
            throw new BusinessValidationException("Operational workflows can only be generated for ACTIVE client services");
        }

        validateClientAccess(service.getClientId());
        validateEntitlement(organizationId, service.getServiceType());

        // Resolve or create period
        ClientServicePeriodEntity period;
        if (request.getPeriodId() != null) {
            period = periodRepository.findByIdAndOrganizationId(request.getPeriodId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClientServicePeriod", "id", request.getPeriodId()));
        } else {
            if (request.getPeriodLabel() == null || request.getPeriodLabel().trim().isBlank()) {
                throw new BusinessValidationException("Period label or Period ID is required to generate a workflow");
            }
            String cleanLabel = request.getPeriodLabel().trim();
            Optional<ClientServicePeriodEntity> existingPeriod = periodRepository.findByOrganizationIdAndClientServiceIdAndPeriodLabel(organizationId, service.getId(), cleanLabel);
            if (existingPeriod.isPresent()) {
                period = existingPeriod.get();
            } else {
                period = ClientServicePeriodEntity.builder()
                        .clientId(service.getClientId())
                        .clientServiceId(service.getId())
                        .periodType(request.getPeriodType() != null ? request.getPeriodType() : com.taxoryn.module.workflow.model.ServicePeriodType.MONTHLY)
                        .periodLabel(cleanLabel)
                        .financialYear(request.getFinancialYear())
                        .assessmentYear(request.getAssessmentYear())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .dueDate(request.getDueDate())
                        .status(ServicePeriodStatus.ACTIVE)
                        .build();
                period.setOrganizationId(organizationId);
                period = periodRepository.save(period);
            }
        }

        // Idempotency: check if workflow already exists for this (service, period)
        Optional<ClientServiceWorkflowEntity> existingWorkflow = workflowRepository.findByOrganizationIdAndClientServiceIdAndPeriodId(organizationId, service.getId(), period.getId());
        if (existingWorkflow.isPresent()) {
            log.info("Workflow already exists for service {} and period {}. Returning existing instance.", service.getId(), period.getId());
            return getWorkflowById(existingWorkflow.get().getId());
        }

        // Resolve Template
        ServiceWorkflowTemplateEntity template;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("ServiceWorkflowTemplate", "id", request.getTemplateId()));
        } else {
            template = templateRepository.findBestTemplate(organizationId, service.getServiceType())
                    .orElseThrow(() -> new BusinessValidationException("No active workflow template found for service type: " + service.getServiceType()));
        }

        List<ServiceWorkflowStepTemplateEntity> stepTemplates = stepTemplateRepository.findAllByWorkflowTemplateIdAndActiveTrueOrderBySequenceAsc(template.getId());
        if (stepTemplates.isEmpty()) {
            throw new BusinessValidationException("Selected workflow template '" + template.getName() + "' contains no active step definitions");
        }

        // Validate Assignee if provided
        UUID assigneeId = request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : service.getAssignedEmployeeId();
        if (assigneeId != null) {
            employeeRepository.findByIdAndOrganizationId(assigneeId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", assigneeId));
        }

        LocalDate workflowDueDate = period.getDueDate();
        String title = request.getCustomTitle() != null && !request.getCustomTitle().isBlank()
                ? request.getCustomTitle().trim()
                : service.getServiceType().getDisplayName() + " - " + period.getPeriodLabel();

        ClientServiceWorkflowEntity workflow = ClientServiceWorkflowEntity.builder()
                .clientId(service.getClientId())
                .clientServiceId(service.getId())
                .periodId(period.getId())
                .templateId(template.getId())
                .title(title)
                .status(ServiceWorkflowStatus.NOT_STARTED)
                .priority(request.getPriority() != null ? request.getPriority() : com.taxoryn.module.task.entity.TaskEntity.TaskPriority.MEDIUM)
                .assignedEmployeeId(assigneeId)
                .currentStepSequence(1)
                .totalSteps(stepTemplates.size())
                .completedSteps(0)
                .dueDate(workflowDueDate)
                .internalTargetDate(request.getInternalTargetDate())
                .waitingForClient(false)
                .build();
        workflow.setOrganizationId(organizationId);

        ClientServiceWorkflowEntity savedWorkflow = workflowRepository.save(workflow);

        // Instantiate Steps
        List<ClientServiceWorkflowStepEntity> stepsToSave = new ArrayList<>();
        for (ServiceWorkflowStepTemplateEntity st : stepTemplates) {
            LocalDate stepDueDate = null;
            if (workflowDueDate != null && st.getDefaultDaysBeforeDueDate() != null) {
                stepDueDate = workflowDueDate.minusDays(st.getDefaultDaysBeforeDueDate());
            }

            ClientServiceWorkflowStepEntity step = ClientServiceWorkflowStepEntity.builder()
                    .workflowId(savedWorkflow.getId())
                    .sequence(st.getSequence())
                    .workType(st.getWorkType())
                    .name(st.getName())
                    .description(st.getDescription())
                    .status(st.getSequence() == 1 ? StepStatus.IN_PROGRESS : StepStatus.PENDING)
                    .assignedEmployeeId(assigneeId)
                    .dueDate(stepDueDate)
                    .mandatory(st.isMandatory())
                    .requiresClientInput(st.isRequiresClientInput())
                    .requiresReview(st.isRequiresReview())
                    .build();
            step.setOrganizationId(organizationId);
            stepsToSave.add(step);
        }

        stepRepository.saveAll(stepsToSave);

        // Update workflow started timestamp and initial status
        savedWorkflow.setStatus(ServiceWorkflowStatus.IN_PROGRESS);
        savedWorkflow.setStartedAt(Instant.now());
        workflowRepository.save(savedWorkflow);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_WORKFLOW_CREATED",
                "CLIENT_SERVICE_WORKFLOW",
                savedWorkflow.getId().toString(),
                null,
                "Generated workflow '" + savedWorkflow.getTitle() + "' from template '" + template.getName() + "'"
        );

        return getWorkflowById(savedWorkflow.getId());
    }

    @Override
    public ClientServiceWorkflowDto getWorkflowById(UUID workflowId) {
        UUID organizationId = getRequiredTenantId();
        ClientServiceWorkflowEntity workflow = workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflow", "id", workflowId));

        validateWorkflowAccess(workflow);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(workflow.getClientId(), organizationId).orElse(null);
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(workflow.getClientServiceId(), organizationId).orElse(null);
        ClientServicePeriodEntity period = periodRepository.findByIdAndOrganizationId(workflow.getPeriodId(), organizationId).orElse(null);
        ServiceWorkflowTemplateEntity template = workflow.getTemplateId() != null
                ? templateRepository.findById(workflow.getTemplateId()).orElse(null)
                : null;

        List<ClientServiceWorkflowStepEntity> steps = stepRepository.findAllByOrganizationIdAndWorkflowIdOrderBySequenceAsc(organizationId, workflow.getId());
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return mapToWorkflowDto(workflow, client, service, period, template, steps, employeeMap);
    }

    @Override
    public ClientServiceWorkflowDto getActiveWorkflowForService(UUID clientServiceId) {
        UUID organizationId = getRequiredTenantId();
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", clientServiceId));

        validateClientAccess(service.getClientId());

        List<ClientServiceWorkflowEntity> list = workflowRepository.findAllByOrganizationIdAndClientServiceIdOrderByCreatedAtDesc(organizationId, clientServiceId);
        Optional<ClientServiceWorkflowEntity> activeWorkflow = list.stream()
                .filter(w -> w.getStatus() != ServiceWorkflowStatus.COMPLETED && w.getStatus() != ServiceWorkflowStatus.CANCELLED)
                .findFirst();

        return activeWorkflow.map(w -> getWorkflowById(w.getId())).orElse(null);
    }

    @Override
    public List<ClientServiceWorkflowDto> getWorkflowsForService(UUID clientServiceId) {
        UUID organizationId = getRequiredTenantId();
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", clientServiceId));

        validateClientAccess(service.getClientId());

        List<ClientServiceWorkflowEntity> list = workflowRepository.findAllByOrganizationIdAndClientServiceIdOrderByCreatedAtDesc(organizationId, clientServiceId);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(service.getClientId(), organizationId).orElse(null);
        Map<UUID, ClientServicePeriodEntity> periodMap = periodRepository.findAllByOrganizationIdAndClientServiceIdOrderByStartDateDesc(organizationId, clientServiceId).stream()
                .collect(Collectors.toMap(ClientServicePeriodEntity::getId, p -> p, (a, b) -> a));
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return list.stream()
                .map(w -> mapToWorkflowDto(w, client, service, periodMap.get(w.getPeriodId()), null, Collections.emptyList(), employeeMap))
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<ClientServiceWorkflowDto> getWorklist(WorkflowFilterRequest filter, Pageable pageable) {
        UUID organizationId = getRequiredTenantId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        Specification<ClientServiceWorkflowEntity> spec = createSpecification(organizationId, filter, accessibleClientIds, scope);
        Page<ClientServiceWorkflowEntity> page = workflowRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return PagedResponse.<ClientServiceWorkflowDto>builder()
                    .content(Collections.emptyList())
                    .pageNumber(page.getNumber())
                    .pageSize(page.getSize())
                    .totalElements(0)
                    .totalPages(0)
                    .isFirst(true)
                    .isLast(true)
                    .build();
        }

        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, page.getContent());
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, page.getContent());
        Map<UUID, ClientServicePeriodEntity> periodMap = loadPeriods(organizationId, page.getContent());
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        List<ClientServiceWorkflowDto> dtos = page.getContent().stream()
                .map(w -> mapToWorkflowDto(w, clientMap.get(w.getClientId()), serviceMap.get(w.getClientServiceId()), periodMap.get(w.getPeriodId()), null, Collections.emptyList(), employeeMap))
                .collect(Collectors.toList());

        return PagedResponse.<ClientServiceWorkflowDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public ClientServiceWorkflowDto updateWorkflowStatus(UUID workflowId, UpdateWorkflowStatusRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientServiceWorkflowEntity workflow = workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflow", "id", workflowId));

        validateWorkflowAccess(workflow);

        ServiceWorkflowStatus current = workflow.getStatus();
        ServiceWorkflowStatus target = request.getStatus();

        if (!current.canTransitionTo(target)) {
            throw new BusinessValidationException("Invalid workflow status transition from " + current + " to " + target);
        }

        workflow.setStatus(target);
        if (target == ServiceWorkflowStatus.WAITING_FOR_CLIENT) {
            workflow.setWaitingForClient(true);
            if (request.getPendingClientActionSummary() != null && !request.getPendingClientActionSummary().isBlank()) {
                workflow.setPendingClientActionSummary(request.getPendingClientActionSummary().trim());
            }
        } else if (target == ServiceWorkflowStatus.IN_PROGRESS || target == ServiceWorkflowStatus.READY_FOR_FILING) {
            workflow.setWaitingForClient(false);
        } else if (target == ServiceWorkflowStatus.COMPLETED) {
            workflow.setWaitingForClient(false);
            workflow.setCompletedAt(Instant.now());
        }

        ClientServiceWorkflowEntity saved = workflowRepository.save(workflow);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_WORKFLOW_STATUS_CHANGED",
                "CLIENT_SERVICE_WORKFLOW",
                saved.getId().toString(),
                current.name(),
                target.name() + (request.getNotes() != null ? " - " + request.getNotes() : "")
        );

        return getWorkflowById(saved.getId());
    }

    @Override
    @Transactional
    public ClientServiceWorkflowStepDto updateStepStatus(UUID workflowId, UUID stepId, UpdateWorkflowStepStatusRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientServiceWorkflowEntity workflow = workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflow", "id", workflowId));

        validateWorkflowAccess(workflow);

        ClientServiceWorkflowStepEntity step = stepRepository.findByIdAndOrganizationId(stepId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflowStep", "id", stepId));

        if (!step.getWorkflowId().equals(workflow.getId())) {
            throw new BadRequestException("Step does not belong to the specified workflow");
        }

        StepStatus previousStatus = step.getStatus();
        step.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            step.setNotes(request.getNotes());
        }

        if (request.getStatus() == StepStatus.COMPLETED) {
            step.setCompletedAt(Instant.now());
            step.setCompletedBy(userId);
        } else {
            step.setCompletedAt(null);
            step.setCompletedBy(null);
        }

        ClientServiceWorkflowStepEntity savedStep = stepRepository.save(step);

        // Recalculate workflow progress and step counts
        List<ClientServiceWorkflowStepEntity> allSteps = stepRepository.findAllByOrganizationIdAndWorkflowIdOrderBySequenceAsc(organizationId, workflow.getId());
        int completedCount = (int) allSteps.stream().filter(s -> s.getStatus() == StepStatus.COMPLETED || s.getStatus() == StepStatus.SKIPPED).count();
        workflow.setCompletedSteps(completedCount);
        workflow.setTotalSteps(allSteps.size());

        // Determine current active step
        Optional<ClientServiceWorkflowStepEntity> firstIncomplete = allSteps.stream()
                .filter(s -> s.getStatus() != StepStatus.COMPLETED && s.getStatus() != StepStatus.SKIPPED)
                .findFirst();

        if (firstIncomplete.isPresent()) {
            workflow.setCurrentStepSequence(firstIncomplete.get().getSequence());
            if (request.getStatus() == StepStatus.WAITING_FOR_CLIENT) {
                workflow.setWaitingForClient(true);
                workflow.setStatus(ServiceWorkflowStatus.WAITING_FOR_CLIENT);
                if (request.getClientActionSummary() != null && !request.getClientActionSummary().isBlank()) {
                    workflow.setPendingClientActionSummary(request.getClientActionSummary().trim());
                }
            } else if (workflow.getStatus() == ServiceWorkflowStatus.WAITING_FOR_CLIENT && request.getStatus() != StepStatus.WAITING_FOR_CLIENT) {
                workflow.setWaitingForClient(false);
                workflow.setStatus(ServiceWorkflowStatus.IN_PROGRESS);
            }
        } else {
            // All steps complete!
            workflow.setCurrentStepSequence(allSteps.size());
            workflow.setWaitingForClient(false);
            workflow.setStatus(ServiceWorkflowStatus.COMPLETED);
            workflow.setCompletedAt(Instant.now());
        }

        workflowRepository.save(workflow);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_WORKFLOW_STEP_CHANGED",
                "CLIENT_SERVICE_WORKFLOW_STEP",
                savedStep.getId().toString(),
                previousStatus.name(),
                request.getStatus().name() + " (Step: " + savedStep.getName() + ")"
        );

        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);
        EmployeeEntity assignee = savedStep.getAssignedEmployeeId() != null ? employeeMap.get(savedStep.getAssignedEmployeeId()) : null;

        return ClientServiceWorkflowStepDto.builder()
                .id(savedStep.getId())
                .organizationId(savedStep.getOrganizationId())
                .workflowId(savedStep.getWorkflowId())
                .sequence(savedStep.getSequence())
                .workType(savedStep.getWorkType())
                .name(savedStep.getName())
                .description(savedStep.getDescription())
                .status(savedStep.getStatus())
                .assignedEmployeeId(savedStep.getAssignedEmployeeId())
                .assignedEmployeeName(assignee != null ? assignee.getFirstName() + (assignee.getLastName() != null ? " " + assignee.getLastName() : "") : null)
                .assignedEmployeeEmail(assignee != null ? assignee.getEmail() : null)
                .taskId(savedStep.getTaskId())
                .dueDate(savedStep.getDueDate())
                .mandatory(savedStep.isMandatory())
                .requiresClientInput(savedStep.isRequiresClientInput())
                .requiresReview(savedStep.isRequiresReview())
                .completedAt(savedStep.getCompletedAt())
                .completedBy(savedStep.getCompletedBy())
                .notes(savedStep.getNotes())
                .createdAt(savedStep.getCreatedAt())
                .updatedAt(savedStep.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ClientServiceWorkflowDto assignWorkflow(UUID workflowId, AssignWorkflowRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientServiceWorkflowEntity workflow = workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflow", "id", workflowId));

        validateWorkflowAccess(workflow);

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        UUID oldAssignee = workflow.getAssignedEmployeeId();
        workflow.setAssignedEmployeeId(request.getAssignedEmployeeId());
        ClientServiceWorkflowEntity saved = workflowRepository.save(workflow);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_WORK_ASSIGNED",
                "CLIENT_SERVICE_WORKFLOW",
                saved.getId().toString(),
                oldAssignee != null ? oldAssignee.toString() : "UNASSIGNED",
                request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId().toString() : "UNASSIGNED"
        );

        return getWorkflowById(saved.getId());
    }

    @Override
    @Transactional
    public ClientServiceWorkflowDto assignStep(UUID workflowId, UUID stepId, AssignWorkflowRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientServiceWorkflowEntity workflow = workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflow", "id", workflowId));

        validateWorkflowAccess(workflow);

        ClientServiceWorkflowStepEntity step = stepRepository.findByIdAndOrganizationId(stepId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflowStep", "id", stepId));

        if (!step.getWorkflowId().equals(workflow.getId())) {
            throw new BadRequestException("Step does not belong to specified workflow");
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        UUID oldAssignee = step.getAssignedEmployeeId();
        step.setAssignedEmployeeId(request.getAssignedEmployeeId());
        stepRepository.save(step);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_WORK_ASSIGNED",
                "CLIENT_SERVICE_WORKFLOW_STEP",
                step.getId().toString(),
                oldAssignee != null ? oldAssignee.toString() : "UNASSIGNED",
                request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId().toString() : "UNASSIGNED"
        );

        return getWorkflowById(workflow.getId());
    }

    @Override
    @Transactional
    public ClientServiceWorkflowDto updateWorkflowPriority(UUID workflowId, UpdateWorkflowPriorityRequest request) {
        UUID organizationId = getRequiredTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientServiceWorkflowEntity workflow = workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServiceWorkflow", "id", workflowId));

        validateWorkflowAccess(workflow);

        com.taxoryn.module.task.entity.TaskEntity.TaskPriority oldPriority = workflow.getPriority();
        workflow.setPriority(request.getPriority());
        ClientServiceWorkflowEntity saved = workflowRepository.save(workflow);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_WORK_PRIORITY_CHANGED",
                "CLIENT_SERVICE_WORKFLOW",
                saved.getId().toString(),
                oldPriority.name(),
                request.getPriority().name()
        );

        return getWorkflowById(saved.getId());
    }

    @Override
    public List<ServiceWorkflowTemplateDto> getWorkflowTemplates(ClientServiceType serviceType) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ServiceWorkflowTemplateEntity> templates;
        if (serviceType != null) {
            templates = templateRepository.findEffectiveTemplatesForService(organizationId != null ? organizationId : UUID.randomUUID(), serviceType);
        } else {
            templates = templateRepository.findAllByOrganizationIdOrOrganizationIdIsNull(organizationId != null ? organizationId : UUID.randomUUID());
        }

        return templates.stream()
                .map(t -> {
                    List<ServiceWorkflowStepTemplateEntity> steps = stepTemplateRepository.findAllByWorkflowTemplateIdAndActiveTrueOrderBySequenceAsc(t.getId());
                    return ServiceWorkflowTemplateDto.builder()
                            .id(t.getId())
                            .organizationId(t.getOrganizationId())
                            .serviceType(t.getServiceType())
                            .serviceTypeName(t.getServiceType().getDisplayName())
                            .name(t.getName())
                            .description(t.getDescription())
                            .isSystemDefault(t.isSystemDefault())
                            .active(t.isActive())
                            .stepCount(steps.size())
                            .stepTemplates(steps.stream().map(s -> ServiceWorkflowTemplateDto.ServiceWorkflowStepTemplateDto.builder()
                                    .id(s.getId())
                                    .workflowTemplateId(s.getWorkflowTemplateId())
                                    .sequence(s.getSequence())
                                    .workType(s.getWorkType())
                                    .workTypeName(s.getWorkType().getDisplayName())
                                    .name(s.getName())
                                    .description(s.getDescription())
                                    .defaultDaysBeforeDueDate(s.getDefaultDaysBeforeDueDate())
                                    .mandatory(s.isMandatory())
                                    .requiresClientInput(s.isRequiresClientInput())
                                    .requiresReview(s.isRequiresReview())
                                    .active(s.isActive())
                                    .build()).collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Helper & Validation Methods
    // =========================================================================

    private UUID getRequiredTenantId() {
        UUID organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            organizationId = SecurityUtils.getCurrentOrganizationId();
        }
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        return organizationId;
    }

    private void validateClientAccess(UUID clientId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds != null && !accessibleClientIds.contains(clientId)) {
            throw new ForbiddenException("Access denied: Client is outside authorized portfolio scope");
        }
    }

    private void validateWorkflowAccess(ClientServiceWorkflowEntity workflow) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        if (accessibleClientIds == null || accessibleClientIds.contains(workflow.getClientId())) {
            return;
        }

        // Direct item assignment exception: employee assigned directly to this workflow
        UUID callerEmpId = scope.getEmployeeId();
        UUID callerUserId = scope.getUserId();
        boolean isDirectAssignee = (callerEmpId != null && callerEmpId.equals(workflow.getAssignedEmployeeId()))
                || (callerUserId != null && callerUserId.equals(workflow.getAssignedEmployeeId()));

        if (!isDirectAssignee) {
            throw new ForbiddenException("Access denied: Client Service Workflow is outside authorized portfolio scope");
        }
    }

    private void validateEntitlement(UUID organizationId, ClientServiceType serviceType) {
        ProductModuleCode module = serviceType.getAssociatedModule();
        if (module != null) {
            final ProductModuleCode targetModule = module;
            try {
                List<OrganizationModuleDto> orgModules = moduleConfigurationService.getOrganizationModules(organizationId);
                Optional<OrganizationModuleDto> moduleOpt = orgModules.stream()
                        .filter(m -> m.getModuleCode() == targetModule)
                        .findFirst();

                if (moduleOpt.isPresent()) {
                    OrganizationModuleDto orgMod = moduleOpt.get();
                    if (!orgMod.isEnabled()) {
                        throw new BusinessValidationException("The module '" + module + "' is disabled in your organization settings.");
                    }
                    if (!orgMod.isEntitled()) {
                        throw new ForbiddenException("Your current subscription plan is not entitled to use the '" + module + "' module.");
                    }
                }
            } catch (BusinessValidationException | ForbiddenException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Could not verify module entitlement for service type {}: {}", serviceType, e.getMessage());
            }
        }
    }

    private Specification<ClientServiceWorkflowEntity> createSpecification(
            UUID organizationId,
            WorkflowFilterRequest filter,
            Set<UUID> accessibleClientIds,
            PracticeSecurityScope scope
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Portfolio scoping
            if (accessibleClientIds != null) {
                Predicate inClientPortfolio = root.get("clientId").in(accessibleClientIds);
                if (scope.getEmployeeId() != null) {
                    Predicate isAssignee = cb.equal(root.get("assignedEmployeeId"), scope.getEmployeeId());
                    predicates.add(cb.or(inClientPortfolio, isAssignee));
                } else {
                    predicates.add(inClientPortfolio);
                }
            }

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }
            if (filter.getClientServiceId() != null) {
                predicates.add(cb.equal(root.get("clientServiceId"), filter.getClientServiceId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filter.getAssignedEmployeeId()));
            }
            if (filter.getDueFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueFrom()));
            }
            if (filter.getDueTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filter.getDueTo()));
            }
            if (Boolean.TRUE.equals(filter.getOverdue())) {
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(root.get("status").in(ServiceWorkflowStatus.COMPLETED, ServiceWorkflowStatus.CANCELLED).not());
            }
            if (Boolean.TRUE.equals(filter.getDueToday())) {
                predicates.add(cb.equal(root.get("dueDate"), LocalDate.now()));
                predicates.add(root.get("status").in(ServiceWorkflowStatus.COMPLETED, ServiceWorkflowStatus.CANCELLED).not());
            }
            if (Boolean.TRUE.equals(filter.getDueThisWeek())) {
                predicates.add(cb.between(root.get("dueDate"), LocalDate.now(), LocalDate.now().plusDays(7)));
                predicates.add(root.get("status").in(ServiceWorkflowStatus.COMPLETED, ServiceWorkflowStatus.CANCELLED).not());
            }
            if (Boolean.TRUE.equals(filter.getMyWorkOnly()) && scope.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), scope.getEmployeeId()));
            }
            if (Boolean.TRUE.equals(filter.getWaitingForClient())) {
                predicates.add(cb.equal(root.get("waitingForClient"), true));
            }
            if (Boolean.TRUE.equals(filter.getReadyForFiling())) {
                predicates.add(cb.equal(root.get("status"), ServiceWorkflowStatus.READY_FOR_FILING));
            }
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ClientServicePeriodDto mapToPeriodDto(ClientServicePeriodEntity entity, ClientEntity client, ClientServiceEntity service, UUID activeWorkflowId) {
        return ClientServicePeriodDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientServiceId(entity.getClientServiceId())
                .serviceType(service != null ? service.getServiceType().name() : null)
                .serviceName(service != null ? service.getServiceType().getDisplayName() : null)
                .periodType(entity.getPeriodType())
                .periodLabel(entity.getPeriodLabel())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .hasActiveWorkflow(activeWorkflowId != null)
                .activeWorkflowId(activeWorkflowId)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    private ClientServiceWorkflowDto mapToWorkflowDto(
            ClientServiceWorkflowEntity workflow,
            ClientEntity client,
            ClientServiceEntity service,
            ClientServicePeriodEntity period,
            ServiceWorkflowTemplateEntity template,
            List<ClientServiceWorkflowStepEntity> steps,
            Map<UUID, EmployeeEntity> employeeMap
    ) {
        EmployeeEntity assignee = workflow.getAssignedEmployeeId() != null ? employeeMap.get(workflow.getAssignedEmployeeId()) : null;

        boolean isOverdue = workflow.getDueDate() != null
                && workflow.getDueDate().isBefore(LocalDate.now())
                && workflow.getStatus() != ServiceWorkflowStatus.COMPLETED
                && workflow.getStatus() != ServiceWorkflowStatus.CANCELLED;

        double progress = workflow.getTotalSteps() > 0
                ? Math.round(((double) workflow.getCompletedSteps() / workflow.getTotalSteps()) * 100.0)
                : 0.0;

        String currentStepName = null;
        if (!steps.isEmpty()) {
            for (ClientServiceWorkflowStepEntity s : steps) {
                if (s.getSequence() == workflow.getCurrentStepSequence()) {
                    currentStepName = s.getName();
                    break;
                }
            }
        }

        List<ClientServiceWorkflowStepDto> stepDtos = steps.stream()
                .map(s -> {
                    EmployeeEntity stepAssignee = s.getAssignedEmployeeId() != null ? employeeMap.get(s.getAssignedEmployeeId()) : null;
                    return ClientServiceWorkflowStepDto.builder()
                            .id(s.getId())
                            .organizationId(s.getOrganizationId())
                            .workflowId(s.getWorkflowId())
                            .sequence(s.getSequence())
                            .workType(s.getWorkType())
                            .name(s.getName())
                            .description(s.getDescription())
                            .status(s.getStatus())
                            .assignedEmployeeId(s.getAssignedEmployeeId())
                            .assignedEmployeeName(stepAssignee != null ? stepAssignee.getFirstName() + (stepAssignee.getLastName() != null ? " " + stepAssignee.getLastName() : "") : null)
                            .assignedEmployeeEmail(stepAssignee != null ? stepAssignee.getEmail() : null)
                            .taskId(s.getTaskId())
                            .dueDate(s.getDueDate())
                            .mandatory(s.isMandatory())
                            .requiresClientInput(s.isRequiresClientInput())
                            .requiresReview(s.isRequiresReview())
                            .completedAt(s.getCompletedAt())
                            .completedBy(s.getCompletedBy())
                            .notes(s.getNotes())
                            .createdAt(s.getCreatedAt())
                            .updatedAt(s.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ClientServiceWorkflowDto.builder()
                .id(workflow.getId())
                .organizationId(workflow.getOrganizationId())
                .clientId(workflow.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientPan(client != null ? client.getPan() : null)
                .clientServiceId(workflow.getClientServiceId())
                .serviceType(service != null ? service.getServiceType().name() : null)
                .serviceName(service != null ? service.getServiceType().getDisplayName() : null)
                .periodId(workflow.getPeriodId())
                .periodLabel(period != null ? period.getPeriodLabel() : null)
                .financialYear(period != null ? period.getFinancialYear() : null)
                .assessmentYear(period != null ? period.getAssessmentYear() : null)
                .templateId(workflow.getTemplateId())
                .templateName(template != null ? template.getName() : null)
                .title(workflow.getTitle())
                .status(workflow.getStatus())
                .priority(workflow.getPriority())
                .assignedEmployeeId(workflow.getAssignedEmployeeId())
                .assignedEmployeeName(assignee != null ? assignee.getFirstName() + (assignee.getLastName() != null ? " " + assignee.getLastName() : "") : null)
                .assignedEmployeeEmail(assignee != null ? assignee.getEmail() : null)
                .currentStepSequence(workflow.getCurrentStepSequence())
                .currentStepName(currentStepName)
                .totalSteps(workflow.getTotalSteps())
                .completedSteps(workflow.getCompletedSteps())
                .progressPercentage(progress)
                .dueDate(workflow.getDueDate())
                .internalTargetDate(workflow.getInternalTargetDate())
                .waitingForClient(workflow.isWaitingForClient())
                .pendingClientActionSummary(workflow.getPendingClientActionSummary())
                .isOverdue(isOverdue)
                .startedAt(workflow.getStartedAt())
                .completedAt(workflow.getCompletedAt())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .version(workflow.getVersion())
                .steps(stepDtos)
                .build();
    }

    private Map<UUID, ClientEntity> loadClients(UUID organizationId, List<ClientServiceWorkflowEntity> list) {
        Set<UUID> ids = list.stream().map(ClientServiceWorkflowEntity::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return clientRepository.findAllById(ids).stream()
                .filter(c -> organizationId.equals(c.getOrganizationId()))
                .collect(Collectors.toMap(ClientEntity::getId, c -> c));
    }

    private Map<UUID, ClientServiceEntity> loadServices(UUID organizationId, List<ClientServiceWorkflowEntity> list) {
        Set<UUID> ids = list.stream().map(ClientServiceWorkflowEntity::getClientServiceId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return clientServiceRepository.findAllById(ids).stream()
                .filter(s -> organizationId.equals(s.getOrganizationId()))
                .collect(Collectors.toMap(ClientServiceEntity::getId, s -> s));
    }

    private Map<UUID, ClientServicePeriodEntity> loadPeriods(UUID organizationId, List<ClientServiceWorkflowEntity> list) {
        Set<UUID> ids = list.stream().map(ClientServiceWorkflowEntity::getPeriodId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return periodRepository.findAllById(ids).stream()
                .filter(p -> organizationId.equals(p.getOrganizationId()))
                .collect(Collectors.toMap(ClientServicePeriodEntity::getId, p -> p));
    }

    private Map<UUID, EmployeeEntity> loadEmployees(UUID organizationId) {
        return employeeRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(EmployeeEntity::getId, e -> e, (a, b) -> a));
    }
}
