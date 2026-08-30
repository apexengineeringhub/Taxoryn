package com.taxoryn.module.compliance.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.AssignComplianceEmployeeRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.mapper.ComplianceMapper;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceObligationRepository obligationRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceRuleService ruleService;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final ComplianceMapper complianceMapper;

    // =========================================================================
    // 1. Calendar, Upcoming, Overdue, Due Today
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ComplianceObligationDto> getCalendar(ComplianceCalendarFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }

            if (filterRequest.getComplianceType() != null) {
                predicates.add(cb.equal(root.get("complianceType"), filterRequest.getComplianceType()));
            }

            if (StringUtils.hasText(filterRequest.getPeriod())) {
                predicates.add(cb.equal(root.get("period"), filterRequest.getPeriod().trim()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (filterRequest.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filterRequest.getAssignedEmployeeId()));
            }

            if (filterRequest.getStartDate() != null && filterRequest.getEndDate() != null) {
                predicates.add(cb.between(root.get("dueDate"), filterRequest.getStartDate(), filterRequest.getEndDate()));
            } else if (filterRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filterRequest.getStartDate()));
            } else if (filterRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filterRequest.getEndDate()));
            }

            LocalDate today = LocalDate.now();
            if (Boolean.TRUE.equals(filterRequest.getIsDueToday())) {
                predicates.add(cb.equal(root.get("dueDate"), today));
            }

            if (Boolean.TRUE.equals(filterRequest.getIsDueThisWeek())) {
                LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                predicates.add(cb.between(root.get("dueDate"), startOfWeek, endOfWeek));
            }

            if (Boolean.TRUE.equals(filterRequest.getIsOverdue())) {
                predicates.add(cb.lessThan(root.get("dueDate"), today));
                predicates.add(root.get("status").in(ComplianceStatus.PENDING, ComplianceStatus.IN_PROGRESS, ComplianceStatus.OVERDUE));
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("period")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ComplianceObligationEntity> page = obligationRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichObligationDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getUpcoming(int daysAhead) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(daysAhead > 0 ? daysAhead : 30);

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organizationId"), organizationId),
                cb.between(root.get("dueDate"), today, targetDate),
                root.get("status").in(ComplianceStatus.PENDING, ComplianceStatus.IN_PROGRESS)
        );

        return obligationRepository.findAll(spec).stream().map(this::enrichObligationDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getOverdue() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();

        Specification<ComplianceObligationEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organizationId"), organizationId),
                cb.lessThan(root.get("dueDate"), today),
                root.get("status").in(ComplianceStatus.PENDING, ComplianceStatus.IN_PROGRESS, ComplianceStatus.OVERDUE)
        );

        return obligationRepository.findAll(spec).stream().map(this::enrichObligationDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getDueToday() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();

        List<ComplianceObligationEntity> obligations = obligationRepository.findAllByOrganizationIdAndDueDate(organizationId, today);
        return obligations.stream().map(this::enrichObligationDto).toList();
    }

    // =========================================================================
    // 2. Executive Dashboard Statistics
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ComplianceDashboardStatsDto getDashboardStats() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate nextWeek = today.plusDays(7);

        List<ComplianceObligationEntity> allOrgObligations = obligationRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId)
        );

        long dueTodayCount = 0;
        long dueThisWeekCount = 0;
        long overdueCount = 0;
        long completedCount = 0;
        long totalActiveCount = 0;
        Map<String, Long> countByType = new HashMap<>();

        List<ComplianceObligationDto> dueTodayList = new ArrayList<>();
        List<ComplianceObligationDto> upcomingList = new ArrayList<>();
        List<ComplianceObligationDto> overdueList = new ArrayList<>();

        for (ComplianceObligationEntity ob : allOrgObligations) {
            boolean isActive = ob.getStatus() == ComplianceStatus.PENDING
                    || ob.getStatus() == ComplianceStatus.IN_PROGRESS
                    || ob.getStatus() == ComplianceStatus.OVERDUE;

            if (ob.getStatus() == ComplianceStatus.COMPLETED) {
                completedCount++;
            }

            if (isActive) {
                totalActiveCount++;
                countByType.merge(ob.getComplianceType().name(), 1L, Long::sum);

                if (ob.getDueDate().isEqual(today)) {
                    dueTodayCount++;
                    dueTodayList.add(enrichObligationDto(ob));
                }

                if (!ob.getDueDate().isBefore(startOfWeek) && !ob.getDueDate().isAfter(endOfWeek)) {
                    dueThisWeekCount++;
                }

                if (ob.getDueDate().isBefore(today)) {
                    overdueCount++;
                    overdueList.add(enrichObligationDto(ob));
                } else if (!ob.getDueDate().isAfter(nextWeek)) {
                    upcomingList.add(enrichObligationDto(ob));
                }
            }
        }

        return ComplianceDashboardStatsDto.builder()
                .dueTodayCount(dueTodayCount)
                .dueThisWeekCount(dueThisWeekCount)
                .overdueCount(overdueCount)
                .completedCount(completedCount)
                .totalActiveCount(totalActiveCount)
                .countByType(countByType)
                .dueTodayList(dueTodayList)
                .upcomingList(upcomingList)
                .overdueList(overdueList)
                .build();
    }

    // =========================================================================
    // 3. Obligation Lifecycle & Assignment
    // =========================================================================

    @Override
    @Transactional
    public ComplianceObligationDto createObligation(CreateComplianceObligationRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(request.getClientId())
                .ruleId(request.getRuleId())
                .title(request.getTitle().trim())
                .complianceType(request.getComplianceType())
                .period(request.getPeriod().trim())
                .dueDate(request.getDueDate())
                .status(request.getStatus() != null ? request.getStatus() : ComplianceStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : CompliancePriority.MEDIUM)
                .assignedEmployeeId(request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : client.getAssignedEmployeeId())
                .notes(request.getNotes())
                .build();
        obligation.setOrganizationId(organizationId);

        ComplianceObligationEntity saved = obligationRepository.save(obligation);
        log.info("Created compliance obligation: id={}, title={}, due={} for tenant={}", saved.getId(), saved.getTitle(), saved.getDueDate(), organizationId);
        return enrichObligationDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceObligationDto getObligationById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));
        return enrichObligationDto(obligation);
    }

    @Override
    @Transactional
    public ComplianceObligationDto updateStatus(UUID id, UpdateComplianceStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        obligation.setStatus(request.getStatus());

        if (request.getStatus() == ComplianceStatus.COMPLETED) {
            obligation.setCompletedAt(Instant.now());
            try {
                obligation.setCompletedBy(SecurityUtils.getCurrentUserEmail());
            } catch (Exception ignored) {
                obligation.setCompletedBy("system");
            }
        }

        if (StringUtils.hasText(request.getNotes())) {
            String currentNotes = StringUtils.hasText(obligation.getNotes()) ? obligation.getNotes() + "\n" : "";
            obligation.setNotes(currentNotes + "[" + LocalDate.now() + " Status -> " + request.getStatus() + "]: " + request.getNotes().trim());
        }

        // Synchronize linked task if present
        if (obligation.getTaskId() != null && request.getStatus() == ComplianceStatus.COMPLETED) {
            taskRepository.findByIdAndOrganizationId(obligation.getTaskId(), organizationId)
                    .ifPresent(task -> {
                        task.setStatus(TaskStatus.COMPLETED);
                        taskRepository.save(task);
                    });
        }

        ComplianceObligationEntity saved = obligationRepository.save(obligation);
        log.info("Updated compliance obligation status: id={}, newStatus={} for tenant={}", id, request.getStatus(), organizationId);
        return enrichObligationDto(saved);
    }

    @Override
    @Transactional
    public ComplianceObligationDto assignEmployee(UUID id, AssignComplianceEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        obligation.setAssignedEmployeeId(request.getEmployeeId());

        if (obligation.getTaskId() != null) {
            taskRepository.findByIdAndOrganizationId(obligation.getTaskId(), organizationId)
                    .ifPresent(task -> {
                        task.setAssignedTo(request.getEmployeeId());
                        taskRepository.save(task);
                    });
        }

        ComplianceObligationEntity saved = obligationRepository.save(obligation);
        log.info("Assigned employee {} to compliance obligation {} for tenant {}", request.getEmployeeId(), id, organizationId);
        return enrichObligationDto(saved);
    }

    @Override
    @Transactional
    public ComplianceObligationDto createTaskForObligation(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Obligation", "id", id));

        if (obligation.getTaskId() != null) {
            return enrichObligationDto(obligation);
        }

        TaskCategory category = mapComplianceTypeToTaskCategory(obligation.getComplianceType());
        TaskPriority priority = mapCompliancePriorityToTaskPriority(obligation.getPriority());

        TaskEntity task = TaskEntity.builder()
                .clientId(obligation.getClientId())
                .assignedTo(obligation.getAssignedEmployeeId())
                .title(obligation.getTitle())
                .description("Generated from statutory compliance obligation: " + obligation.getTitle() + " for period " + obligation.getPeriod())
                .taskCategory(category)
                .priority(priority)
                .dueDate(obligation.getDueDate())
                .complianceId(obligation.getId())
                .status(TaskStatus.TODO)
                .build();
        task.setOrganizationId(organizationId);

        TaskEntity savedTask = taskRepository.save(task);
        obligation.setTaskId(savedTask.getId());
        obligation.setStatus(ComplianceStatus.IN_PROGRESS);

        ComplianceObligationEntity saved = obligationRepository.save(obligation);
        log.info("Created Task {} linked to compliance obligation {} for tenant {}", savedTask.getId(), id, organizationId);
        return enrichObligationDto(saved);
    }

    // =========================================================================
    // 4. Batch & Scheduled Generation
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

                    ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                            .clientId(client.getId())
                            .ruleId(rule.getId())
                            .title(title)
                            .complianceType(rule.getComplianceType())
                            .period(period)
                            .dueDate(dueDate)
                            .status(ComplianceStatus.PENDING)
                            .priority(CompliancePriority.MEDIUM)
                            .assignedEmployeeId(client.getAssignedEmployeeId())
                            .notes(notes)
                            .build();
                    ob.setOrganizationId(organizationId);

                    createdEntities.add(obligationRepository.save(ob));
                }
            }
        }

        log.info("Batch generated {} compliance obligations for period {} in tenant {}", createdEntities.size(), period, organizationId);
        return createdEntities.stream().map(this::enrichObligationDto).toList();
    }

    @Override
    @Transactional
    public int processOverdueObligations() {
        LocalDate today = LocalDate.now();
        List<ComplianceObligationEntity> pendingOrInProgress = obligationRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.lessThan(root.get("dueDate"), today),
                        root.get("status").in(ComplianceStatus.PENDING, ComplianceStatus.IN_PROGRESS)
                )
        );

        for (ComplianceObligationEntity ob : pendingOrInProgress) {
            ob.setStatus(ComplianceStatus.OVERDUE);
            obligationRepository.save(ob);
        }

        if (!pendingOrInProgress.isEmpty()) {
            log.info("Scheduled job marked {} compliance obligations as OVERDUE", pendingOrInProgress.size());
        }
        return pendingOrInProgress.size();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ComplianceObligationDto enrichObligationDto(ComplianceObligationEntity entity) {
        ComplianceObligationDto dto = complianceMapper.toObligationDto(entity);
        LocalDate today = LocalDate.now();

        if (entity.getDueDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, entity.getDueDate());
            dto.setDaysRemaining(days);
            dto.setOverdue(entity.getDueDate().isBefore(today) && entity.getStatus() != ComplianceStatus.COMPLETED && entity.getStatus() != ComplianceStatus.WAIVED && entity.getStatus() != ComplianceStatus.CANCELLED);
        }

        clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                .ifPresent(c -> {
                    dto.setClientName(c.getDisplayName());
                    dto.setPan(c.getPan());
                    dto.setGstin(c.getGstin());
                });

        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getAssignedEmployeeId(), entity.getOrganizationId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFullName()));
        }

        return dto;
    }

    private TaskCategory mapComplianceTypeToTaskCategory(ComplianceType type) {
        if (type == null) return TaskCategory.COMPLIANCE;
        return switch (type) {
            case GST -> TaskCategory.GST;
            case ITR -> TaskCategory.ITR;
            case TDS, ROC, ADVANCE_TAX, OTHER -> TaskCategory.COMPLIANCE;
        };
    }

    private TaskPriority mapCompliancePriorityToTaskPriority(CompliancePriority priority) {
        if (priority == null) return TaskPriority.MEDIUM;
        return switch (priority) {
            case LOW -> TaskPriority.LOW;
            case MEDIUM -> TaskPriority.MEDIUM;
            case HIGH -> TaskPriority.HIGH;
            case CRITICAL -> TaskPriority.URGENT;
        };
    }
}
