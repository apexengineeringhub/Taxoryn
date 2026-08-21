package com.taxoryn.module.dashboard.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GstProfileRepository gstProfileRepository;

    @Mock
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Mock
    private ItrProfileRepository itrProfileRepository;

    @Mock
    private ItrReturnRepository itrReturnRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("DASHBOARD_VIEW"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(PracticeSecurityScope.firmAdmin(userId));
        lenient().when(securityScopeEvaluator.hasBillingAccess(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully aggregate and return organization dashboard metrics")
    void testGetOrganizationDashboardMetrics() {
        UUID empId = UUID.randomUUID();
        UUID empUserId = UUID.randomUUID();

        // 1. Clients Mock
        when(clientRepository.getClientDashboardStats(tenantId))
                .thenReturn(Collections.singletonList(new Object[]{100L, 85L, 15L}));

        // 2. Employees Mock
        when(employeeRepository.getEmployeeDashboardStats(tenantId))
                .thenReturn(Collections.singletonList(new Object[]{10L, 8L}));

        // 3. Tasks Mock
        when(taskRepository.getTaskDashboardStats(eq(tenantId), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new Object[]{50L, 20L, 5L, 25L}));

        // 4. GST Mock
        when(gstProfileRepository.countDistinctClientsByOrganizationId(tenantId))
                .thenReturn(60L);
        when(gstReturnFilingRepository.getGstDashboardStats(eq(tenantId), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new Object[]{12L, 3L, 45L}));

        // 5. ITR Mock
        when(itrProfileRepository.countDistinctClientsByOrganizationId(tenantId))
                .thenReturn(70L);
        when(itrReturnRepository.getItrDashboardStats(eq(tenantId), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new Object[]{18L, 50L, 4L}));

        // 6. Billing Mock
        when(invoiceRepository.getBillingDashboardStatsSummary(tenantId))
                .thenReturn(Collections.singletonList(new Object[]{new BigDecimal("500000.00"), new BigDecimal("420000.00"), new BigDecimal("80000.00")}));

        // 7. Employee Workload Mock
        EmployeeEntity emp = EmployeeEntity.builder()
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan@taxpractice.com")
                .department("Taxation")
                .designation("Senior Associate")
                .userId(empUserId)
                .status(EmployeeStatus.ACTIVE)
                .build();
        emp.setId(empId);
        emp.setOrganizationId(tenantId);

        when(employeeRepository.findAllByOrganizationIdAndStatus(tenantId, EmployeeStatus.ACTIVE))
                .thenReturn(List.of(emp));

        when(taskRepository.getEmployeeTaskWorkloadStats(eq(tenantId), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new Object[]{empId, 15L, 10L, 2L}));

        OrganizationDashboardDto result = dashboardService.getOrganizationDashboard();

        assertNotNull(result);

        // Clients
        assertEquals(100L, result.getClients().getTotal());
        assertEquals(85L, result.getClients().getActive());
        assertEquals(15L, result.getClients().getInactive());

        // Employees
        assertEquals(10L, result.getEmployees().getTotal());
        assertEquals(8L, result.getEmployees().getActive());

        // Tasks
        assertEquals(50L, result.getTasks().getTotal());
        assertEquals(20L, result.getTasks().getPending());
        assertEquals(5L, result.getTasks().getOverdue());
        assertEquals(25L, result.getTasks().getCompleted());

        // GST
        assertEquals(60L, result.getGst().getTotalGstClients());
        assertEquals(12L, result.getGst().getReturnsDue());
        assertEquals(3L, result.getGst().getReturnsOverdue());
        assertEquals(45L, result.getGst().getReturnsFiled());

        // ITR
        assertEquals(70L, result.getItr().getTotalItrClients());
        assertEquals(18L, result.getItr().getPending());
        assertEquals(50L, result.getItr().getFiled());
        assertEquals(4L, result.getItr().getOverdue());

        // Billing
        assertEquals(new BigDecimal("500000.00"), result.getBilling().getTotalInvoiceAmount());
        assertEquals(new BigDecimal("420000.00"), result.getBilling().getPaidAmount());
        assertEquals(new BigDecimal("80000.00"), result.getBilling().getOutstandingAmount());

        // Workload
        assertEquals(1, result.getEmployeeWorkload().size());
        assertEquals(empId, result.getEmployeeWorkload().get(0).getEmployeeId());
        assertEquals("EMP-001", result.getEmployeeWorkload().get(0).getEmployeeCode());
        assertEquals("Rohan Deshmukh", result.getEmployeeWorkload().get(0).getEmployeeName());
        assertEquals(15L, result.getEmployeeWorkload().get(0).getAssignedTasks());
        assertEquals(10L, result.getEmployeeWorkload().get(0).getPendingTasks());
        assertEquals(2L, result.getEmployeeWorkload().get(0).getOverdueTasks());
    }

    @Test
    @DisplayName("Should handle empty/null database stats gracefully with zeroes")
    void testGetOrganizationDashboardEmptyData() {
        when(clientRepository.getClientDashboardStats(tenantId)).thenReturn(Collections.emptyList());
        when(employeeRepository.getEmployeeDashboardStats(tenantId)).thenReturn(Collections.emptyList());
        when(taskRepository.getTaskDashboardStats(eq(tenantId), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(gstProfileRepository.countDistinctClientsByOrganizationId(tenantId)).thenReturn(0L);
        when(gstReturnFilingRepository.getGstDashboardStats(eq(tenantId), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(itrProfileRepository.countDistinctClientsByOrganizationId(tenantId)).thenReturn(0L);
        when(itrReturnRepository.getItrDashboardStats(eq(tenantId), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(invoiceRepository.getBillingDashboardStatsSummary(tenantId)).thenReturn(Collections.emptyList());
        when(employeeRepository.findAllByOrganizationIdAndStatus(tenantId, EmployeeStatus.ACTIVE)).thenReturn(Collections.emptyList());
        when(employeeRepository.findAllByOrganizationId(tenantId)).thenReturn(Collections.emptyList());
        when(taskRepository.getEmployeeTaskWorkloadStats(eq(tenantId), any(LocalDate.class))).thenReturn(Collections.emptyList());

        OrganizationDashboardDto result = dashboardService.getOrganizationDashboard();

        assertNotNull(result);
        assertEquals(0L, result.getClients().getTotal());
        assertEquals(0L, result.getEmployees().getTotal());
        assertEquals(0L, result.getTasks().getTotal());
        assertEquals(0L, result.getGst().getTotalGstClients());
        assertEquals(0L, result.getItr().getTotalItrClients());
        assertEquals(BigDecimal.ZERO, result.getBilling().getTotalInvoiceAmount());
        assertEquals(0, result.getEmployeeWorkload().size());
    }
}
