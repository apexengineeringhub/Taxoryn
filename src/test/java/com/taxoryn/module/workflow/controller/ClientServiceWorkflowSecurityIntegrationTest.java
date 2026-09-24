package com.taxoryn.module.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.workflow.dto.AssignWorkflowRequest;
import com.taxoryn.module.workflow.dto.CreateServicePeriodRequest;
import com.taxoryn.module.workflow.dto.GenerateWorkflowRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowPriorityRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStatusRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStepStatusRequest;
import com.taxoryn.module.workflow.entity.ClientServicePeriodEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowStepEntity;
import com.taxoryn.module.workflow.entity.ServiceWorkflowStepTemplateEntity;
import com.taxoryn.module.workflow.entity.ServiceWorkflowTemplateEntity;
import com.taxoryn.module.workflow.model.ServicePeriodType;
import com.taxoryn.module.workflow.model.ServiceWorkType;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import com.taxoryn.module.workflow.model.StepStatus;
import com.taxoryn.module.workflow.repository.ClientServicePeriodRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowStepRepository;
import com.taxoryn.module.workflow.repository.ServiceWorkflowStepTemplateRepository;
import com.taxoryn.module.workflow.repository.ServiceWorkflowTemplateRepository;
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
import java.util.List;
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
class ClientServiceWorkflowSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClientServiceWorkflowRepository workflowRepository;
    @Autowired private ClientServiceWorkflowStepRepository stepRepository;
    @Autowired private ClientServicePeriodRepository periodRepository;
    @Autowired private ServiceWorkflowTemplateRepository templateRepository;
    @Autowired private ServiceWorkflowStepTemplateRepository stepTemplateRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ClientServiceRepository clientServiceRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProductModuleRepository productModuleRepository;
    @Autowired private OrganizationModuleRepository organizationModuleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminUserA;
    private UserEntity practitionerUserA;
    private UserEntity adminUserB;
    private EmployeeEntity employeeA1;
    private EmployeeEntity employeeA2;
    private EmployeeEntity employeeB1;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private ClientEntity clientB1;
    private ClientServiceEntity serviceA1_GST;
    private String adminTokenA;
    private String practitionerTokenA;
    private String adminTokenB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        stepRepository.deleteAll();
        workflowRepository.deleteAll();
        periodRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        productModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organizations
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Advisory Org A")
                .email("admin@orga-wf.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Advisory Org B")
                .email("admin@orgb-wf.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup Modules for Org A
        TenantContext.setTenantId(orgA.getId());
        for (ProductModuleCode code : ProductModuleCode.values()) {
            if (productModuleRepository.findByCode(code).isEmpty()) {
                productModuleRepository.save(ProductModuleEntity.builder()
                        .code(code)
                        .name(code.name())
                        .category(ProductModuleCategory.TAX)
                        .status("ACTIVE")
                        .build());
            }
            organizationModuleRepository.save(OrganizationModuleEntity.builder()
                    .moduleCode(code)
                    .enabled(true)
                    .build());
        }
        TenantContext.clear();

        // 3. Create Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity practitionerRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTITIONER")
                .name("Practitioner")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Create Users
        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin@orga-wf.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Admin")
                .lastName("OrgA")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        practitionerUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("practitioner@orga-wf.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Practitioner")
                .lastName("One")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practitionerRole)))
                .build());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin@orgb-wf.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Admin")
                .lastName("OrgB")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        // 5. Create Employees
        TenantContext.setTenantId(orgA.getId());
        employeeA1 = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUserA.getId())
                .employeeCode("EMP-A01")
                .firstName("Admin")
                .lastName("OrgA")
                .email(adminUserA.getEmail())
                .status(EmployeeStatus.ACTIVE)
                .build());

        employeeA2 = employeeRepository.save(EmployeeEntity.builder()
                .userId(practitionerUserA.getId())
                .employeeCode("EMP-A02")
                .firstName("Practitioner")
                .lastName("One")
                .email(practitionerUserA.getEmail())
                .status(EmployeeStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());
        employeeB1 = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUserB.getId())
                .employeeCode("EMP-B01")
                .firstName("Admin")
                .lastName("OrgB")
                .email(adminUserB.getEmail())
                .status(EmployeeStatus.ACTIVE)
                .build());

        // 6. Create Clients & Services
        TenantContext.setTenantId(orgA.getId());
        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Client 1")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employeeA2.getId())
                .build());

        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Client 2")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employeeA1.getId())
                .build());

        serviceA1_GST = clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(clientA1.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .assignedEmployeeId(employeeA2.getId())
                .build());

        TenantContext.setTenantId(orgB.getId());
        clientB1 = clientRepository.save(ClientEntity.builder()
                .displayName("Beta Client 1")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employeeB1.getId())
                .build());

        TenantContext.clear();

        // 7. Seed GST Template if not present
        if (templateRepository.findBestTemplate(orgA.getId(), ClientServiceType.GST_COMPLIANCE).isEmpty()) {
            ServiceWorkflowTemplateEntity gstTemplate = templateRepository.save(ServiceWorkflowTemplateEntity.builder()
                    .serviceType(ClientServiceType.GST_COMPLIANCE)
                    .name("GST Monthly Workflow")
                    .isSystemDefault(true)
                    .active(true)
                    .build());

            stepTemplateRepository.save(ServiceWorkflowStepTemplateEntity.builder()
                    .workflowTemplateId(gstTemplate.getId())
                    .sequence(1)
                    .workType(ServiceWorkType.DATA_COLLECTION)
                    .name("Invoice Collection")
                    .mandatory(true)
                    .requiresClientInput(true)
                    .active(true)
                    .build());

            stepTemplateRepository.save(ServiceWorkflowStepTemplateEntity.builder()
                    .workflowTemplateId(gstTemplate.getId())
                    .sequence(2)
                    .workType(ServiceWorkType.PREPARATION)
                    .name("2B ITC Reconciliation")
                    .mandatory(true)
                    .active(true)
                    .build());
        }

        // 8. Generate JWT Tokens
        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE")
        );

        practitionerTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerUserA.getId(),
                orgA.getId(),
                practitionerUserA.getEmail(),
                Set.of("PRACTITIONER"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE")
        );

        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create period, generate workflow, update step status and record audit log")
    void testEndToEndWorkflowOperations() throws Exception {
        // 1. Create Service Period
        CreateServicePeriodRequest periodReq = CreateServicePeriodRequest.builder()
                .clientServiceId(serviceA1_GST.getId())
                .periodType(ServicePeriodType.MONTHLY)
                .periodLabel("September 2026")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 10, 20))
                .build();

        String periodResp = mockMvc.perform(post("/api/v1/client-services/" + serviceA1_GST.getId() + "/periods")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(periodReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.periodLabel").value("September 2026"))
                .andReturn().getResponse().getContentAsString();

        UUID periodId = UUID.fromString(objectMapper.readTree(periodResp).path("data").path("id").asText());

        // 2. Generate Workflow
        GenerateWorkflowRequest genReq = GenerateWorkflowRequest.builder()
                .clientServiceId(serviceA1_GST.getId())
                .periodId(periodId)
                .priority(TaskPriority.HIGH)
                .build();

        String wfResp = mockMvc.perform(post("/api/v1/client-services/" + serviceA1_GST.getId() + "/workflows/generate")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.steps.length()").isNumber())
                .andReturn().getResponse().getContentAsString();

        UUID workflowId = UUID.fromString(objectMapper.readTree(wfResp).path("data").path("id").asText());
        UUID step1Id = UUID.fromString(objectMapper.readTree(wfResp).path("data").path("steps").get(0).path("id").asText());

        // 3. Mark Step 1 Waiting for Client
        UpdateWorkflowStepStatusRequest waitReq = UpdateWorkflowStepStatusRequest.builder()
                .status(StepStatus.WAITING_FOR_CLIENT)
                .clientActionSummary("Waiting for September purchase invoice folder upload")
                .build();

        mockMvc.perform(patch("/api/v1/service-workflows/" + workflowId + "/steps/" + step1Id + "/status")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_FOR_CLIENT"));

        // Verify Workflow status changed to WAITING_FOR_CLIENT
        mockMvc.perform(get("/api/v1/service-workflows/" + workflowId)
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitingForClient").value(true))
                .andExpect(jsonPath("$.data.status").value("WAITING_FOR_CLIENT"));

        // 4. Verify Audit Logs
        List<AuditLogEntity> logs = auditLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "SERVICE_PERIOD_CREATED".equals(l.getAction()));
        assertThat(logs).anyMatch(l -> "SERVICE_WORKFLOW_CREATED".equals(l.getAction()));
        assertThat(logs).anyMatch(l -> "SERVICE_WORKFLOW_STEP_CHANGED".equals(l.getAction()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Org B cannot access Org A workflows")
    void testCrossTenantIsolation() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientServicePeriodEntity period = periodRepository.save(ClientServicePeriodEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .periodType(ServicePeriodType.MONTHLY)
                .periodLabel("August 2026")
                .build());

        ClientServiceWorkflowEntity workflow = workflowRepository.save(ClientServiceWorkflowEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .periodId(period.getId())
                .title("Org A Secret Workflow")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/service-workflows/" + workflow.getId())
                        .header("Authorization", adminTokenB))
                .andExpect(status().isNotFound());

        UpdateWorkflowPriorityRequest prioReq = UpdateWorkflowPriorityRequest.builder()
                .priority(TaskPriority.URGENT)
                .build();

        mockMvc.perform(patch("/api/v1/service-workflows/" + workflow.getId() + "/priority")
                        .header("Authorization", adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prioReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Client Portfolio Scoping: Practitioner A cannot access unassigned Client A2 workflow")
    void testClientPortfolioScoping() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientServiceEntity serviceA2 = clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(clientA2.getId())
                .serviceType(ClientServiceType.ITR_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .assignedEmployeeId(employeeA1.getId())
                .build());

        ClientServicePeriodEntity periodA2 = periodRepository.save(ClientServicePeriodEntity.builder()
                .clientId(clientA2.getId())
                .clientServiceId(serviceA2.getId())
                .periodType(ServicePeriodType.ANNUAL)
                .periodLabel("AY 2026-27")
                .build());

        ClientServiceWorkflowEntity workflowA2 = workflowRepository.save(ClientServiceWorkflowEntity.builder()
                .clientId(clientA2.getId())
                .clientServiceId(serviceA2.getId())
                .periodId(periodA2.getId())
                .title("ITR AY 2026-27")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .assignedEmployeeId(employeeA1.getId())
                .build());
        TenantContext.clear();

        // Practitioner A (only assigned to Client A1) tries to access Client A2 workflow
        mockMvc.perform(get("/api/v1/service-workflows/" + workflowA2.getId())
                        .header("Authorization", practitionerTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject cross-tenant employee assignment")
    void testRejectCrossTenantEmployeeAssignment() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientServicePeriodEntity period = periodRepository.save(ClientServicePeriodEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .periodType(ServicePeriodType.MONTHLY)
                .periodLabel("July 2026")
                .build());

        ClientServiceWorkflowEntity workflow = workflowRepository.save(ClientServiceWorkflowEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .periodId(period.getId())
                .title("GST July 2026")
                .status(ServiceWorkflowStatus.IN_PROGRESS)
                .build());
        TenantContext.clear();

        AssignWorkflowRequest assignReq = AssignWorkflowRequest.builder()
                .assignedEmployeeId(employeeB1.getId()) // Employee from Org B!
                .build();

        mockMvc.perform(patch("/api/v1/service-workflows/" + workflow.getId() + "/assignment")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isNotFound());
    }
}
