package com.taxoryn.module.compliance.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.dto.ComplianceCalendarSummaryDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.entity.ComplianceCycleTemplateEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceRecurrenceType;
import com.taxoryn.module.compliance.repository.ComplianceCycleTemplateRepository;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceReminderRuleRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.workflow.entity.ClientServicePeriodEntity;
import com.taxoryn.module.workflow.repository.ClientServicePeriodRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceCalendarServiceTest {

    @Mock
    private ComplianceObligationRepository obligationRepository;
    @Mock
    private ComplianceCycleTemplateRepository cycleTemplateRepository;
    @Mock
    private ComplianceReminderRuleRepository reminderRuleRepository;
    @Mock
    private ComplianceRuleRepository ruleRepository;
    @Mock
    private ComplianceRuleService ruleService;
    @Mock
    private DueDateCalculationService dueDateCalculationService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ClientServiceRepository clientServiceRepository;
    @Mock
    private ClientServicePeriodRepository clientServicePeriodRepository;
    @Mock
    private ClientServiceWorkflowRepository clientServiceWorkflowRepository;
    @Mock
    private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @Mock
    private ModuleConfigurationService moduleConfigurationService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ComplianceServiceImpl complianceService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private final UUID organizationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentOrganizationId).thenReturn(organizationId);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("Should create custom compliance obligation and calculate internal target date")
    void testCreateObligation() {
        UUID clientId = UUID.randomUUID();
        LocalDate statutoryDue = LocalDate.of(2026, 8, 20);
        LocalDate internalTarget = LocalDate.of(2026, 8, 17);

        ClientEntity client = ClientEntity.builder()
                .displayName("Alpha Corp")
                .pan("AAACA1234D")
                .build();
        client.setId(clientId);
        client.setOrganizationId(organizationId);

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(client));
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(PracticeSecurityScope.builder().organizationId(organizationId).userId(userId).isFirmAdmin(true).build());
        when(securityScopeEvaluator.getAccessibleClientIds(any())).thenReturn(Set.of(clientId));
        when(dueDateCalculationService.calculateInternalTargetDate(statutoryDue, 3)).thenReturn(internalTarget);

        ComplianceObligationEntity savedEntity = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B July 2026")
                .periodLabel("July 2026")
                .statutoryDueDate(statutoryDue)
                .internalTargetDate(internalTarget)
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .build();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setOrganizationId(organizationId);

        when(obligationRepository.save(any(ComplianceObligationEntity.class))).thenReturn(savedEntity);
        when(obligationRepository.findByIdAndOrganizationId(savedEntity.getId(), organizationId)).thenReturn(Optional.of(savedEntity));

        CreateComplianceObligationRequest request = CreateComplianceObligationRequest.builder()
                .clientId(clientId)
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B July 2026")
                .periodLabel("July 2026")
                .statutoryDueDate(statutoryDue)
                .priority(TaskPriority.HIGH)
                .build();

        ComplianceObligationDto dto = complianceService.createObligation(request);

        assertNotNull(dto);
        assertEquals(savedEntity.getId(), dto.getId());
        assertEquals("GSTR-3B July 2026", dto.getTitle());
        assertEquals(statutoryDue, dto.getStatutoryDueDate());
        assertEquals(internalTarget, dto.getInternalTargetDate());
        verify(auditService).logEvent(eq(organizationId), eq(userId), eq("COMPLIANCE_OBLIGATION_CREATED"), eq("COMPLIANCE_OBLIGATION"), any(), any(), any());
    }

    @Test
    @DisplayName("Should generate obligation from ClientServicePeriod idempotently")
    void testGenerateObligationForServicePeriod() {
        UUID serviceId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .build();
        service.setId(serviceId);
        service.setOrganizationId(organizationId);

        ClientServicePeriodEntity period = ClientServicePeriodEntity.builder()
                .clientId(clientId)
                .clientServiceId(serviceId)
                .periodLabel("August 2026")
                .dueDate(LocalDate.of(2026, 9, 20))
                .build();
        period.setId(periodId);
        period.setOrganizationId(organizationId);

        when(clientServiceRepository.findByIdAndOrganizationId(serviceId, organizationId)).thenReturn(Optional.of(service));
        when(clientServicePeriodRepository.findByIdAndOrganizationId(periodId, organizationId)).thenReturn(Optional.of(period));
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(PracticeSecurityScope.builder().organizationId(organizationId).userId(userId).isFirmAdmin(true).build());
        when(securityScopeEvaluator.getAccessibleClientIds(any())).thenReturn(Set.of(clientId));

        // When already exists, return existing without saving
        ComplianceObligationEntity existing = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .clientServiceId(serviceId)
                .servicePeriodId(periodId)
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("August 2026 - GST Compliance & Returns")
                .periodLabel("August 2026")
                .statutoryDueDate(LocalDate.of(2026, 9, 20))
                .status(ComplianceObligationStatus.UPCOMING)
                .build();
        existing.setId(UUID.randomUUID());
        existing.setOrganizationId(organizationId);

        when(obligationRepository.findByOrganizationIdAndClientServiceIdAndServicePeriodIdAndObligationType(
                organizationId, serviceId, periodId, ComplianceObligationType.GST_RETURN
        )).thenReturn(Optional.of(existing));
        when(obligationRepository.findByIdAndOrganizationId(existing.getId(), organizationId)).thenReturn(Optional.of(existing));

        ComplianceObligationDto dto = complianceService.generateObligationForServicePeriod(serviceId, periodId);

        assertNotNull(dto);
        assertEquals(existing.getId(), dto.getId());
        assertEquals("August 2026 - GST Compliance & Returns", dto.getTitle());
    }

    @Test
    @DisplayName("Should evaluate DueDateCalculationService correctly for monthly and annual cycles")
    void testDueDateCalculation() {
        DueDateCalculationService calcService = new DueDateCalculationServiceImpl();

        // 1. Monthly GST: Period ends 2026-07-31 -> Due 2026-08-20
        ComplianceCycleTemplateEntity gstTemplate = ComplianceCycleTemplateEntity.builder()
                .recurrenceType(ComplianceRecurrenceType.MONTHLY)
                .defaultDueDay(20)
                .defaultDueMonthOffset(1)
                .build();

        LocalDate gstDue = calcService.calculateStatutoryDueDate(
                gstTemplate,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "2026-27",
                "2027-28"
        );
        assertEquals(LocalDate.of(2026, 8, 20), gstDue);

        // Internal target 3 days before: 2026-08-17
        LocalDate internalTarget = calcService.calculateInternalTargetDate(gstDue, 3);
        assertEquals(LocalDate.of(2026, 8, 17), internalTarget);

        // 2. Annual ITR Non-Audit: AY 2026-27 -> Due 2026-07-31
        ComplianceCycleTemplateEntity itrTemplate = ComplianceCycleTemplateEntity.builder()
                .recurrenceType(ComplianceRecurrenceType.ANNUALLY)
                .defaultDueDay(31)
                .fixedDueMonth(7)
                .build();

        LocalDate itrDue = calcService.calculateStatutoryDueDate(
                itrTemplate,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 3, 31),
                "2025-26",
                "2026-27"
        );
        assertEquals(LocalDate.of(2026, 7, 31), itrDue);
    }
}
