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
import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.dto.AssignComplianceEmployeeRequest;
import com.taxoryn.module.compliance.dto.AssignObligationRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarSummaryDto;
import com.taxoryn.module.compliance.dto.ComplianceCycleTemplateDto;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceCycleTemplateEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceCycleTemplateRepository;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceReminderRuleRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.workflow.entity.ClientServicePeriodEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.repository.ClientServicePeriodRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceObligationRepository obligationRepository;
    private final ComplianceCycleTemplateRepository cycleTemplateRepository;
    private final ComplianceReminderRuleRepository reminderRuleRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleService ruleService;
    private final DueDateCalculationService dueDateCalculationService;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final ClientServicePeriodRepository clientServicePeriodRepository;
    private final ClientServiceWorkflowRepository clientServiceWorkflowRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final ModuleConfigurationService moduleConfigurationService;
    private final AuditService auditService;

    // =========================================================================
    // 1. Calendar, Upcoming, Overdue, Due Today
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ComplianceObligationDto> getCalendar(ComplianceCalendarFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        Specification<ComplianceObligationEntity> spec = createSpecification(organizationId, filterRequest, accessibleClientIds, scope);
        Page<ComplianceObligationEntity> page = obligationRepository.findAll(spec, filterRequest.toPageable());

        if (page.isEmpty()) {
            return PagedResponse.<ComplianceObligationDto>builder()
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
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, page.getContent());
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        List<ComplianceObligationDto> dtos = page.getContent().stream()
                .map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap))
                .collect(Collectors.toList());

        return PagedResponse.<ComplianceObligationDto>builder()
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
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getUpcoming(int daysAhead) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(daysAhead > 0 ? daysAhead : 30);

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            predicates.add(cb.between(root.get("statutoryDueDate"), today, targetDate));
            predicates.add(root.get("status").in(
                    ComplianceObligationStatus.UPCOMING,
                    ComplianceObligationStatus.READY,
                    ComplianceObligationStatus.IN_PROGRESS,
                    ComplianceObligationStatus.WAITING_FOR_CLIENT,
                    ComplianceObligationStatus.READY_FOR_FILING
            ));

            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ComplianceObligationEntity> list = obligationRepository.findAll(spec);
        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, list);
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, list);
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, list);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return list.stream()
                .map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getOverdue() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        LocalDate today = LocalDate.now();

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            predicates.add(cb.lessThan(root.get("statutoryDueDate"), today));
            predicates.add(root.get("status").in(
                    ComplianceObligationStatus.UPCOMING,
                    ComplianceObligationStatus.READY,
                    ComplianceObligationStatus.IN_PROGRESS,
                    ComplianceObligationStatus.WAITING_FOR_CLIENT,
                    ComplianceObligationStatus.READY_FOR_FILING,
                    ComplianceObligationStatus.OVERDUE
            ));

            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ComplianceObligationEntity> list = obligationRepository.findAll(spec);
        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, list);
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, list);
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, list);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return list.stream()
                .map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getDueToday() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        LocalDate today = LocalDate.now();

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            predicates.add(cb.equal(root.get("statutoryDueDate"), today));

            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ComplianceObligationEntity> list = obligationRepository.findAll(spec);
        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, list);
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, list);
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, list);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return list.stream()
                .map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap))
                .toList();
    }

    // =========================================================================
    // 2. Executive Dashboard & Summary Statistics (Zero-leakage filtered)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ComplianceCalendarSummaryDto getCalendarSummary() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ComplianceObligationEntity> obligations = obligationRepository.findAll(spec);

        long dueTodayCount = 0;
        long dueThisWeekCount = 0;
        long dueThisMonthCount = 0;
        long overdueCount = 0;
        long waitingForClientCount = 0;
        long readyForFilingCount = 0;
        long completedCount = 0;
        long totalActiveCount = 0;
        Map<String, Long> countByType = new HashMap<>();

        List<ComplianceObligationEntity> dueTodayEntities = new ArrayList<>();
        List<ComplianceObligationEntity> upcomingEntities = new ArrayList<>();
        List<ComplianceObligationEntity> overdueEntities = new ArrayList<>();

        for (ComplianceObligationEntity ob : obligations) {
            boolean isCompleted = ob.getStatus() == ComplianceObligationStatus.COMPLETED;
            boolean isCancelled = ob.getStatus() == ComplianceObligationStatus.CANCELLED;
            boolean isActive = ob.getStatus().isActive();

            if (isCompleted) {
                completedCount++;
            }

            if (isActive && !isCancelled) {
                totalActiveCount++;
                String typeKey = ob.getObligationType() != null ? ob.getObligationType().name() : "OTHER";
                countByType.merge(typeKey, 1L, Long::sum);

                if (ob.getComplianceType() != null) {
                    countByType.putIfAbsent(ob.getComplianceType().name(), countByType.get(typeKey));
                } else if (ob.getObligationType() != null) {
                    String legacyKey = switch (ob.getObligationType()) {
                        case GST_RETURN -> "GST";
                        case ITR_FILING, TAX_AUDIT -> "ITR";
                        case TDS_RETURN -> "TDS";
                        case ROC_COMPLIANCE -> "ROC";
                        case ADVANCE_TAX, SELF_ASSESSMENT_TAX -> "ADVANCE_TAX";
                        default -> "OTHER";
                    };
                    countByType.putIfAbsent(legacyKey, countByType.get(typeKey));
                }

                if (ob.getStatus() == ComplianceObligationStatus.WAITING_FOR_CLIENT) {
                    waitingForClientCount++;
                }
                if (ob.getStatus() == ComplianceObligationStatus.READY_FOR_FILING) {
                    readyForFilingCount++;
                }

                LocalDate dueDate = ob.getStatutoryDueDate();
                if (dueDate != null) {
                    if (dueDate.isEqual(today)) {
                        dueTodayCount++;
                        dueTodayEntities.add(ob);
                    }
                    if (!dueDate.isBefore(startOfWeek) && !dueDate.isAfter(endOfWeek)) {
                        dueThisWeekCount++;
                    }
                    if (!dueDate.isBefore(startOfMonth) && !dueDate.isAfter(endOfMonth)) {
                        dueThisMonthCount++;
                    }
                    if (dueDate.isBefore(today)) {
                        overdueCount++;
                        overdueEntities.add(ob);
                    } else if (dueDate.isAfter(today) && !dueDate.isAfter(today.plusDays(14))) {
                        upcomingEntities.add(ob);
                    }
                }
            }
        }

        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, obligations);
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, obligations);
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, obligations);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return ComplianceCalendarSummaryDto.builder()
                .dueTodayCount(dueTodayCount)
                .dueToday(dueTodayCount)
                .dueThisWeekCount(dueThisWeekCount)
                .dueThisWeek(dueThisWeekCount)
                .dueThisMonthCount(dueThisMonthCount)
                .dueThisMonth(dueThisMonthCount)
                .upcoming(upcomingEntities.size())
                .overdueCount(overdueCount)
                .overdue(overdueCount)
                .waitingForClientCount(waitingForClientCount)
                .readyForFilingCount(readyForFilingCount)
                .completedCount(completedCount)
                .completed(completedCount)
                .totalActiveCount(totalActiveCount)
                .totalObligations(totalActiveCount + completedCount)
                .countByType(countByType)
                .dueTodayList(dueTodayEntities.stream().limit(10).map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap)).toList())
                .upcomingList(upcomingEntities.stream().limit(10).map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap)).toList())
                .overdueList(overdueEntities.stream().limit(10).map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap)).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceDashboardStatsDto getDashboardStats() {
        ComplianceCalendarSummaryDto summary = getCalendarSummary();
        return ComplianceDashboardStatsDto.builder()
                .dueTodayCount(summary.getDueTodayCount())
                .dueThisWeekCount(summary.getDueThisWeekCount())
                .overdueCount(summary.getOverdueCount())
                .completedCount(summary.getCompletedCount())
                .totalActiveCount(summary.getTotalActiveCount())
                .countByType(summary.getCountByType())
                .dueTodayList(summary.getDueTodayList())
                .upcomingList(summary.getUpcomingList())
                .overdueList(summary.getOverdueList())
                .build();
    }

    // =========================================================================
    // 3. Obligation Lifecycle & Assignment
    // =========================================================================

    @Override
    @Transactional
    public ComplianceObligationDto createObligation(CreateComplianceObligationRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        validateClientAccess(client.getId());

        if (request.getClientServiceId() != null) {
            clientServiceRepository.findByIdAndOrganizationId(request.getClientServiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", request.getClientServiceId()));
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        LocalDate statutoryDueDate = request.getStatutoryDueDate();
        LocalDate internalTargetDate = request.getInternalTargetDate() != null
                ? request.getInternalTargetDate()
                : dueDateCalculationService.calculateInternalTargetDate(statutoryDueDate, 3);

        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .clientServiceId(request.getClientServiceId())
                .servicePeriodId(request.getServicePeriodId())
                .workflowId(request.getWorkflowId())
                .ruleId(request.getRuleId())
                .obligationType(request.getObligationType() != null ? request.getObligationType() : ComplianceObligationType.OTHER)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .periodLabel(request.getPeriodLabel().trim())
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .statutoryDueDate(statutoryDueDate)
                .internalTargetDate(internalTargetDate)
                .status(request.getStatus() != null ? request.getStatus() : ComplianceObligationStatus.UPCOMING)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .assignedEmployeeId(request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : client.getAssignedEmployeeId())
                .notes(request.getNotes())
                .build();
        obligation.setOrganizationId(organizationId);
        obligation.syncLegacyFields();

        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_OBLIGATION_CREATED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                null,
                "Created obligation: " + saved.getTitle() + " due " + saved.getStatutoryDueDate()
        );

        return getObligationById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceObligationDto getObligationById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        validateClientAccess(obligation.getClientId());

        ClientEntity client = clientRepository.findByIdAndOrganizationId(obligation.getClientId(), organizationId).orElse(null);
        ClientServiceEntity service = obligation.getClientServiceId() != null
                ? clientServiceRepository.findByIdAndOrganizationId(obligation.getClientServiceId(), organizationId).orElse(null)
                : null;
        ClientServiceWorkflowEntity workflow = obligation.getWorkflowId() != null
                ? clientServiceWorkflowRepository.findByIdAndOrganizationId(obligation.getWorkflowId(), organizationId).orElse(null)
                : null;
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return mapToDto(obligation, client, service, workflow, employeeMap);
    }

    @Override
    @Transactional
    public ComplianceObligationDto updateObligation(UUID id, UpdateComplianceObligationRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        validateClientAccess(obligation.getClientId());

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            obligation.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            obligation.setDescription(request.getDescription());
        }
        if (request.getObligationType() != null) {
            obligation.setObligationType(request.getObligationType());
        }
        if (request.getPeriodLabel() != null && !request.getPeriodLabel().isBlank()) {
            obligation.setPeriodLabel(request.getPeriodLabel().trim());
        }
        if (request.getFinancialYear() != null) {
            obligation.setFinancialYear(request.getFinancialYear());
        }
        if (request.getAssessmentYear() != null) {
            obligation.setAssessmentYear(request.getAssessmentYear());
        }
        if (request.getStatutoryDueDate() != null) {
            obligation.setStatutoryDueDate(request.getStatutoryDueDate());
        }
        if (request.getInternalTargetDate() != null) {
            obligation.setInternalTargetDate(request.getInternalTargetDate());
        }
        if (request.getPriority() != null) {
            obligation.setPriority(request.getPriority());
        }
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getAssignedEmployeeId()));
            obligation.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }
        if (request.getNotes() != null) {
            obligation.setNotes(request.getNotes());
        }

        obligation.syncLegacyFields();
        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_OBLIGATION_UPDATED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                null,
                "Updated details for obligation: " + saved.getTitle()
        );

        return getObligationById(saved.getId());
    }

    @Override
    @Transactional
    public ComplianceObligationDto updateStatus(UUID id, UpdateObligationStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        validateClientAccess(obligation.getClientId());

        ComplianceObligationStatus currentStatus = obligation.getStatus();
        ComplianceObligationStatus targetStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessValidationException("Invalid compliance status transition from " + currentStatus + " to " + targetStatus);
        }

        obligation.setStatus(targetStatus);

        if (targetStatus == ComplianceObligationStatus.FILED) {
            obligation.setFiledDate(LocalDate.now());
            obligation.setFiledAt(Instant.now());
            try {
                obligation.setFiledBy(SecurityUtils.getCurrentUserEmail());
            } catch (Exception ignored) {
                obligation.setFiledBy("system");
            }
        }
        if (targetStatus == ComplianceObligationStatus.COMPLETED) {
            obligation.setCompletedAt(Instant.now());
            try {
                obligation.setCompletedBy(SecurityUtils.getCurrentUserEmail());
            } catch (Exception ignored) {
                obligation.setCompletedBy("system");
            }
        }

        if (StringUtils.hasText(request.getNotes())) {
            obligation.setNotes(request.getNotes().trim());
        }

        // Synchronize linked task if present
        if (obligation.getTaskId() != null && targetStatus == ComplianceObligationStatus.COMPLETED) {
            taskRepository.findByIdAndOrganizationId(obligation.getTaskId(), organizationId)
                    .ifPresent(task -> {
                        task.setStatus(TaskStatus.COMPLETED);
                        taskRepository.save(task);
                    });
        }

        obligation.syncLegacyFields();
        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_OBLIGATION_STATUS_UPDATED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                currentStatus.name(),
                targetStatus.name() + (request.getNotes() != null ? " - " + request.getNotes() : "")
        );

        return getObligationById(saved.getId());
    }

    @Override
    @Transactional
    public ComplianceObligationDto updateStatus(UUID id, UpdateComplianceStatusRequest request) {
        ComplianceObligationStatus targetStatus = switch (request.getStatus()) {
            case PENDING -> ComplianceObligationStatus.UPCOMING;
            case IN_PROGRESS -> ComplianceObligationStatus.IN_PROGRESS;
            case COMPLETED -> ComplianceObligationStatus.COMPLETED;
            case OVERDUE -> ComplianceObligationStatus.OVERDUE;
            case WAIVED, CANCELLED -> ComplianceObligationStatus.CANCELLED;
        };
        return updateStatus(id, UpdateObligationStatusRequest.builder()
                .status(targetStatus)
                .notes(request.getNotes())
                .build());
    }

    @Override
    @Transactional
    public ComplianceObligationDto assignEmployee(UUID id, AssignObligationRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        validateClientAccess(obligation.getClientId());

        if (employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), organizationId).isEmpty()) {
            throw new BusinessValidationException("Assigned employee not found or does not belong to your practice organization");
        }

        obligation.setAssignedEmployeeId(request.getEmployeeId());

        if (obligation.getTaskId() != null) {
            taskRepository.findByIdAndOrganizationId(obligation.getTaskId(), organizationId)
                    .ifPresent(task -> {
                        task.setAssignedTo(request.getEmployeeId());
                        taskRepository.save(task);
                    });
        }

        obligation.syncLegacyFields();
        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_OBLIGATION_ASSIGNED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                null,
                "Assigned employee " + request.getEmployeeId() + " to obligation " + saved.getTitle()
        );

        return getObligationById(saved.getId());
    }

    @Override
    @Transactional
    public ComplianceObligationDto assignEmployee(UUID id, AssignComplianceEmployeeRequest request) {
        return assignEmployee(id, AssignObligationRequest.builder()
                .employeeId(request.getEmployeeId())
                .build());
    }

    @Override
    @Transactional
    public ComplianceObligationDto createTaskForObligation(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        validateClientAccess(obligation.getClientId());

        if (obligation.getTaskId() != null) {
            return getObligationById(obligation.getId());
        }

        TaskCategory category = mapObligationTypeToTaskCategory(obligation.getObligationType());

        TaskEntity task = TaskEntity.builder()
                .clientId(obligation.getClientId())
                .assignedTo(obligation.getAssignedEmployeeId())
                .title(obligation.getTitle())
                .description("Generated from statutory compliance obligation: " + obligation.getTitle() + " for period " + obligation.getPeriodLabel())
                .taskCategory(category)
                .priority(obligation.getPriority() != null ? obligation.getPriority() : TaskPriority.MEDIUM)
                .dueDate(obligation.getStatutoryDueDate())
                .complianceId(obligation.getId())
                .status(TaskStatus.TODO)
                .build();
        task.setOrganizationId(organizationId);

        TaskEntity savedTask = taskRepository.save(task);
        obligation.setTaskId(savedTask.getId());
        if (obligation.getStatus() == ComplianceObligationStatus.UPCOMING) {
            obligation.setStatus(ComplianceObligationStatus.READY);
        }

        obligation.syncLegacyFields();
        ComplianceObligationEntity saved = obligationRepository.save(obligation);
        return getObligationById(saved.getId());
    }

    @Override
    @Transactional
    public void deleteObligation(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        validateClientAccess(obligation.getClientId());

        obligationRepository.delete(obligation);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_OBLIGATION_DELETED",
                "COMPLIANCE_OBLIGATION",
                id.toString(),
                null,
                "Deleted obligation " + obligation.getTitle()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getObligationsByClientId(UUID clientId) {
        validateClientAccess(clientId);
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        List<ComplianceObligationEntity> obligations = obligationRepository.findByOrganizationIdAndClientIdOrderByStatutoryDueDateAsc(organizationId, clientId);
        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, obligations);
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, obligations);
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, obligations);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return obligations.stream()
                .map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getObligationsByServiceId(UUID clientServiceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", clientServiceId));

        validateClientAccess(service.getClientId());

        List<ComplianceObligationEntity> obligations = obligationRepository.findByOrganizationIdAndClientServiceIdOrderByStatutoryDueDateAsc(organizationId, clientServiceId);
        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, obligations);
        Map<UUID, ClientServiceEntity> serviceMap = Map.of(service.getId(), service);
        Map<UUID, ClientServiceWorkflowEntity> workflowMap = loadWorkflows(organizationId, obligations);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return obligations.stream()
                .map(ob -> mapToDto(ob, clientMap.get(ob.getClientId()), serviceMap.get(ob.getClientServiceId()), workflowMap.get(ob.getWorkflowId()), employeeMap))
                .toList();
    }

    // =========================================================================
    // 4. Service Period & Cycle Integration
    // =========================================================================

    @Override
    @Transactional
    public ComplianceObligationDto generateObligationForServicePeriod(UUID clientServiceId, UUID servicePeriodId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", clientServiceId));

        ClientServicePeriodEntity period = clientServicePeriodRepository.findByIdAndOrganizationId(servicePeriodId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientServicePeriod", "id", servicePeriodId));

        validateClientAccess(service.getClientId());

        ComplianceObligationType obligationType = mapServiceTypeToDefaultObligationType(service.getServiceType());

        // Check if obligation already exists for this service and period (idempotent generation)
        Optional<ComplianceObligationEntity> existing = obligationRepository
                .findByOrganizationIdAndClientServiceIdAndServicePeriodIdAndObligationType(
                        organizationId, clientServiceId, servicePeriodId, obligationType);

        if (existing.isPresent()) {
            return getObligationById(existing.get().getId());
        }

        // Find applicable cycle template
        List<ComplianceCycleTemplateEntity> templates = cycleTemplateRepository.findByServiceTypeAndObligationType(
                organizationId, service.getServiceType(), obligationType);
        ComplianceCycleTemplateEntity template = templates.isEmpty() ? null : templates.get(0);

        LocalDate statutoryDueDate = period.getDueDate() != null
                ? period.getDueDate()
                : dueDateCalculationService.calculateStatutoryDueDate(
                        template,
                        period.getStartDate(),
                        period.getEndDate(),
                        period.getFinancialYear(),
                        period.getAssessmentYear()
                );

        LocalDate internalTargetDate = dueDateCalculationService.calculateInternalTargetDate(
                statutoryDueDate,
                template != null ? template.getInternalTargetOffsetDays() : 3
        );

        String serviceName = service.getServiceType() != null ? service.getServiceType().getDisplayName() : "Compliance Service";
        String title = period.getPeriodLabel() + " - " + serviceName;
        UUID activeWorkflowId = clientServiceWorkflowRepository.findByOrganizationIdAndClientServiceIdAndPeriodId(organizationId, service.getId(), period.getId())
                .map(ClientServiceWorkflowEntity::getId).orElse(null);

        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(service.getClientId())
                .clientServiceId(service.getId())
                .servicePeriodId(period.getId())
                .workflowId(activeWorkflowId)
                .obligationType(obligationType)
                .title(title)
                .description("Periodic compliance obligation for " + serviceName + " (" + period.getPeriodLabel() + ")")
                .periodLabel(period.getPeriodLabel())
                .financialYear(period.getFinancialYear())
                .assessmentYear(period.getAssessmentYear())
                .statutoryDueDate(statutoryDueDate)
                .internalTargetDate(internalTargetDate)
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.MEDIUM)
                .assignedEmployeeId(service.getAssignedEmployeeId())
                .build();
        obligation.setOrganizationId(organizationId);
        obligation.syncLegacyFields();

        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_OBLIGATION_CREATED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                null,
                "Generated compliance obligation for service period: " + saved.getTitle()
        );

        return getObligationById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceCycleTemplateDto> getCycleTemplates() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ComplianceCycleTemplateEntity> templates = cycleTemplateRepository.findActiveTemplatesForOrganization(organizationId);
        return templates.stream().map(t -> ComplianceCycleTemplateDto.builder()
                .id(t.getId())
                .organizationId(t.getOrganizationId())
                .serviceType(t.getServiceType())
                .obligationType(t.getObligationType())
                .name(t.getName())
                .description(t.getDescription())
                .recurrenceType(t.getRecurrenceType())
                .defaultDueDay(t.getDefaultDueDay())
                .defaultDueMonthOffset(t.getDefaultDueMonthOffset())
                .fixedDueMonth(t.getFixedDueMonth())
                .internalTargetOffsetDays(t.getInternalTargetOffsetDays())
                .isActive(t.getIsActive())
                .isSystem(t.getIsSystem())
                .build()).toList();
    }

    // =========================================================================
    // 5. Batch & Scheduled Generation
    // =========================================================================

    @Override
    @Transactional
    public List<ComplianceObligationDto> generateComplianceObligations(GenerateComplianceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String period = request.getPeriod().trim();

        List<ComplianceRuleEntity> rules = ruleRepository.findActiveRulesForOrganization(organizationId);
        if (request.getComplianceTypes() != null && !request.getComplianceTypes().isEmpty()) {
            rules = rules.stream()
                    .filter(r -> request.getComplianceTypes().contains(r.getComplianceType()))
                    .toList();
        }

        List<ClientEntity> clients;
        if (request.getClientIds() != null && !request.getClientIds().isEmpty()) {
            clients = clientRepository.findAllById(request.getClientIds()).stream()
                    .filter(c -> c.getOrganizationId().equals(organizationId) && c.getStatus() == ClientStatus.ACTIVE)
                    .toList();
        } else {
            clients = clientRepository.findAllByOrganizationIdAndStatus(organizationId, ClientStatus.ACTIVE);
        }

        List<ComplianceObligationEntity> createdEntities = new ArrayList<>();

        for (ClientEntity client : clients) {
            for (ComplianceRuleEntity rule : rules) {
                if (!ruleService.isRuleApplicableToClient(rule, client)) {
                    continue;
                }

                boolean alreadyExists = obligationRepository.existsByOrganizationIdAndClientIdAndPeriodAndRuleId(
                        organizationId, client.getId(), period, rule.getId());

                if (!alreadyExists) {
                    LocalDate dueDate = ruleService.calculateDueDate(rule, period);
                    String title = ruleService.formatTitle(rule, period) + " - " + client.getDisplayName();
                    String notes = ruleService.formatDescription(rule, period);

                    ComplianceObligationType obType = switch (rule.getComplianceType()) {
                        case GST -> ComplianceObligationType.GST_RETURN;
                        case ITR -> ComplianceObligationType.ITR_FILING;
                        case TDS -> ComplianceObligationType.TDS_RETURN;
                        case ROC -> ComplianceObligationType.ROC_COMPLIANCE;
                        case ADVANCE_TAX -> ComplianceObligationType.ADVANCE_TAX;
                        default -> ComplianceObligationType.OTHER;
                    };

                    ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                            .clientId(client.getId())
                            .ruleId(rule.getId())
                            .title(title)
                            .obligationType(obType)
                            .periodLabel(period)
                            .statutoryDueDate(dueDate)
                            .internalTargetDate(dueDate.minusDays(3))
                            .status(ComplianceObligationStatus.UPCOMING)
                            .priority(TaskPriority.MEDIUM)
                            .assignedEmployeeId(client.getAssignedEmployeeId())
                            .notes(notes)
                            .build();
                    ob.setOrganizationId(organizationId);
                    ob.syncLegacyFields();

                    createdEntities.add(obligationRepository.save(ob));
                }
            }
        }

        log.info("Batch generated {} compliance obligations for period {} in tenant {}", createdEntities.size(), period, organizationId);
        return createdEntities.stream().map(e -> getObligationById(e.getId())).toList();
    }

    @Override
    @Transactional
    public int processOverdueObligations() {
        LocalDate today = LocalDate.now();
        List<ComplianceObligationEntity> pendingOrInProgress = obligationRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.lessThan(root.get("statutoryDueDate"), today),
                        root.get("status").in(
                                ComplianceObligationStatus.UPCOMING,
                                ComplianceObligationStatus.READY,
                                ComplianceObligationStatus.IN_PROGRESS,
                                ComplianceObligationStatus.WAITING_FOR_CLIENT,
                                ComplianceObligationStatus.READY_FOR_FILING
                        )
                )
        );

        for (ComplianceObligationEntity ob : pendingOrInProgress) {
            ob.setStatus(ComplianceObligationStatus.OVERDUE);
            ob.syncLegacyFields();
            obligationRepository.save(ob);
        }

        if (!pendingOrInProgress.isEmpty()) {
            log.info("Scheduled job marked {} compliance obligations as OVERDUE", pendingOrInProgress.size());
        }
        return pendingOrInProgress.size();
    }

    // =========================================================================
    // Helpers & Mappers
    // =========================================================================

    private void validateClientAccess(UUID clientId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        if (accessibleClientIds != null && !accessibleClientIds.contains(clientId)) {
            throw new ForbiddenException("Access denied: Client is outside authorized portfolio scope");
        }
    }

    private Specification<ComplianceObligationEntity> createSpecification(
            UUID organizationId,
            ComplianceCalendarFilterRequest filter,
            Set<UUID> accessibleClientIds,
            PracticeSecurityScope scope
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }

            if (filter.getClientServiceId() != null) {
                predicates.add(cb.equal(root.get("clientServiceId"), filter.getClientServiceId()));
            }

            if (filter.getServicePeriodId() != null) {
                predicates.add(cb.equal(root.get("servicePeriodId"), filter.getServicePeriodId()));
            }

            if (filter.getObligationType() != null) {
                predicates.add(cb.equal(root.get("obligationType"), filter.getObligationType()));
            }

            if (StringUtils.hasText(filter.getPeriodLabel())) {
                predicates.add(cb.equal(root.get("periodLabel"), filter.getPeriodLabel().trim()));
            }

            if (StringUtils.hasText(filter.getFinancialYear())) {
                predicates.add(cb.equal(root.get("financialYear"), filter.getFinancialYear().trim()));
            }

            if (StringUtils.hasText(filter.getAssessmentYear())) {
                predicates.add(cb.equal(root.get("assessmentYear"), filter.getAssessmentYear().trim()));
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

            if (Boolean.TRUE.equals(filter.getMyObligationsOnly())) {
                UUID currentUserId = SecurityUtils.getCurrentUserId();
                predicates.add(cb.equal(root.get("assignedEmployeeId"), currentUserId));
            }

            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                predicates.add(cb.between(root.get("statutoryDueDate"), filter.getStartDate(), filter.getEndDate()));
            } else if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("statutoryDueDate"), filter.getStartDate()));
            } else if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("statutoryDueDate"), filter.getEndDate()));
            }

            LocalDate today = LocalDate.now();
            if (Boolean.TRUE.equals(filter.getIsDueToday())) {
                predicates.add(cb.equal(root.get("statutoryDueDate"), today));
            }

            if (Boolean.TRUE.equals(filter.getIsDueThisWeek())) {
                LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                predicates.add(cb.between(root.get("statutoryDueDate"), startOfWeek, endOfWeek));
            }

            if (Boolean.TRUE.equals(filter.getIsOverdue())) {
                predicates.add(cb.lessThan(root.get("statutoryDueDate"), today));
                predicates.add(root.get("status").in(
                        ComplianceObligationStatus.UPCOMING,
                        ComplianceObligationStatus.READY,
                        ComplianceObligationStatus.IN_PROGRESS,
                        ComplianceObligationStatus.WAITING_FOR_CLIENT,
                        ComplianceObligationStatus.READY_FOR_FILING,
                        ComplianceObligationStatus.OVERDUE
                ));
            }

            if (Boolean.TRUE.equals(filter.getIsWaitingForClient())) {
                predicates.add(cb.equal(root.get("status"), ComplianceObligationStatus.WAITING_FOR_CLIENT));
            }

            if (Boolean.TRUE.equals(filter.getIsReadyForFiling())) {
                predicates.add(cb.equal(root.get("status"), ComplianceObligationStatus.READY_FOR_FILING));
            }

            if (StringUtils.hasText(filter.getSearch())) {
                String pattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("periodLabel")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ComplianceObligationDto mapToDto(
            ComplianceObligationEntity entity,
            ClientEntity client,
            ClientServiceEntity service,
            ClientServiceWorkflowEntity workflow,
            Map<UUID, EmployeeEntity> employeeMap
    ) {
        LocalDate today = LocalDate.now();
        LocalDate statutoryDueDate = entity.getStatutoryDueDate();
        long daysRemaining = statutoryDueDate != null ? java.time.temporal.ChronoUnit.DAYS.between(today, statutoryDueDate) : 0;
        boolean isOverdue = statutoryDueDate != null && statutoryDueDate.isBefore(today) && !entity.getStatus().isTerminal();

        EmployeeEntity employee = entity.getAssignedEmployeeId() != null ? employeeMap.get(entity.getAssignedEmployeeId()) : null;

        return ComplianceObligationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientDisplayName(client != null ? client.getDisplayName() : null)
                .pan(client != null ? client.getPan() : null)
                .gstin(client != null ? client.getGstin() : null)
                .clientServiceId(entity.getClientServiceId())
                .serviceName(service != null && service.getServiceType() != null ? service.getServiceType().getDisplayName() : null)
                .servicePeriodId(entity.getServicePeriodId())
                .workflowId(entity.getWorkflowId())
                .workflowTitle(workflow != null ? workflow.getTitle() : null)
                .workflowStatus(workflow != null ? workflow.getStatus().name() : null)
                .ruleId(entity.getRuleId())
                .obligationType(entity.getObligationType() != null ? entity.getObligationType() : ComplianceObligationType.OTHER)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .periodLabel(entity.getPeriodLabel() != null ? entity.getPeriodLabel() : entity.getPeriod())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .statutoryDueDate(statutoryDueDate)
                .internalTargetDate(entity.getInternalTargetDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .assignedEmployeeId(entity.getAssignedEmployeeId())
                .assignedEmployeeName(employee != null ? employee.getFullName() : null)
                .assignedEmployeeEmail(employee != null ? employee.getEmail() : null)
                .taskId(entity.getTaskId())
                .completedAt(entity.getCompletedAt())
                .completedBy(entity.getCompletedBy())
                .filedDate(entity.getFiledDate())
                .filedAt(entity.getFiledAt())
                .filedBy(entity.getFiledBy())
                .notes(entity.getNotes())
                .daysRemaining(daysRemaining)
                .isOverdue(isOverdue)
                .period(entity.getPeriodLabel() != null ? entity.getPeriodLabel() : entity.getPeriod())
                .dueDate(statutoryDueDate)
                .complianceType(entity.getComplianceType() != null ? entity.getComplianceType().name() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ComplianceObligationType mapServiceTypeToDefaultObligationType(ClientServiceType serviceType) {
        if (serviceType == null) return ComplianceObligationType.OTHER;
        return switch (serviceType) {
            case GST_COMPLIANCE -> ComplianceObligationType.GST_RETURN;
            case ITR_COMPLIANCE -> ComplianceObligationType.ITR_FILING;
            case TDS_COMPLIANCE -> ComplianceObligationType.TDS_RETURN;
            case TAX_NOTICE_MANAGEMENT -> ComplianceObligationType.TAX_NOTICE_RESPONSE;
            case AUDIT_ASSURANCE -> ComplianceObligationType.TAX_AUDIT;
            default -> ComplianceObligationType.OTHER;
        };
    }

    private TaskCategory mapObligationTypeToTaskCategory(ComplianceObligationType type) {
        if (type == null) return TaskCategory.COMPLIANCE;
        return switch (type) {
            case GST_RETURN -> TaskCategory.GST;
            case ITR_FILING, TAX_AUDIT -> TaskCategory.ITR;
            default -> TaskCategory.COMPLIANCE;
        };
    }

    private Map<UUID, ClientEntity> loadClients(UUID orgId, List<ComplianceObligationEntity> obligations) {
        Set<UUID> clientIds = obligations.stream()
                .map(ComplianceObligationEntity::getClientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (clientIds.isEmpty()) return Collections.emptyMap();
        return clientRepository.findAllById(clientIds).stream()
                .filter(c -> c.getOrganizationId().equals(orgId))
                .collect(Collectors.toMap(ClientEntity::getId, c -> c));
    }

    private Map<UUID, ClientServiceEntity> loadServices(UUID orgId, List<ComplianceObligationEntity> obligations) {
        Set<UUID> serviceIds = obligations.stream()
                .map(ComplianceObligationEntity::getClientServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (serviceIds.isEmpty()) return Collections.emptyMap();
        return clientServiceRepository.findAllById(serviceIds).stream()
                .filter(s -> s.getOrganizationId().equals(orgId))
                .collect(Collectors.toMap(ClientServiceEntity::getId, s -> s));
    }

    private Map<UUID, ClientServiceWorkflowEntity> loadWorkflows(UUID orgId, List<ComplianceObligationEntity> obligations) {
        Set<UUID> workflowIds = obligations.stream()
                .map(ComplianceObligationEntity::getWorkflowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (workflowIds.isEmpty()) return Collections.emptyMap();
        return clientServiceWorkflowRepository.findAllById(workflowIds).stream()
                .filter(w -> w.getOrganizationId().equals(orgId))
                .collect(Collectors.toMap(ClientServiceWorkflowEntity::getId, w -> w));
    }

    private Map<UUID, EmployeeEntity> loadEmployees(UUID orgId) {
        return employeeRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(EmployeeEntity::getId, e -> e));
    }
}
