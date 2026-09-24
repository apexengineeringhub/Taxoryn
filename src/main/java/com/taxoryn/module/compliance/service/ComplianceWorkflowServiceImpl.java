package com.taxoryn.module.compliance.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.CompleteComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkbenchFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkbenchSummaryDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowChecklistItemDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDetailDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.compliance.dto.CreateWorkflowTaskRequest;
import com.taxoryn.module.compliance.dto.MarkWorkflowFiledRequest;
import com.taxoryn.module.compliance.dto.RequestWorkflowChangesRequest;
import com.taxoryn.module.compliance.dto.UpdateChecklistItemRequest;
import com.taxoryn.module.compliance.dto.WaitClientWorkflowRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowChecklistItemEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
public class ComplianceWorkflowServiceImpl implements ComplianceWorkflowService {

    private final ComplianceWorkflowRepository workflowRepository;
    private final ComplianceWorkflowChecklistItemRepository checklistItemRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final ClientRepository clientRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    // Standard 9 Execution Checkpoints
    private static final List<ChecklistSeedSpec> DEFAULT_CHECKLIST_SEEDS = List.of(
            new ChecklistSeedSpec("DOCUMENTS_REQUESTED", "Request Supporting Documents", "Request books, bank statements, sales/purchase registers and invoices from client", 1, true),
            new ChecklistSeedSpec("DOCUMENTS_RECEIVED", "Receive Client Documents", "Confirm receipt and completeness of all requested accounting records", 2, true),
            new ChecklistSeedSpec("DATA_VALIDATED", "Validate & Reconcile Data", "Perform 2B reconciliation, books vs returns validation, TDS mismatch check", 3, true),
            new ChecklistSeedSpec("COMPUTATION_PREPARED", "Prepare Tax Computation", "Draft return computation, eligible ITC/deductions, and tax liability", 4, true),
            new ChecklistSeedSpec("PRACTITIONER_REVIEW_COMPLETED", "Practitioner Internal Review", "Internal review of computation sheets and working papers", 5, true),
            new ChecklistSeedSpec("CLIENT_CONFIRMATION_RECEIVED", "Client Approval & Confirmation", "Obtain client confirmation/sign-off on final tax liability and computation", 6, true),
            new ChecklistSeedSpec("FILING_PACKAGE_PREPARED", "Prepare Filing Package & JSON", "Generate portal-ready JSON / XML or filing payload and verify challans", 7, true),
            new ChecklistSeedSpec("FILING_COMPLETED", "File on Government Portal", "Upload and submit return on government portal via DSC / EVC", 8, true),
            new ChecklistSeedSpec("ACKNOWLEDGEMENT_RECEIVED", "Download & Archive Acknowledgement", "Download ITR-V, GSTR-3B ARN receipt, TDS challan and deliver to client", 9, true)
    );

    private record ChecklistSeedSpec(String key, String title, String description, int order, boolean required) {}

    // =========================================================================
    // 1. Get or Create Workflow for Obligation (Idempotent)
    // =========================================================================

    @Override
    @Transactional
    public ComplianceWorkflowDto getOrCreateWorkflowForObligation(UUID obligationId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(obligationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found: " + obligationId));

        validateClientAccess(scope, obligation.getClientId());

        Optional<ComplianceWorkflowEntity> existing = workflowRepository.findByComplianceObligationIdAndOrganizationId(obligationId, organizationId);
        if (existing.isPresent()) {
            return mapToDto(existing.get(), obligation);
        }

        ComplianceWorkflowStatus initialStatus = determineInitialWorkflowStatus(obligation.getStatus());

        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(obligation.getClientId())
                .clientServiceId(obligation.getClientServiceId())
                .complianceObligationId(obligation.getId())
                .workflowStatus(initialStatus)
                .priority(obligation.getPriority() != null ? obligation.getPriority() : TaskPriority.MEDIUM)
                .targetDate(obligation.getInternalTargetDate() != null ? obligation.getInternalTargetDate() : obligation.getStatutoryDueDate())
                .statutoryDueDate(obligation.getStatutoryDueDate())
                .assignedEmployeeId(obligation.getAssignedEmployeeId())
                .build();
        workflow.setOrganizationId(organizationId);

        // Seed the 9 operational checklist checkpoints
        for (ChecklistSeedSpec seed : DEFAULT_CHECKLIST_SEEDS) {
            ComplianceWorkflowChecklistItemEntity item = ComplianceWorkflowChecklistItemEntity.builder()
                    .itemKey(seed.key())
                    .title(seed.title())
                    .description(seed.description())
                    .sequenceOrder(seed.order())
                    .isRequired(seed.required())
                    .isCompleted(false)
                    .build();
            workflow.addChecklistItem(item);
        }

        ComplianceWorkflowEntity saved = workflowRepository.save(workflow);

        // Link workflow back to obligation
        obligation.setWorkflowId(saved.getId());
        obligationRepository.save(obligation);

        auditService.logEvent(
                "WORKFLOW_CREATED",
                "COMPLIANCE_WORKFLOW",
                saved.getId().toString(),
                null,
                saved.getWorkflowStatus().name()
        );

        return mapToDto(saved, obligation);
    }

    // =========================================================================
    // 2. Get Detailed Workflow by ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ComplianceWorkflowDetailDto getWorkflowById(UUID workflowId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = workflowRepository.findByIdWithChecklist(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance workflow not found: " + workflowId));

        validateClientAccess(scope, workflow.getClientId());

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .orElse(null);

        ComplianceWorkflowDto baseDto = mapToDto(workflow, obligation);

        List<ComplianceWorkflowChecklistItemDto> checklistDtos = workflow.getChecklistItems().stream()
                .map(this::mapToChecklistItemDto)
                .toList();

        List<TaskEntity> linkedTaskEntities = taskRepository.findAllByOrganizationIdAndComplianceId(organizationId, workflow.getComplianceObligationId());
        List<TaskDto> linkedTasks = taskMapper.toDtoList(linkedTaskEntities);

        return ComplianceWorkflowDetailDto.builder()
                .workflow(baseDto)
                .checklistItems(checklistDtos)
                .linkedTasks(linkedTasks)
                .reviewNotes(workflow.getReviewNotes())
                .changesRequestedReason(workflow.getChangesRequestedReason())
                .approvedBy(workflow.getApprovedBy())
                .waitingRequestedBy(workflow.getWaitingRequestedBy())
                .filedBy(workflow.getFiledBy())
                .completedBy(workflow.getCompletedBy())
                .notes(workflow.getNotes())
                .build();
    }

    // =========================================================================
    // 3. Workbench Workflows List (Scoped & Filtered)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ComplianceWorkflowDto> getWorkbenchWorkflows(ComplianceWorkbenchFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        Specification<ComplianceWorkflowEntity> spec = createSpecification(organizationId, filter, accessibleClientIds, scope);
        Page<ComplianceWorkflowEntity> page = workflowRepository.findAll(spec, filter.toPageable());

        if (page.isEmpty()) {
            return PagedResponse.<ComplianceWorkflowDto>builder()
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
        Map<UUID, ComplianceObligationEntity> obligationMap = loadObligations(organizationId, page.getContent());
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        List<ComplianceWorkflowDto> dtos = page.getContent().stream()
                .map(wf -> mapToDto(wf, obligationMap.get(wf.getComplianceObligationId()), clientMap.get(wf.getClientId()), serviceMap.get(wf.getClientServiceId()), employeeMap))
                .collect(Collectors.toList());

        return PagedResponse.<ComplianceWorkflowDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    // =========================================================================
    // 4. Workbench Summary Metrics
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ComplianceWorkbenchSummaryDto getWorkbenchSummary() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        Instant firstDayOfMonth = today.withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        long active = workflowRepository.countActiveWorkflows(organizationId, accessibleClients);
        long dueToday = workflowRepository.countDueTodayWorkflows(organizationId, today, accessibleClients);
        long dueThisWeek = workflowRepository.countDueInRangeWorkflows(organizationId, today, weekEnd, accessibleClients);
        long overdue = workflowRepository.countOverdueWorkflows(organizationId, today, accessibleClients);
        long waiting = workflowRepository.countWaitingForClientWorkflows(organizationId, accessibleClients);
        long underReview = workflowRepository.countByStatus(organizationId, ComplianceWorkflowStatus.UNDER_REVIEW, accessibleClients);
        long readyForFiling = workflowRepository.countByStatus(organizationId, ComplianceWorkflowStatus.READY_FOR_FILING, accessibleClients);
        long completedThisMonth = workflowRepository.countCompletedSince(organizationId, firstDayOfMonth, accessibleClients);

        UUID currentEmployeeId = scope != null ? scope.getEmployeeId() : null;
        long myAssigned = currentEmployeeId != null ? workflowRepository.countMyAssignedWorkflows(organizationId, currentEmployeeId, accessibleClients) : 0L;
        long myReviews = currentEmployeeId != null ? workflowRepository.countMyReviewWorkflows(organizationId, currentEmployeeId, accessibleClients) : 0L;

        return ComplianceWorkbenchSummaryDto.builder()
                .totalActive(active)
                .dueToday(dueToday)
                .dueThisWeek(dueThisWeek)
                .overdue(overdue)
                .waitingForClient(waiting)
                .underReview(underReview)
                .readyForFiling(readyForFiling)
                .completedThisMonth(completedThisMonth)
                .myAssigned(myAssigned)
                .myReviews(myReviews)
                .build();
    }

    // =========================================================================
    // 5. Workflow State Transitions & Actions
    // =========================================================================

    @Override
    @Transactional
    public ComplianceWorkflowDto assignWorkflow(UUID workflowId, AssignComplianceWorkflowRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new BadRequestException("Assigned employee not found: " + request.getAssignedEmployeeId()));
            workflow.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }

        if (request.getReviewerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getReviewerEmployeeId(), organizationId)
                    .orElseThrow(() -> new BadRequestException("Reviewer employee not found: " + request.getReviewerEmployeeId()));
            workflow.setReviewerEmployeeId(request.getReviewerEmployeeId());
        }

        if (request.getPriority() != null) {
            workflow.setPriority(request.getPriority());
        }

        if (request.getTargetDate() != null) {
            workflow.setTargetDate(request.getTargetDate());
        }

        workflowRepository.save(workflow);

        // Sync with underlying obligation
        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    if (request.getAssignedEmployeeId() != null) ob.setAssignedEmployeeId(request.getAssignedEmployeeId());
                    if (request.getPriority() != null) ob.setPriority(request.getPriority());
                    if (request.getTargetDate() != null) ob.setInternalTargetDate(request.getTargetDate());
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_ASSIGNED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                null,
                "Assigned=" + workflow.getAssignedEmployeeId() + ", Reviewer=" + workflow.getReviewerEmployeeId()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto startWorkflow(UUID workflowId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.IN_PROGRESS);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.IN_PROGRESS);
        if (workflow.getStartedAt() == null) {
            workflow.setStartedAt(Instant.now());
        }

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    if (ob.getStatus() == ComplianceObligationStatus.UPCOMING || ob.getStatus() == ComplianceObligationStatus.READY) {
                        ob.setStatus(ComplianceObligationStatus.IN_PROGRESS);
                        obligationRepository.save(ob);
                    }
                });

        auditService.logEvent(
                "WORKFLOW_STARTED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.IN_PROGRESS.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto waitClient(UUID workflowId, WaitClientWorkflowRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.WAITING_FOR_CLIENT);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.WAITING_FOR_CLIENT);
        workflow.setWaitingForClient(true);
        workflow.setWaitingReason(request.getReason());
        workflow.setWaitingRequestedAt(Instant.now());
        workflow.setWaitingRequestedBy(SecurityUtils.getCurrentUserEmail());
        workflow.setExpectedResponseDate(request.getExpectedResponseDate());

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.WAITING_FOR_CLIENT);
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_WAITING_FOR_CLIENT",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.WAITING_FOR_CLIENT.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto resumeClient(UUID workflowId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        if (workflow.getWorkflowStatus() != ComplianceWorkflowStatus.WAITING_FOR_CLIENT && !workflow.isWaitingForClient()) {
            throw new BusinessValidationException("Workflow is not currently in WAITING_FOR_CLIENT state");
        }

        workflow.validateTransition(ComplianceWorkflowStatus.IN_PROGRESS);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.IN_PROGRESS);
        workflow.setWaitingForClient(false);

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.IN_PROGRESS);
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_RESUMED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.IN_PROGRESS.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto submitReview(UUID workflowId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.UNDER_REVIEW);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.UNDER_REVIEW);
        workflow.setSubmittedAt(Instant.now());

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.IN_PROGRESS);
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_SUBMITTED_FOR_REVIEW",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.UNDER_REVIEW.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto requestChanges(UUID workflowId, RequestWorkflowChangesRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.CHANGES_REQUIRED);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.CHANGES_REQUIRED);
        workflow.setChangesRequestedReason(request.getReason());
        workflow.setReviewedAt(Instant.now());
        workflow.setReviewedBy(SecurityUtils.getCurrentUserEmail());

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.IN_PROGRESS);
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_CHANGES_REQUESTED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.CHANGES_REQUIRED.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto approveWorkflow(UUID workflowId, ApproveWorkflowRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.READY_FOR_FILING);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.READY_FOR_FILING);
        workflow.setApprovedAt(Instant.now());
        workflow.setApprovedBy(SecurityUtils.getCurrentUserEmail());
        if (StringUtils.hasText(request.getNotes())) {
            workflow.setReviewNotes(request.getNotes());
        }

        // Auto-mark practitioner review completed checklist item
        workflow.getChecklistItems().stream()
                .filter(i -> "PRACTITIONER_REVIEW_COMPLETED".equalsIgnoreCase(i.getItemKey()))
                .findFirst()
                .ifPresent(item -> {
                    item.setCompleted(true);
                    item.setCompletedAt(Instant.now());
                    item.setCompletedByName(SecurityUtils.getCurrentUserEmail());
                    item.setCompletedByUserId(SecurityUtils.getCurrentUserId());
                });

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.READY_FOR_FILING);
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_APPROVED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.READY_FOR_FILING.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto markFiled(UUID workflowId, MarkWorkflowFiledRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.FILED);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.FILED);
        LocalDate filedDate = request.getFiledDate() != null ? request.getFiledDate() : LocalDate.now();
        workflow.setFiledDate(filedDate);
        workflow.setFiledAt(Instant.now());
        workflow.setFiledBy(SecurityUtils.getCurrentUserEmail());

        if (StringUtils.hasText(request.getAcknowledgementNumber())) {
            workflow.setAcknowledgementNumber(request.getAcknowledgementNumber());
        }

        // Mark checklist items
        workflow.getChecklistItems().stream()
                .filter(i -> "FILING_PACKAGE_PREPARED".equalsIgnoreCase(i.getItemKey()) || "FILING_COMPLETED".equalsIgnoreCase(i.getItemKey()))
                .forEach(item -> {
                    item.setCompleted(true);
                    if (item.getCompletedAt() == null) item.setCompletedAt(Instant.now());
                    if (item.getCompletedByName() == null) item.setCompletedByName(SecurityUtils.getCurrentUserEmail());
                });

        if (StringUtils.hasText(request.getAcknowledgementNumber())) {
            workflow.getChecklistItems().stream()
                    .filter(i -> "ACKNOWLEDGEMENT_RECEIVED".equalsIgnoreCase(i.getItemKey()))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setCompleted(true);
                        if (item.getCompletedAt() == null) item.setCompletedAt(Instant.now());
                        if (item.getCompletedByName() == null) item.setCompletedByName(SecurityUtils.getCurrentUserEmail());
                    });
        }

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.FILED);
                    ob.setFiledDate(filedDate);
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_FILED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.FILED.name()
        );

        return mapToDto(workflow);
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto completeWorkflow(UUID workflowId, CompleteComplianceWorkflowRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        workflow.validateTransition(ComplianceWorkflowStatus.COMPLETED);
        String oldStatus = workflow.getWorkflowStatus().name();

        workflow.setWorkflowStatus(ComplianceWorkflowStatus.COMPLETED);
        workflow.setCompletedAt(Instant.now());
        workflow.setCompletedBy(SecurityUtils.getCurrentUserEmail());

        if (StringUtils.hasText(request.getAcknowledgementNumber())) {
            workflow.setAcknowledgementNumber(request.getAcknowledgementNumber());
        }
        if (StringUtils.hasText(request.getNotes())) {
            workflow.setNotes(request.getNotes());
        }

        // Mark remaining checklist items complete
        workflow.getChecklistItems().stream()
                .filter(i -> !i.isCompleted())
                .forEach(item -> {
                    item.setCompleted(true);
                    item.setCompletedAt(Instant.now());
                    item.setCompletedByName(SecurityUtils.getCurrentUserEmail());
                });

        workflowRepository.save(workflow);

        obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId)
                .ifPresent(ob -> {
                    ob.setStatus(ComplianceObligationStatus.COMPLETED);
                    ob.setCompletedAt(Instant.now());
                    obligationRepository.save(ob);
                });

        auditService.logEvent(
                "WORKFLOW_COMPLETED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                oldStatus,
                ComplianceWorkflowStatus.COMPLETED.name()
        );

        return mapToDto(workflow);
    }

    // =========================================================================
    // 6. Checklist Item Updates
    // =========================================================================

    @Override
    @Transactional
    public ComplianceWorkflowChecklistItemDto updateChecklistItem(UUID workflowId, UUID itemId, UpdateChecklistItemRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        ComplianceWorkflowChecklistItemEntity item = checklistItemRepository.findByIdAndOrganizationId(itemId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        if (!item.getWorkflow().getId().equals(workflowId)) {
            throw new BadRequestException("Checklist item does not belong to workflow: " + workflowId);
        }

        boolean isCompleted = Boolean.TRUE.equals(request.getIsCompleted());
        item.setCompleted(isCompleted);
        if (isCompleted) {
            item.setCompletedAt(Instant.now());
            item.setCompletedByUserId(SecurityUtils.getCurrentUserId());
            item.setCompletedByName(SecurityUtils.getCurrentUserEmail());
        } else {
            item.setCompletedAt(null);
            item.setCompletedByUserId(null);
            item.setCompletedByName(null);
        }

        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }

        ComplianceWorkflowChecklistItemEntity saved = checklistItemRepository.save(item);

        auditService.logEvent(
                "CHECKLIST_ITEM_UPDATED",
                "COMPLIANCE_WORKFLOW_CHECKLIST",
                saved.getId().toString(),
                null,
                saved.isCompleted() ? "COMPLETED" : "PENDING"
        );

        return mapToChecklistItemDto(saved);
    }

    // =========================================================================
    // 7. Workflow Task Creation
    // =========================================================================

    @Override
    @Transactional
    public TaskDto createWorkflowTask(UUID workflowId, CreateWorkflowTaskRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, organizationId);
        validateClientAccess(scope, workflow.getClientId());

        TaskEntity task = TaskEntity.builder()
                .clientId(workflow.getClientId())
                .assignedTo(request.getAssignedTo() != null ? request.getAssignedTo() : workflow.getAssignedEmployeeId())
                .title(request.getTitle())
                .description(request.getDescription())
                .taskCategory(request.getCategory() != null ? request.getCategory() : TaskCategory.COMPLIANCE)
                .status(TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : workflow.getPriority())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : workflow.getTargetDate())
                .complianceId(workflow.getComplianceObligationId())
                .build();
        task.setOrganizationId(organizationId);

        TaskEntity saved = taskRepository.save(task);

        auditService.logEvent(
                "TASK_CREATED_FROM_WORKFLOW",
                "TASK",
                saved.getId().toString(),
                null,
                saved.getStatus().name()
        );

        return taskMapper.toDto(saved);
    }

    // =========================================================================
    // Helper & Mapping Methods
    // =========================================================================

    private ComplianceWorkflowEntity getWorkflowOrThrow(UUID workflowId, UUID organizationId) {
        return workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance workflow not found: " + workflowId));
    }

    private void validateClientAccess(PracticeSecurityScope scope, UUID clientId) {
        if (scope == null || scope.isFirmAdmin()) {
            return;
        }
        Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClients != null && !accessibleClients.contains(clientId)) {
            throw new ForbiddenException("Access denied: You do not have permission to access client portfolio record: " + clientId);
        }
    }

    private ComplianceWorkflowStatus determineInitialWorkflowStatus(ComplianceObligationStatus obligationStatus) {
        if (obligationStatus == null) {
            return ComplianceWorkflowStatus.READY;
        }
        return switch (obligationStatus) {
            case UPCOMING -> ComplianceWorkflowStatus.CREATED;
            case READY -> ComplianceWorkflowStatus.READY;
            case IN_PROGRESS, OVERDUE -> ComplianceWorkflowStatus.IN_PROGRESS;
            case WAITING_FOR_CLIENT -> ComplianceWorkflowStatus.WAITING_FOR_CLIENT;
            case READY_FOR_FILING -> ComplianceWorkflowStatus.READY_FOR_FILING;
            case FILED -> ComplianceWorkflowStatus.FILED;
            case COMPLETED -> ComplianceWorkflowStatus.COMPLETED;
            case CANCELLED -> ComplianceWorkflowStatus.CANCELLED;
        };
    }

    private Specification<ComplianceWorkflowEntity> createSpecification(
            UUID organizationId,
            ComplianceWorkbenchFilterRequest filter,
            Set<UUID> accessibleClientIds,
            PracticeSecurityScope scope
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Client portfolio access boundaries
            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            if (filter != null) {
                if (filter.getClientId() != null) {
                    predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
                }
                if (filter.getClientServiceId() != null) {
                    predicates.add(cb.equal(root.get("clientServiceId"), filter.getClientServiceId()));
                }
                if (filter.getAssignedEmployeeId() != null) {
                    predicates.add(cb.equal(root.get("assignedEmployeeId"), filter.getAssignedEmployeeId()));
                }
                if (filter.getReviewerEmployeeId() != null) {
                    predicates.add(cb.equal(root.get("reviewerEmployeeId"), filter.getReviewerEmployeeId()));
                }
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("workflowStatus"), filter.getStatus()));
                }
                if (filter.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
                }
                if (filter.getWaitingForClient() != null) {
                    predicates.add(cb.equal(root.get("waitingForClient"), filter.getWaitingForClient()));
                }
                if (filter.getDueFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("statutoryDueDate"), filter.getDueFrom()));
                }
                if (filter.getDueTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("statutoryDueDate"), filter.getDueTo()));
                }
                if (filter.getTargetFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("targetDate"), filter.getTargetFrom()));
                }
                if (filter.getTargetTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("targetDate"), filter.getTargetTo()));
                }

                // View Type Quick Filters
                if (StringUtils.hasText(filter.getViewType())) {
                    LocalDate today = LocalDate.now();
                    switch (filter.getViewType().toUpperCase()) {
                        case "MY_ASSIGNED" -> {
                            if (scope != null && scope.getEmployeeId() != null) {
                                predicates.add(cb.equal(root.get("assignedEmployeeId"), scope.getEmployeeId()));
                            }
                            predicates.add(root.get("workflowStatus").in(
                                    ComplianceWorkflowStatus.READY,
                                    ComplianceWorkflowStatus.IN_PROGRESS,
                                    ComplianceWorkflowStatus.CHANGES_REQUIRED
                            ));
                        }
                        case "MY_REVIEWS" -> {
                            if (scope != null && scope.getEmployeeId() != null) {
                                predicates.add(cb.equal(root.get("reviewerEmployeeId"), scope.getEmployeeId()));
                            }
                            predicates.add(cb.equal(root.get("workflowStatus"), ComplianceWorkflowStatus.UNDER_REVIEW));
                        }
                        case "WAITING_FOR_CLIENT" -> predicates.add(cb.equal(root.get("waitingForClient"), true));
                        case "READY_FOR_FILING" -> predicates.add(cb.equal(root.get("workflowStatus"), ComplianceWorkflowStatus.READY_FOR_FILING));
                        case "DUE_TODAY" -> {
                            predicates.add(cb.equal(root.get("targetDate"), today));
                            predicates.add(root.get("workflowStatus").in(
                                    ComplianceWorkflowStatus.CREATED,
                                    ComplianceWorkflowStatus.READY,
                                    ComplianceWorkflowStatus.IN_PROGRESS,
                                    ComplianceWorkflowStatus.WAITING_FOR_CLIENT,
                                    ComplianceWorkflowStatus.UNDER_REVIEW,
                                    ComplianceWorkflowStatus.CHANGES_REQUIRED,
                                    ComplianceWorkflowStatus.READY_FOR_FILING
                            ));
                        }
                        case "OVERDUE" -> {
                            predicates.add(cb.lessThan(root.get("targetDate"), today));
                            predicates.add(root.get("workflowStatus").in(
                                    ComplianceWorkflowStatus.CREATED,
                                    ComplianceWorkflowStatus.READY,
                                    ComplianceWorkflowStatus.IN_PROGRESS,
                                    ComplianceWorkflowStatus.WAITING_FOR_CLIENT,
                                    ComplianceWorkflowStatus.UNDER_REVIEW,
                                    ComplianceWorkflowStatus.CHANGES_REQUIRED,
                                    ComplianceWorkflowStatus.READY_FOR_FILING
                            ));
                        }
                        case "COMPLETED" -> predicates.add(cb.equal(root.get("workflowStatus"), ComplianceWorkflowStatus.COMPLETED));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ComplianceWorkflowDto mapToDto(ComplianceWorkflowEntity workflow) {
        return mapToDto(workflow, null, null, null, null);
    }

    private ComplianceWorkflowDto mapToDto(ComplianceWorkflowEntity workflow, ComplianceObligationEntity obligation) {
        return mapToDto(workflow, obligation, null, null, null);
    }

    private ComplianceWorkflowDto mapToDto(
            ComplianceWorkflowEntity workflow,
            ComplianceObligationEntity obligation,
            ClientEntity client,
            ClientServiceEntity service,
            Map<UUID, EmployeeEntity> employeeMap
    ) {
        UUID organizationId = workflow.getOrganizationId();

        if (obligation == null && workflow.getComplianceObligationId() != null) {
            obligation = obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), organizationId).orElse(null);
        }
        if (client == null && workflow.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(workflow.getClientId(), organizationId).orElse(null);
        }
        if (service == null && workflow.getClientServiceId() != null) {
            service = clientServiceRepository.findByIdAndOrganizationId(workflow.getClientServiceId(), organizationId).orElse(null);
        }

        String assignedName = null;
        if (workflow.getAssignedEmployeeId() != null) {
            if (employeeMap != null && employeeMap.containsKey(workflow.getAssignedEmployeeId())) {
                assignedName = employeeMap.get(workflow.getAssignedEmployeeId()).getFullName();
            } else {
                assignedName = employeeRepository.findByIdAndOrganizationId(workflow.getAssignedEmployeeId(), organizationId)
                        .map(EmployeeEntity::getFullName)
                        .orElse(null);
            }
        }

        String reviewerName = null;
        if (workflow.getReviewerEmployeeId() != null) {
            if (employeeMap != null && employeeMap.containsKey(workflow.getReviewerEmployeeId())) {
                reviewerName = employeeMap.get(workflow.getReviewerEmployeeId()).getFullName();
            } else {
                reviewerName = employeeRepository.findByIdAndOrganizationId(workflow.getReviewerEmployeeId(), organizationId)
                        .map(EmployeeEntity::getFullName)
                        .orElse(null);
            }
        }

        int totalSteps = workflow.getChecklistItems() != null ? workflow.getChecklistItems().size() : 9;
        int completedSteps = workflow.getChecklistItems() != null
                ? (int) workflow.getChecklistItems().stream().filter(ComplianceWorkflowChecklistItemEntity::isCompleted).count()
                : 0;
        int progress = totalSteps > 0 ? (completedSteps * 100) / totalSteps : 0;

        LocalDate today = LocalDate.now();
        LocalDate statutoryDue = workflow.getStatutoryDueDate() != null ? workflow.getStatutoryDueDate() : (obligation != null ? obligation.getStatutoryDueDate() : null);
        LocalDate target = workflow.getTargetDate() != null ? workflow.getTargetDate() : statutoryDue;

        long daysRemaining = statutoryDue != null ? ChronoUnit.DAYS.between(today, statutoryDue) : 0L;
        long targetDaysRemaining = target != null ? ChronoUnit.DAYS.between(today, target) : 0L;
        boolean isOverdue = statutoryDue != null && today.isAfter(statutoryDue) && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.COMPLETED && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.CANCELLED;

        return ComplianceWorkflowDto.builder()
                .id(workflow.getId())
                .organizationId(workflow.getOrganizationId())
                .clientId(workflow.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .pan(client != null ? client.getPan() : null)
                .gstin(client != null ? client.getGstin() : null)
                .clientServiceId(workflow.getClientServiceId())
                .serviceName(service != null && service.getServiceType() != null ? service.getServiceType().name() : null)
                .complianceObligationId(workflow.getComplianceObligationId())
                .obligationTitle(obligation != null ? obligation.getTitle() : null)
                .obligationType(obligation != null ? obligation.getObligationType() : null)
                .periodLabel(obligation != null ? obligation.getPeriodLabel() : null)
                .statutoryDueDate(statutoryDue)
                .targetDate(target)
                .statutoryStatus(obligation != null ? obligation.getStatus() : null)
                .workflowStatus(workflow.getWorkflowStatus())
                .priority(workflow.getPriority())
                .assignedEmployeeId(workflow.getAssignedEmployeeId())
                .assignedEmployeeName(assignedName)
                .reviewerEmployeeId(workflow.getReviewerEmployeeId())
                .reviewerEmployeeName(reviewerName)
                .waitingForClient(workflow.isWaitingForClient())
                .waitingReason(workflow.getWaitingReason())
                .expectedResponseDate(workflow.getExpectedResponseDate())
                .totalChecklistSteps(totalSteps)
                .completedChecklistSteps(completedSteps)
                .progressPercentage(progress)
                .daysRemaining(daysRemaining)
                .targetDaysRemaining(targetDaysRemaining)
                .isOverdue(isOverdue)
                .filedDate(workflow.getFiledDate())
                .acknowledgementNumber(workflow.getAcknowledgementNumber())
                .startedAt(workflow.getStartedAt())
                .submittedAt(workflow.getSubmittedAt())
                .approvedAt(workflow.getApprovedAt())
                .completedAt(workflow.getCompletedAt())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }

    private ComplianceWorkflowChecklistItemDto mapToChecklistItemDto(ComplianceWorkflowChecklistItemEntity entity) {
        return ComplianceWorkflowChecklistItemDto.builder()
                .id(entity.getId())
                .workflowId(entity.getWorkflow().getId())
                .itemKey(entity.getItemKey())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .sequenceOrder(entity.getSequenceOrder())
                .isCompleted(entity.isCompleted())
                .isRequired(entity.isRequired())
                .completedAt(entity.getCompletedAt())
                .completedByUserId(entity.getCompletedByUserId())
                .completedByName(entity.getCompletedByName())
                .notes(entity.getNotes())
                .build();
    }

    private Map<UUID, ClientEntity> loadClients(UUID organizationId, List<ComplianceWorkflowEntity> list) {
        Set<UUID> ids = list.stream().map(ComplianceWorkflowEntity::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return clientRepository.findAllById(ids).stream().collect(Collectors.toMap(ClientEntity::getId, c -> c));
    }

    private Map<UUID, ClientServiceEntity> loadServices(UUID organizationId, List<ComplianceWorkflowEntity> list) {
        Set<UUID> ids = list.stream().map(ComplianceWorkflowEntity::getClientServiceId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return clientServiceRepository.findAllById(ids).stream().collect(Collectors.toMap(ClientServiceEntity::getId, s -> s));
    }

    private Map<UUID, ComplianceObligationEntity> loadObligations(UUID organizationId, List<ComplianceWorkflowEntity> list) {
        Set<UUID> ids = list.stream().map(ComplianceWorkflowEntity::getComplianceObligationId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return obligationRepository.findAllById(ids).stream().collect(Collectors.toMap(ComplianceObligationEntity::getId, o -> o));
    }

    private Map<UUID, EmployeeEntity> loadEmployees(UUID organizationId) {
        List<EmployeeEntity> list = employeeRepository.findAllByOrganizationId(organizationId);
        Map<UUID, EmployeeEntity> map = new HashMap<>();
        for (EmployeeEntity emp : list) {
            map.put(emp.getId(), emp);
        }
        return map;
    }
}
