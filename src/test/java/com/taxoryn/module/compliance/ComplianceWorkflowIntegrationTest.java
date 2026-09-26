package com.taxoryn.module.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.CompleteComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.CreateComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.MarkWorkflowFiledRequest;
import com.taxoryn.module.compliance.dto.RequestWorkflowChangesRequest;
import com.taxoryn.module.compliance.dto.UpdateChecklistItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkflowStatusRequest;
import com.taxoryn.module.compliance.dto.WaitClientWorkflowRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceWorkflowRepository workflowRepository;

    @Autowired
    private ComplianceWorkflowChecklistItemRepository checklistItemRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity location;
    private UserEntity adminUser;
    private String adminToken;
    private ClientEntity client;
    private ComplianceObligationEntity obligation;

    @BeforeEach
    void setUp() {
        cleanUp();

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Tax Advisors LLP")
                .legalName("Zenith Tax Advisors LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("contact-" + UUID.randomUUID() + "@zenithtax.in")
                .build());

        LocationEntity loc = LocationEntity.builder()
                .name("Bengaluru Tech Office")
                .code("BLR-01")
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(org.getId());
        location = locationRepository.save(loc);

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin-" + UUID.randomUUID() + "@zenithtax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Zenith")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                org.getId(),
                adminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "GST_VIEW", "GST_EDIT")
        );

        ClientEntity clientEntity = ClientEntity.builder()
                .displayName("Nexus Infosystems")
                .legalName("Nexus Infosystems Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("NEXUS1234F")
                .gstin("29NEXUS1234F1Z5")
                .build();
        clientEntity.setOrganizationId(org.getId());
        client = clientRepository.save(clientEntity);

        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Filing for July 2026")
                .periodLabel("July 2026")
                .financialYear("2026-27")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .internalTargetDate(LocalDate.of(2026, 8, 17))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .locationId(location.getId())
                .build();
        ob.setOrganizationId(org.getId());
        obligation = obligationRepository.save(ob);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        checklistItemRepository.deleteAll();
        workflowRepository.deleteAll();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create generic compliance workflow and verify standard 9 checklist items are seeded")
    void testCreateGenericWorkflowWithChecklist() throws Exception {
        CreateComplianceWorkflowRequest request = CreateComplianceWorkflowRequest.builder()
                .complianceObligationId(obligation.getId())
                .workflowType("GST_3B_STANDARD")
                .priority(TaskPriority.HIGH)
                .assignedUserId(adminUser.getId())
                .locationId(location.getId())
                .notes("Standard filing flow")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/compliance/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.workflowType").value("GST_3B_STANDARD"))
                .andExpect(jsonPath("$.data.workflowStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.assignedUserId").value(adminUser.getId().toString()))
                .andExpect(jsonPath("$.data.locationId").value(location.getId().toString()))
                .andExpect(jsonPath("$.data.locationName").value("Bengaluru Tech Office"))
                .andReturn().getResponse().getContentAsString();

        UUID workflowId = UUID.fromString(objectMapper.readTree(responseJson).get("data").get("id").asText());

        // Verify checklist endpoint returns 9 items
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/checklist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[0].itemKey").value("DOCUMENTS_REQUESTED"))
                .andExpect(jsonPath("$.data[8].itemKey").value("ACKNOWLEDGEMENT_RECEIVED"));
    }

    @Test
    @DisplayName("Toggle checklist item completion and verify progress updates")
    void testChecklistItemToggleAndProgress() throws Exception {
        // Initialize workflow via obligation
        String wfJson = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workflowId = UUID.fromString(objectMapper.readTree(wfJson).get("data").get("id").asText());

        // Get checklist
        String clJson = mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/checklist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID firstItemId = UUID.fromString(objectMapper.readTree(clJson).get("data").get(0).get("id").asText());

        // Complete 1st item
        mockMvc.perform(patch("/api/v1/compliance/workflows/" + workflowId + "/checklist/" + firstItemId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateChecklistItemRequest.builder()
                                .isCompleted(true)
                                .notes("Books received via client portal")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));

        // Verify workflow detail progress reflects 1/9 = 11%
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflow.completedChecklistSteps").value(1))
                .andExpect(jsonPath("$.data.workflow.progressPercentage").value(11));
    }

    @Test
    @DisplayName("Complete maker-checker operational workflow cycle")
    void testMakerCheckerWorkflowCycle() throws Exception {
        // 1. Initialize
        String wfJson = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workflowId = UUID.fromString(objectMapper.readTree(wfJson).get("data").get("id").asText());

        // 2. Start
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"));

        // 3. Wait client
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/wait-client")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(WaitClientWorkflowRequest.builder()
                                .reason("Awaiting sales register for SEZ unit")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("WAITING_FOR_CLIENT"))
                .andExpect(jsonPath("$.data.waitingForClient").value(true));

        // 4. Resume
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/resume-client")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.waitingForClient").value(false));

        // 5. Submit for Review
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 6. Request Changes
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/request-changes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RequestWorkflowChangesRequest.builder()
                                .reason("Please re-check RCM liability on freight inward")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("CHANGES_REQUIRED"));

        // 7. Resubmit -> Submit review
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 8. Approve
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder()
                                .approvalNotes("RCM checked. Computation approved.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));

        // 9. Mark Filed
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .acknowledgementNumber("ARN-AA2908260012345")
                                .filedDate(LocalDate.now())
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("FILED"))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("ARN-AA2908260012345"));

        // 10. Complete
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompleteComplianceWorkflowRequest.builder()
                                .acknowledgementNumber("ARN-AA2908260012345")
                                .notes("Acknowledgement archived and sent to client")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("COMPLETED"));

        // Verify underlying obligation is now COMPLETED
        ComplianceObligationEntity updatedOb = obligationRepository.findById(obligation.getId()).orElseThrow();
        assertThat(updatedOb.getStatus()).isEqualTo(ComplianceObligationStatus.COMPLETED);
    }

    @Test
    @DisplayName("Generic status transition endpoint updates workflow and syncs obligation")
    void testGenericStatusUpdateEndpoint() throws Exception {
        String wfJson = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workflowId = UUID.fromString(objectMapper.readTree(wfJson).get("data").get("id").asText());

        // Update to IN_PROGRESS via generic PUT /status
        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateComplianceWorkflowStatusRequest.builder()
                                .status(ComplianceWorkflowStatus.IN_PROGRESS)
                                .reason("Direct status update test")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"));

        // Verify obligation synced to IN_PROGRESS
        ComplianceObligationEntity ob = obligationRepository.findById(obligation.getId()).orElseThrow();
        assertThat(ob.getStatus()).isEqualTo(ComplianceObligationStatus.IN_PROGRESS);
    }
}
