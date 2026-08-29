package com.taxoryn.module.task;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.service.ComplianceService;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.service.DocumentRequestService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.TaskWorklistFilterRequest;
import com.taxoryn.module.task.dto.TaskWorklistFilterRequest.WorklistBucket;
import com.taxoryn.module.task.dto.TaskWorklistFilterRequest.WorklistScope;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.dto.WorklistSummaryDto;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.service.TaskService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskComplianceIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ComplianceService complianceService;

    @Autowired
    private DocumentRequestService docRequestService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    private OrganizationEntity testOrg;
    private UserEntity adminUser;
    private UserEntity staffUser;
    private EmployeeEntity staffEmployee;
    private EmployeeEntity adminEmployee;
    private ClientEntity testClient;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@apextax.in")
                .phone("9876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff").isSystemRole(true).build()));

        adminUser = userRepository.save(UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("$2a$10$dummyHashAdmin123456789012345678901234567890")
                .firstName("CA Vikram")
                .lastName("Mehta")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        staffUser = userRepository.save(UserEntity.builder()
                .email("staff-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("$2a$10$dummyHashStaff123456789012345678901234567890")
                .firstName("Rahul")
                .lastName("Sharma")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        EmployeeEntity adminEmp = EmployeeEntity.builder()
                .userId(adminUser.getId())
                .employeeCode("EMP-ADM-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("CA Vikram")
                .lastName("Mehta")
                .email(adminUser.getEmail())
                .designation("Partner")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        adminEmp.setOrganizationId(testOrg.getId());
        adminEmployee = employeeRepository.save(adminEmp);

        EmployeeEntity staffEmp = EmployeeEntity.builder()
                .userId(staffUser.getId())
                .employeeCode("EMP-STF-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Rahul")
                .lastName("Sharma")
                .email(staffUser.getEmail())
                .designation("Senior Associate")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        staffEmp.setOrganizationId(testOrg.getId());
        staffEmployee = employeeRepository.save(staffEmp);

        ClientEntity clientEntity = ClientEntity.builder()
                .displayName("ABC Manufacturing Pvt Ltd")
                .legalName("ABC Manufacturing Private Limited")
                .pan("AABCA9999C")
                .gstin("27AABCA9999C1Z5")
                .clientType(ClientEntity.ClientType.PRIVATE_LIMITED)
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        clientEntity.setOrganizationId(testOrg.getId());
        testClient = clientRepository.save(clientEntity);

        setAuthContext(adminUser, "ORG_ADMIN", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "GST_VIEW", "ITR_VIEW", "DOC_REQUEST_CREATE", "DOC_REQUEST_VIEW", "COMPLIANCE_VIEW", "COMPLIANCE_MANAGE");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void setAuthContext(UserEntity user, String role, String... permissions) {
        Set<String> roles = Set.of(role);
        Set<String> perms = Set.of(permissions);
        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(user.getOrganizationId())
                .email(user.getEmail())
                .roles(roles)
                .permissions(perms)
                .enabled(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(user.getOrganizationId());
    }

    @Test
    @DisplayName("1. Create Task with Compliance and Document Request Links")
    void testCreateTaskWithComplianceAndDocumentRequestLink() {
        // Step 1: Create a Compliance Obligation (GSTR-3B)
        ComplianceObligationDto obligation = complianceService.createObligation(
                CreateComplianceObligationRequest.builder()
                        .clientId(testClient.getId())
                        .title("GSTR-3B Monthly Return - August 2026")
                        .complianceType(ComplianceType.GST)
                        .period("August 2026")
                        .dueDate(LocalDate.of(2026, 9, 20))
                        .priority(CompliancePriority.HIGH)
                        .assignedEmployeeId(staffEmployee.getId())
                        .build()
        );
        assertThat(obligation).isNotNull();

        // Step 2: Create a Document Request for Purchase Register
        DocumentRequestDto docRequest = docRequestService.createAndSendRequest(
                CreateDocumentRequest.builder()
                        .clientId(testClient.getId())
                        .purpose("GSTR-3B August 2026 Verification")
                        .dueDate(LocalDate.of(2026, 9, 15))
                        .items(List.of(
                                CreateDocumentRequestItem.builder()
                                        .documentType(com.taxoryn.module.document.entity.DocumentEntity.DocumentType.GST_INVOICE_PURCHASE)
                                        .title("August 2026 Purchase Register")
                                        .required(true)
                                        .build(),
                                CreateDocumentRequestItem.builder()
                                        .documentType(com.taxoryn.module.document.entity.DocumentEntity.DocumentType.GST_INVOICE_SALE)
                                        .title("August 2026 Sales Register")
                                        .required(true)
                                        .build()
                        ))
                        .build()
        );
        assertThat(docRequest).isNotNull();
        assertThat(docRequest.getItems()).hasSize(2);

        // Step 3: Create Task linked to both Compliance and Document Request
        TaskDto task = taskService.createTask(
                CreateTaskRequest.builder()
                        .clientId(testClient.getId())
                        .assignedTo(staffEmployee.getId())
                        .title("Prepare GSTR-3B Return")
                        .description("Compute ITC, match purchase registers and prepare draft 3B return")
                        .taskCategory(TaskCategory.GST)
                        .priority(TaskPriority.HIGH)
                        .dueDate(LocalDate.of(2026, 9, 18)) // Internal task deadline
                        .complianceId(obligation.getId())
                        .documentRequestId(docRequest.getId())
                        .build()
        );

        assertThat(task).isNotNull();
        assertThat(task.getClientName()).isEqualTo("ABC Manufacturing Pvt Ltd");
        assertThat(task.getComplianceId()).isEqualTo(obligation.getId());
        assertThat(task.getComplianceTitle()).isEqualTo("GSTR-3B Monthly Return - August 2026");
        assertThat(task.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 18));
        assertThat(task.getDocumentRequestId()).isEqualTo(docRequest.getId());
        assertThat(task.getDocumentRequestNumber()).isEqualTo(docRequest.getRequestNumber());
        assertThat(task.getDocumentRequestItemsCount()).isEqualTo(2);
        assertThat(task.getDocumentRequestReceivedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("2. Task Blocked Lifecycle & Reason Management")
    void testTaskBlockedLifecycle() {
        TaskDto created = taskService.createTask(
                CreateTaskRequest.builder()
                        .clientId(testClient.getId())
                        .assignedTo(staffEmployee.getId())
                        .title("Prepare ITR for Corporate Client")
                        .taskCategory(TaskCategory.ITR)
                        .priority(TaskPriority.HIGH)
                        .dueDate(LocalDate.now().plusDays(5))
                        .build()
        );

        // Block task waiting for Form 16 / 26AS
        TaskDto blocked = taskService.updateTask(
                created.getId(),
                UpdateTaskRequest.builder()
                        .status(TaskStatus.BLOCKED)
                        .blockedReason("Waiting for Form 26AS and AIS reconciliation from client")
                        .build()
        );

        assertThat(blocked.getStatus()).isEqualTo(TaskStatus.BLOCKED);
        assertThat(blocked.getBlockedReason()).isEqualTo("Waiting for Form 26AS and AIS reconciliation from client");

        // Unblock task when documents arrive
        TaskDto unblocked = taskService.updateTask(
                created.getId(),
                UpdateTaskRequest.builder()
                        .status(TaskStatus.IN_PROGRESS)
                        .clearBlockedReason(true)
                        .build()
        );

        assertThat(unblocked.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(unblocked.getBlockedReason()).isNull();
    }

    @Test
    @DisplayName("3. Unified Worklist Bucketing (Overdue, Due Today, Due This Week, Blocked)")
    void testUnifiedWorklistQueryAndSorting() {
        LocalDate today = LocalDate.now();

        // 1. Overdue Task (Due 3 days ago)
        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Overdue TDS Payment Verification")
                .taskCategory(TaskCategory.TDS)
                .priority(TaskPriority.URGENT)
                .dueDate(today.minusDays(3))
                .build());

        // 2. Due Today Task
        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Due Today GSTR-1 Review")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.HIGH)
                .dueDate(today)
                .build());

        // 3. Due This Week Task (Due in 4 days)
        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Due This Week Tax Audit Computation")
                .taskCategory(TaskCategory.AUDIT)
                .priority(TaskPriority.MEDIUM)
                .dueDate(today.plusDays(4))
                .build());

        // 4. Blocked Task
        TaskDto blockedTaskCreated = taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Blocked Task Missing Bank Statements")
                .taskCategory(TaskCategory.ITR)
                .priority(TaskPriority.LOW)
                .dueDate(today.plusDays(10))
                .build());
        taskService.updateTask(blockedTaskCreated.getId(), UpdateTaskRequest.builder()
                .status(TaskStatus.BLOCKED)
                .blockedReason("Waiting for 12-month bank statement PDF")
                .build());

        // Test Worklist: OVERDUE Bucket
        PagedResponse<TaskDto> overdueWorklist = taskService.getWorklist(
                TaskWorklistFilterRequest.builder()
                        .bucket(WorklistBucket.OVERDUE)
                        .scope(WorklistScope.TEAM_WORK)
                        .build()
        );
        assertThat(overdueWorklist.getContent()).isNotEmpty();
        assertThat(overdueWorklist.getContent()).allMatch(t -> Boolean.TRUE.equals(t.getIsOverdue()));

        // Test Worklist: DUE_TODAY Bucket
        PagedResponse<TaskDto> dueTodayWorklist = taskService.getWorklist(
                TaskWorklistFilterRequest.builder()
                        .bucket(WorklistBucket.DUE_TODAY)
                        .scope(WorklistScope.TEAM_WORK)
                        .build()
        );
        assertThat(dueTodayWorklist.getContent()).isNotEmpty();
        assertThat(dueTodayWorklist.getContent()).allMatch(t -> Boolean.TRUE.equals(t.getIsDueToday()));

        // Test Worklist: DUE_THIS_WEEK Bucket
        PagedResponse<TaskDto> dueThisWeekWorklist = taskService.getWorklist(
                TaskWorklistFilterRequest.builder()
                        .bucket(WorklistBucket.DUE_THIS_WEEK)
                        .scope(WorklistScope.TEAM_WORK)
                        .build()
        );
        assertThat(dueThisWeekWorklist.getContent()).isNotEmpty();
        assertThat(dueThisWeekWorklist.getContent()).allMatch(t -> Boolean.TRUE.equals(t.getIsDueThisWeek()));

        // Test Worklist: BLOCKED Bucket
        PagedResponse<TaskDto> blockedWorklist = taskService.getWorklist(
                TaskWorklistFilterRequest.builder()
                        .bucket(WorklistBucket.BLOCKED)
                        .scope(WorklistScope.TEAM_WORK)
                        .build()
        );
        assertThat(blockedWorklist.getContent()).isNotEmpty();
        assertThat(blockedWorklist.getContent()).allMatch(t -> t.getStatus() == TaskStatus.BLOCKED);
    }

    @Test
    @DisplayName("4. Worklist Summary Aggregation Metrics")
    void testWorklistSummaryMetrics() {
        LocalDate today = LocalDate.now();

        // Create tasks in different states
        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Summary Test Overdue Task")
                .dueDate(today.minusDays(2))
                .build());

        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Summary Test Due Today Task")
                .dueDate(today)
                .build());

        TaskDto summaryBlocked = taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Summary Test Blocked Task")
                .dueDate(today.plusDays(3))
                .build());
        taskService.updateTask(summaryBlocked.getId(), UpdateTaskRequest.builder()
                .status(TaskStatus.BLOCKED)
                .blockedReason("Waiting for Form 16")
                .build());

        WorklistSummaryDto summary = taskService.getWorklistSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getOverdueCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getDueTodayCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getDueThisWeekCount()).isGreaterThanOrEqualTo(2);
        assertThat(summary.getBlockedCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getTeamTasksCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("5. RBAC Worklist Isolation: Staff vs Firm Admin")
    void testRBACAndWorklistIsolation() {
        // Create task assigned specifically to staffEmployee
        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffEmployee.getId())
                .title("Staff Assigned Workload Item")
                .dueDate(LocalDate.now().plusDays(2))
                .build());

        // Create task assigned to adminEmployee
        taskService.createTask(CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(adminEmployee.getId())
                .title("Partner Executive Review Item")
                .dueDate(LocalDate.now().plusDays(2))
                .build());

        // 1. As Staff: Only assigned task should be accessible
        setAuthContext(staffUser, "STAFF", "TASK_VIEW");
        PagedResponse<TaskDto> staffWorklist = taskService.getWorklist(
                TaskWorklistFilterRequest.builder()
                        .scope(WorklistScope.MY_WORK)
                        .build()
        );
        assertThat(staffWorklist.getContent()).isNotEmpty();
        assertThat(staffWorklist.getContent()).allMatch(t ->
                t.getTitle().contains("Staff Assigned") || t.getAssigneeEmail().equals(staffUser.getEmail()));

        // 2. As Admin: Can see both My Work and Team Work
        setAuthContext(adminUser, "ORG_ADMIN", "TASK_VIEW");
        PagedResponse<TaskDto> teamWorklist = taskService.getWorklist(
                TaskWorklistFilterRequest.builder()
                        .scope(WorklistScope.TEAM_WORK)
                        .build()
        );
        assertThat(teamWorklist.getContent().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("6. Statutory vs Internal Due Date Independence")
    void testStatutoryVsInternalDueDateDistinction() {
        // Statutory compliance deadline is 20 Sep (e.g. GSTR-3B)
        LocalDate statutoryDeadline = LocalDate.of(2026, 9, 20);
        ComplianceObligationDto obligation = complianceService.createObligation(
                CreateComplianceObligationRequest.builder()
                        .clientId(testClient.getId())
                        .title("GSTR-3B Filing August 2026")
                        .complianceType(ComplianceType.GST)
                        .period("08-2026")
                        .dueDate(statutoryDeadline)
                        .build()
        );

        // Internal task deadline is 17 Sep (3 days buffer for review)
        LocalDate internalDeadline = LocalDate.of(2026, 9, 17);
        TaskDto task = taskService.createTask(
                CreateTaskRequest.builder()
                        .clientId(testClient.getId())
                        .assignedTo(staffEmployee.getId())
                        .title("Draft & Audit GSTR-3B")
                        .complianceId(obligation.getId())
                        .dueDate(internalDeadline)
                        .build()
        );

        assertThat(task.getDueDate()).isEqualTo(internalDeadline);
        assertThat(task.getStatutoryDueDate()).isEqualTo(statutoryDeadline);
        assertThat(task.getDueDate()).isBefore(task.getStatutoryDueDate());
    }
}
