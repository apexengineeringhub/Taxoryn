package com.taxoryn.module.client.businesscontext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.service.BusinessContextResolver;
import com.taxoryn.module.client.entity.ClientBranchEntity;
import com.taxoryn.module.client.entity.ClientBranchType;
import com.taxoryn.module.client.entity.ClientContactEntity;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.entity.ContactRole;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BusinessContextResolverIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessContextResolver businessContextResolver;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private ClientContactRepository clientContactRepository;

    @Autowired
    private ClientBranchRepository clientBranchRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanUp();

        // Create Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory Services " + UUID.randomUUID())
                .legalName("Apex Advisory Services LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("info-" + UUID.randomUUID() + "@apex.test")
                .country("IN")
                .build());

        // Create Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Consultants " + UUID.randomUUID())
                .legalName("Beta Tax Consultants Pvt Ltd")
                .status(OrganizationStatus.ACTIVE)
                .email("info-" + UUID.randomUUID() + "@beta.test")
                .country("IN")
                .build());

        for (ProductModuleCode code : ProductModuleCode.values()) {
            OrganizationModuleEntity modA = OrganizationModuleEntity.builder()
                    .moduleCode(code)
                    .enabled(true)
                    .build();
            modA.setOrganizationId(tenantA.getId());
            organizationModuleRepository.save(modA);

            OrganizationModuleEntity modB = OrganizationModuleEntity.builder()
                    .moduleCode(code)
                    .enabled(true)
                    .build();
            modB.setOrganizationId(tenantB.getId());
            organizationModuleRepository.save(modB);
        }

        RoleEntity adminRoleA = roleRepository.save(RoleEntity.builder()
                .code("ROLE_PRACTICE_ADMIN")
                .name("PRACTICE_ADMIN")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity adminRoleB = roleRepository.save(RoleEntity.builder()
                .code("ROLE_PRACTICE_ADMIN")
                .name("PRACTICE_ADMIN")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        userA = UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@apex.test")
                .passwordHash(passwordEncoder.encode("Test@123456"))
                .firstName("Aditya")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRoleA)))
                .build();
        userA.setOrganizationId(tenantA.getId());
        userA = userRepository.save(userA);

        userB = UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@beta.test")
                .passwordHash(passwordEncoder.encode("Test@123456"))
                .firstName("Bhavna")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRoleB)))
                .build();
        userB.setOrganizationId(tenantB.getId());
        userB = userRepository.save(userB);

        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                userA.getId(),
                tenantA.getId(),
                userA.getEmail(),
                Set.of("ROLE_PRACTICE_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE", "ENGAGEMENT_VIEW")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                userB.getId(),
                tenantB.getId(),
                userB.getEmail(),
                Set.of("ROLE_PRACTICE_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE", "ENGAGEMENT_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        TenantContext.clear();
        taskRepository.deleteAll();
        engagementRepository.deleteAll();
        clientBranchRepository.deleteAll();
        clientContactRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private ClientEntity createSampleClient(OrganizationEntity org, String name, String code) {
        ClientEntity client = ClientEntity.builder()
                .displayName(name)
                .legalName(name + " Private Limited")
                .clientCode(code)
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAACV" + (int)(1000 + Math.random() * 8999) + "C")
                .email("finance@" + code.toLowerCase() + ".test")
                .phone("+919876543210")
                .build();
        client.setOrganizationId(org.getId());
        return clientRepository.save(client);
    }

    @Test
    @DisplayName("1. Resolve client context with primary contact and branch via API and service")
    void testResolveClientContext() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Vertex Global Corp", "VRTX001");

        // Primary Contact
        ClientContactEntity contact = ClientContactEntity.builder()
                .clientId(client.getId())
                .firstName("Rajesh")
                .lastName("Kumar")
                .displayName("Rajesh Kumar")
                .email("rajesh@vertex.test")
                .phone("+919876500001")
                .contactRole(ContactRole.PRIMARY)
                .primaryContact(true)
                .active(true)
                .build();
        contact.setOrganizationId(tenantA.getId());
        clientContactRepository.save(contact);

        // Primary Branch
        ClientBranchEntity branch = ClientBranchEntity.builder()
                .clientId(client.getId())
                .branchName("Corporate Headquarters")
                .branchType(ClientBranchType.PRINCIPAL_PLACE_OF_BUSINESS)
                .gstin("27AAACV1234C1Z5")
                .city("Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .primaryBranch(true)
                .active(true)
                .build();
        branch.setOrganizationId(tenantA.getId());
        clientBranchRepository.save(branch);

        // Test programmatic service
        BusinessContextDto contextDto = businessContextResolver.resolveClientContext(client.getId());
        assertThat(contextDto).isNotNull();
        assertThat(contextDto.getOrganizationId()).isEqualTo(tenantA.getId());
        assertThat(contextDto.getClient()).isNotNull();
        assertThat(contextDto.getClient().getClientId()).isEqualTo(client.getId());
        assertThat(contextDto.getClient().getDisplayName()).isEqualTo("Vertex Global Corp");
        assertThat(contextDto.getClient().getPrimaryContact()).isNotNull();
        assertThat(contextDto.getClient().getPrimaryContact().getName()).isEqualTo("Rajesh Kumar");
        assertThat(contextDto.getClient().getPrimaryBranch()).isNotNull();
        assertThat(contextDto.getClient().getPrimaryBranch().getBranchName()).isEqualTo("Corporate Headquarters");

        // Test API endpoint
        mockMvc.perform(get("/api/v1/business-context/clients/" + client.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.client.displayName").value("Vertex Global Corp"))
                .andExpect(jsonPath("$.data.client.primaryContact.name").value("Rajesh Kumar"))
                .andExpect(jsonPath("$.data.client.primaryBranch.city").value("Mumbai"))
                .andExpect(jsonPath("$.data.temporalContext.currentDate").isNotEmpty())
                .andExpect(jsonPath("$.data.actorContext.currentUserEmail").value(userA.getEmail()));
    }

    @Test
    @DisplayName("2. Resolve service relationship context linked to client")
    void testResolveServiceContext() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Starlight Dynamics", "STR001");

        ClientServiceEntity serviceEntity = ClientServiceEntity.builder()
                .clientId(client.getId())
                .serviceOfferingId(UUID.randomUUID())
                .serviceType(ClientServiceType.GST)
                .status(ClientServiceStatus.ACTIVE)
                .agreedPrice(new BigDecimal("15000.00"))
                .billingFrequency("MONTHLY")
                .startDate(LocalDate.now().minusMonths(2))
                .endDate(LocalDate.now().plusMonths(10))
                .build();
        serviceEntity.setOrganizationId(tenantA.getId());
        serviceEntity = clientServiceRepository.save(serviceEntity);

        BusinessContextRequest request = BusinessContextRequest.builder()
                .clientId(client.getId())
                .serviceRelationshipId(serviceEntity.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.service.serviceRelationshipId").value(serviceEntity.getId().toString()))
                .andExpect(jsonPath("$.data.service.serviceName").value("GST Compliance & Returns"))
                .andExpect(jsonPath("$.data.service.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.service.rate").value(15000.00));
    }

    @Test
    @DisplayName("3. Reject service belonging to another client")
    void testRejectServiceBelongingToAnotherClient() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client1 = createSampleClient(tenantA, "Client Alpha", "ALP001");
        ClientEntity client2 = createSampleClient(tenantA, "Client Beta", "BET001");

        // Service belongs to client2
        ClientServiceEntity serviceEntity = ClientServiceEntity.builder()
                .clientId(client2.getId())
                .serviceOfferingId(UUID.randomUUID())
                .serviceType(ClientServiceType.ITR)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        serviceEntity.setOrganizationId(tenantA.getId());
        serviceEntity = clientServiceRepository.save(serviceEntity);

        // Requesting for client1 with client2's service
        BusinessContextRequest request = BusinessContextRequest.builder()
                .clientId(client1.getId())
                .serviceRelationshipId(serviceEntity.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Resolve valid engagement linked to client and service")
    void testResolveEngagementContext() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Zenith Healthcare", "ZNT001");

        ClientServiceEntity clientService = ClientServiceEntity.builder()
                .clientId(client.getId())
                .serviceOfferingId(UUID.randomUUID())
                .serviceType(ClientServiceType.TDS)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        clientService.setOrganizationId(tenantA.getId());
        clientService = clientServiceRepository.save(clientService);

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(client.getId())
                .serviceId(clientService.getServiceOfferingId())
                .clientServiceId(clientService.getId())
                .engagementCode("ENG-2026-001")
                .name("Q2 TDS Filing Engagement")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .assignedUserId(userA.getId())
                .build();
        engagement.setOrganizationId(tenantA.getId());
        engagement = engagementRepository.save(engagement);

        BusinessContextRequest request = BusinessContextRequest.builder()
                .clientId(client.getId())
                .serviceRelationshipId(clientService.getId())
                .engagementId(engagement.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.service.serviceRelationshipId").value(clientService.getId().toString()))
                .andExpect(jsonPath("$.data.engagement.engagementId").value(engagement.getId().toString()))
                .andExpect(jsonPath("$.data.engagement.engagementCode").value("ENG-2026-001"))
                .andExpect(jsonPath("$.data.engagement.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.engagement.active").value(true));
    }

    @Test
    @DisplayName("5. Reject engagement belonging to another client")
    void testRejectEngagementBelongingToAnotherClient() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client1 = createSampleClient(tenantA, "Client Alpha", "ALP001");
        ClientEntity client2 = createSampleClient(tenantA, "Client Beta", "BET001");

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(client2.getId())
                .serviceId(UUID.randomUUID())
                .name("Beta Client Audit")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.MEDIUM)
                .build();
        engagement.setOrganizationId(tenantA.getId());
        engagement = engagementRepository.save(engagement);

        // Requesting for client1 with client2's engagement
        BusinessContextRequest request = BusinessContextRequest.builder()
                .clientId(client1.getId())
                .engagementId(engagement.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Reject inconsistent engagement and serviceRelationshipId")
    void testRejectInconsistentServiceAndEngagement() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Client Matrix", "MTX001");

        ClientServiceEntity service1 = ClientServiceEntity.builder()
                .clientId(client.getId())
                .serviceOfferingId(UUID.randomUUID())
                .serviceType(ClientServiceType.GST)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        service1.setOrganizationId(tenantA.getId());
        service1 = clientServiceRepository.save(service1);

        ClientServiceEntity service2 = ClientServiceEntity.builder()
                .clientId(client.getId())
                .serviceOfferingId(UUID.randomUUID())
                .serviceType(ClientServiceType.ITR)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        service2.setOrganizationId(tenantA.getId());
        service2 = clientServiceRepository.save(service2);

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(client.getId())
                .serviceId(service1.getServiceOfferingId())
                .clientServiceId(service1.getId())
                .name("GST Return Engagement")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .build();
        engagement.setOrganizationId(tenantA.getId());
        engagement = engagementRepository.save(engagement);

        // Requesting engagement (which is linked to service1) with service2
        BusinessContextRequest request = BusinessContextRequest.builder()
                .clientId(client.getId())
                .serviceRelationshipId(service2.getId())
                .engagementId(engagement.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Resolve composite context (Client + Service + Engagement + Task)")
    void testResolveCompositeContext() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Apex Composite Client", "APX-COMP");

        ClientServiceEntity clientService = ClientServiceEntity.builder()
                .clientId(client.getId())
                .serviceOfferingId(UUID.randomUUID())
                .serviceType(ClientServiceType.AUDIT_ASSURANCE)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        clientService.setOrganizationId(tenantA.getId());
        clientService = clientServiceRepository.save(clientService);

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(client.getId())
                .serviceId(clientService.getServiceOfferingId())
                .clientServiceId(clientService.getId())
                .engagementCode("ENG-AUD-2026")
                .name("FY26 Statutory Audit")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .build();
        engagement.setOrganizationId(tenantA.getId());
        engagement = engagementRepository.save(engagement);

        TaskEntity task = TaskEntity.builder()
                .clientId(client.getId())
                .engagementId(engagement.getId())
                .title("Draft Management Representation Letter")
                .taskCategory(TaskCategory.AUDIT)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .assignedTo(userA.getId())
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        task.setOrganizationId(tenantA.getId());
        task = taskRepository.save(task);

        BusinessContextRequest request = BusinessContextRequest.builder()
                .clientId(client.getId())
                .serviceRelationshipId(clientService.getId())
                .engagementId(engagement.getId())
                .taskId(task.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.service.serviceRelationshipId").value(clientService.getId().toString()))
                .andExpect(jsonPath("$.data.engagement.engagementId").value(engagement.getId().toString()))
                .andExpect(jsonPath("$.data.work.taskId").value(task.getId().toString()))
                .andExpect(jsonPath("$.data.work.taskTitle").value("Draft Management Representation Letter"))
                .andExpect(jsonPath("$.data.work.taskStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.temporalContext.effectiveDueDate").isNotEmpty())
                .andExpect(jsonPath("$.data.attention").exists())
                .andExpect(jsonPath("$.data.actorContext.canAccessClient").value(true));
    }

    @Test
    @DisplayName("8. Deduce clientId automatically from taskId when clientId is omitted")
    void testDeduceClientIdFromTaskId() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Auto Resolved Client", "AUTO-001");

        TaskEntity task = TaskEntity.builder()
                .clientId(client.getId())
                .title("Verify GSTR-2B Input Tax Credit")
                .taskCategory(TaskCategory.GST)
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .build();
        task.setOrganizationId(tenantA.getId());
        task = taskRepository.save(task);

        // Omit clientId from request
        BusinessContextRequest request = BusinessContextRequest.builder()
                .taskId(task.getId())
                .build();

        mockMvc.perform(post("/api/v1/business-context/resolve")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.client.displayName").value("Auto Resolved Client"))
                .andExpect(jsonPath("$.data.work.taskId").value(task.getId().toString()))
                .andExpect(jsonPath("$.data.work.taskTitle").value("Verify GSTR-2B Input Tax Credit"));
    }

    @Test
    @DisplayName("9. Tenant isolation — Tenant B cannot resolve context for Tenant A's client")
    void testTenantIsolation() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity clientA = createSampleClient(tenantA, "Tenant A Private Client", "TNA001");

        // Tenant B caller trying to resolve Tenant A's client context
        mockMvc.perform(get("/api/v1/business-context/clients/" + clientA.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Non-mutating & zero audit log guarantee on context resolution")
    void testContextResolutionIsNonMutating() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientEntity client = createSampleClient(tenantA, "Pure Read Client", "READ001");

        long auditCountBefore = auditLogRepository.count();

        // Resolve context multiple times
        businessContextResolver.resolveClientContext(client.getId());
        businessContextResolver.resolveClientContext(client.getId());

        long auditCountAfter = auditLogRepository.count();

        // Zero mutation, 0 audit log emitted during resolution
        assertThat(auditCountAfter).isEqualTo(auditCountBefore);

        ClientEntity freshClient = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(freshClient.getDisplayName()).isEqualTo("Pure Read Client");
        assertThat(freshClient.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }
}
