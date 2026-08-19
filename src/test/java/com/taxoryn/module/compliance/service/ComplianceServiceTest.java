package com.taxoryn.module.compliance.service;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceFrequency;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.mapper.ComplianceMapper;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private ComplianceObligationRepository obligationRepository;

    @Mock
    private ComplianceRuleRepository ruleRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ComplianceMapper complianceMapper;

    @Spy
    private ComplianceRuleServiceImpl ruleService = new ComplianceRuleServiceImpl(null, null);

    @InjectMocks
    private ComplianceServiceImpl complianceService;

    private UUID tenantId;
    private UUID clientId;
    private UUID employeeId;
    private UUID obligationId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        obligationId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("TASK_CREATE", "TASK_VIEW", "TASK_UPDATE", "GST_VIEW", "ITR_VIEW"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Dynamic rule evaluation calculates correct due dates without hardcoding")
    void testCalculateDueDateRules() {
        // GSTR-1: Monthly, due 11th of succeeding month
        ComplianceRuleEntity gstr1 = ComplianceRuleEntity.builder()
                .ruleCode("GST_GSTR1_MONTHLY")
                .frequency(ComplianceFrequency.MONTHLY)
                .dueDay(11)
                .dueMonthOffset(1)
                .build();
        LocalDate gstr1Due = ruleService.calculateDueDate(gstr1, "2026-08");
        assertEquals(LocalDate.of(2026, 9, 11), gstr1Due);

        // GSTR-3B: Monthly, due 20th of succeeding month
        ComplianceRuleEntity gstr3b = ComplianceRuleEntity.builder()
                .ruleCode("GST_GSTR3B_MONTHLY")
                .frequency(ComplianceFrequency.MONTHLY)
                .dueDay(20)
                .dueMonthOffset(1)
                .build();
        LocalDate gstr3bDue = ruleService.calculateDueDate(gstr3b, "2026-08");
        assertEquals(LocalDate.of(2026, 9, 20), gstr3bDue);

        // TDS Challan 281: Monthly, due 7th of succeeding month
        ComplianceRuleEntity tdsChallan = ComplianceRuleEntity.builder()
                .ruleCode("TDS_CHALLAN_281")
                .frequency(ComplianceFrequency.MONTHLY)
                .dueDay(7)
                .dueMonthOffset(1)
                .build();
        LocalDate tdsDue = ruleService.calculateDueDate(tdsChallan, "2026-08");
        assertEquals(LocalDate.of(2026, 9, 7), tdsDue);

        // ITR Non-Audit: Annual, July 31
        ComplianceRuleEntity itrNonAudit = ComplianceRuleEntity.builder()
                .ruleCode("ITR_NON_AUDIT")
                .frequency(ComplianceFrequency.ANNUALLY)
                .dueDay(31)
                .fixedDueMonth(7)
                .build();
        LocalDate itrDue = ruleService.calculateDueDate(itrNonAudit, "2026-27");
        assertEquals(LocalDate.of(2026, 7, 31), itrDue);
    }

    @Test
    @DisplayName("Create custom compliance obligation")
    void testCreateObligationSuccess() {
        CreateComplianceObligationRequest request = CreateComplianceObligationRequest.builder()
                .clientId(clientId)
                .title("TDS Return Form 26Q Q2")
                .complianceType(ComplianceType.TDS)
                .period("2026-Q2")
                .dueDate(LocalDate.of(2026, 10, 31))
                .priority(CompliancePriority.HIGH)
                .build();

        ClientEntity client = ClientEntity.builder().displayName("ABC Traders").build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));

        ComplianceObligationEntity saved = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .title("TDS Return Form 26Q Q2")
                .complianceType(ComplianceType.TDS)
                .period("2026-Q2")
                .dueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceStatus.PENDING)
                .priority(CompliancePriority.HIGH)
                .build();
        saved.setId(obligationId);
        saved.setOrganizationId(tenantId);

        when(obligationRepository.save(any(ComplianceObligationEntity.class))).thenReturn(saved);
        when(complianceMapper.toObligationDto(saved)).thenReturn(ComplianceObligationDto.builder()
                .id(obligationId)
                .title("TDS Return Form 26Q Q2")
                .complianceType(ComplianceType.TDS)
                .status(ComplianceStatus.PENDING)
                .dueDate(LocalDate.of(2026, 10, 31))
                .build());

        ComplianceObligationDto result = complianceService.createObligation(request);

        assertNotNull(result);
        assertEquals("TDS Return Form 26Q Q2", result.getTitle());
        assertEquals(ComplianceStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("Convert obligation to actionable Task")
    void testCreateTaskForObligation() {
        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .title("GSTR-3B Filing for August 2026")
                .complianceType(ComplianceType.GST)
                .period("2026-08")
                .dueDate(LocalDate.of(2026, 9, 20))
                .status(ComplianceStatus.PENDING)
                .priority(CompliancePriority.HIGH)
                .assignedEmployeeId(employeeId)
                .build();
        obligation.setId(obligationId);
        obligation.setOrganizationId(tenantId);

        when(obligationRepository.findByIdAndOrganizationId(obligationId, tenantId)).thenReturn(Optional.of(obligation));

        TaskEntity savedTask = TaskEntity.builder()
                .title("GSTR-3B Filing for August 2026")
                .status(TaskEntity.TaskStatus.TODO)
                .build();
        UUID taskId = UUID.randomUUID();
        savedTask.setId(taskId);
        savedTask.setOrganizationId(tenantId);

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(savedTask);
        when(obligationRepository.save(obligation)).thenReturn(obligation);
        when(complianceMapper.toObligationDto(obligation)).thenReturn(ComplianceObligationDto.builder()
                .id(obligationId)
                .taskId(taskId)
                .status(ComplianceStatus.IN_PROGRESS)
                .build());

        ComplianceObligationDto result = complianceService.createTaskForObligation(obligationId);

        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(ComplianceStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    @DisplayName("Get Executive Dashboard Statistics")
    void testGetDashboardStats() {
        LocalDate today = LocalDate.now();

        ComplianceObligationEntity todayObligation = ComplianceObligationEntity.builder()
                .complianceType(ComplianceType.GST)
                .dueDate(today)
                .status(ComplianceStatus.PENDING)
                .build();
        todayObligation.setId(UUID.randomUUID());
        todayObligation.setOrganizationId(tenantId);

        ComplianceObligationEntity overdueObligation = ComplianceObligationEntity.builder()
                .complianceType(ComplianceType.ITR)
                .dueDate(today.minusDays(5))
                .status(ComplianceStatus.OVERDUE)
                .build();
        overdueObligation.setId(UUID.randomUUID());
        overdueObligation.setOrganizationId(tenantId);

        ComplianceObligationEntity completedObligation = ComplianceObligationEntity.builder()
                .complianceType(ComplianceType.TDS)
                .dueDate(today.minusDays(2))
                .status(ComplianceStatus.COMPLETED)
                .build();
        completedObligation.setId(UUID.randomUUID());
        completedObligation.setOrganizationId(tenantId);

        when(obligationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(todayObligation, overdueObligation, completedObligation));

        when(complianceMapper.toObligationDto(any(ComplianceObligationEntity.class)))
                .thenReturn(ComplianceObligationDto.builder().build());

        ComplianceDashboardStatsDto stats = complianceService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(1, stats.getDueTodayCount());
        assertEquals(1, stats.getOverdueCount());
        assertEquals(1, stats.getCompletedCount());
        assertEquals(2, stats.getTotalActiveCount());
        assertTrue(stats.getCountByType().containsKey("GST"));
        assertTrue(stats.getCountByType().containsKey("ITR"));
    }
}
