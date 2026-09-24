package com.taxoryn.module.compliance.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.CompleteComplianceWorkflowRequest;
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
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceWorkflowServiceTest {

    @Mock
    private ComplianceWorkflowRepository workflowRepository;
    @Mock
    private ComplianceWorkflowChecklistItemRepository checklistItemRepository;
    @Mock
    private ComplianceObligationRepository obligationRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientServiceRepository clientServiceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ComplianceWorkflowServiceImpl workflowService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private final UUID orgId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID obligationId = UUID.randomUUID();
    private final UUID workflowId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID reviewerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentOrganizationId).thenReturn(orgId);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
        securityUtilsMock.when(SecurityUtils::getCurrentUserEmail).thenReturn("practitioner@taxoryn.com");

        PracticeSecurityScope firmAdminScope = PracticeSecurityScope.builder()
                .organizationId(orgId)
                .isFirmAdmin(true)
                .build();
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(firmAdminScope);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    @DisplayName("Should initialize workflow with 9 default operational checklist checkpoints")
    void testGetOrCreateWorkflow_SeedsNineChecklistItems() {
        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Monthly Return - July 2026")
                .periodLabel("July 2026")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .internalTargetDate(LocalDate.of(2026, 8, 17))
                .status(ComplianceObligationStatus.READY)
                .priority(TaskPriority.HIGH)
                .assignedEmployeeId(employeeId)
                .build();
        obligation.setId(obligationId);
        obligation.setOrganizationId(orgId);

        when(obligationRepository.findByIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.of(obligation));
        when(workflowRepository.findByComplianceObligationIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.empty());

        when(workflowRepository.save(any(ComplianceWorkflowEntity.class))).thenAnswer(invocation -> {
            ComplianceWorkflowEntity wf = invocation.getArgument(0);
            wf.setId(workflowId);
            return wf;
        });

        ComplianceWorkflowDto dto = workflowService.getOrCreateWorkflowForObligation(obligationId);

        assertThat(dto).isNotNull();
        assertThat(dto.getComplianceObligationId()).isEqualTo(obligationId);
        assertThat(dto.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.READY);
        assertThat(dto.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(dto.getTargetDate()).isEqualTo(LocalDate.of(2026, 8, 17));

        verify(workflowRepository).save(any(ComplianceWorkflowEntity.class));
        verify(obligationRepository).save(obligation);
        verify(auditService).logEvent(eq("WORKFLOW_CREATED"), eq("COMPLIANCE_WORKFLOW"), any(), any(), any());
    }

    @Test
    @DisplayName("Should return existing workflow idempotently if already created")
    void testGetOrCreateWorkflow_Idempotent() {
        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .title("GSTR-1 Monthly")
                .statutoryDueDate(LocalDate.of(2026, 8, 11))
                .build();
        obligation.setId(obligationId);
        obligation.setOrganizationId(orgId);

        ComplianceWorkflowEntity existingWorkflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .complianceObligationId(obligationId)
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .statutoryDueDate(LocalDate.of(2026, 8, 11))
                .build();
        existingWorkflow.setId(workflowId);
        existingWorkflow.setOrganizationId(orgId);

        when(obligationRepository.findByIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.of(obligation));
        when(workflowRepository.findByComplianceObligationIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.of(existingWorkflow));

        ComplianceWorkflowDto dto = workflowService.getOrCreateWorkflowForObligation(obligationId);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(workflowId);
        assertThat(dto.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should execute complete happy-path lifecycle: START -> REVIEW -> APPROVE -> FILED -> COMPLETED")
    void testWorkflowLifecycle_FullHappyPath() {
        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .complianceObligationId(obligationId)
                .workflowStatus(ComplianceWorkflowStatus.READY)
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .targetDate(LocalDate.of(2026, 8, 17))
                .assignedEmployeeId(employeeId)
                .reviewerEmployeeId(reviewerId)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));
        when(obligationRepository.findByIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.of(new ComplianceObligationEntity()));

        // 1. Start
        ComplianceWorkflowDto started = workflowService.startWorkflow(workflowId);
        assertThat(started.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.IN_PROGRESS);
        assertThat(workflow.getStartedAt()).isNotNull();

        // 2. Submit Review
        ComplianceWorkflowDto underReview = workflowService.submitReview(workflowId);
        assertThat(underReview.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.UNDER_REVIEW);
        assertThat(workflow.getSubmittedAt()).isNotNull();

        // 3. Approve
        ApproveWorkflowRequest approveReq = ApproveWorkflowRequest.builder().approvalNotes("Computation verified").build();
        ComplianceWorkflowDto approved = workflowService.approveWorkflow(workflowId, approveReq);
        assertThat(approved.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.READY_FOR_FILING);
        assertThat(workflow.getApprovedAt()).isNotNull();

        // 4. Mark Filed
        MarkWorkflowFiledRequest filedReq = MarkWorkflowFiledRequest.builder()
                .filedDate(LocalDate.of(2026, 8, 16))
                .acknowledgementNumber("ARN123456789")
                .build();
        ComplianceWorkflowDto filed = workflowService.markFiled(workflowId, filedReq);
        assertThat(filed.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.FILED);
        assertThat(workflow.getAcknowledgementNumber()).isEqualTo("ARN123456789");

        // 5. Complete
        CompleteComplianceWorkflowRequest completeReq = CompleteComplianceWorkflowRequest.builder()
                .notes("Archived ACK and emailed client")
                .build();
        ComplianceWorkflowDto completed = workflowService.completeWorkflow(workflowId, completeReq);
        assertThat(completed.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.COMPLETED);
        assertThat(workflow.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw BusinessValidationException on illegal state transition")
    void testInvalidTransition_ThrowsBusinessValidationException() {
        ComplianceWorkflowEntity completedWorkflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .complianceObligationId(obligationId)
                .workflowStatus(ComplianceWorkflowStatus.COMPLETED)
                .build();
        completedWorkflow.setId(workflowId);
        completedWorkflow.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(completedWorkflow));

        assertThatThrownBy(() -> workflowService.startWorkflow(workflowId))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Invalid workflow transition");
    }

    @Test
    @DisplayName("Should handle Waiting for Client pause and resume lifecycle cleanly")
    void testWaitingForClientAndResumeFlow() {
        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .complianceObligationId(obligationId)
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));
        when(obligationRepository.findByIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.of(new ComplianceObligationEntity()));

        // Pause waiting for client
        WaitClientWorkflowRequest waitReq = WaitClientWorkflowRequest.builder()
                .reason("Awaiting July purchase register Excel")
                .expectedResponseDate(LocalDate.of(2026, 8, 14))
                .build();
        ComplianceWorkflowDto waiting = workflowService.waitClient(workflowId, waitReq);

        assertThat(waiting.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.WAITING_FOR_CLIENT);
        assertThat(waiting.isWaitingForClient()).isTrue();
        assertThat(workflow.getWaitingReason()).isEqualTo("Awaiting July purchase register Excel");

        // Resume after client responds
        ComplianceWorkflowDto resumed = workflowService.resumeClient(workflowId);
        assertThat(resumed.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.IN_PROGRESS);
        assertThat(resumed.isWaitingForClient()).isFalse();
    }

    @Test
    @DisplayName("Should handle Maker-Checker Revisions Request")
    void testMakerChecker_RequestChangesFlow() {
        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .complianceObligationId(obligationId)
                .workflowStatus(ComplianceWorkflowStatus.UNDER_REVIEW)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));
        when(obligationRepository.findByIdAndOrganizationId(obligationId, orgId)).thenReturn(Optional.of(new ComplianceObligationEntity()));

        RequestWorkflowChangesRequest changesReq = RequestWorkflowChangesRequest.builder()
                .reason("Table 4B ITC reversal missing for Rule 42")
                .build();

        ComplianceWorkflowDto dto = workflowService.requestChanges(workflowId, changesReq);

        assertThat(dto.getWorkflowStatus()).isEqualTo(ComplianceWorkflowStatus.CHANGES_REQUIRED);
        assertThat(workflow.getChangesRequestedReason()).isEqualTo("Table 4B ITC reversal missing for Rule 42");
    }

    @Test
    @DisplayName("Should update checklist checkpoint and record practitioner user metadata")
    void testUpdateChecklistItem() {
        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);

        UUID itemId = UUID.randomUUID();
        ComplianceWorkflowChecklistItemEntity item = ComplianceWorkflowChecklistItemEntity.builder()
                .workflow(workflow)
                .itemKey("DOCUMENTS_RECEIVED")
                .title("Receive Client Documents")
                .sequenceOrder(2)
                .isCompleted(false)
                .build();
        item.setId(itemId);
        item.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));
        when(checklistItemRepository.findByIdAndOrganizationId(itemId, orgId)).thenReturn(Optional.of(item));
        when(checklistItemRepository.save(any(ComplianceWorkflowChecklistItemEntity.class))).thenAnswer(i -> i.getArgument(0));

        UpdateChecklistItemRequest req = UpdateChecklistItemRequest.builder()
                .isCompleted(true)
                .notes("Received sales report via WhatsApp")
                .build();

        ComplianceWorkflowChecklistItemDto updated = workflowService.updateChecklistItem(workflowId, itemId, req);

        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getNotes()).isEqualTo("Received sales report via WhatsApp");
        assertThat(updated.getCompletedByName()).isEqualTo("practitioner@taxoryn.com");
    }

    @Test
    @DisplayName("Should create discrete task linked to compliance workflow")
    void testCreateWorkflowTask() {
        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .complianceObligationId(obligationId)
                .priority(TaskPriority.HIGH)
                .targetDate(LocalDate.of(2026, 8, 15))
                .assignedEmployeeId(employeeId)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));

        TaskEntity savedTask = TaskEntity.builder()
                .clientId(clientId)
                .complianceId(obligationId)
                .title("Reconcile 2B ledger")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .build();
        savedTask.setId(UUID.randomUUID());
        savedTask.setOrganizationId(orgId);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(savedTask);

        TaskDto mockDto = TaskDto.builder().title("Reconcile 2B ledger").status(TaskStatus.TODO).build();
        when(taskMapper.toDto(savedTask)).thenReturn(mockDto);

        CreateWorkflowTaskRequest req = CreateWorkflowTaskRequest.builder()
                .title("Reconcile 2B ledger")
                .priority(TaskPriority.HIGH)
                .build();

        TaskDto taskDto = workflowService.createWorkflowTask(workflowId, req);

        assertThat(taskDto).isNotNull();
        assertThat(taskDto.getTitle()).isEqualTo("Reconcile 2B ledger");
        verify(taskRepository).save(any(TaskEntity.class));
    }

    @Test
    @DisplayName("Should calculate scoped workbench summary metrics")
    void testGetWorkbenchSummary() {
        when(workflowRepository.countActiveWorkflows(eq(orgId), any())).thenReturn(25L);
        when(workflowRepository.countDueTodayWorkflows(eq(orgId), any(), any())).thenReturn(3L);
        when(workflowRepository.countDueInRangeWorkflows(eq(orgId), any(), any(), any())).thenReturn(8L);
        when(workflowRepository.countOverdueWorkflows(eq(orgId), any(), any())).thenReturn(2L);
        when(workflowRepository.countWaitingForClientWorkflows(eq(orgId), any())).thenReturn(5L);
        when(workflowRepository.countByStatus(eq(orgId), eq(ComplianceWorkflowStatus.UNDER_REVIEW), any())).thenReturn(4L);
        when(workflowRepository.countByStatus(eq(orgId), eq(ComplianceWorkflowStatus.READY_FOR_FILING), any())).thenReturn(6L);
        when(workflowRepository.countCompletedSince(eq(orgId), any(), any())).thenReturn(15L);

        ComplianceWorkbenchSummaryDto summary = workflowService.getWorkbenchSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalActive()).isEqualTo(25L);
        assertThat(summary.getDueToday()).isEqualTo(3L);
        assertThat(summary.getDueThisWeek()).isEqualTo(8L);
        assertThat(summary.getOverdue()).isEqualTo(2L);
        assertThat(summary.getWaitingForClient()).isEqualTo(5L);
        assertThat(summary.getUnderReview()).isEqualTo(4L);
        assertThat(summary.getReadyForFiling()).isEqualTo(6L);
        assertThat(summary.getCompletedThisMonth()).isEqualTo(15L);
    }

    @Test
    @DisplayName("Should enforce security scope and deny access if client is not in practitioner portfolio")
    void testClientScopeAccess_ForbiddenException() {
        PracticeSecurityScope restrictedStaffScope = PracticeSecurityScope.builder()
                .organizationId(orgId)
                .isFirmAdmin(false)
                .build();
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(restrictedStaffScope);
        when(securityScopeEvaluator.getAccessibleClientIds(restrictedStaffScope)).thenReturn(Set.of(UUID.randomUUID())); // Does not include clientId

        ComplianceWorkflowEntity workflow = ComplianceWorkflowEntity.builder()
                .clientId(clientId)
                .build();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);

        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> workflowService.startWorkflow(workflowId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }
}
