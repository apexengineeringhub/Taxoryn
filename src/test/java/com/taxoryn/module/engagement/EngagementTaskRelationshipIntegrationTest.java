package com.taxoryn.module.engagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EngagementTaskRelationshipIntegrationTest {

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
    private EngagementRepository engagementRepository;

    @Autowired
    private WorkInstanceRepository workInstanceRepository;

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

    private OrganizationEntity primaryOrg;
    private OrganizationEntity secondaryOrg;
    private LocationEntity primaryLocation;
    private ClientEntity primaryClient;
    private ClientEntity secondaryClient;
    private EngagementEntity primaryEngagement;
    private WorkInstanceEntity primaryWorkInstance;
    private UserEntity adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantContext.clear();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Practice Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        primaryOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Relationship Test Practice " + UUID.randomUUID())
                .legalName("Rel Practice LLP")
                .email("admin." + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(primaryOrg.getId());

        secondaryOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Practice " + UUID.randomUUID())
                .legalName("Other Practice LLP")
                .email("admin." + UUID.randomUUID() + "@other.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity loc = LocationEntity.builder()
                .name("Main Office")
                .code("MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(primaryOrg.getId());
        primaryLocation = locationRepository.save(loc);

        ClientEntity client1 = ClientEntity.builder()
                .displayName("Acme Global Corp")
                .pan("ABCDE1234F")
                .clientType(ClientType.COMPANY)
                .locationId(primaryLocation.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client1.setOrganizationId(primaryOrg.getId());
        primaryClient = clientRepository.save(client1);

        ClientEntity client2 = ClientEntity.builder()
                .displayName("Beta Enterprises")
                .pan("XYZAB5678C")
                .clientType(ClientType.INDIVIDUAL)
                .locationId(primaryLocation.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client2.setOrganizationId(primaryOrg.getId());
        secondaryClient = clientRepository.save(client2);

        EngagementEntity eng = EngagementEntity.builder()
                .locationId(primaryLocation.getId())
                .clientId(primaryClient.getId())
                .engagementCode("ENG-REL-001")
                .name("Statutory Audit FY 2025-26")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .startDate(LocalDate.now())
                .build();
        eng.setOrganizationId(primaryOrg.getId());
        primaryEngagement = engagementRepository.save(eng);

        WorkInstanceEntity workInstance = WorkInstanceEntity.builder()
                .engagementId(primaryEngagement.getId())
                .title("Audit Q1 2025-26")
                .periodStart(LocalDate.of(2025, 4, 1))
                .periodEnd(LocalDate.of(2025, 6, 30))
                .dueDate(LocalDate.of(2025, 7, 31))
                .status(WorkInstanceStatus.NOT_STARTED)
                .build();
        workInstance.setOrganizationId(primaryOrg.getId());
        primaryWorkInstance = workInstanceRepository.save(workInstance);

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(primaryOrg.getId())
                .email("admin." + UUID.randomUUID() + "@taxoryn.test")
                .firstName("Practice")
                .lastName("Admin")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                primaryOrg.getId(),
                adminUser.getEmail(),
                Set.of("ORG_ADMIN", "PRACTICE_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_VIEW", "CLIENT_UPDATE", "TASK_CREATE", "TASK_VIEW", "TASK_WRITE", "TASK_UPDATE", "ORG_WRITE")
        );
    }

    private void cleanUp() {
        taskRepository.deleteAll();
        workInstanceRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create standalone task with no engagement or work instance")
    void testCreateStandaloneTask() throws Exception {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .title("General office maintenance")
                .taskCategory(TaskEntity.TaskCategory.OTHER)
                .priority(TaskEntity.TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("General office maintenance"))
                .andExpect(jsonPath("$.data.engagementId").doesNotExist())
                .andExpect(jsonPath("$.data.workInstanceId").doesNotExist());
    }

    @Test
    @DisplayName("Should create direct engagement task and correctly populate engagement relationship")
    void testCreateDirectEngagementTask() throws Exception {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .engagementId(primaryEngagement.getId())
                .title("Prepare audit scope memo")
                .taskCategory(TaskEntity.TaskCategory.AUDIT)
                .priority(TaskEntity.TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Prepare audit scope memo"))
                .andExpect(jsonPath("$.data.engagementId").value(primaryEngagement.getId().toString()))
                .andExpect(jsonPath("$.data.clientId").value(primaryClient.getId().toString()))
                .andExpect(jsonPath("$.data.engagementTitle").value("Statutory Audit FY 2025-26"))
                .andExpect(jsonPath("$.data.workInstanceId").doesNotExist());
    }

    @Test
    @DisplayName("Should create work instance task and authoritatively inherit engagement relationship")
    void testCreateWorkInstanceTask() throws Exception {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .workInstanceId(primaryWorkInstance.getId())
                .title("Vouch bank statements for Q1")
                .taskCategory(TaskEntity.TaskCategory.AUDIT)
                .priority(TaskEntity.TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Vouch bank statements for Q1"))
                .andExpect(jsonPath("$.data.workInstanceId").value(primaryWorkInstance.getId().toString()))
                .andExpect(jsonPath("$.data.engagementId").value(primaryEngagement.getId().toString()))
                .andExpect(jsonPath("$.data.clientId").value(primaryClient.getId().toString()));
    }

    @Test
    @DisplayName("Should reject task creation when clientId conflicts with engagement clientId")
    void testRejectMismatchedClientOnTaskCreation() throws Exception {
        CreateTaskRequest req = CreateTaskRequest.builder()
                .engagementId(primaryEngagement.getId())
                .clientId(secondaryClient.getId()) // Different client!
                .title("Conflicting client task")
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("clientId does not match")));
    }

    @Test
    @DisplayName("Should reject cross-tenant or nonexistent engagementId")
    void testRejectCrossTenantEngagementId() throws Exception {
        TenantContext.setTenantId(secondaryOrg.getId());
        EngagementEntity otherOrgEngagement = EngagementEntity.builder()
                .clientId(UUID.randomUUID())
                .name("Cross Tenant Engagement")
                .build();
        otherOrgEngagement.setOrganizationId(secondaryOrg.getId());
        otherOrgEngagement = engagementRepository.save(otherOrgEngagement);
        TenantContext.setTenantId(primaryOrg.getId());

        CreateTaskRequest req = CreateTaskRequest.builder()
                .engagementId(otherOrgEngagement.getId())
                .title("Unauthorized task")
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject cross-tenant or nonexistent workInstanceId")
    void testRejectCrossTenantWorkInstanceId() throws Exception {
        TenantContext.setTenantId(secondaryOrg.getId());
        WorkInstanceEntity otherOrgInstance = WorkInstanceEntity.builder()
                .engagementId(UUID.randomUUID())
                .title("Other Work Instance")
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusMonths(1))
                .build();
        otherOrgInstance.setOrganizationId(secondaryOrg.getId());
        otherOrgInstance = workInstanceRepository.save(otherOrgInstance);
        TenantContext.setTenantId(primaryOrg.getId());

        CreateTaskRequest req = CreateTaskRequest.builder()
                .workInstanceId(otherOrgInstance.getId())
                .title("Unauthorized work instance task")
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prevent modifying engagement or client on a task bound to a WorkInstance")
    void testPreventAlteringWorkInstanceTaskEngagement() throws Exception {
        TaskEntity workTask = TaskEntity.builder()
                .clientId(primaryClient.getId())
                .engagementId(primaryEngagement.getId())
                .workInstanceId(primaryWorkInstance.getId())
                .title("Protected work instance task")
                .taskCategory(TaskEntity.TaskCategory.AUDIT)
                .status(TaskEntity.TaskStatus.TODO)
                .build();
        workTask.setOrganizationId(primaryOrg.getId());
        taskRepository.save(workTask);

        UUID differentEngagementId = UUID.randomUUID();
        UpdateTaskRequest updateReq = UpdateTaskRequest.builder()
                .engagementId(differentEngagementId)
                .build();

        mockMvc.perform(put("/api/v1/tasks/" + workTask.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot change the engagement of a task generated from a work instance")));
    }

    @Test
    @DisplayName("Should retrieve all tasks associated with an engagement via GET /api/v1/engagements/{id}/tasks")
    void testGetTasksByEngagementId() throws Exception {
        // Create 2 tasks for primaryEngagement (1 direct, 1 workInstance)
        TaskEntity t1 = TaskEntity.builder()
                .clientId(primaryClient.getId())
                .engagementId(primaryEngagement.getId())
                .title("Direct engagement task")
                .taskCategory(TaskEntity.TaskCategory.AUDIT)
                .status(TaskEntity.TaskStatus.TODO)
                .build();
        t1.setOrganizationId(primaryOrg.getId());
        taskRepository.save(t1);

        TaskEntity t2 = TaskEntity.builder()
                .clientId(primaryClient.getId())
                .engagementId(primaryEngagement.getId())
                .workInstanceId(primaryWorkInstance.getId())
                .title("Work instance task")
                .taskCategory(TaskEntity.TaskCategory.AUDIT)
                .status(TaskEntity.TaskStatus.IN_PROGRESS)
                .build();
        t2.setOrganizationId(primaryOrg.getId());
        taskRepository.save(t2);

        // Create 1 standalone task (should not be in engagement tasks)
        TaskEntity standalone = TaskEntity.builder()
                .title("Standalone task")
                .taskCategory(TaskEntity.TaskCategory.OTHER)
                .status(TaskEntity.TaskStatus.TODO)
                .build();
        standalone.setOrganizationId(primaryOrg.getId());
        taskRepository.save(standalone);

        mockMvc.perform(get("/api/v1/engagements/" + primaryEngagement.getId() + "/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].engagementId").value(primaryEngagement.getId().toString()))
                .andExpect(jsonPath("$.data[1].engagementId").value(primaryEngagement.getId().toString()));
    }
}
