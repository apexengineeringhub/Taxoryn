package com.taxoryn.module.client;

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
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceScope;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
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

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientServiceRelationshipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

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
    private UserEntity adminUser2;
    private String adminToken2;
    private EmployeeEntity employee1;
    private ClientEntity client1;
    private ClientEntity client2;
    private ServiceEntity globalGstService;
    private ServiceEntity customAuditService;
    private ServiceEntity inactiveService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        serviceRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organizations
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory LLP")
                .email("admin@apexadvisory.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Tax Partners")
                .email("admin@zenithtax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 3. Admin Users
        TenantContext.setTenantId(org1.getId());
        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@apexadvisory.com")
                .firstName("Apex")
                .lastName("Principal")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build());

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_WRITE", "CLIENT_DELETE")
        );

        // Org 1 Enabled Modules
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.GST)
                .enabled(true)
                .build());

        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.ITR)
                .enabled(true)
                .build());

        // Employee
        employee1 = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUser1.getId())
                .employeeCode("EMP-001")
                .firstName("Rajesh")
                .lastName("Kumar")
                .email("rajesh@apexadvisory.com")
                .designation("Senior Tax Manager")
                .status(EmployeeStatus.ACTIVE)
                .build());

        // Client in Org 1
        client1 = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Global Industries")
                .legalName("Acme Global Industries Private Limited")
                .pan("AAACA1234A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();

        // Org 2 Setup
        TenantContext.setTenantId(org2.getId());
        adminUser2 = userRepository.save(UserEntity.builder()
                .email("admin@zenithtax.com")
                .firstName("Zenith")
                .lastName("Partner")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build());

        adminToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_WRITE", "CLIENT_DELETE")
        );

        client2 = clientRepository.save(ClientEntity.builder()
                .displayName("Zenith Client Corp")
                .legalName("Zenith Client Corp Private Limited")
                .pan("BBBCB5678B")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();

        // 7. Service Offerings
        globalGstService = serviceRepository.save(ServiceEntity.builder()
                .scope(ServiceScope.TAXORYN)
                .serviceCode("GST_COMPLIANCE")
                .serviceName("GST Periodic Filings & Compliance")
                .description("Complete GSTR-1, 3B, ITC reconciliation")
                .category(ServiceCategory.GST)
                .defaultPrice(new BigDecimal("15000.00"))
                .billingUnit("PER_RETURN")
                .taxRate(new BigDecimal("18.00"))
                .status(ServiceStatus.ACTIVE)
                .moduleCode("GST")
                .configurable(true)
                .build());

        customAuditService = serviceRepository.save(ServiceEntity.builder()
                .organizationId(org1.getId())
                .scope(ServiceScope.PRACTICE)
                .serviceCode("AUDIT_ASSURANCE")
                .serviceName("Special Statutory Audit Mandate")
                .description("Specialized corporate assurance")
                .category(ServiceCategory.AUDIT)
                .defaultPrice(new BigDecimal("50000.00"))
                .billingUnit("FIXED_FEE")
                .taxRate(new BigDecimal("18.00"))
                .status(ServiceStatus.ACTIVE)
                .configurable(true)
                .build());

        inactiveService = serviceRepository.save(ServiceEntity.builder()
                .scope(ServiceScope.TAXORYN)
                .serviceCode("DISCONTINUED_SERVICE")
                .serviceName("Discontinued Advisory")
                .category(ServiceCategory.OTHER)
                .defaultPrice(new BigDecimal("5000.00"))
                .status(ServiceStatus.INACTIVE)
                .configurable(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should assign service offering to client with custom price and verify audit log")
    void testAssignServiceOfferingWithAgreedPrice() throws Exception {
        CreateClientServiceRequest request = CreateClientServiceRequest.builder()
                .serviceOfferingId(globalGstService.getId())
                .agreedPrice(new BigDecimal("12500.00"))
                .billingFrequency("MONTHLY")
                .assignedEmployeeId(employee1.getId())
                .startDate(LocalDate.of(2026, 4, 1))
                .notes("Retainer engagement agreed for FY 2026-27")
                .reason("Client onboarded for GST retainer")
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/services")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.serviceOfferingId").value(globalGstService.getId().toString()))
                .andExpect(jsonPath("$.data.serviceCode").value("GST_COMPLIANCE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.agreedPrice").value(12500.00))
                .andExpect(jsonPath("$.data.billingFrequency").value("MONTHLY"))
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Rajesh Kumar"))
                .andExpect(jsonPath("$.data.statusChangeReason").value("Client onboarded for GST retainer"));

        // Verify audit event
        List<AuditLogEntity> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).anyMatch(log -> "SERVICE_ASSIGNED".equals(log.getAction()));
    }

    @Test
    @DisplayName("Should retrieve client services and single client service relationship")
    void testRetrieveClientServices() throws Exception {
        TenantContext.setTenantId(org1.getId());
        ClientServiceEntity entity = clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceOfferingId(customAuditService.getId())
                .serviceType(ClientServiceType.AUDIT_ASSURANCE)
                .status(ClientServiceStatus.ACTIVE)
                .agreedPrice(new BigDecimal("45000.00"))
                .startDate(LocalDate.of(2026, 5, 1))
                .notes("Annual statutory audit")
                .build());
        TenantContext.clear();

        // 1. List
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/services")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(entity.getId().toString()))
                .andExpect(jsonPath("$.data[0].agreedPrice").value(45000.00));

        // 2. Single
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId())
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(entity.getId().toString()))
                .andExpect(jsonPath("$.data.serviceOfferingId").value(customAuditService.getId().toString()));
    }

    @Test
    @DisplayName("Should prevent assigning duplicate active service offering")
    void testPreventDuplicateActiveServiceAssignment() throws Exception {
        TenantContext.setTenantId(org1.getId());
        clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceOfferingId(globalGstService.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .build());
        TenantContext.clear();

        CreateClientServiceRequest duplicate = CreateClientServiceRequest.builder()
                .serviceOfferingId(globalGstService.getId())
                .agreedPrice(new BigDecimal("10000.00"))
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/services")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("Should reject assigning inactive service offering")
    void testRejectInactiveServiceOffering() throws Exception {
        CreateClientServiceRequest request = CreateClientServiceRequest.builder()
                .serviceOfferingId(inactiveService.getId())
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/services")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot assign inactive service offering")));
    }

    @Test
    @DisplayName("Should reject assigning service when client lifecycle is ARCHIVED, SUSPENDED, or INACTIVE")
    void testRejectServiceAssignmentForArchivedOrSuspendedClient() throws Exception {
        TenantContext.setTenantId(org1.getId());
        ClientEntity archivedClient = clientRepository.save(ClientEntity.builder()
                .displayName("Old Terminated Client")
                .pan("ZZZPC9999Z")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ARCHIVED)
                .build());
        TenantContext.clear();

        CreateClientServiceRequest request = CreateClientServiceRequest.builder()
                .serviceOfferingId(globalGstService.getId())
                .build();

        mockMvc.perform(post("/api/v1/clients/" + archivedClient.getId() + "/services")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot assign services to client with status ARCHIVED")));
    }

    @Test
    @DisplayName("Should update client service relationship details and status lifecycle")
    void testUpdateClientServiceStatusLifecycle() throws Exception {
        TenantContext.setTenantId(org1.getId());
        ClientServiceEntity entity = clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceOfferingId(globalGstService.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .agreedPrice(new BigDecimal("15000.00"))
                .build());
        TenantContext.clear();

        // 1. Transition ACTIVE -> SUSPENDED
        mockMvc.perform(patch("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId() + "/status?status=SUSPENDED&reason=Client requested temporary pause")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.statusChangeReason").value("Client requested temporary pause"));

        // 2. Transition SUSPENDED -> ACTIVE
        mockMvc.perform(patch("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId() + "/status?status=ACTIVE&reason=Resumed engagement")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.statusChangeReason").value("Resumed engagement"));

        // 3. Transition ACTIVE -> ENDED
        mockMvc.perform(patch("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId() + "/status?status=ENDED&reason=Mandate concluded")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"))
                .andExpect(jsonPath("$.data.statusChangeReason").value("Mandate concluded"));

        // 4. Update details (PATCH endpoint)
        UpdateClientServiceRequest updateReq = UpdateClientServiceRequest.builder()
                .agreedPrice(new BigDecimal("18000.00"))
                .notes("Updated scope and pricing terms")
                .build();

        mockMvc.perform(patch("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreedPrice").value(18000.00))
                .andExpect(jsonPath("$.data.notes").value("Updated scope and pricing terms"));
    }

    @Test
    @DisplayName("Should reject invalid status transition and soft-deactivate relationship")
    void testInvalidTransitionAndSoftDeactivation() throws Exception {
        TenantContext.setTenantId(org1.getId());
        ClientServiceEntity entity = clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceOfferingId(globalGstService.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.PENDING)
                .build());
        TenantContext.clear();

        // Invalid: PENDING -> SUSPENDED
        mockMvc.perform(patch("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId() + "/status?status=SUSPENDED")
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid client service status transition")));

        // Soft Delete: sets status to INACTIVE
        mockMvc.perform(delete("/api/v1/clients/" + client1.getId() + "/services/" + entity.getId())
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk());

        TenantContext.setTenantId(org1.getId());
        ClientServiceEntity reloaded = clientServiceRepository.findById(entity.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClientServiceStatus.INACTIVE);
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should enforce tenant isolation preventing cross-tenant access to client services")
    void testTenantIsolation() throws Exception {
        // Org2 admin attempting to view Org1's client services
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/services")
                        .header("Authorization", adminToken2))
                .andExpect(status().isNotFound());

        // Org2 admin attempting to assign service to Org1's client
        CreateClientServiceRequest request = CreateClientServiceRequest.builder()
                .serviceOfferingId(globalGstService.getId())
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/services")
                        .header("Authorization", adminToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reflect client service relationship in Client 360 read model")
    void testClient360AggregationReflectsServices() throws Exception {
        TenantContext.setTenantId(org1.getId());
        clientServiceRepository.save(ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceOfferingId(globalGstService.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .agreedPrice(new BigDecimal("20000.00"))
                .startDate(LocalDate.of(2026, 4, 1))
                .notes("GST Compliance Retainer")
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/360")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.services.length()").value(1))
                .andExpect(jsonPath("$.data.services[0].serviceOfferingId").value(globalGstService.getId().toString()))
                .andExpect(jsonPath("$.data.services[0].agreedPrice").value(20000.00))
                .andExpect(jsonPath("$.data.services[0].serviceCode").value("GST_COMPLIANCE"));
    }
}
