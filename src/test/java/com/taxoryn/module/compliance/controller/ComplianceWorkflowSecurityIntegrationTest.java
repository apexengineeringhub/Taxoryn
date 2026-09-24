package com.taxoryn.module.compliance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.CompleteComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.MarkWorkflowFiledRequest;
import com.taxoryn.module.compliance.dto.UpdateChecklistItemRequest;
import com.taxoryn.module.compliance.dto.WaitClientWorkflowRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceWorkflowSecurityIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Tenant A
    private OrganizationEntity tenantA;
    private UserEntity tenantAAdminUser;
    private String tenantAAdminToken;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private EmployeeEntity employeeA;
    private UserEntity staffUserA;
    private String staffTokenA;

    // Tenant B
    private OrganizationEntity tenantB;
    private UserEntity tenantBAdminUser;
    private String tenantBAdminToken;
    private ClientEntity clientB1;

    @BeforeEach
    void setUp() {
        tearDown();

        // 1. Setup Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A CA Firm")
                .email("admin-a-" + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity adminRoleA = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        tenantAAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin@tenant-a-" + UUID.randomUUID() + ".com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("TenantA")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRoleA)))
                .build());

        tenantAAdminToken = jwtTokenProvider.generateAccessToken(
                tenantAAdminUser.getId(),
                tenantA.getId(),
                tenantAAdminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "CLIENT_VIEW", "CLIENT_EDIT", "GST_VIEW", "ITR_VIEW", "TDS_VIEW")
        );

        TenantContext.setTenantId(tenantA.getId());

        employeeA = employeeRepository.save(EmployeeEntity.builder()
                .employeeCode("EMP001")
                .firstName("Rajesh")
                .lastName("Sharma")
                .email("rajesh@tenant-a.com")
                .designation("Senior Associate")
                .department("Direct Tax")
                .status(EmployeeStatus.ACTIVE)
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        staffUserA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("staff@tenant-a-" + UUID.randomUUID() + ".com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Staff")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        employeeA.setUserId(staffUserA.getId());
        employeeRepository.save(employeeA);

        staffTokenA = jwtTokenProvider.generateAccessToken(
                staffUserA.getId(),
                tenantA.getId(),
                staffUserA.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "CLIENT_VIEW")
        );

        // Client A1 assigned to employeeA
        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Enterprises")
                .legalName("Alpha Enterprises Pvt Ltd")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAACB1234D")
                .assignedEmployeeId(employeeA.getId())
                .build());

        // Client A2 NOT assigned to employeeA
        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Beta Logistics")
                .legalName("Beta Logistics LLP")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .pan("BBBCB1234D")
                .build());

        // 2. Setup Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Practice")
                .email("admin-b-" + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenantBAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin@tenant-b-" + UUID.randomUUID() + ".com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("AdminB")
                .lastName("TenantB")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRoleA)))
                .build());

        tenantBAdminToken = jwtTokenProvider.generateAccessToken(
                tenantBAdminUser.getId(),
                tenantB.getId(),
                tenantBAdminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "CLIENT_VIEW", "CLIENT_EDIT", "GST_VIEW", "ITR_VIEW", "TDS_VIEW")
        );

        TenantContext.setTenantId(tenantB.getId());
        clientB1 = clientRepository.save(ClientEntity.builder()
                .displayName("Gamma Holdings")
                .legalName("Gamma Holdings Inc")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("CCCCB1234D")
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        checklistItemRepository.deleteAll();
        workflowRepository.deleteAll();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should initialize execution workflow with 9 checkpoints for an obligation")
    void testInitializeWorkflowForObligation() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ComplianceObligationEntity obligation = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B July 2026")
                .periodLabel("July 2026")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .internalTargetDate(LocalDate.of(2026, 8, 17))
                .status(ComplianceObligationStatus.READY)
                .priority(TaskPriority.HIGH)
                .build());
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.complianceObligationId").value(obligation.getId().toString()))
                .andExpect(jsonPath("$.data.workflowStatus").value("READY"))
                .andExpect(jsonPath("$.data.totalChecklistSteps").value(9))
                .andExpect(jsonPath("$.data.completedChecklistSteps").value(0))
                .andExpect(jsonPath("$.data.progressPercentage").value(0));
    }

    @Test
    @DisplayName("Staff without client assignment cannot access unassigned client workflow (403 Forbidden)")
    void testStaffWithoutClientAccess_Forbidden() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ComplianceObligationEntity unassignedObligation = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA2.getId()) // Not assigned to employeeA
                .obligationType(ComplianceObligationType.ITR_FILING)
                .title("ITR-6 AY 2026-27")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceObligationStatus.READY)
                .build());

        ComplianceWorkflowEntity workflow = workflowRepository.save(ComplianceWorkflowEntity.builder()
                .clientId(clientA2.getId())
                .complianceObligationId(unassignedObligation.getId())
                .workflowStatus(ComplianceWorkflowStatus.READY)
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .build());
        TenantContext.clear();

        // Staff attempts to access workflow for Client A2
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflow.getId())
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access Tenant A compliance workflows")
    void testCrossTenantIsolation() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ComplianceObligationEntity obligationA = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Secret")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .status(ComplianceObligationStatus.READY)
                .build());

        ComplianceWorkflowEntity workflowA = workflowRepository.save(ComplianceWorkflowEntity.builder()
                .clientId(clientA1.getId())
                .complianceObligationId(obligationA.getId())
                .workflowStatus(ComplianceWorkflowStatus.READY)
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .build());
        TenantContext.clear();

        // Tenant B Admin tries to access Tenant A's workflow
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowA.getId())
                        .header("Authorization", "Bearer " + tenantBAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Workbench summary returns scoped metrics")
    void testWorkbenchSummaryMetrics() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ComplianceObligationEntity obligation = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("GSTR-3B")
                .statutoryDueDate(LocalDate.now())
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build());

        workflowRepository.save(ComplianceWorkflowEntity.builder()
                .clientId(clientA1.getId())
                .complianceObligationId(obligation.getId())
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .targetDate(LocalDate.now())
                .statutoryDueDate(LocalDate.now())
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/compliance/workbench/summary")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActive").value(1))
                .andExpect(jsonPath("$.data.dueToday").value(1));
    }

    @Test
    @DisplayName("Workflow state transitions: START -> WAIT_CLIENT -> RESUME -> APPROVE -> FILED -> COMPLETE")
    void testWorkflowStateTransitions() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ComplianceObligationEntity obligation = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("TDS 26Q Q1")
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .status(ComplianceObligationStatus.READY)
                .build());

        ComplianceWorkflowEntity workflow = workflowRepository.save(ComplianceWorkflowEntity.builder()
                .clientId(clientA1.getId())
                .complianceObligationId(obligation.getId())
                .workflowStatus(ComplianceWorkflowStatus.READY)
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .build());
        TenantContext.clear();

        // 1. Start
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/start")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"));

        // 2. Wait for Client
        WaitClientWorkflowRequest waitReq = WaitClientWorkflowRequest.builder()
                .reason("Need 194C contractor invoices")
                .expectedResponseDate(LocalDate.of(2026, 7, 25))
                .build();
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/wait-client")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("WAITING_FOR_CLIENT"))
                .andExpect(jsonPath("$.data.waitingForClient").value(true));

        // 3. Resume
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/resume-client")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.waitingForClient").value(false));

        // 4. Submit Review
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/submit-review")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 5. Approve
        ApproveWorkflowRequest approveReq = ApproveWorkflowRequest.builder()
                .approvalNotes("Verified challan payments")
                .build();
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/approve")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));

        // 6. Mark Filed
        MarkWorkflowFiledRequest filedReq = MarkWorkflowFiledRequest.builder()
                .filedDate(LocalDate.of(2026, 7, 28))
                .acknowledgementNumber("PRN789012345")
                .build();
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/mark-filed")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("FILED"))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("PRN789012345"));

        // 7. Complete
        CompleteComplianceWorkflowRequest completeReq = CompleteComplianceWorkflowRequest.builder()
                .notes("TDS return filed and challan receipt saved")
                .build();
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflow.getId() + "/complete")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("COMPLETED"));
    }
}
