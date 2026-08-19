package com.taxoryn.module.dashboard.service;

import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.billing.repository.InvoiceRepository;
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
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    @Transactional(readOnly = true)
    public OrganizationDashboardDto getOrganizationDashboard() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();

        log.debug("Generating organization dashboard summary for tenant {}", organizationId);

        // 1. Clients Statistics Aggregation
        ClientStatsDto clientStats = getClientStats(organizationId);

        // 2. Employees Statistics Aggregation
        EmployeeStatsDto employeeStats = getEmployeeStats(organizationId);

        // 3. Tasks Statistics Aggregation
        TaskStatsDto taskStats = getTaskStats(organizationId, today);

        // 4. GST Compliance Statistics Aggregation
        GstStatsDto gstStats = getGstStats(organizationId, today);

        // 5. ITR Compliance Statistics Aggregation
        ItrStatsDto itrStats = getItrStats(organizationId, today);

        // 6. Billing and Financial Summary Aggregation
        BillingStatsDto billingStats = getBillingStats(organizationId);

        // 7. Employee Workload Distribution
        List<EmployeeWorkloadItemDto> employeeWorkload = getEmployeeWorkload(organizationId, today);

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

    private ClientStatsDto getClientStats(UUID organizationId) {
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

    private EmployeeStatsDto getEmployeeStats(UUID organizationId) {
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

    private TaskStatsDto getTaskStats(UUID organizationId, LocalDate today) {
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

    private GstStatsDto getGstStats(UUID organizationId, LocalDate today) {
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

    private ItrStatsDto getItrStats(UUID organizationId, LocalDate today) {
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

    private List<EmployeeWorkloadItemDto> getEmployeeWorkload(UUID organizationId, LocalDate today) {
        List<EmployeeEntity> employees = employeeRepository.findAllByOrganizationIdAndStatus(organizationId, EmployeeStatus.ACTIVE);
        if (employees.isEmpty()) {
            employees = employeeRepository.findAllByOrganizationId(organizationId);
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
