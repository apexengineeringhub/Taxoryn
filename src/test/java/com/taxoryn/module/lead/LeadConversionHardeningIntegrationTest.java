package com.taxoryn.module.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.lead.dto.ConvertPracticeLeadRequest;
import com.taxoryn.module.lead.entity.PracticeLeadEntity;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.LeadPriority;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.LeadSource;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.LeadStatus;
import com.taxoryn.module.lead.repository.PracticeLeadActivityRepository;
import com.taxoryn.module.lead.repository.PracticeLeadRepository;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LeadConversionHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PracticeLeadRepository leadRepository;

    @Autowired
    private PracticeLeadActivityRepository activityRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired(required = false)
    private ClientContactRepository clientContactRepository;

    @Autowired(required = false)
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuditLogRepository auditLogRepository;

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

        // Org A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Lead Conversion Org A " + UUID.randomUUID())
                .legalName("Lead Conversion A LLP")
                .email("admin." + UUID.randomUUID() + "@leadtesta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin." + UUID.randomUUID() + "@leadtesta.com")
                .firstName("Admin")
                .lastName("A")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        employeeA = EmployeeEntity.builder()
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 8))
                .firstName("Senior")
                .lastName("Practitioner")
                .email("practitioner." + UUID.randomUUID() + "@leadtesta.com")
                .joiningDate(java.time.LocalDate.now())
                .userId(adminUserA.getId())
                .build();
        employeeA.setOrganizationId(orgA.getId());
        employeeA = employeeRepository.save(employeeA);

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE", "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE")
        );

        // Org B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Lead Conversion Org B " + UUID.randomUUID())
                .legalName("Lead Conversion B LLP")
                .email("admin." + UUID.randomUUID() + "@leadtestb.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@leadtestb.com")
                .firstName("Admin")
                .lastName("B")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE", "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE")
        );
    }

    private void cleanUp() {
        if (clientContactRepository != null) clientContactRepository.deleteAll();
        if (clientServiceRepository != null) clientServiceRepository.deleteAll();
        activityRepository.deleteAll();
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
    @DisplayName("Should successfully convert a qualified lead to client with onboarding status and preserve history")
    void testSuccessfulLeadToClientConversion() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        PracticeLeadEntity lead = PracticeLeadEntity.builder()
                .leadType(PracticeLeadEntity.LeadType.BUSINESS)
                .name("Vikram Malhotra")
                .businessName("Malhotra Technologies Pvt Ltd")
                .email("vikram@malhotratech.com")
                .phone("+919876543210")
                .source(LeadSource.REFERRAL)
                .status(LeadStatus.QUALIFIED)
                .priority(LeadPriority.HIGH)
                .assignedEmployeeId(employeeA.getId())
                .build();
        lead.setOrganizationId(orgA.getId());
        lead = leadRepository.save(lead);

        ConvertPracticeLeadRequest request = new ConvertPracticeLeadRequest();
        CreateClientRequest clientReq = new CreateClientRequest();
        clientReq.setClientType(ClientType.PRIVATE_LIMITED);
        clientReq.setDisplayName("Malhotra Technologies Pvt Ltd");
        clientReq.setLegalName("Malhotra Technologies Private Limited");
        clientReq.setEmail("contact@malhotratech.com");
        clientReq.setPhone("+919876543210");
        request.setClient(clientReq);

        String response = mockMvc.perform(post("/api/v1/leads/" + lead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                .andExpect(jsonPath("$.data.convertedClientId", notNullValue()))
                .andExpect(jsonPath("$.data.convertedAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String clientIdStr = objectMapper.readTree(response).path("data").path("convertedClientId").asText();
        UUID clientId = UUID.fromString(clientIdStr);

        // Verify client in database
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, orgA.getId()).orElse(null);
        assertNotNull(client);
        assertEquals("Malhotra Technologies Pvt Ltd", client.getDisplayName());
        assertEquals(ClientStatus.ONBOARDING, client.getStatus());
        assertEquals(ClientType.PRIVATE_LIMITED, client.getClientType());

        // Verify lead entity in database
        PracticeLeadEntity updatedLead = leadRepository.findByIdAndOrganizationId(lead.getId(), orgA.getId()).orElse(null);
        assertNotNull(updatedLead);
        assertEquals(LeadStatus.CONVERTED, updatedLead.getStatus());
        assertEquals(clientId, updatedLead.getConvertedClientId());
        assertNotNull(updatedLead.getConvertedAt());
        assertNotNull(updatedLead.getConvertedBy());
    }

    @Test
    @DisplayName("Should be idempotent: repeated conversion calls return existing converted client without duplicates")
    void testIdempotentRepeatedConversion() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        PracticeLeadEntity lead = PracticeLeadEntity.builder()
                .leadType(PracticeLeadEntity.LeadType.INDIVIDUAL)
                .name("Sunita Rao")
                .email("sunita.rao@example.com")
                .phone("+919811223344")
                .source(LeadSource.WEBSITE)
                .status(LeadStatus.PROPOSAL_SENT)
                .priority(LeadPriority.MEDIUM)
                .build();
        lead.setOrganizationId(orgA.getId());
        lead = leadRepository.save(lead);

        ConvertPracticeLeadRequest request = new ConvertPracticeLeadRequest();
        CreateClientRequest clientReq = new CreateClientRequest();
        clientReq.setDisplayName("Sunita Rao");
        clientReq.setClientType(ClientType.INDIVIDUAL);
        clientReq.setEmail("sunita.rao@example.com");
        request.setClient(clientReq);

        // 1st conversion call
        String firstResponse = mockMvc.perform(post("/api/v1/leads/" + lead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                .andExpect(jsonPath("$.data.convertedClientId", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String firstClientId = objectMapper.readTree(firstResponse).path("data").path("convertedClientId").asText();
        long initialClientCount = clientRepository.count();

        // 2nd idempotent conversion call
        String secondResponse = mockMvc.perform(post("/api/v1/leads/" + lead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                .andExpect(jsonPath("$.data.convertedClientId").value(firstClientId))
                .andReturn().getResponse().getContentAsString();

        // 3rd idempotent conversion call with empty payload
        mockMvc.perform(post("/api/v1/leads/" + lead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                .andExpect(jsonPath("$.data.convertedClientId").value(firstClientId));

        // Ensure no duplicate clients were created
        long finalClientCount = clientRepository.count();
        assertEquals(initialClientCount, finalClientCount, "Idempotent conversion should not create duplicate clients");
    }

    @Test
    @DisplayName("Should reject conversion if lead is in non-eligible status (NEW, CONTACTED, LOST)")
    void testConversionRejectionForNonEligibleStatuses() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // 1. NEW status
        PracticeLeadEntity newLead = PracticeLeadEntity.builder()
                .name("New Inquirer")
                .status(LeadStatus.NEW)
                .build();
        newLead.setOrganizationId(orgA.getId());
        newLead = leadRepository.save(newLead);

        mockMvc.perform(post("/api/v1/leads/" + newLead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // 2. CONTACTED status
        PracticeLeadEntity contactedLead = PracticeLeadEntity.builder()
                .name("Contacted Inquirer")
                .status(LeadStatus.CONTACTED)
                .build();
        contactedLead.setOrganizationId(orgA.getId());
        contactedLead = leadRepository.save(contactedLead);

        mockMvc.perform(post("/api/v1/leads/" + contactedLead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // 3. LOST status
        PracticeLeadEntity lostLead = PracticeLeadEntity.builder()
                .name("Lost Lead")
                .status(LeadStatus.LOST)
                .lostReason("Budget constraints")
                .build();
        lostLead.setOrganizationId(orgA.getId());
        lostLead = leadRepository.save(lostLead);

        mockMvc.perform(post("/api/v1/leads/" + lostLead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle duplicate PAN conflict safely with 409 Conflict")
    void testDuplicatePanConflict() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Create an existing client with PAN
        ClientEntity existingClient = ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Existing Client")
                .pan("ABCDE1234F")
                .status(ClientStatus.ACTIVE)
                .build();
        existingClient.setOrganizationId(orgA.getId());
        clientRepository.save(existingClient);

        // Create a lead to convert
        PracticeLeadEntity lead = PracticeLeadEntity.builder()
                .name("Duplicate PAN Lead")
                .status(LeadStatus.QUALIFIED)
                .build();
        lead.setOrganizationId(orgA.getId());
        lead = leadRepository.save(lead);

        ConvertPracticeLeadRequest request = new ConvertPracticeLeadRequest();
        CreateClientRequest clientReq = new CreateClientRequest();
        clientReq.setDisplayName("Duplicate PAN Client");
        clientReq.setClientType(ClientType.INDIVIDUAL);
        clientReq.setPan("ABCDE1234F");
        request.setClient(clientReq);

        mockMvc.perform(post("/api/v1/leads/" + lead.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should enforce strict tenant isolation on conversion across organizations")
    void testTenantIsolationOnConversion() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        PracticeLeadEntity leadOrgA = PracticeLeadEntity.builder()
                .name("Org A Confidential Lead")
                .status(LeadStatus.QUALIFIED)
                .build();
        leadOrgA.setOrganizationId(orgA.getId());
        leadOrgA = leadRepository.save(leadOrgA);

        // Org B tries to convert Org A's lead -> 404 Not Found
        mockMvc.perform(post("/api/v1/leads/" + leadOrgA.getId() + "/convert")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
