package com.taxoryn.module.worktemplate;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.service.service.ServiceCatalogService;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.EnableEngagementTemplateRequest;
import com.taxoryn.module.worktemplate.dto.EngagementWorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.GenerateWorkInstanceRequest;
import com.taxoryn.module.worktemplate.dto.ReorderTasksRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkInstanceStatusRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateStatusRequest;
import com.taxoryn.module.worktemplate.dto.WorkInstanceDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateTaskDto;
import com.taxoryn.module.worktemplate.entity.EngagementWorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateTaskEntity;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import com.taxoryn.module.worktemplate.repository.EngagementWorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateTaskRepository;
import com.taxoryn.module.worktemplate.service.EngagementWorkServiceImpl;
import com.taxoryn.module.worktemplate.service.WorkTemplateServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WorkTemplateBusinessLogicTest {

    @Mock
    private WorkTemplateRepository workTemplateRepository;
    @Mock
    private WorkTemplateTaskRepository workTemplateTaskRepository;
    @Mock
    private EngagementWorkTemplateRepository engagementWorkTemplateRepository;
    @Mock
    private WorkInstanceRepository workInstanceRepository;
    @Mock
    private EngagementRepository engagementRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceCatalogService serviceCatalogService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private WorkTemplateServiceImpl workTemplateService;

    @InjectMocks
    private EngagementWorkServiceImpl engagementWorkService;

    private UUID orgId;
    private UUID gstServiceId;
    private UUID tdsServiceId;
    private ServiceEntity gstService;
    private ServiceEntity tdsService;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        TenantContext.setTenantId(orgId);

        gstServiceId = UUID.randomUUID();
        gstService = ServiceEntity.builder()
                .serviceCode("GST_COMPLIANCE")
                .serviceName("GST Compliance & Returns")
                .category(ServiceCategory.GST)
                .status(ServiceStatus.ACTIVE)
                .build();
        gstService.setId(gstServiceId);

        tdsServiceId = UUID.randomUUID();
        tdsService = ServiceEntity.builder()
                .serviceCode("TDS_COMPLIANCE")
                .serviceName("TDS Compliance")
                .category(ServiceCategory.TDS)
                .status(ServiceStatus.ACTIVE)
                .build();
        tdsService.setId(tdsServiceId);

        when(serviceRepository.findById(gstServiceId)).thenReturn(Optional.of(gstService));
        when(serviceRepository.findById(tdsServiceId)).thenReturn(Optional.of(tdsService));
        when(serviceRepository.findAccessibleServiceById(eq(gstServiceId), any())).thenReturn(Optional.of(gstService));
        when(serviceRepository.findAccessibleServiceById(eq(tdsServiceId), any())).thenReturn(Optional.of(tdsService));
        when(serviceCatalogService.isServiceAvailableForPractice(eq(orgId), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create WorkTemplate with initial tasks and verify code generation")
    void testCreateWorkTemplate() {
        when(workTemplateRepository.countByOrganizationId(orgId)).thenReturn(0L);
        when(workTemplateRepository.existsByOrganizationIdAndTemplateCodeIgnoreCase(eq(orgId), any())).thenReturn(false);
        when(workTemplateRepository.save(any(WorkTemplateEntity.class))).thenAnswer(invocation -> {
            WorkTemplateEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateWorkTemplateRequest request = CreateWorkTemplateRequest.builder()
                .serviceId(gstServiceId)
                .name("Monthly GST Filing")
                .description("GSTR-1, 3B workflow")
                .category(ServiceCategory.GST)
                .recurrenceType(RecurrenceType.MONTHLY)
                .recurrenceInterval(1)
                .initialTasks(List.of(
                        CreateWorkTemplateTaskRequest.builder()
                                .name("Collect Invoices")
                                .sequenceOrder(1)
                                .relativeDueDays(5)
                                .mandatory(true)
                                .build(),
                        CreateWorkTemplateTaskRequest.builder()
                                .name("Prepare Return")
                                .sequenceOrder(2)
                                .relativeDueDays(12)
                                .mandatory(true)
                                .build()
                ))
                .build();

        WorkTemplateDto result = workTemplateService.createTemplate(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Monthly GST Filing");
        assertThat(result.getTemplateCode()).isEqualTo("GST_TPL_0001");
        assertThat(result.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
        verify(auditService).logEvent(eq("WORK_TEMPLATE_CREATED"), eq("WORK_TEMPLATE"), any(), any(), any());
    }

    @Test
    @DisplayName("Activate, Deactivate and Archive WorkTemplate lifecycle")
    void testTemplateStatusLifecycle() {
        UUID templateId = UUID.randomUUID();
        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .organizationId(orgId)
                .serviceId(gstServiceId)
                .templateCode("GST_TPL_0001")
                .name("Monthly GST")
                .status(WorkTemplateStatus.DRAFT)
                .build();
        template.setId(templateId);

        when(workTemplateRepository.findByIdAndOrganizationId(templateId, orgId)).thenReturn(Optional.of(template));
        when(workTemplateRepository.save(any(WorkTemplateEntity.class))).thenAnswer(i -> i.getArgument(0));

        // DRAFT -> ACTIVE
        WorkTemplateDto activated = workTemplateService.updateTemplateStatus(templateId,
                UpdateWorkTemplateStatusRequest.builder().status(WorkTemplateStatus.ACTIVE).build());
        assertThat(activated.getStatus()).isEqualTo(WorkTemplateStatus.ACTIVE);

        // ACTIVE -> INACTIVE
        WorkTemplateDto deactivated = workTemplateService.updateTemplateStatus(templateId,
                UpdateWorkTemplateStatusRequest.builder().status(WorkTemplateStatus.INACTIVE).build());
        assertThat(deactivated.getStatus()).isEqualTo(WorkTemplateStatus.INACTIVE);

        // INACTIVE -> ARCHIVED
        WorkTemplateDto archived = workTemplateService.updateTemplateStatus(templateId,
                UpdateWorkTemplateStatusRequest.builder().status(WorkTemplateStatus.ARCHIVED).build());
        assertThat(archived.getStatus()).isEqualTo(WorkTemplateStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Add and reorder template tasks")
    void testTemplateTaskManagement() {
        UUID templateId = UUID.randomUUID();
        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .organizationId(orgId)
                .serviceId(gstServiceId)
                .name("GST Workflow")
                .build();
        template.setId(templateId);

        when(workTemplateRepository.findByIdAndOrganizationId(templateId, orgId)).thenReturn(Optional.of(template));
        when(workTemplateTaskRepository.findMaxSequenceOrderByTemplateId(templateId)).thenReturn(0);

        UUID task1Id = UUID.randomUUID();
        UUID task2Id = UUID.randomUUID();
        WorkTemplateTaskEntity task1 = WorkTemplateTaskEntity.builder()
                .templateId(templateId).name("Task 1").sequenceOrder(1).build();
        task1.setId(task1Id);
        WorkTemplateTaskEntity task2 = WorkTemplateTaskEntity.builder()
                .templateId(templateId).name("Task 2").sequenceOrder(2).build();
        task2.setId(task2Id);

        when(workTemplateTaskRepository.save(any(WorkTemplateTaskEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(templateId))
                .thenReturn(List.of(task1, task2));

        // Reorder tasks (2 then 1)
        List<WorkTemplateTaskDto> reordered = workTemplateService.reorderTemplateTasks(
                templateId, ReorderTasksRequest.builder().orderedTaskIds(List.of(task2Id, task1Id)).build()
        );

        assertThat(reordered).hasSize(2);
        verify(auditService).logEvent(eq("WORK_TEMPLATE_TASKS_REORDERED"), eq("WORK_TEMPLATE"), eq(templateId.toString()), any(), any());
    }

    @Test
    @DisplayName("Enable template for engagement with matching service succeeds")
    void testEnableTemplateMatchingService() {
        UUID engagementId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        EngagementEntity engagement = EngagementEntity.builder()
                .serviceId(gstServiceId)
                .name("ABC Ltd GST FY 2026-27")
                .status(EngagementStatus.ACTIVE)
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(orgId);

        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .serviceId(gstServiceId)
                .templateCode("GST_MONTHLY")
                .name("GST Monthly Compliance")
                .status(WorkTemplateStatus.ACTIVE)
                .recurrenceType(RecurrenceType.MONTHLY)
                .build();
        template.setId(templateId);

        when(engagementRepository.findByIdAndOrganizationId(engagementId, orgId)).thenReturn(Optional.of(engagement));
        when(workTemplateRepository.findAccessibleById(templateId, orgId)).thenReturn(Optional.of(template));
        when(workTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(engagementWorkTemplateRepository.findByOrganizationIdAndEngagementIdAndTemplateId(orgId, engagementId, templateId))
                .thenReturn(Optional.empty());
        when(engagementWorkTemplateRepository.save(any(EngagementWorkTemplateEntity.class))).thenAnswer(i -> i.getArgument(0));

        EngagementWorkTemplateDto result = engagementWorkService.enableTemplateForEngagement(
                engagementId, templateId, EnableEngagementTemplateRequest.builder().build()
        );

        assertThat(result).isNotNull();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
        verify(auditService).logEvent(eq("TEMPLATE_ASSIGNED_TO_ENGAGEMENT"), eq("ENGAGEMENT"), eq(engagementId.toString()), any(), any());
    }

    @Test
    @DisplayName("Enable template for engagement with mismatched service throws BusinessValidationException (Requirement 22)")
    void testEnableTemplateMismatchedServiceFails() {
        UUID engagementId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        // Engagement configured for TDS
        EngagementEntity engagement = EngagementEntity.builder()
                .serviceId(tdsServiceId)
                .name("ABC Ltd TDS FY 2026-27")
                .status(EngagementStatus.ACTIVE)
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(orgId);

        // Template belongs to GST
        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .serviceId(gstServiceId)
                .name("GST Monthly Compliance")
                .status(WorkTemplateStatus.ACTIVE)
                .build();
        template.setId(templateId);

        when(engagementRepository.findByIdAndOrganizationId(engagementId, orgId)).thenReturn(Optional.of(engagement));
        when(workTemplateRepository.findAccessibleById(templateId, orgId)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> engagementWorkService.enableTemplateForEngagement(
                engagementId, templateId, EnableEngagementTemplateRequest.builder().build()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("TEMPLATE_SERVICE_MISMATCH");
    }

    @Test
    @DisplayName("Generate WorkInstance creates work instance and unified task instances with relative due dates")
    void testGenerateWorkInstanceSuccess() {
        UUID engagementId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID assignedUserId = UUID.randomUUID();

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(clientId)
                .serviceId(gstServiceId)
                .engagementCode("ENG-2026-00001")
                .name("ABC Ltd GST Compliance")
                .assignedUserId(assignedUserId)
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(orgId);

        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .serviceId(gstServiceId)
                .templateCode("GST_MONTHLY")
                .name("GST Monthly Compliance")
                .category(ServiceCategory.GST)
                .status(WorkTemplateStatus.ACTIVE)
                .recurrenceType(RecurrenceType.MONTHLY)
                .build();
        template.setId(templateId);

        WorkTemplateTaskEntity t1 = WorkTemplateTaskEntity.builder()
                .templateId(templateId).name("Collect Documents").relativeDueDays(5)
                .defaultPriority(TaskPriority.MEDIUM).active(true).build();
        t1.setId(UUID.randomUUID());

        WorkTemplateTaskEntity t2 = WorkTemplateTaskEntity.builder()
                .templateId(templateId).name("File Return").relativeDueDays(20)
                .defaultPriority(TaskPriority.HIGH).active(true).build();
        t2.setId(UUID.randomUUID());

        LocalDate periodStart = LocalDate.of(2026, 4, 1);
        LocalDate periodEnd = LocalDate.of(2026, 4, 30);

        when(engagementRepository.findByIdAndOrganizationId(engagementId, orgId)).thenReturn(Optional.of(engagement));
        when(engagementRepository.findById(engagementId)).thenReturn(Optional.of(engagement));
        when(workTemplateRepository.findAccessibleById(templateId, orgId)).thenReturn(Optional.of(template));
        when(workTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(workInstanceRepository.existsByOrganizationIdAndEngagementIdAndTemplateIdAndPeriodStartAndPeriodEnd(
                orgId, engagementId, templateId, periodStart, periodEnd)).thenReturn(false);
        when(workInstanceRepository.save(any(WorkInstanceEntity.class))).thenAnswer(i -> {
            WorkInstanceEntity inst = i.getArgument(0);
            inst.setId(UUID.randomUUID());
            return inst;
        });
        when(workTemplateTaskRepository.findAllByTemplateIdOrderBySequenceOrderAsc(templateId))
                .thenReturn(List.of(t1, t2));
        when(engagementWorkTemplateRepository.findByOrganizationIdAndEngagementIdAndTemplateId(orgId, engagementId, templateId))
                .thenReturn(Optional.empty());

        GenerateWorkInstanceRequest genReq = GenerateWorkInstanceRequest.builder()
                .templateId(templateId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build();

        WorkInstanceDto workInstance = engagementWorkService.generateWorkInstance(engagementId, genReq);

        assertThat(workInstance).isNotNull();
        assertThat(workInstance.getTitle()).contains("GST Monthly Compliance — April 2026");
        assertThat(workInstance.getPeriodStart()).isEqualTo(periodStart);
        assertThat(workInstance.getPeriodEnd()).isEqualTo(periodEnd);
        assertThat(workInstance.getStatus()).isEqualTo(WorkInstanceStatus.NOT_STARTED);

        // Verify task instances were instantiated
        verify(taskRepository, org.mockito.Mockito.times(2)).save(any(TaskEntity.class));
        verify(auditService).logEvent(eq("WORK_INSTANCE_CREATED"), eq("WORK_INSTANCE"), any(), any(), any());
    }

    @Test
    @DisplayName("Generate WorkInstance duplicate protection throws DuplicateResourceException (Requirement 11)")
    void testGenerateWorkInstanceDuplicateProtection() {
        UUID engagementId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        EngagementEntity engagement = EngagementEntity.builder()
                .serviceId(gstServiceId)
                .name("ABC Ltd GST Compliance")
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(orgId);

        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .serviceId(gstServiceId)
                .name("GST Monthly Compliance")
                .status(WorkTemplateStatus.ACTIVE)
                .recurrenceType(RecurrenceType.MONTHLY)
                .build();
        template.setId(templateId);

        LocalDate periodStart = LocalDate.of(2026, 4, 1);
        LocalDate periodEnd = LocalDate.of(2026, 4, 30);

        when(engagementRepository.findByIdAndOrganizationId(engagementId, orgId)).thenReturn(Optional.of(engagement));
        when(workTemplateRepository.findAccessibleById(templateId, orgId)).thenReturn(Optional.of(template));
        // Work instance already exists!
        when(workInstanceRepository.existsByOrganizationIdAndEngagementIdAndTemplateIdAndPeriodStartAndPeriodEnd(
                orgId, engagementId, templateId, periodStart, periodEnd)).thenReturn(true);

        GenerateWorkInstanceRequest genReq = GenerateWorkInstanceRequest.builder()
                .templateId(templateId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build();

        assertThatThrownBy(() -> engagementWorkService.generateWorkInstance(engagementId, genReq))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists for this engagement");
    }

    @Test
    @DisplayName("Update WorkInstance status lifecycle transitions")
    void testWorkInstanceStatusUpdate() {
        UUID instanceId = UUID.randomUUID();
        WorkInstanceEntity instance = WorkInstanceEntity.builder()
                .engagementId(UUID.randomUUID())
                .title("GST Compliance — April 2026")
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .status(WorkInstanceStatus.NOT_STARTED)
                .build();
        instance.setId(instanceId);
        instance.setOrganizationId(orgId);

        when(workInstanceRepository.findByIdAndOrganizationId(instanceId, orgId)).thenReturn(Optional.of(instance));
        when(workInstanceRepository.save(any(WorkInstanceEntity.class))).thenAnswer(i -> i.getArgument(0));

        // NOT_STARTED -> IN_PROGRESS
        WorkInstanceDto inProgress = engagementWorkService.updateWorkInstanceStatus(
                instanceId, UpdateWorkInstanceStatusRequest.builder().status(WorkInstanceStatus.IN_PROGRESS).build()
        );
        assertThat(inProgress.getStatus()).isEqualTo(WorkInstanceStatus.IN_PROGRESS);

        // IN_PROGRESS -> COMPLETED
        WorkInstanceDto completed = engagementWorkService.updateWorkInstanceStatus(
                instanceId, UpdateWorkInstanceStatusRequest.builder().status(WorkInstanceStatus.COMPLETED).build()
        );
        assertThat(completed.getStatus()).isEqualTo(WorkInstanceStatus.COMPLETED);
        verify(auditService).logEvent(eq("WORK_INSTANCE_COMPLETED"), eq("WORK_INSTANCE"), eq(instanceId.toString()), any(), any());
    }
}
