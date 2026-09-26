package com.taxoryn.module.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkItemTaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ComplianceWorkflowRepository workflowRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity loc;
    private UserEntity user1;
    private UserEntity user2;
    private ClientEntity client;
    private ComplianceWorkflowEntity workflow;
    private String token;

    @BeforeEach
    void setUp() {
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Tax Advisory - " + UUID.randomUUID())
                .legalName("Global Tax Advisory LLP")
                .email("contact." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org.getId());

        LocationEntity l = LocationEntity.builder()
                .name("Bengaluru Office")
                .code("BLR-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user1 = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("lead." + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Senior")
                .lastName("Manager")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        user2 = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("staff." + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Staff")
                .lastName("Assistant")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user1.getId(),
                org.getId(),
                user1.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Infosys Tech Partner")
                .legalName("Infosys Tech Partner Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .pan("BBBBB1234B")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        ComplianceObligationEntity obl = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.ITR_FILING)
                .title("ITR-6 AY 2026-27")
                .periodLabel("AY 2026-27")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceObligationStatus.UPCOMING)
                .locationId(loc.getId())
                .build();
        obl.setOrganizationId(org.getId());
        obligationRepository.save(obl);

        ComplianceWorkflowEntity wf = ComplianceWorkflowEntity.builder()
                .clientId(client.getId())
                .complianceObligationId(obl.getId())
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .workflowType("ITR_ITR6")
                .locationId(loc.getId())
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .build();
        wf.setOrganizationId(org.getId());
        workflow = workflowRepository.save(wf);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        taskRepository.deleteAll();
        workItemRepository.deleteAll();
        workflowRepository.deleteAll();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create child tasks under Work Item and verify task aggregation counters in WorkItemDto")
    void testCreateTasksUnderWorkItemAndVerifyMetrics() throws Exception {
        // 1. Create Work Item
        CreateWorkItemRequest wiReq = CreateWorkItemRequest.builder()
                .title("Tax Audit 44AB Preparation")
                .description("Prepare 3CD Annexures and Depreciation schedules")
                .clientId(client.getId())
                .locationId(loc.getId())
                .workflowId(workflow.getId())
                .priority(WorkItemPriority.HIGH)
                .assignedUserId(user1.getId())
                .dueDate(LocalDate.of(2026, 9, 30))
                .build();

        String wiResp = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wiReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workItemId = UUID.fromString(objectMapper.readTree(wiResp).path("data").path("id").asText());

        // Initial check: totalTasks = 0, completedTasks = 0
        mockMvc.perform(get("/api/v1/work-items/" + workItemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTasks").value(0))
                .andExpect(jsonPath("$.data.completedTasks").value(0));

        // 2. Add Task 1 under Work Item via POST /api/v1/work-items/{id}/tasks
        CreateTaskRequest task1Req = CreateTaskRequest.builder()
                .title("Draft Form 3CD Clause 16-25 Working Papers")
                .description("Verify bonus/commission payments under 43B and prior period items")
                .priority(TaskPriority.HIGH)
                .assignedTo(user2.getId())
                .dueDate(LocalDate.of(2026, 9, 15))
                .build();

        String t1Resp = mockMvc.perform(post("/api/v1/work-items/" + workItemId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task1Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Draft Form 3CD Clause 16-25 Working Papers"))
                .andExpect(jsonPath("$.data.workItemId").value(workItemId.toString()))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        UUID task1Id = UUID.fromString(objectMapper.readTree(t1Resp).path("data").path("id").asText());

        // 3. Add Task 2 under Work Item directly via POST /api/v1/tasks with workItemId
        CreateTaskRequest task2Req = CreateTaskRequest.builder()
                .title("Prepare Fixed Asset Register and Depreciation Schedule")
                .description("Compute IT Act depreciation vs Companies Act depreciation")
                .clientId(client.getId())
                .locationId(loc.getId())
                .complianceId(workflow.getId())
                .workItemId(workItemId)
                .priority(TaskPriority.MEDIUM)
                .assignedTo(user1.getId())
                .dueDate(LocalDate.of(2026, 9, 20))
                .build();

        String t2Resp = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task2Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.workItemId").value(workItemId.toString()))
                .andReturn().getResponse().getContentAsString();

        UUID task2Id = UUID.fromString(objectMapper.readTree(t2Resp).path("data").path("id").asText());

        // 4. List tasks under Work Item via GET /api/v1/work-items/{id}/tasks
        mockMvc.perform(get("/api/v1/work-items/" + workItemId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 5. Verify Work Item metrics: totalTasks = 2, completedTasks = 0
        mockMvc.perform(get("/api/v1/work-items/" + workItemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTasks").value(2))
                .andExpect(jsonPath("$.data.completedTasks").value(0));

        // 6. Complete Task 1 via PATCH /api/v1/tasks/{taskId}/status
        UpdateTaskRequest statusReq = UpdateTaskRequest.builder()
                .status(TaskStatus.COMPLETED)
                .notes("All 3CD clauses reconciled with general ledger")
                .build();

        mockMvc.perform(patch("/api/v1/tasks/" + task1Id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // 7. Verify Work Item metrics: totalTasks = 2, completedTasks = 1
        mockMvc.perform(get("/api/v1/work-items/" + workItemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTasks").value(2))
                .andExpect(jsonPath("$.data.completedTasks").value(1));

        // 8. Reassign Task 2 via PATCH /api/v1/tasks/{taskId}/assign
        UpdateTaskRequest assignReq = UpdateTaskRequest.builder()
                .assignedTo(user2.getId())
                .notes("Reassign to staff for final verification")
                .build();

        mockMvc.perform(patch("/api/v1/tasks/" + task2Id + "/assign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedTo").value(user2.getId().toString()));
    }
}
