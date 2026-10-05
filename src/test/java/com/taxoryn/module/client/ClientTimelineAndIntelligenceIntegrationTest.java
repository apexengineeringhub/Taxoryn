package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
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
import com.taxoryn.module.client.repository.ClientRelationshipRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.client.service.ClientContextService;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientTimelineAndIntelligenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private ClientContactRepository clientContactRepository;

    @Autowired
    private ClientBranchRepository clientBranchRepository;

    @Autowired
    private ClientRelationshipRepository clientRelationshipRepository;

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

    @Autowired
    private ClientContextService clientContextService;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String authToken1;
    private ClientEntity client1;
    private ClientEntity client2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        clientRelationshipRepository.deleteAll();
        clientContactRepository.deleteAll();
        clientBranchRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA & Advisors LLP")
                .legalName("Apex Chartered Accountants LLP")
                .email("admin@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Create Organization 2
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Tax Firm")
                .legalName("Other Tax Firm Pvt Ltd")
                .email("admin@othertax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());

        OrganizationModuleEntity mod = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build();
        mod.setOrganizationId(org1.getId());
        organizationModuleRepository.save(mod);

        // 4. Admin User
        adminUser1 = UserEntity.builder()
                .email("partner@apexca.com")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUser1.setOrganizationId(org1.getId());
        adminUser1 = userRepository.save(adminUser1);

        authToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE")
        );

        // Create Test Clients
        ClientEntity c1 = ClientEntity.builder()
                .displayName("Acme Global Infotech Pvt Ltd")
                .legalName("Acme Global Infotech Private Limited")
                .clientCode("CL-ACM001")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAACA1234A")
                .gstin("27AAACA1234A1Z5")
                .tan("PNEB12345C")
                .cin("U72900PN2020PTC123456")
                .email("finance@acmeglobal.com")
                .phone("9876543210")
                .city("Pune")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("411001")
                .businessActivity("Information Technology Software Services")
                .industry("IT / SaaS")
                .businessScale("MEDIUM")
                .dateOfIncorporation(LocalDate.of(2020, 1, 15))
                .build();
        c1.setOrganizationId(org1.getId());
        client1 = clientRepository.save(c1);

        // Create client in Org 2
        TenantContext.setTenantId(org2.getId());
        OrganizationModuleEntity mod2 = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build();
        mod2.setOrganizationId(org2.getId());
        organizationModuleRepository.save(mod2);

        ClientEntity c2 = ClientEntity.builder()
                .displayName("Other Org Client")
                .legalName("Other Org Client Pvt Ltd")
                .clientCode("CL-OTH001")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        c2.setOrganizationId(org2.getId());
        client2 = clientRepository.save(c2);

        TenantContext.setTenantId(org1.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Phase 28.6 - Timeline: Query normalized client timeline entries with actor and sorting")
    void testClientTimelineRetrievalAndOrdering() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // Insert audit logs
        Instant t1 = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant t2 = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant t3 = Instant.now().minus(1, ChronoUnit.HOURS);

        AuditLogEntity log1 = AuditLogEntity.builder()
                .organizationId(org1.getId())
                .userId(adminUser1.getId())
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(client1.getId().toString())
                .newValue("Acme Global Infotech Pvt Ltd created")
                .createdAt(t1)
                .build();
        auditLogRepository.save(log1);

        AuditLogEntity log2 = AuditLogEntity.builder()
                .organizationId(org1.getId())
                .userId(adminUser1.getId())
                .action("CLIENT_PROFILE_UPDATED")
                .entityType("CLIENT_PROFILE")
                .entityId(client1.getId().toString())
                .oldValue("PAN=null")
                .newValue("PAN=AAACA1234A")
                .createdAt(t2)
                .build();
        auditLogRepository.save(log2);

        AuditLogEntity log3 = AuditLogEntity.builder()
                .organizationId(org1.getId())
                .userId(adminUser1.getId())
                .action("CLIENT_SERVICE_CREATED")
                .entityType("CLIENT_SERVICE")
                .entityId(client1.getId().toString())
                .newValue("GST_COMPLIANCE service subscribed")
                .createdAt(t3)
                .build();
        auditLogRepository.save(log3);

        mockMvc.perform(get("/api/v1/clients/{clientId}/timeline", client1.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                // Top item is newest (t3)
                .andExpect(jsonPath("$.data.content[0].eventType").value("CLIENT_SERVICE_CREATED"))
                .andExpect(jsonPath("$.data.content[0].eventCategory").value("SERVICE"))
                .andExpect(jsonPath("$.data.content[0].actorName").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.data.content[0].severity").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].sourceModule").value("CLIENT_SERVICE"))
                // Second item (t2)
                .andExpect(jsonPath("$.data.content[1].eventType").value("CLIENT_PROFILE_UPDATED"))
                .andExpect(jsonPath("$.data.content[1].eventCategory").value("PROFILE"))
                // Third item (t1)
                .andExpect(jsonPath("$.data.content[2].eventType").value("CLIENT_CREATED"))
                .andExpect(jsonPath("$.data.content[2].eventCategory").value("CLIENT"));
    }

    @Test
    @DisplayName("Phase 28.6 - Timeline: Category and event type filtering")
    void testClientTimelineFiltering() throws Exception {
        TenantContext.setTenantId(org1.getId());

        auditLogRepository.save(AuditLogEntity.builder()
                .organizationId(org1.getId())
                .userId(adminUser1.getId())
                .action("CLIENT_CONTACT_CREATED")
                .entityType("CLIENT_CONTACT")
                .entityId(client1.getId().toString())
                .newValue("Contact added")
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build());

        auditLogRepository.save(AuditLogEntity.builder()
                .organizationId(org1.getId())
                .userId(adminUser1.getId())
                .action("CLIENT_SERVICE_CREATED")
                .entityType("CLIENT_SERVICE")
                .entityId(client1.getId().toString())
                .newValue("Service added")
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());

        // Filter by category CONTACT
        mockMvc.perform(get("/api/v1/clients/{clientId}/timeline", client1.getId())
                        .header("Authorization", authToken1)
                        .param("category", "CONTACT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].eventType").value("CLIENT_CONTACT_CREATED"))
                .andExpect(jsonPath("$.data.content[0].eventCategory").value("CONTACT"));

        // Filter by category SERVICE
        mockMvc.perform(get("/api/v1/clients/{clientId}/timeline", client1.getId())
                        .header("Authorization", authToken1)
                        .param("category", "SERVICE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].eventType").value("CLIENT_SERVICE_CREATED"));
    }

    @Test
    @DisplayName("Phase 28.6 - Timeline: Tenant isolation prevents cross-tenant timeline leaks")
    void testClientTimelineTenantIsolation() throws Exception {
        TenantContext.setTenantId(org2.getId());

        AuditLogEntity log = AuditLogEntity.builder()
                .organizationId(org2.getId())
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(client2.getId().toString())
                .newValue("Client 2 created")
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);

        TenantContext.setTenantId(org1.getId());

        // Org 1 tries to access Org 2's client timeline -> 404
        mockMvc.perform(get("/api/v1/clients/{clientId}/timeline", client2.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Phase 28.6 - Intelligence: Evaluates deterministic signals and next-best-action recommendations")
    void testDeterministicIntelligenceEvaluation() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // Client 1 currently has:
        // - completeness >= 80% (has PAN, GSTIN, TAN, CIN, email, phone, city, state, pincode, etc.)
        // - NO primary contact (0 contacts)
        // - NO primary branch (0 branches)
        // - NO active service (0 services)

        mockMvc.perform(get("/api/v1/clients/{clientId}/intelligence", client1.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.totalSignalsCount", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.highPrioritySignalsCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.needsAttentionSignals").isArray())
                .andExpect(jsonPath("$.data.recommendations").isArray());

        // Verify recommendations endpoint directly
        mockMvc.perform(get("/api/v1/clients/{clientId}/recommendations", client1.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].actionable").value(true))
                .andExpect(jsonPath("$.data[0].suggestedRoute", notNullValue()));
    }

    @Test
    @DisplayName("Phase 28.6 - Intelligence: Resolving items clears attention signals dynamically")
    void testIntelligenceSignalDynamicClearing() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // 1. Add primary contact
        ClientContactEntity contact = ClientContactEntity.builder()
                .clientId(client1.getId())
                .firstName("Aditya")
                .lastName("Kulkarni")
                .displayName("Aditya Kulkarni")
                .email("aditya@acmeglobal.com")
                .phone("9876500001")
                .contactRole(ContactRole.DIRECTOR)
                .primaryContact(true)
                .active(true)
                .build();
        contact.setOrganizationId(org1.getId());
        clientContactRepository.save(contact);

        // 2. Add primary branch
        ClientBranchEntity branch = ClientBranchEntity.builder()
                .clientId(client1.getId())
                .branchName("Pune Head Office")
                .branchCode("BR-PUN01")
                .branchType(ClientBranchType.REGISTERED_OFFICE)
                .addressLine1("Tech Park, Baner")
                .city("Pune")
                .state("Maharashtra")
                .stateCode("27")
                .country("India")
                .pincode("411045")
                .primaryBranch(true)
                .active(true)
                .build();
        branch.setOrganizationId(org1.getId());
        clientBranchRepository.save(branch);

        // 3. Add active service
        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        service.setOrganizationId(org1.getId());
        clientServiceRepository.save(service);

        // Re-evaluate intelligence: NO_PRIMARY_CONTACT, NO_PRIMARY_BRANCH, and NO_ACTIVE_SERVICE must be cleared!
        mockMvc.perform(get("/api/v1/clients/{clientId}/intelligence", client1.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.highPrioritySignalsCount").value(0));
    }

    @Test
    @DisplayName("Phase 28.6 - Intelligence: Suspended service raises high-priority attention signal")
    void testSuspendedServiceSignal() throws Exception {
        TenantContext.setTenantId(org1.getId());

        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceType(ClientServiceType.ITR_COMPLIANCE)
                .status(ClientServiceStatus.SUSPENDED)
                .statusChangeReason("Pending fee payment")
                .build();
        service.setOrganizationId(org1.getId());
        clientServiceRepository.save(service);

        mockMvc.perform(get("/api/v1/clients/{clientId}/intelligence", client1.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needsAttentionSignals[?(@.signalCode == 'SUSPENDED_SERVICE')]").exists())
                .andExpect(jsonPath("$.data.recommendations[?(@.actionType == 'RESUME_SERVICE')]").exists());
    }

    @Test
    @DisplayName("Phase 28.6 - Intelligence: Non-mutating evaluation produces zero side effects")
    void testIntelligenceEvaluationIsNonMutating() throws Exception {
        TenantContext.setTenantId(org1.getId());

        long auditCountBefore = auditLogRepository.count();
        ClientEntity beforeEntity = clientRepository.findById(client1.getId()).orElseThrow();

        // Perform intelligence evaluations multiple times
        mockMvc.perform(get("/api/v1/clients/{clientId}/intelligence", client1.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients/{clientId}/recommendations", client1.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isOk());

        long auditCountAfter = auditLogRepository.count();
        ClientEntity afterEntity = clientRepository.findById(client1.getId()).orElseThrow();

        // Zero audit logs generated during evaluation
        assertThat(auditCountAfter).isEqualTo(auditCountBefore);
        // Entity state remains completely unchanged
        assertThat(afterEntity.getUpdatedAt()).isEqualTo(beforeEntity.getUpdatedAt());
        assertThat(afterEntity.getVersion()).isEqualTo(beforeEntity.getVersion());
    }

    @Test
    @DisplayName("Phase 28.6 - Client 360 & Context: Unified read model contains intelligence summary & recent timeline")
    void testClient360AndContextIntegration() throws Exception {
        TenantContext.setTenantId(org1.getId());

        AuditLogEntity log = AuditLogEntity.builder()
                .organizationId(org1.getId())
                .userId(adminUser1.getId())
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(client1.getId().toString())
                .newValue("Client created")
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);

        // 1. Verify Client 360 response contains intelligence summary and recent timeline
        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.id").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.intelligenceSummary").isMap())
                .andExpect(jsonPath("$.data.intelligenceSummary.needsAttentionSignals").isArray())
                .andExpect(jsonPath("$.data.recentTimeline").isArray())
                .andExpect(jsonPath("$.data.recentTimeline", hasSize(1)))
                .andExpect(jsonPath("$.data.recentTimeline[0].eventType").value("CLIENT_CREATED"));

        // 2. Verify ClientContextSummaryDto contains attention signal counts
        ClientContextSummaryDto contextSummary = clientContextService.requireClientContext(org1.getId(), client1.getId());
        assertThat(contextSummary).isNotNull();
        assertThat(contextSummary.getAttentionSignalsCount()).isNotNull();
        assertThat(contextSummary.getAttentionSignalsCount()).isGreaterThan(0);
        assertThat(contextSummary.getHighPrioritySignalsCount()).isNotNull();
        assertThat(contextSummary.getHighPrioritySignalsCount()).isGreaterThan(0);
    }
}
