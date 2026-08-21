package com.taxoryn.module.dashboard.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.dashboard.dto.BillingStatsDto;
import com.taxoryn.module.dashboard.dto.ClientStatsDto;
import com.taxoryn.module.dashboard.dto.EmployeeStatsDto;
import com.taxoryn.module.dashboard.dto.EmployeeWorkloadItemDto;
import com.taxoryn.module.dashboard.dto.GstStatsDto;
import com.taxoryn.module.dashboard.dto.ItrStatsDto;
import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;
import com.taxoryn.module.dashboard.dto.TaskStatsDto;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final GstProfileRepository gstProfileRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final InvoiceRepository invoiceRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Override
    @Transactional(readOnly = true)
    public OrganizationDashboardDto getOrganizationDashboard() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        log.debug("Generating dashboard summary for tenant {} (Tier: {})", organizationId, scope.getRoleTier());

        // 1. Clients Statistics Aggregation
        ClientStatsDto clientStats = getClientStats(organizationId, scope);

        // 2. Employees Statistics Aggregation
        EmployeeStatsDto employeeStats = getEmployeeStats(organizationId, scope);

        // 3. Tasks Statistics Aggregation
        TaskStatsDto taskStats = getTaskStats(organizationId, today, scope);

        // 4. GST Compliance Statistics Aggregation
        GstStatsDto gstStats = getGstStats(organizationId, today, scope);

        // 5. ITR Compliance Statistics Aggregation
        ItrStatsDto itrStats = getItrStats(organizationId, today, scope);

        // 6. Billing & Financial Summary - Confidential data isolation
        BillingStatsDto billingStats = securityScopeEvaluator.hasBillingAccess(scope)
                ? getBillingStats(organizationId)
                : null;

        // 7. Employee Workload Distribution
        List<EmployeeWorkloadItemDto> employeeWorkload = getEmployeeWorkload(organizationId, today, scope);

        return OrganizationDashboardDto.builder()
                .clients(clientStats)
                .employees(employeeStats)
                .tasks(taskStats)
                .gst(gstStats)
                .itr(itrStats)
                .billing(billingStats)
                .employeeWorkload(employeeWorkload)
                .build();
    }

    private ClientStatsDto getClientStats(UUID organizationId, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            List<Object[]> results = clientRepository.getClientDashboardStats(organizationId);
            long total = 0;
            long active = 0;
            long inactive = 0;

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                total = toLong(row[0]);
                active = toLong(row[1]);
                inactive = toLong(row[2]);
            }

            return ClientStatsDto.builder()
                    .total(total)
                    .active(active)
                    .inactive(inactive)
                    .build();
        }

        Set<UUID> accessibleIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleIds == null || accessibleIds.isEmpty()) {
            return ClientStatsDto.builder()
                    .total(0)
                    .active(0)
                    .inactive(0)
                    .build();
        }

        List<ClientEntity> accessibleClients = clientRepository.findAllById(accessibleIds);
        long total = accessibleClients.size();
        long active = accessibleClients.stream().filter(c -> c.getStatus() == ClientEntity.ClientStatus.ACTIVE).count();
        long inactive = total - active;

        return ClientStatsDto.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .build();
    }

    private EmployeeStatsDto getEmployeeStats(UUID organizationId, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            List<Object[]> results = employeeRepository.getEmployeeDashboardStats(organizationId);
            long total = 0;
            long active = 0;

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                total = toLong(row[0]);
                active = toLong(row[1]);
            }

            return EmployeeStatsDto.builder()
                    .total(total)
                    .active(active)
                    .build();
        }

        if (scope.isStaff()) {
            return EmployeeStatsDto.builder()
                    .total(1)
                    .active(1)
                    .build();
        }

        // Department Manager
        String dept = scope.getDepartment();
        if (StringUtils.hasText(dept)) {
            List<EmployeeEntity> deptEmps = employeeRepository.findAllByOrganizationId(organizationId).stream()
                    .filter(e -> dept.equalsIgnoreCase(e.getDepartment()))
                    .toList();
            long total = deptEmps.size();
            long active = deptEmps.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count();
            return EmployeeStatsDto.builder().total(total).active(active).build();
        }

        return EmployeeStatsDto.builder().total(1).active(1).build();
    }

    private TaskStatsDto getTaskStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            List<Object[]> results = taskRepository.getTaskDashboardStats(organizationId, today);
            long total = 0;
            long pending = 0;
            long overdue = 0;
            long completed = 0;

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                total = toLong(row[0]);
                pending = toLong(row[1]);
                overdue = toLong(row[2]);
                completed = toLong(row[3]);
            }

            return TaskStatsDto.builder()
                    .total(total)
                    .pending(pending)
                    .overdue(overdue)
                    .completed(completed)
                    .build();
        }

        Set<UUID> assigneeIds = scope.getAccessibleAssigneeIds();
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return TaskStatsDto.builder().total(0).pending(0).overdue(0).completed(0).build();
        }

        long total = taskRepository.countAssignedTasks(organizationId, assigneeIds);
        long completed = taskRepository.countByStatuses(organizationId, assigneeIds, Set.of(TaskStatus.COMPLETED));
        Set<TaskStatus> pendingStatuses = Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW);
        long pending = taskRepository.countByStatuses(organizationId, assigneeIds, pendingStatuses);
        long overdue = taskRepository.countOverdueTasks(organizationId, assigneeIds, pendingStatuses, today);

        return TaskStatsDto.builder()
                .total(total)
                .pending(pending)
                .overdue(overdue)
                .completed(completed)
                .build();
    }

    private GstStatsDto getGstStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            long totalGstClients = gstProfileRepository.countDistinctClientsByOrganizationId(organizationId);
            List<Object[]> results = gstReturnFilingRepository.getGstDashboardStats(organizationId, today);

            long returnsDue = 0;
            long returnsOverdue = 0;
            long returnsFiled = 0;

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                returnsDue = toLong(row[0]);
                returnsOverdue = toLong(row[1]);
                returnsFiled = toLong(row[2]);
            }

            return GstStatsDto.builder()
                    .totalGstClients(totalGstClients)
                    .returnsDue(returnsDue)
                    .returnsOverdue(returnsOverdue)
                    .returnsFiled(returnsFiled)
                    .build();
        }

        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
            return GstStatsDto.builder().totalGstClients(0).returnsDue(0).returnsOverdue(0).returnsFiled(0).build();
        }

        List<GstProfileEntity> profiles = gstProfileRepository.findAllByOrganizationId(organizationId).stream()
                .filter(p -> accessibleClientIds.contains(p.getClientId()))
                .toList();

        long totalGstClients = profiles.stream().map(GstProfileEntity::getClientId).distinct().count();

        List<GstReturnFilingEntity> allFilings = new ArrayList<>();
        for (GstProfileEntity prof : profiles) {
            allFilings.addAll(gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(organizationId, prof.getClientId()));
        }

        long returnsFiled = allFilings.stream().filter(f -> f.getFilingStatus() == GstFilingStatus.FILED).count();
        long returnsOverdue = allFilings.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && f.getDueDate() != null && f.getDueDate().isBefore(today)).count();
        long returnsDue = allFilings.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && (f.getDueDate() == null || !f.getDueDate().isBefore(today))).count();

        return GstStatsDto.builder()
                .totalGstClients(totalGstClients)
                .returnsDue(returnsDue)
                .returnsOverdue(returnsOverdue)
                .returnsFiled(returnsFiled)
                .build();
    }

    private ItrStatsDto getItrStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            long totalItrClients = itrProfileRepository.countDistinctClientsByOrganizationId(organizationId);
            List<Object[]> results = itrReturnRepository.getItrDashboardStats(organizationId, today);

            long pending = 0;
            long filed = 0;
            long overdue = 0;

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                pending = toLong(row[0]);
                filed = toLong(row[1]);
                overdue = toLong(row[2]);
            }

            return ItrStatsDto.builder()
                    .totalItrClients(totalItrClients)
                    .pending(pending)
                    .filed(filed)
                    .overdue(overdue)
                    .build();
        }

        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
            return ItrStatsDto.builder().totalItrClients(0).pending(0).filed(0).overdue(0).build();
        }

        List<ItrReturnEntity> returns = itrReturnRepository.findAllByOrganizationId(organizationId).stream()
                .filter(r -> accessibleClientIds.contains(r.getClientId()))
                .toList();

        long totalItrClients = returns.stream().map(ItrReturnEntity::getClientId).distinct().count();
        long filed = returns.stream().filter(r -> r.getStatus() == ItrStatus.FILED || r.getStatus() == ItrStatus.COMPLETED).count();
        long overdue = returns.stream().filter(r -> r.getStatus() != ItrStatus.FILED && r.getStatus() != ItrStatus.COMPLETED && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();
        long pending = returns.stream().filter(r -> r.getStatus() != ItrStatus.FILED && r.getStatus() != ItrStatus.COMPLETED && (r.getDueDate() == null || !r.getDueDate().isBefore(today))).count();

        return ItrStatsDto.builder()
                .totalItrClients(totalItrClients)
                .pending(pending)
                .filed(filed)
                .overdue(overdue)
                .build();
    }

    private BillingStatsDto getBillingStats(UUID organizationId) {
        List<Object[]> results = invoiceRepository.getBillingDashboardStatsSummary(organizationId);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;

        if (results != null && !results.isEmpty() && results.get(0) != null) {
            Object[] row = results.get(0);
            total = toBigDecimal(row[0]);
            paid = toBigDecimal(row[1]);
            outstanding = toBigDecimal(row[2]);
        }

        return BillingStatsDto.builder()
                .totalInvoiceAmount(total)
                .paidAmount(paid)
                .outstandingAmount(outstanding)
                .build();
    }

    private List<EmployeeWorkloadItemDto> getEmployeeWorkload(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        List<EmployeeEntity> employees;
        if (scope.isFirmAdmin()) {
            employees = employeeRepository.findAllByOrganizationIdAndStatus(organizationId, EmployeeStatus.ACTIVE);
            if (employees.isEmpty()) {
                employees = employeeRepository.findAllByOrganizationId(organizationId);
            }
        } else if (scope.isDepartmentManager() && StringUtils.hasText(scope.getDepartment())) {
            employees = employeeRepository.findAllByOrganizationId(organizationId).stream()
                    .filter(e -> scope.getDepartment().equalsIgnoreCase(e.getDepartment()))
                    .toList();
        } else {
            // Staff individual: only self
            if (scope.getEmployee() != null) {
                employees = List.of(scope.getEmployee());
            } else {
                employees = List.of();
            }
        }

        // Fetch task stats grouped by assignedTo in single aggregation query
        List<Object[]> taskGroupResults = taskRepository.getEmployeeTaskWorkloadStats(organizationId, today);
        Map<UUID, TaskAgg> taskStatsMap = new HashMap<>();

        if (taskGroupResults != null) {
            for (Object[] row : taskGroupResults) {
                if (row[0] instanceof UUID assigneeId) {
                    long assigned = toLong(row[1]);
                    long pending = toLong(row[2]);
                    long overdue = toLong(row[3]);
                    taskStatsMap.put(assigneeId, new TaskAgg(assigned, pending, overdue));
                }
            }
        }

        List<EmployeeWorkloadItemDto> workloadList = new ArrayList<>();
        for (EmployeeEntity emp : employees) {
            long totalAssigned = 0;
            long pending = 0;
            long overdue = 0;

            TaskAgg direct = taskStatsMap.get(emp.getId());
            if (direct != null) {
                totalAssigned += direct.assigned;
                pending += direct.pending;
                overdue += direct.overdue;
            }

            if (emp.getUserId() != null && !emp.getUserId().equals(emp.getId())) {
                TaskAgg userAssigned = taskStatsMap.get(emp.getUserId());
                if (userAssigned != null) {
                    totalAssigned += userAssigned.assigned;
                    pending += userAssigned.pending;
                    overdue += userAssigned.overdue;
                }
            }

            workloadList.add(EmployeeWorkloadItemDto.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getFullName())
                    .email(emp.getEmail())
                    .department(emp.getDepartment())
                    .designation(emp.getDesignation())
                    .assignedTasks(totalAssigned)
                    .pendingTasks(pending)
                    .overdueTasks(overdue)
                    .build());
        }

        return workloadList;
    }

    private static long toLong(Object val) {
        if (val == null) {
            return 0L;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        if (val instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private record TaskAgg(long assigned, long pending, long overdue) {}
}
