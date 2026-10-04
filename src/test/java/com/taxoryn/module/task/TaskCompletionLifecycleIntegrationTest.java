package com.taxoryn.module.task;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.dto.CompleteTaskRequest;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.TaskService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TaskCompletionLifecycleIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

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

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private WorkInstanceRepository workInstanceRepository;

    private OrganizationEntity testOrg;
    private UserEntity adminUser;
    private UserEntity staffUser;
    private EmployeeEntity staffEmployee;
    private EmployeeEntity adminEmployee;
    private ClientEntity testClient;
    private EngagementEntity testEngagement;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Completion Lifecycle Practice " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@lifecyclepractice.in")
                .phone("9876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff").isSystemRole(true).build()));

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@lifecyclepractice.in")
                .firstName("Managing")
                .lastName("Partner")
                .passwordHash("$2a$10$dummyhashfortestingpurposesonly")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        staffUser = userRepository.save(UserEntity.builder()
                .email("staff-" + UUID.randomUUID() + "@lifecyclepractice.in")
                .firstName("Senior")
                .lastName("Associate")
                .passwordHash("$2a$10$dummyhashfortestingpurposesonly")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        adminEmployee = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUser.getId())
                .employeeCode("EMP-ADM-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Managing")
                .lastName("Partner")
                .email(adminUser.getEmail())
                .designation("Partner")
                .department("Taxation")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        adminEmployee.setOrganizationId(testOrg.getId());
        employeeRepository.save(adminEmployee);

        staffEmployee = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffUser.getId())
                .employeeCode("EMP-STF-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Senior")
                .lastName("Associate")
                .email(staffUser.getEmail())
                .designation("Senior Associate")
                .department("Direct Tax")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        staffEmployee.setOrganizationId(testOrg.getId());
        employeeRepository.save(staffEmployee);

        testClient = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Global Industries Ltd " + UUID.randomUUID())
                .clientType(ClientEntity.ClientType.COMPANY)
                .pan("AABCA1234F")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());
        testClient.setOrganizationId(testOrg.getId());
        clientRepository.save(testClient);

        testEngagement = EngagementEntity.builder()
                .clientId(testClient.getId())
                .engagementCode("ENG-" + UUID.randomUUID().toString().substring(0, 6))
                .name("FY 2026-27 Comprehensive Tax Retainer")
                .description("Ongoing tax compliance & advisory")
                .status(EngagementStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 4, 1))
                .build();
        testEngagement.setOrganizationId(testOrg.getId());
        engagementRepository.save(testEngagement);

        setAuthUser(adminUser, Set.of("ROLE_ORG_ADMIN"), Set.of("TASK_CREATE", "TASK_VIEW", "TASK_UPDATE", "TASK_WRITE", "TASK_READ"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void setAuthUser(UserEntity user, Set<String> roles, Set<String> perms) {
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
    @DisplayName("1. New task created must have completed_at = null and completed_by = null")
    void testNewTaskHasNullCompletionMetadata() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Draft Form 3CD Tax Audit Report")
                .taskCategory(TaskCategory.AUDIT)
                .priority(TaskPriority.HIGH)
                .startDate(LocalDate.of(2026, 10, 1))
                .dueDate(LocalDate.of(2026, 10, 20))
                .estimatedMinutes(180)
                .build();

        TaskDto task = taskService.createTask(req);

        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getCompletedBy()).isNull();
        assertThat(task.getCompletedByName()).isNull();

        TaskEntity entity = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(entity.getCompletedAt()).isNull();
        assertThat(entity.getCompletedBy()).isNull();
    }

    @Test
    @DisplayName("2. Completing a task via completeTask stamps completed_at and completed_by")
    void testCompleteTaskStampsMetadata() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .engagementId(testEngagement.getId())
                .assignedTo(staffUser.getId())
                .title("Prepare GSTR-3B Verification Sheet")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 10, 20))
                .estimatedMinutes(60)
                .build();

        TaskDto created = taskService.createTask(req);
        Instant beforeCompletion = Instant.now().minusSeconds(1);

        // Complete the task as adminUser
        CompleteTaskRequest completeReq = CompleteTaskRequest.builder()
                .actualMinutes(50)
                .notes("All outward tax liabilities matched with e-way bills.")
                .build();

        TaskDto completed = taskService.completeTask(created.getId(), completeReq);
        Instant afterCompletion = Instant.now().plusSeconds(1);

        assertThat(completed.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getCompletedAt()).isAfterOrEqualTo(beforeCompletion).isBeforeOrEqualTo(afterCompletion);
        assertThat(completed.getCompletedBy()).isEqualTo(adminUser.getId());
        assertThat(completed.getCompletedByName()).isEqualTo(adminEmployee.getFullName());
        assertThat(completed.getActualMinutes()).isEqualTo(50);
        assertThat(completed.getStartDate()).isEqualTo(created.getStartDate());
        assertThat(completed.getDueDate()).isEqualTo(created.getDueDate());
        assertThat(completed.getEstimatedMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("3. Updating status to COMPLETED via updateTask stamps completed_at and completed_by")
    void testUpdateTaskToCompletedStampsMetadata() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Verify Section 194C TDS Deductions")
                .taskCategory(TaskCategory.TDS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 10, 7))
                .build();

        TaskDto created = taskService.createTask(req);
        assertThat(created.getCompletedAt()).isNull();

        UpdateTaskRequest updateReq = UpdateTaskRequest.builder()
                .status(TaskStatus.COMPLETED)
                .actualMinutes(40)
                .build();

        TaskDto updated = taskService.updateTask(created.getId(), updateReq);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getCompletedBy()).isEqualTo(adminUser.getId());
        assertThat(updated.getCompletedByName()).isEqualTo(adminEmployee.getFullName());
        assertThat(updated.getActualMinutes()).isEqualTo(40);
    }

    @Test
    @DisplayName("4. Reopening a completed task clears completed_at and completed_by")
    void testReopenCompletedTaskClearsMetadata() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Annual GST Return GSTR-9 Preparation")
                .taskCategory(TaskCategory.GST)
                .dueDate(LocalDate.of(2026, 12, 31))
                .build();

        TaskDto created = taskService.createTask(req);
        taskService.completeTask(created.getId(), CompleteTaskRequest.builder().actualMinutes(90).build());

        TaskDto completed = taskService.getTaskById(created.getId());
        assertThat(completed.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getCompletedBy()).isNotNull();

        // Reopen task (COMPLETED -> IN_PROGRESS)
        UpdateTaskRequest reopenReq = UpdateTaskRequest.builder()
                .status(TaskStatus.IN_PROGRESS)
                .build();

        TaskDto reopened = taskService.updateTask(created.getId(), reopenReq);

        assertThat(reopened.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(reopened.getCompletedAt()).isNull();
        assertThat(reopened.getCompletedBy()).isNull();
        assertThat(reopened.getCompletedByName()).isNull();

        TaskEntity entity = taskRepository.findById(created.getId()).orElseThrow();
        assertThat(entity.getCompletedAt()).isNull();
        assertThat(entity.getCompletedBy()).isNull();
    }

    @Test
    @DisplayName("5. Re-completing a reopened task generates a new completed_at timestamp")
    void testRecompletingTaskGeneratesNewTimestamp() throws InterruptedException {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("ITR-6 Schedule BP Computation")
                .taskCategory(TaskCategory.ITR)
                .dueDate(LocalDate.of(2026, 10, 31))
                .build();

        TaskDto created = taskService.createTask(req);
        taskService.completeTask(created.getId(), CompleteTaskRequest.builder().actualMinutes(60).build());

        TaskDto firstCompleted = taskService.getTaskById(created.getId());
        Instant firstCompletedAt = firstCompleted.getCompletedAt();
        assertThat(firstCompletedAt).isNotNull();

        // Reopen task
        taskService.updateTask(created.getId(), UpdateTaskRequest.builder().status(TaskStatus.IN_PROGRESS).build());

        // Wait small interval
        Thread.sleep(20);

        // Switch authenticated user to staffUser and re-complete
        setAuthUser(staffUser, Set.of("ROLE_STAFF"), Set.of("TASK_CREATE", "TASK_VIEW", "TASK_UPDATE", "TASK_WRITE", "TASK_READ"));

        CompleteTaskRequest secondCompleteReq = CompleteTaskRequest.builder()
                .actualMinutes(85)
                .notes("Rework completed after client provided ledger confirmations.")
                .build();

        TaskDto secondCompleted = taskService.completeTask(created.getId(), secondCompleteReq);

        assertThat(secondCompleted.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(secondCompleted.getCompletedAt()).isNotNull();
        assertThat(secondCompleted.getCompletedAt()).isAfter(firstCompletedAt);
        assertThat(secondCompleted.getCompletedBy()).isEqualTo(staffUser.getId());
        assertThat(secondCompleted.getCompletedByName()).isEqualTo(staffEmployee.getFullName());
        assertThat(secondCompleted.getActualMinutes()).isEqualTo(85);
    }

    @Test
    @DisplayName("6. WorkInstance instantiated task completion records completed_at and completed_by")
    void testWorkInstanceTaskCompletion() {
        WorkInstanceEntity workInstance = WorkInstanceEntity.builder()
                .engagementId(testEngagement.getId())
                .title("October 2026 Monthly Compliance Instance")
                .periodStart(LocalDate.of(2026, 10, 1))
                .periodEnd(LocalDate.of(2026, 10, 31))
                .dueDate(LocalDate.of(2026, 10, 20))
                .status(WorkInstanceStatus.IN_PROGRESS)
                .build();
        workInstance.setOrganizationId(testOrg.getId());
        workInstanceRepository.save(workInstance);

        TaskEntity wiTask = TaskEntity.builder()
                .clientId(testClient.getId())
                .engagementId(testEngagement.getId())
                .workInstanceId(workInstance.getId())
                .title("Reconcile GSTR-2B with Purchase Register")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 10, 14))
                .status(TaskStatus.IN_PROGRESS)
                .build();
        wiTask.setOrganizationId(testOrg.getId());
        taskRepository.save(wiTask);

        TaskDto completed = taskService.completeTask(wiTask.getId(), CompleteTaskRequest.builder().actualMinutes(45).build());

        assertThat(completed.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.getWorkInstanceId()).isEqualTo(workInstance.getId());
        assertThat(completed.getEngagementId()).isEqualTo(testEngagement.getId());
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getCompletedBy()).isEqualTo(adminUser.getId());
    }
}
