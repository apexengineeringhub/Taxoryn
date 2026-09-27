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
import com.taxoryn.module.task.dto.AssignWorkItemRequest;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateWorkItemStatusRequest;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkItemIntegrationTest {

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

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locA;
    private UserEntity userA;
    private UserEntity userA2;
    private UserEntity userB;
    private ClientEntity clientA;
    private ComplianceObligationEntity obligationA;
    private ComplianceWorkflowEntity workflowA;
    private String tokenA;
    private String tokenB;

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

        // Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha CA Firm - " + UUID.randomUUID())
                .legalName("Alpha CA Firm LLP")
                .email("admin.alpha." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        LocationEntity loc = LocationEntity.builder()
                .name("HQ Office")
                .code("HQ-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(orgA.getId());
        locA = locationRepository.save(loc);

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("lead." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Lead")
                .lastName("Practitioner")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        userA2 = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("associate." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Associate")
                .lastName("One")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Acme Global Pvt Ltd")
                .legalName("Acme Global Technologies Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .pan("AACCA1234A")
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(c);

        ComplianceObligationEntity obl = ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B July 2026")
                .periodLabel("2026-07")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .status(ComplianceObligationStatus.UPCOMING)
                .locationId(locA.getId())
                .build();
        obl.setOrganizationId(orgA.getId());
        obligationA = obligationRepository.save(obl);

        ComplianceWorkflowEntity wf = ComplianceWorkflowEntity.builder()
                .clientId(clientA.getId())
                .complianceObligationId(obligationA.getId())
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .workflowType("GST_GSTR3B")
                .locationId(locA.getId())
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .build();
        wf.setOrganizationId(orgA.getId());
        workflowA = workflowRepository.save(wf);

        // Tenant B (Isolation Check)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Firm - " + UUID.randomUUID())
                .legalName("Beta Tax Firm LLP")
                .email("admin.beta." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@beta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE", "CLIENT_VIEW")
        );

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
    @DisplayName("Create Work Item linked to Client and Workflow and retrieve details")
    void testCreateAndGetWorkItem() throws Exception {
        CreateWorkItemRequest request = CreateWorkItemRequest.builder()
                .title("GSTR-3B Working Papers Verification")
                .description("Reconcile ITC with 2B auto-drafted statement and prepare tax liability workings")
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .workflowId(workflowA.getId())
                .priority(WorkItemPriority.HIGH)
                .assignedUserId(userA.getId())
                .dueDate(LocalDate.of(2026, 8, 18))
                .notes("Verify Rule 37A reversal before final computation")
                .build();

        String response = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("GSTR-3B Working Papers Verification"))
                .andExpect(jsonPath("$.data.clientId").value(clientA.getId().toString()))
                .andExpect(jsonPath("$.data.workflowId").value(workflowA.getId().toString()))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();

        UUID workItemId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/work-items/" + workItemId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(workItemId.toString()))
                .andExpect(jsonPath("$.data.clientName").value("Acme Global Pvt Ltd"))
                .andExpect(jsonPath("$.data.locationName").value("HQ Office"));
    }

    @Test
    @DisplayName("Update Work Item status to COMPLETED and verify completedAt timestamp")
    void testUpdateWorkItemStatus() throws Exception {
        CreateWorkItemRequest request = CreateWorkItemRequest.builder()
                .title("Reconcile TDS with 26AS")
                .clientId(clientA.getId())
                .build();

        String response = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workItemId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        UpdateWorkItemStatusRequest statusReq = UpdateWorkItemStatusRequest.builder()
                .status(WorkItemStatus.COMPLETED)
                .notes("All TDS challans verified against TRACES portal")
                .build();

        mockMvc.perform(patch("/api/v1/work-items/" + workItemId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.notes").value("All TDS challans verified against TRACES portal"));
    }

    @Test
    @DisplayName("Reassign Work Item to another user")
    void testAssignWorkItem() throws Exception {
        CreateWorkItemRequest request = CreateWorkItemRequest.builder()
                .title("Prepare Draft Financial Statements")
                .clientId(clientA.getId())
                .assignedUserId(userA.getId())
                .build();

        String response = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workItemId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        AssignWorkItemRequest assignReq = AssignWorkItemRequest.builder()
                .assignedUserId(userA2.getId())
                .notes("Reassigned to Associate One for initial schedule preparation")
                .build();

        mockMvc.perform(patch("/api/v1/work-items/" + workItemId + "/assign")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedUserId").value(userA2.getId().toString()))
                .andExpect(jsonPath("$.data.assignedUserName").value(userA2.getFullName()));
    }

    @Test
    @DisplayName("Retrieve Work Items by Client ID (Client 360) and Workflow ID")
    void testRetrieveWorkItemsByClientAndWorkflow() throws Exception {
        CreateWorkItemRequest req1 = CreateWorkItemRequest.builder()
                .title("Item 1 Workflow")
                .clientId(clientA.getId())
                .workflowId(workflowA.getId())
                .build();

        CreateWorkItemRequest req2 = CreateWorkItemRequest.builder()
                .title("Item 2 Standalone")
                .clientId(clientA.getId())
                .build();

        mockMvc.perform(post("/api/v1/work-items").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/work-items").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Client 360 endpoint
        mockMvc.perform(get("/api/v1/work-items/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Workflow endpoint
        mockMvc.perform(get("/api/v1/work-items/workflows/" + workflowA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Item 1 Workflow"));
    }

    @Test
    @DisplayName("Tenant Isolation: Tenant B cannot access Tenant A Work Item")
    void testTenantIsolation() throws Exception {
        CreateWorkItemRequest request = CreateWorkItemRequest.builder()
                .title("Confidential Internal Review")
                .clientId(clientA.getId())
                .build();

        String response = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workItemId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Tenant B attempts to read -> 404
        mockMvc.perform(get("/api/v1/work-items/" + workItemId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Tenant B attempts to update -> 404
        mockMvc.perform(put("/api/v1/work-items/" + workItemId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateWorkItemRequest.builder().title("Hijack").build())))
                .andExpect(status().isNotFound());
    }
}
