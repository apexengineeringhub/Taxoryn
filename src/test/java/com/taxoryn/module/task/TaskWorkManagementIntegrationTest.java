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
import com.taxoryn.module.task.dto.TaskCalendarDto;
import com.taxoryn.module.task.dto.TaskCalendarFilterRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.TeamWorkloadSummaryDto;
import com.taxoryn.module.task.dto.UpdateTaskPriorityRequest;
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
@ActiveProfiles("test")
public class TaskWorkManagementIntegrationTest {

    @Autowired
    private TaskService taskService;

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
                .name("Work Management CA Practice " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@capractice.in")
                .phone("9988776655")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff").isSystemRole(true).build()));

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@capractice.in")
                .firstName("Partner")
                .lastName("CA")
                .passwordHash("$2a$10$dummyhashfortestingpurposesonly")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        staffUser = userRepository.save(UserEntity.builder()
                .email("staff-" + UUID.randomUUID() + "@capractice.in")
                .firstName("Article")
                .lastName("Assistant 1")
                .passwordHash("$2a$10$dummyhashfortestingpurposesonly")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        adminEmployee = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUser.getId())
                .employeeCode("EMP-ADM-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Partner")
                .lastName("CA")
                .email(adminUser.getEmail())
                .designation("Managing Partner")
                .department("Tax & Audit")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        adminEmployee.setOrganizationId(testOrg.getId());
        employeeRepository.save(adminEmployee);

        staffEmployee = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffUser.getId())
                .employeeCode("EMP-STF-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Article")
                .lastName("Assistant 1")
                .email(staffUser.getEmail())
                .designation("Article Assistant")
                .department("Direct Tax")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        staffEmployee.setOrganizationId(testOrg.getId());
        employeeRepository.save(staffEmployee);

        testClient = clientRepository.save(ClientEntity.builder()
                .displayName("Tata Motors Ltd " + UUID.randomUUID())
                .clientType(ClientEntity.ClientType.COMPANY)
                .pan("AABCT1234D")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());
        testClient.setOrganizationId(testOrg.getId());
        clientRepository.save(testClient);

        testEngagement = EngagementEntity.builder()
                .clientId(testClient.getId())
                .engagementCode("ENG-" + UUID.randomUUID().toString().substring(0, 6))
                .name("FY 2026-27 Corporate Tax Advisory")
                .description("Comprehensive corporate tax filing & advisory")
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
    @DisplayName("Create standalone task linked to client and engagement, verify enrichment")
    void testCreateStandaloneTaskWithEngagement() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .engagementId(testEngagement.getId())
                .assignedTo(staffUser.getId())
                .title("Prepare Draft Compute of Income")
                .description("Calculate depreciation under Section 32")
                .taskCategory(TaskCategory.ITR)
                .priority(TaskPriority.HIGH)
                .startDate(LocalDate.of(2026, 10, 1))
                .dueDate(LocalDate.of(2026, 10, 15))
                .estimatedMinutes(120)
                .build();

        TaskDto created = taskService.createTask(req);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getClientId()).isEqualTo(testClient.getId());
        assertThat(created.getClientName()).isEqualTo(testClient.getDisplayName());
        assertThat(created.getEngagementId()).isEqualTo(testEngagement.getId());
        assertThat(created.getEngagementTitle()).isEqualTo(testEngagement.getName());
        assertThat(created.getEngagementCode()).isEqualTo(testEngagement.getEngagementCode());
        assertThat(created.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(created.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(created.getEstimatedMinutes()).isEqualTo(120);
        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(created.getAssigneeName()).isEqualTo(staffEmployee.getFullName());
    }

    @Test
    @DisplayName("Complete task records actualMinutes, notes, and stamps completedBy user")
    void testCompleteTask() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .engagementId(testEngagement.getId())
                .assignedTo(staffUser.getId())
                .title("TDS Reconciliation Q2")
                .taskCategory(TaskCategory.TDS)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(5))
                .estimatedMinutes(90)
                .build();

        TaskDto task = taskService.createTask(req);

        CompleteTaskRequest completeReq = CompleteTaskRequest.builder()
                .actualMinutes(75)
                .notes("Matched with 26AS successfully")
                .build();

        TaskDto completed = taskService.completeTask(task.getId(), completeReq);

        assertThat(completed.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.getActualMinutes()).isEqualTo(75);
        assertThat(completed.getCompletedBy()).isEqualTo(adminUser.getId());
        assertThat(completed.getCompletedByName()).isEqualTo(adminEmployee.getFullName());
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getNotes()).contains("[Completion Notes]: Matched with 26AS successfully");
    }

    @Test
    @DisplayName("Update task priority via PATCH priority")
    void testUpdateTaskPriority() {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Verify GST ITC Ledger")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.LOW)
                .dueDate(LocalDate.now().plusDays(3))
                .build();

        TaskDto task = taskService.createTask(req);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.LOW);

        TaskDto updated = taskService.updateTaskPriority(task.getId(),
                UpdateTaskPriorityRequest.builder().priority(TaskPriority.URGENT).build());

        assertThat(updated.getPriority()).isEqualTo(TaskPriority.URGENT);
    }

    @Test
    @DisplayName("Get Calendar tasks projections with date range filters")
    void testGetCalendarTasks() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        CreateTaskRequest t1 = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .engagementId(testEngagement.getId())
                .title("GSTR-1 Filing October")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.HIGH)
                .startDate(LocalDate.of(2026, 10, 5))
                .dueDate(LocalDate.of(2026, 10, 11))
                .build();
        taskService.createTask(t1);

        CreateTaskRequest t2 = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Advance Tax Q3 Planning (December)")
                .taskCategory(TaskCategory.ITR)
                .priority(TaskPriority.MEDIUM)
                .startDate(LocalDate.of(2026, 12, 1))
                .dueDate(LocalDate.of(2026, 12, 15))
                .build();
        taskService.createTask(t2);

        TaskCalendarFilterRequest filter = TaskCalendarFilterRequest.builder()
                .startDate(start)
                .endDate(end)
                .build();

        List<TaskCalendarDto> calTasks = taskService.getCalendarTasks(filter);

        assertThat(calTasks).isNotEmpty();
        assertThat(calTasks).anyMatch(c -> c.getTitle().equals("GSTR-1 Filing October")
                && c.getEngagementTitle().equals(testEngagement.getName()));
        assertThat(calTasks).noneMatch(c -> c.getTitle().equals("Advance Tax Q3 Planning (December)"));
    }

    @Test
    @DisplayName("Get Team Workload aggregation by staff member")
    void testGetTeamWorkload() {
        CreateTaskRequest t1 = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffUser.getId())
                .title("Staff Task 1")
                .taskCategory(TaskCategory.COMPLIANCE)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(2))
                .estimatedMinutes(60)
                .build();
        taskService.createTask(t1);

        CreateTaskRequest t2 = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .assignedTo(staffUser.getId())
                .title("Staff Task 2 Overdue")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().minusDays(3))
                .estimatedMinutes(45)
                .build();
        taskService.createTask(t2);

        List<TeamWorkloadSummaryDto> teamWorkload = taskService.getTeamWorkload();

        assertThat(teamWorkload).isNotEmpty();
        TeamWorkloadSummaryDto staffSummary = teamWorkload.stream()
                .filter(w -> w.getEmail().equals(staffUser.getEmail()))
                .findFirst()
                .orElse(null);

        assertThat(staffSummary).isNotNull();
        assertThat(staffSummary.getTotalAssigned()).isGreaterThanOrEqualTo(2);
        assertThat(staffSummary.getOverdueCount()).isGreaterThanOrEqualTo(1);
        assertThat(staffSummary.getTotalEstimatedMinutes()).isGreaterThanOrEqualTo(105);
    }
}
