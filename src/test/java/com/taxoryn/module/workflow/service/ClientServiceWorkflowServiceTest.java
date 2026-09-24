package com.taxoryn.module.workflow.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
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
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowStepDto;
import com.taxoryn.module.workflow.dto.CreateServicePeriodRequest;
import com.taxoryn.module.workflow.dto.GenerateWorkflowRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowPriorityRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStatusRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStepStatusRequest;
import com.taxoryn.module.workflow.entity.ClientServicePeriodEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowStepEntity;
import com.taxoryn.module.workflow.entity.ServiceWorkflowStepTemplateEntity;
import com.taxoryn.module.workflow.entity.ServiceWorkflowTemplateEntity;
import com.taxoryn.module.workflow.model.ServicePeriodStatus;
import com.taxoryn.module.workflow.model.ServicePeriodType;
import com.taxoryn.module.workflow.model.ServiceWorkType;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import com.taxoryn.module.workflow.model.StepStatus;
import com.taxoryn.module.workflow.repository.ClientServicePeriodRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowStepRepository;
import com.taxoryn.module.workflow.repository.ServiceWorkflowStepTemplateRepository;
import com.taxoryn.module.workflow.repository.ServiceWorkflowTemplateRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceWorkflowServiceTest {

    @Mock private ClientServiceWorkflowRepository workflowRepository;
    @Mock private ClientServiceWorkflowStepRepository stepRepository;
    @Mock private ClientServicePeriodRepository periodRepository;
    @Mock private ServiceWorkflowTemplateRepository templateRepository;
    @Mock private ServiceWorkflowStepTemplateRepository stepTemplateRepository;
    @Mock private ClientServiceRepository clientServiceRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ModuleConfigurationService moduleConfigurationService;
    @Mock private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @Mock private AuditService auditService;

    @InjectMocks
    private ClientServiceWorkflowServiceImpl workflowService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    private UUID organizationId;
    private UUID userId;
    private UUID clientId;
    private UUID clientServiceId;
    private UUID periodId;
    private UUID workflowId;
    private UUID templateId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        clientServiceId = UUID.randomUUID();
        periodId = UUID.randomUUID();
        workflowId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        mockedSecurityUtils.when(SecurityUtils::getCurrentOrganizationId).thenReturn(organizationId);
        mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

        OrganizationModuleDto gstMod = OrganizationModuleDto.builder().moduleCode(ProductModuleCode.GST).enabled(true).entitled(true).build();
        OrganizationModuleDto itrMod = OrganizationModuleDto.builder().moduleCode(ProductModuleCode.ITR).enabled(true).entitled(true).build();
        OrganizationModuleDto tdsMod = OrganizationModuleDto.builder().moduleCode(ProductModuleCode.TDS).enabled(true).entitled(true).build();
        OrganizationModuleDto noticeMod = OrganizationModuleDto.builder().moduleCode(ProductModuleCode.TAX_NOTICES).enabled(true).entitled(true).build();

        org.mockito.Mockito.lenient().when(moduleConfigurationService.getOrganizationModules(organizationId))
                .thenReturn(List.of(gstMod, itrMod, tdsMod, noticeMod));

        org.mockito.Mockito.lenient().when(securityScopeEvaluator.evaluateCurrentScope())
                .thenReturn(PracticeSecurityScope.builder().isFirmAdmin(true).build());
        org.mockito.Mockito.lenient().when(securityScopeEvaluator.getAccessibleClientIds(any()))
                .thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    @DisplayName("Should generate workflow from template successfully")
    void testGenerateWorkflowSuccess() {
        ClientEntity client = ClientEntity.builder().displayName("Zenith Tech Pvt Ltd").build();
        client.setId(clientId);

        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        service.setId(clientServiceId);

        ClientServicePeriodEntity period = ClientServicePeriodEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .periodLabel("September 2026")
                .periodType(ServicePeriodType.MONTHLY)
                .dueDate(LocalDate.of(2026, 10, 20))
                .status(ServicePeriodStatus.ACTIVE)
                .build();
        period.setId(periodId);

        ServiceWorkflowTemplateEntity template = ServiceWorkflowTemplateEntity.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .name("Standard GST Monthly Compliance Workflow")
                .isSystemDefault(true)
                .active(true)
                .build();
        template.setId(templateId);

        ServiceWorkflowStepTemplateEntity stepTemplate1 = ServiceWorkflowStepTemplateEntity.builder()
                .workflowTemplateId(templateId)
                .sequence(1)
                .workType(ServiceWorkType.DATA_COLLECTION)
                .name("Invoices Collection")
                .mandatory(true)
                .requiresClientInput(true)
                .active(true)
                .build();

        ServiceWorkflowStepTemplateEntity stepTemplate2 = ServiceWorkflowStepTemplateEntity.builder()
                .workflowTemplateId(templateId)
                .sequence(2)
                .workType(ServiceWorkType.PREPARATION)
                .name("GSTR-2B Reconciliation")
                .mandatory(true)
                .active(true)
                .build();

        when(clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)).thenReturn(Optional.of(service));
        when(periodRepository.findByIdAndOrganizationId(periodId, organizationId)).thenReturn(Optional.of(period));
        when(workflowRepository.findByOrganizationIdAndClientServiceIdAndPeriodId(organizationId, clientServiceId, periodId)).thenReturn(Optional.empty());
        when(templateRepository.findBestTemplate(organizationId, ClientServiceType.GST_COMPLIANCE)).thenReturn(Optional.of(template));
        when(stepTemplateRepository.findAllByWorkflowTemplateIdAndActiveTrueOrderBySequenceAsc(templateId)).thenReturn(List.of(stepTemplate1, stepTemplate2));

        ClientServiceWorkflowEntity savedWorkflow = ClientServiceWorkflowEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .periodId(periodId)
                .templateId(templateId)
                .title("GST Compliance - September 2026")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .currentStepSequence(1)
                .totalSteps(2)
                .completedSteps(0)
                .dueDate(LocalDate.of(2026, 10, 20))
                .build();
        savedWorkflow.setId(workflowId);
        savedWorkflow.setOrganizationId(organizationId);

        when(workflowRepository.save(any(ClientServiceWorkflowEntity.class))).thenReturn(savedWorkflow);
        when(workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)).thenReturn(Optional.of(savedWorkflow));
        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(client));

        GenerateWorkflowRequest request = GenerateWorkflowRequest.builder()
                .clientServiceId(clientServiceId)
                .periodId(periodId)
                .priority(TaskPriority.HIGH)
                .build();

        ClientServiceWorkflowDto result = workflowService.generateWorkflow(request);

        assertNotNull(result);
        assertEquals("GST Compliance - September 2026", result.getTitle());
        assertEquals(ServiceWorkflowStatus.IN_PROGRESS, result.getStatus());
        assertEquals(TaskPriority.HIGH, result.getPriority());
        verify(auditService).logEvent(eq(organizationId), eq(userId), eq("SERVICE_WORKFLOW_CREATED"), eq("CLIENT_SERVICE_WORKFLOW"), eq(workflowId.toString()), any(), any());
    }

    @Test
    @DisplayName("Should prevent duplicate workflow generation for same period")
    void testPreventDuplicateWorkflowGeneration() {
        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        service.setId(clientServiceId);

        ClientServicePeriodEntity period = ClientServicePeriodEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .periodLabel("September 2026")
                .build();
        period.setId(periodId);

        ClientServiceWorkflowEntity existingWorkflow = ClientServiceWorkflowEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .periodId(periodId)
                .title("Existing Workflow")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .build();
        existingWorkflow.setId(workflowId);
        existingWorkflow.setOrganizationId(organizationId);

        when(clientServiceRepository.findByIdAndOrganizationId(clientServiceId, organizationId)).thenReturn(Optional.of(service));
        when(periodRepository.findByIdAndOrganizationId(periodId, organizationId)).thenReturn(Optional.of(period));
        when(workflowRepository.findByOrganizationIdAndClientServiceIdAndPeriodId(organizationId, clientServiceId, periodId)).thenReturn(Optional.of(existingWorkflow));
        when(workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)).thenReturn(Optional.of(existingWorkflow));

        GenerateWorkflowRequest request = GenerateWorkflowRequest.builder()
                .clientServiceId(clientServiceId)
                .periodId(periodId)
                .build();

        ClientServiceWorkflowDto result = workflowService.generateWorkflow(request);

        assertNotNull(result);
        assertEquals(workflowId, result.getId());
    }

    @Test
    @DisplayName("Should advance step status and mark waiting for client")
    void testStepProgressionAndWaitingForClient() {
        UUID step1Id = UUID.randomUUID();
        UUID step2Id = UUID.randomUUID();

        ClientServiceWorkflowEntity workflow = ClientServiceWorkflowEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .periodId(periodId)
                .title("ITR Filing AY 2026-27")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .currentStepSequence(1)
                .totalSteps(2)
                .completedSteps(0)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(organizationId);

        ClientServiceWorkflowStepEntity step1 = ClientServiceWorkflowStepEntity.builder()
                .workflowId(workflowId)
                .sequence(1)
                .workType(ServiceWorkType.DOCUMENT_COLLECTION)
                .name("Document Gathering")
                .status(StepStatus.IN_PROGRESS)
                .mandatory(true)
                .requiresClientInput(true)
                .build();
        step1.setId(step1Id);
        step1.setOrganizationId(organizationId);

        ClientServiceWorkflowStepEntity step2 = ClientServiceWorkflowStepEntity.builder()
                .workflowId(workflowId)
                .sequence(2)
                .workType(ServiceWorkType.COMPUTATION)
                .name("Computation of Total Income")
                .status(StepStatus.PENDING)
                .mandatory(true)
                .build();
        step2.setId(step2Id);
        step2.setOrganizationId(organizationId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)).thenReturn(Optional.of(workflow));
        when(stepRepository.findByIdAndOrganizationId(step1Id, organizationId)).thenReturn(Optional.of(step1));
        when(stepRepository.save(any(ClientServiceWorkflowStepEntity.class))).thenReturn(step1);
        when(stepRepository.findAllByOrganizationIdAndWorkflowIdOrderBySequenceAsc(organizationId, workflowId)).thenReturn(List.of(step1, step2));
        when(employeeRepository.findAllByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        // 1. Mark Step 1 Waiting for Client
        UpdateWorkflowStepStatusRequest waitReq = UpdateWorkflowStepStatusRequest.builder()
                .status(StepStatus.WAITING_FOR_CLIENT)
                .clientActionSummary("Pending AIS confirmation from client")
                .build();

        ClientServiceWorkflowStepDto stepDto = workflowService.updateStepStatus(workflowId, step1Id, waitReq);
        assertEquals(StepStatus.WAITING_FOR_CLIENT, stepDto.getStatus());
        assertTrue(workflow.isWaitingForClient());
        assertEquals(ServiceWorkflowStatus.WAITING_FOR_CLIENT, workflow.getStatus());

        // 2. Complete Step 1
        UpdateWorkflowStepStatusRequest completeReq = UpdateWorkflowStepStatusRequest.builder()
                .status(StepStatus.COMPLETED)
                .build();
        step1.setStatus(StepStatus.COMPLETED);

        workflowService.updateStepStatus(workflowId, step1Id, completeReq);
        assertEquals(1, workflow.getCompletedSteps());
        assertEquals(2, workflow.getCurrentStepSequence());
        assertFalse(workflow.isWaitingForClient());
    }

    @Test
    @DisplayName("Should enforce client portfolio scope")
    void testClientPortfolioScopeEnforcement() {
        ClientServiceWorkflowEntity workflow = ClientServiceWorkflowEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .periodId(periodId)
                .title("Restricted Workflow")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(organizationId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)).thenReturn(Optional.of(workflow));

        PracticeSecurityScope restrictedScope = PracticeSecurityScope.builder()
                .isFirmAdmin(false)
                .employeeId(UUID.randomUUID())
                .userId(userId)
                .build();
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(restrictedScope);
        when(securityScopeEvaluator.getAccessibleClientIds(restrictedScope)).thenReturn(Set.of(UUID.randomUUID())); // Other client

        assertThrows(ForbiddenException.class, () -> workflowService.getWorkflowById(workflowId));
    }
}
