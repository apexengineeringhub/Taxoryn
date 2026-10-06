package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.*;
import com.taxoryn.module.client.entity.*;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.*;
import com.taxoryn.module.client.service.*;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.lead.dto.ConvertPracticeLeadRequest;
import com.taxoryn.module.lead.dto.PracticeLeadRequest;
import com.taxoryn.module.lead.entity.PracticeLeadEntity;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.LeadPriority;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.LeadSource;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.LeadStatus;
import com.taxoryn.module.lead.repository.PracticeLeadActivityRepository;
import com.taxoryn.module.lead.repository.PracticeLeadRepository;
import com.taxoryn.module.lead.service.PracticeLeadService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Phase28FinalIntegrationAndFreezeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Autowired private PracticeLeadRepository leadRepository;
    @Autowired private PracticeLeadActivityRepository leadActivityRepository;
    @Autowired private PracticeLeadService leadService;

    @Autowired private ClientRepository clientRepository;
    @Autowired private ClientContactRepository clientContactRepository;
    @Autowired private ClientBranchRepository clientBranchRepository;
    @Autowired private ClientRelationshipRepository clientRelationshipRepository;
    @Autowired private ClientServiceRepository clientServiceRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Autowired private ClientService clientService;
    @Autowired private ClientLifecycleService clientLifecycleService;
    @Autowired private ClientContactService clientContactService;
    @Autowired private ClientBranchService clientBranchService;
    @Autowired private ClientRelationshipService clientRelationshipService;
    @Autowired private ClientEngagementService clientEngagementService;
    @Autowired private ClientContextService clientContextService;
    @Autowired private ClientTimelineService clientTimelineService;
    @Autowired private ClientIntelligenceService clientIntelligenceService;

    private OrganizationEntity orgA;
    private UserEntity adminUserA;
    private EmployeeEntity employeeA;
    private String adminTokenA;

    private OrganizationEntity orgB;
    private UserEntity adminUserB;
    private String adminTokenB;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantContext.clear();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Chartered Accountants " + UUID.randomUUID())
                .legalName("Apex CA LLP")
                .email("admin." + UUID.randomUUID() + "@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("partner." + UUID.randomUUID() + "@apexca.com")
                .firstName("Rajesh")
                .lastName("Verma")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        employeeA = EmployeeEntity.builder()
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 8))
                .firstName("Rajesh")
                .lastName("Verma")
                .email("partner." + UUID.randomUUID() + "@apexca.com")
                .joiningDate(LocalDate.now())
                .userId(adminUserA.getId())
                .build();
        employeeA.setOrganizationId(orgA.getId());
        employeeA = employeeRepository.save(employeeA);

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN", "PARTNER"),
                Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE",
                        "CLIENT_VIEW", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_UPDATE", "CLIENT_DELETE",
                        "CLIENT_SERVICES_MANAGE", "BILLING_VIEW", "DOCUMENTS_VIEW")
        );

        // Tenant B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beacon Tax Advisory " + UUID.randomUUID())
                .legalName("Beacon Tax Advisory LLP")
                .email("admin." + UUID.randomUUID() + "@beacontax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("partner." + UUID.randomUUID() + "@beacontax.com")
                .firstName("Suresh")
                .lastName("Mehta")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE",
                        "CLIENT_VIEW", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_UPDATE", "CLIENT_DELETE")
        );
    }

    private void cleanUp() {
        clientRelationshipRepository.deleteAll();
        clientBranchRepository.deleteAll();
        clientContactRepository.deleteAll();
        clientServiceRepository.deleteAll();
        leadActivityRepository.deleteAll();
        leadRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Master Journey: Lead Creation → Qualification → Conversion → Onboarding → Profile → Contacts → Branches → Services → Context → Timeline → Intelligence → Client 360")
    void testCompletePhase28EndToEndBusinessJourney() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // 1. LEAD CREATION (NEW)
        PracticeLeadRequest leadReq = new PracticeLeadRequest();
        leadReq.setLeadType(PracticeLeadEntity.LeadType.BUSINESS);
        leadReq.setName("Anil Kothari");
        leadReq.setBusinessName("Kothari Infotech Pvt Ltd");
        leadReq.setEmail("anil@kotharitech.com");
        leadReq.setPhone("+919820011223");
        leadReq.setSource(LeadSource.REFERRAL);
        leadReq.setStatus(LeadStatus.NEW);
        leadReq.setPriority(LeadPriority.HIGH);
        leadReq.setDescription("Looking for comprehensive GST and ITR compliance retainer.");

        String leadCreateResp = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();

        UUID leadId = UUID.fromString(objectMapper.readTree(leadCreateResp).path("data").path("id").asText());

        // 2. LEAD QUALIFICATION (NEW -> CONTACTED -> QUALIFIED)
        leadReq.setStatus(LeadStatus.CONTACTED);
        mockMvc.perform(put("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTACTED"));

        leadReq.setStatus(LeadStatus.QUALIFIED);
        leadReq.setAssignedEmployeeId(employeeA.getId());
        mockMvc.perform(put("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUALIFIED"));

        // 3. LEAD CONVERSION
        ConvertPracticeLeadRequest convertReq = new ConvertPracticeLeadRequest();
        CreateClientRequest clientCreateReq = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Kothari Infotech Pvt Ltd")
                .legalName("Kothari Infotech Private Limited")
                .tradeName("Kothari Tech")
                .pan("AABCK1234D")
                .gstin("27AABCK1234D1Z5")
                .email("finance@kotharitech.com")
                .phone("+919820011223")
                .addressLine1("Tower B, Technopark")
                .city("Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("400093")
                .assignedEmployeeId(employeeA.getId())
                .build();
        convertReq.setClient(clientCreateReq);

        String convertResp = mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convertReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                .andExpect(jsonPath("$.data.convertedClientId", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        UUID clientId = UUID.fromString(objectMapper.readTree(convertResp).path("data").path("convertedClientId").asText());

        // 4. VERIFY CLIENT CREATED IN ONBOARDING STATUS
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, orgA.getId()).orElseThrow();
        assertEquals(ClientStatus.ONBOARDING, client.getStatus());
        assertEquals(ClientType.PRIVATE_LIMITED, client.getClientType());
        assertEquals("AABCK1234D", client.getPan());

        // 5. CLIENT PROFILE COMPLETENESS (Phase 28.2)
        mockMvc.perform(get("/api/v1/clients/" + clientId + "/profile/completeness")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completionPercentage", greaterThan(50)))
                .andExpect(jsonPath("$.data.completedSections", notNullValue()));

        // 6. CONTACT MANAGEMENT (Phase 28.5)
        CreateClientContactRequest contactReq = CreateClientContactRequest.builder()
                .firstName("Anil")
                .lastName("Kothari")
                .displayName("Anil Kothari")
                .designation("Managing Director")
                .email("anil@kotharitech.com")
                .phone("+919820011223")
                .contactRole(ContactRole.PRIMARY)
                .primaryContact(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/contacts")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primaryContact").value(true));

        // 7. BRANCH MANAGEMENT (Phase 28.5)
        CreateClientBranchRequest branchReq = CreateClientBranchRequest.builder()
                .branchName("Mumbai Head Office")
                .branchCode("HO-MUM")
                .branchType(ClientBranchType.REGISTERED_OFFICE)
                .addressLine1("Tower B, Technopark")
                .city("Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("400093")
                .primaryBranch(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/branches")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(branchReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primaryBranch").value(true));

        // 8. CLIENT SERVICE RELATIONSHIP (Phase 28.4)
        CreateClientServiceRequest serviceReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .agreedPrice(BigDecimal.valueOf(15000.00))
                .startDate(LocalDate.now())
                .billingFrequency("MONTHLY")
                .notes("Monthly GSTR-1 and GSTR-3B filings")
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/services")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.serviceType").value("GST_COMPLIANCE"));

        // 9. CLIENT LIFECYCLE TRANSITION: ONBOARDING -> ACTIVE (Phase 28.3)
        UpdateClientStatusRequest statusReq = new UpdateClientStatusRequest();
        statusReq.setStatus(ClientStatus.ACTIVE);
        statusReq.setReason("Onboarding verification and compliance setup completed.");

        mockMvc.perform(put("/api/v1/clients/" + clientId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 10. BUSINESS CONTEXT RESOLUTION (Phase 28.7)
        mockMvc.perform(get("/api/v1/business-context/clients/" + clientId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.data.client.displayName").value("Kothari Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.client.lifecycleStatus").value("ACTIVE"));

        // 11. CLIENT TIMELINE (Phase 28.6)
        mockMvc.perform(get("/api/v1/clients/" + clientId + "/timeline")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // 12. CLIENT INTELLIGENCE & RECOMMENDATIONS (Phase 28.6)
        mockMvc.perform(get("/api/v1/clients/" + clientId + "/intelligence")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.data.needsAttentionSignals").isArray())
                .andExpect(jsonPath("$.data.recommendations").isArray());

        // 13. CLIENT 360 CONNECTED VIEW (Phase 28.8)
        mockMvc.perform(get("/api/v1/clients/" + clientId + "/360")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.id").value(clientId.toString()))
                .andExpect(jsonPath("$.data.client.displayName").value("Kothari Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.client.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.services").isArray())
                .andExpect(jsonPath("$.data.keyContacts").isArray())
                .andExpect(jsonPath("$.data.keyBranches").isArray())
                .andExpect(jsonPath("$.data.intelligenceSummary").exists());
    }

    @Test
    @DisplayName("Verify Client Lifecycle State Machine Integrity & Disallowed Transitions")
    void testClientLifecycleStateMachine() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        CreateClientRequest createReq = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Lifecycle Test Individual")
                .status(ClientStatus.ONBOARDING)
                .build();

        String createResp = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID cid = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // 1. ONBOARDING -> ACTIVE (Valid)
        UpdateClientStatusRequest r1 = new UpdateClientStatusRequest();
        r1.setStatus(ClientStatus.ACTIVE);
        r1.setReason("Activated after verification");
        mockMvc.perform(put("/api/v1/clients/" + cid + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 2. ACTIVE -> SUSPENDED (Valid)
        UpdateClientStatusRequest r2 = new UpdateClientStatusRequest();
        r2.setStatus(ClientStatus.SUSPENDED);
        r2.setReason("Suspended due to audit non-compliance");
        mockMvc.perform(put("/api/v1/clients/" + cid + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // 3. SUSPENDED -> ACTIVE (Valid)
        UpdateClientStatusRequest r3 = new UpdateClientStatusRequest();
        r3.setStatus(ClientStatus.ACTIVE);
        r3.setReason("Re-activated after compliance resolution");
        mockMvc.perform(put("/api/v1/clients/" + cid + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. ACTIVE -> ARCHIVED (Valid)
        UpdateClientStatusRequest r4 = new UpdateClientStatusRequest();
        r4.setStatus(ClientStatus.ARCHIVED);
        r4.setReason("Client archived");
        mockMvc.perform(put("/api/v1/clients/" + cid + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // 5. ARCHIVED -> SUSPENDED (Invalid transition)
        UpdateClientStatusRequest r5 = new UpdateClientStatusRequest();
        r5.setStatus(ClientStatus.SUSPENDED);
        r5.setReason("Invalid transition from archived to suspended");
        mockMvc.perform(put("/api/v1/clients/" + cid + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify Strict Multi-Tenant Isolation across All Phase 28 Domains")
    void testCrossTenantIsolation() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        CreateClientRequest createReq = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Org A Confidential Client")
                .build();

        String createResp = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID clientAId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // Org B attempts to access Client A directly
        mockMvc.perform(get("/api/v1/clients/" + clientAId)
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // Org B attempts to access Client 360 of Client A
        mockMvc.perform(get("/api/v1/clients/" + clientAId + "/client-360")
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // Org B attempts to resolve Business Context of Client A
        mockMvc.perform(get("/api/v1/business-context")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .param("clientId", clientAId.toString()))
                .andExpect(status().isNotFound());
    }
}
