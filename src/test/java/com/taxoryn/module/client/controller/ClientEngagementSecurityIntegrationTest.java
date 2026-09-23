package com.taxoryn.module.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.CreateClientServiceRequest;
import com.taxoryn.module.client.dto.UpdateClientServiceRequest;
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
class ClientEngagementSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String adminToken1;
    private UserEntity practitionerUser1;
    private String practitionerToken1;
    private UserEntity practitionerUser2;
    private String practitionerToken2;
    private UserEntity clientPortalUser1;
    private String clientPortalToken1;
    private EmployeeEntity employee1;
    private EmployeeEntity employee2;
    private ClientEntity client1;
    private ClientEntity client2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        productModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .email("admin@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Tax Advisory")
                .email("admin@globaltax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

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

        RoleEntity clientPortalRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client User")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());

        // Admin User
        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "TASK_VIEW", "TASK_CREATE")
        );

        // Practitioner 1
        practitionerUser1 = userRepository.save(UserEntity.builder()
                .email("practitioner1@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Priya")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practitionerRole)))
                .build());

        practitionerToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerUser1.getId(),
                org1.getId(),
                practitionerUser1.getEmail(),
                Set.of("PRACTITIONER"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE")
        );

        // Practitioner 2
        practitionerUser2 = userRepository.save(UserEntity.builder()
                .email("practitioner2@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Karan")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practitionerRole)))
                .build());

        practitionerToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerUser2.getId(),
                org1.getId(),
                practitionerUser2.getEmail(),
                Set.of("PRACTITIONER"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE")
        );

        // Employees
        employee1 = employeeRepository.save(EmployeeEntity.builder()
                .userId(practitionerUser1.getId())
                .employeeCode("EMP-001")
                .firstName("Priya")
                .lastName("Patel")
                .email("priya@apextax.com")
                .department("Direct Tax")
                .designation("Tax Senior")
                .status(EmployeeStatus.ACTIVE)
                .build());

        employee2 = employeeRepository.save(EmployeeEntity.builder()
                .userId(practitionerUser2.getId())
                .employeeCode("EMP-002")
                .firstName("Karan")
                .lastName("Mehta")
                .department("Indirect Tax")
                .designation("GST Specialist")
                .status(EmployeeStatus.ACTIVE)
                .build());

        // Client 1 assigned to Employee 1
        client1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .tan("MUMZ12345A")
                .email("finance@zenithinfo.com")
                .phone("+919811122233")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build());

        // Client 2 assigned to Employee 2
        client2 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Rohan Deshmukh")
                .pan("ABCDE1234F")
                .email("rohan@gmail.com")
                .assignedEmployeeId(employee2.getId())
                .status(ClientStatus.ACTIVE)
                .build());

        // Client Portal User
        clientPortalUser1 = userRepository.save(UserEntity.builder()
                .email("clientportal@zenithinfo.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Zenith")
                .lastName("Client")
                .clientId(client1.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientPortalRole)))
                .build());

        clientPortalToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientPortalUser1.getId(),
                org1.getId(),
                clientPortalUser1.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of("CLIENT_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        productModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("1. GET /api/v1/client-services/catalog returns standard practice service master catalog")
    void testGetServiceCatalog_Success() throws Exception {
        mockMvc.perform(get("/api/v1/client-services/catalog")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(ClientServiceType.values().length))
                .andExpect(jsonPath("$.data[0].serviceType").exists())
                .andExpect(jsonPath("$.data[0].displayName").exists())
                .andExpect(jsonPath("$.data[0].category").exists());
    }

    @Test
    @DisplayName("2. POST /api/v1/clients/{clientId}/services creates new client service engagement")
    void testCreateClientService_Success() throws Exception {
        CreateClientServiceRequest req = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .assignedEmployeeId(employee1.getId())
                .billingFrequency("MONTHLY")
                .notes("Monthly GSTR-1 and GSTR-3B filings with 2B reconciliation")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceType").value("GST_COMPLIANCE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.assignedEmployeeId").value(employee1.getId().toString()))
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Priya Patel"))
                .andExpect(jsonPath("$.data.billingFrequency").value("MONTHLY"));

        // Verify audit trail entry
        TenantContext.setTenantId(org1.getId());
        List<AuditLogEntity> audits = auditLogRepository.findAll();
        assertThat(audits).anyMatch(a -> "CLIENT_SERVICE_CREATED".equals(a.getAction()));
        TenantContext.clear();
    }

    @Test
    @DisplayName("3. Duplicate active client service of same type is rejected")
    void testCreateDuplicateActiveClientService_Rejected() throws Exception {
        CreateClientServiceRequest req = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.ITR_COMPLIANCE)
                .startDate(LocalDate.now())
                .build();

        // First creation succeeds
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second creation of same active service fails with 400
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Practitioner can manage service for their assigned client, but is rejected for foreign client")
    void testPractitionerClientScope_Enforced() throws Exception {
        CreateClientServiceRequest req = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.TDS_COMPLIANCE)
                .startDate(LocalDate.now())
                .build();

        // Practitioner 1 has client1 assigned -> SUCCESS
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Practitioner 1 does NOT have client2 assigned -> 403 FORBIDDEN
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client2.getId())
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Cross-tenant client service creation and retrieval is strictly blocked")
    void testCrossTenantServiceAccess_Blocked() throws Exception {
        String otherOrgAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                org2.getId(),
                "admin@globaltax.com",
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE")
        );

        CreateClientServiceRequest req = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.TAX_NOTICE_MANAGEMENT)
                .startDate(LocalDate.now())
                .build();

        // Org 2 admin trying to add service to Org 1 client -> 404 NOT FOUND
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", otherOrgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        // Org 2 admin trying to list services for Org 1 client -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", otherOrgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Update and soft-deactivate (delete) client service lifecycle")
    void testUpdateAndDeactivateClientService() throws Exception {
        // 1. Create service
        CreateClientServiceRequest createReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.ACCOUNTING_BOOKKEEPING)
                .startDate(LocalDate.now())
                .billingFrequency("MONTHLY")
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String serviceId = objectMapper.readTree(responseStr).get("data").get("id").asText();

        // 2. Update service
        UpdateClientServiceRequest updateReq = UpdateClientServiceRequest.builder()
                .billingFrequency("QUARTERLY")
                .notes("Updated to quarterly ledger audit")
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/services/{serviceId}", client1.getId(), serviceId)
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingFrequency").value("QUARTERLY"))
                .andExpect(jsonPath("$.data.notes").value("Updated to quarterly ledger audit"));

        // 3. Patch status to SUSPENDED
        mockMvc.perform(patch("/api/v1/clients/{clientId}/services/{serviceId}/status", client1.getId(), serviceId)
                        .param("status", "SUSPENDED")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // 4. Soft-delete (deactivate) service
        mockMvc.perform(delete("/api/v1/clients/{clientId}/services/{serviceId}", client1.getId(), serviceId)
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk());

        // Verify status transitioned to INACTIVE
        mockMvc.perform(get("/api/v1/clients/{clientId}/services/{serviceId}", client1.getId(), serviceId)
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("7. Client Portal user cannot configure practice client services")
    void testClientPortalUser_CannotManageServices() throws Exception {
        CreateClientServiceRequest req = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .startDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", clientPortalToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Client 360 overview dynamically reflects configured client service engagements")
    void testClient360Overview_IncludesConfiguredServices() throws Exception {
        CreateClientServiceRequest req = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.AUDIT_ASSURANCE)
                .startDate(LocalDate.now())
                .notes("Statutory audit engagement under Companies Act 2013")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.services").isArray())
                .andExpect(jsonPath("$.data.services[?(@.serviceCode == 'AUDIT_ASSURANCE')].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.services[?(@.serviceCode == 'AUDIT_ASSURANCE')].summary").value("Statutory audit engagement under Companies Act 2013"));
    }
}
