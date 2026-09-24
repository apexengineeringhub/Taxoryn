package com.taxoryn.module.compliance.controller;

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
import com.taxoryn.module.compliance.dto.AssignComplianceWorkRequest;
import com.taxoryn.module.compliance.dto.CreateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceWorkItemEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import com.taxoryn.module.compliance.entity.ComplianceWorkType;
import com.taxoryn.module.compliance.repository.ComplianceWorkItemRepository;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceWorkItemSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceWorkItemRepository workItemRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductModuleRepository productModuleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        workItemRepository.deleteAll();
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
                .email("admin@orga-comp.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Advisory Org B")
                .email("admin@orgb-comp.test")
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
                .email("admin@orga-comp.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Admin")
                .lastName("OrgA")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        practitionerUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("practitioner@orga-comp.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Practitioner")
                .lastName("One")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practitionerRole)))
                .build());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin@orgb-comp.test")
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

        // 6. Create Clients
        TenantContext.setTenantId(orgA.getId());
        // Client A1 is assigned to employeeA2 (practitionerUserA)
        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Client 1")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employeeA2.getId())
                .build());

        // Client A2 is assigned to admin only
        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Client 2")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employeeA1.getId())
                .build());

        // Client Services for Client A1
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

        // 7. Generate JWT Tokens
        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "COMPLIANCE_VIEW", "COMPLIANCE_MANAGE")
        );

        practitionerTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerUserA.getId(),
                orgA.getId(),
                practitionerUserA.getEmail(),
                Set.of("PRACTITIONER"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "COMPLIANCE_VIEW", "COMPLIANCE_MANAGE")
        );

        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "COMPLIANCE_VIEW", "COMPLIANCE_MANAGE")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create, retrieve, and update compliance work item successfully")
    void testCreateRetrieveUpdateComplianceWorkItem() throws Exception {
        CreateComplianceWorkItemRequest createReq = CreateComplianceWorkItemRequest.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-3B September 2026")
                .description("Monthly summary return preparation and filing")
                .financialYear("2026-27")
                .compliancePeriod("September 2026")
                .statutoryDueDate(LocalDate.of(2026, 10, 20))
                .internalTargetDate(LocalDate.of(2026, 10, 15))
                .assignedEmployeeId(employeeA2.getId())
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/compliance-work")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("GSTR-3B September 2026"))
                .andExpect(jsonPath("$.data.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.workType").value("GST_RETURN"))
                .andExpect(jsonPath("$.data.clientName").value("Alpha Client 1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID workItemId = UUID.fromString(objectMapper.readTree(responseStr).path("data").path("id").asText());

        // Verify direct GET by ID
        mockMvc.perform(get("/api/v1/compliance-work/" + workItemId)
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workItemId.toString()))
                .andExpect(jsonPath("$.data.assignedEmployeeName").isNotEmpty());

        // Update status to IN_PREPARATION
        UpdateComplianceWorkStatusRequest statusReq = UpdateComplianceWorkStatusRequest.builder()
                .status(ComplianceWorkStatus.IN_PREPARATION)
                .notes("Started working on 2B reconciliation")
                .build();

        mockMvc.perform(patch("/api/v1/compliance-work/" + workItemId + "/status")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PREPARATION"));

        // Verify Audit Log
        List<AuditLogEntity> logs = auditLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "COMPLIANCE_WORK_CREATED".equals(l.getAction()));
        assertThat(logs).anyMatch(l -> "COMPLIANCE_WORK_STATUS_CHANGED".equals(l.getAction()));
    }

    @Test
    @DisplayName("Should reject invalid status transition according to controlled lifecycle")
    void testInvalidStatusTransition() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ComplianceWorkItemEntity workItem = workItemRepository.save(ComplianceWorkItemEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-1 September 2026")
                .status(ComplianceWorkStatus.COMPLETED) // Terminal state
                .build());
        TenantContext.clear();

        UpdateComplianceWorkStatusRequest statusReq = UpdateComplianceWorkStatusRequest.builder()
                .status(ComplianceWorkStatus.IN_PREPARATION) // Invalid from COMPLETED
                .build();

        mockMvc.perform(patch("/api/v1/compliance-work/" + workItem.getId() + "/status")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cross-tenant protection: Org B cannot access Org A compliance work items")
    void testCrossTenantIsolation() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ComplianceWorkItemEntity workItem = workItemRepository.save(ComplianceWorkItemEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .workType(ComplianceWorkType.GST_RETURN)
                .title("Org A Secret Work Item")
                .status(ComplianceWorkStatus.NOT_STARTED)
                .build());
        TenantContext.clear();

        // Org B attempts to read Org A work item
        mockMvc.perform(get("/api/v1/compliance-work/" + workItem.getId())
                        .header("Authorization", adminTokenB))
                .andExpect(status().isNotFound());

        // Org B attempts to update Org A work item
        UpdateComplianceWorkItemRequest updateReq = UpdateComplianceWorkItemRequest.builder()
                .title("Tampered Title")
                .build();

        mockMvc.perform(put("/api/v1/compliance-work/" + workItem.getId())
                        .header("Authorization", adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Client Portfolio Scoping: Practitioner A cannot access unassigned Client A2 work item")
    void testClientPortfolioScoping() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        // Client Service for Client A2 (unassigned to practitioner A)
        ClientServiceEntity serviceA2 = clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(clientA2.getId())
                .serviceType(ClientServiceType.ITR_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .assignedEmployeeId(employeeA1.getId())
                .build());

        ComplianceWorkItemEntity workItemA2 = workItemRepository.save(ComplianceWorkItemEntity.builder()
                .clientId(clientA2.getId())
                .clientServiceId(serviceA2.getId())
                .workType(ComplianceWorkType.ITR_RETURN)
                .title("ITR-6 AY 2026-27")
                .status(ComplianceWorkStatus.NOT_STARTED)
                .assignedEmployeeId(employeeA1.getId())
                .build());
        TenantContext.clear();

        // Practitioner A (only assigned to Client A1) tries to access Client A2's work item
        mockMvc.perform(get("/api/v1/compliance-work/" + workItemA2.getId())
                        .header("Authorization", practitionerTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject cross-tenant employee assignment")
    void testCrossTenantEmployeeAssignment() throws Exception {
        CreateComplianceWorkItemRequest createReq = CreateComplianceWorkItemRequest.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-3B with Foreign Assignee")
                .assignedEmployeeId(employeeB1.getId()) // Employee belongs to Org B!
                .build();

        mockMvc.perform(post("/api/v1/compliance-work")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should retrieve compliance work items by client and by service")
    void testRetrieveByClientAndService() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        workItemRepository.save(ComplianceWorkItemEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-1 Q2")
                .status(ComplianceWorkStatus.NOT_STARTED)
                .statutoryDueDate(LocalDate.of(2026, 10, 11))
                .build());

        workItemRepository.save(ComplianceWorkItemEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(serviceA1_GST.getId())
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-3B Q2")
                .status(ComplianceWorkStatus.IN_PREPARATION)
                .statutoryDueDate(LocalDate.of(2026, 10, 20))
                .build());
        TenantContext.clear();

        // Get by Client
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/compliance-work")
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Get by Service
        mockMvc.perform(get("/api/v1/client-services/" + serviceA1_GST.getId() + "/compliance-work")
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
