package com.taxoryn.module.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.lead.dto.PracticeLeadActivityRequest;
import com.taxoryn.module.lead.dto.PracticeLeadRequest;
import com.taxoryn.module.lead.entity.PracticeLeadActivityEntity.ActivityType;
import com.taxoryn.module.lead.entity.PracticeLeadEntity;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.*;
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

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PracticeLeadIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private UserEntity adminUserA;
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
                .name("Lead Test Org A " + UUID.randomUUID())
                .legalName("Lead Test A LLP")
                .email("admin." + UUID.randomUUID() + "@orga.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin." + UUID.randomUUID() + "@orga.com")
                .firstName("Lead")
                .lastName("AdminA")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN", "PRACTICE_ADMIN"),
                Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE", "CLIENT_VIEW")
        );

        // Org B
        TenantContext.clear();
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Lead Test Org B " + UUID.randomUUID())
                .legalName("Lead Test B LLP")
                .email("admin." + UUID.randomUUID() + "@orgb.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@orgb.com")
                .firstName("Lead")
                .lastName("AdminB")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN", "PRACTICE_ADMIN"),
                Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE", "CLIENT_VIEW")
        );
    }

    private void cleanUp() {
        activityRepository.deleteAll();
        leadRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should list leads when empty without server error")
    void testListLeadsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("Should create, retrieve, update, and add activity to a lead")
    void testLeadLifecycle() throws Exception {
        // 1. Create Lead
        PracticeLeadRequest createReq = new PracticeLeadRequest();
        createReq.setLeadType(LeadType.BUSINESS);
        createReq.setName("Acme Corp Lead");
        createReq.setBusinessName("Acme Solutions Pvt Ltd");
        createReq.setEmail("contact@acmecorp.com");
        createReq.setPhone("+919876543210");
        createReq.setSource(LeadSource.WEBSITE);
        createReq.setStatus(LeadStatus.NEW);
        createReq.setPriority(LeadPriority.HIGH);
        createReq.setInterestedServiceCode("GST_MONTHLY");
        createReq.setDescription("Interested in monthly GST compliance & advisory.");

        String createResponse = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("Acme Corp Lead"))
                .andExpect(jsonPath("$.data.businessName").value("Acme Solutions Pvt Ltd"))
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andReturn().getResponse().getContentAsString();

        String leadIdStr = objectMapper.readTree(createResponse).path("data").path("id").asText();
        UUID leadId = UUID.fromString(leadIdStr);

        // 2. Get Lead by ID
        mockMvc.perform(get("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(leadIdStr))
                .andExpect(jsonPath("$.data.name").value("Acme Corp Lead"))
                .andExpect(jsonPath("$.data.interestedServiceCode").value("GST_MONTHLY"));

        // 3. Update Lead
        createReq.setStatus(LeadStatus.QUALIFIED);
        createReq.setPriority(LeadPriority.URGENT);
        createReq.setDescription("Updated requirement description.");

        mockMvc.perform(put("/api/v1/leads/" + leadId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUALIFIED"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));

        // 4. Add Activity
        PracticeLeadActivityRequest actReq = new PracticeLeadActivityRequest();
        actReq.setActivityType(ActivityType.PHONE_CALL);
        actReq.setSubject("Initial Discovery Call");
        actReq.setContent("Discussed GST requirements and pricing structure.");
        actReq.setOccurredAt(Instant.now());

        mockMvc.perform(post("/api/v1/leads/" + leadId + "/activities")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activityType").value("PHONE_CALL"))
                .andExpect(jsonPath("$.data.subject").value("Initial Discovery Call"));

        // 5. Get Activities
        mockMvc.perform(get("/api/v1/leads/" + leadId + "/activities")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].subject").value("Initial Discovery Call"));
    }

    @Test
    @DisplayName("Should filter and search leads correctly")
    void testFilterAndSearchLeads() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Create 3 leads in Org A
        PracticeLeadEntity lead1 = leadRepository.save(PracticeLeadEntity.builder()
                .organizationId(orgA.getId())
                .leadType(LeadType.INDIVIDUAL)
                .name("Rohan Sharma")
                .businessName("Sharma Enterprises")
                .email("rohan@sharma.in")
                .phone("9820012345")
                .source(LeadSource.REFERRAL)
                .status(LeadStatus.QUALIFIED)
                .priority(LeadPriority.HIGH)
                .interestedServiceCode("ITR_FILING")
                .build());

        PracticeLeadEntity lead2 = leadRepository.save(PracticeLeadEntity.builder()
                .organizationId(orgA.getId())
                .leadType(LeadType.BUSINESS)
                .name("Priya Patel")
                .businessName("Patel Logistics")
                .email("priya@patellog.com")
                .phone("9820054321")
                .source(LeadSource.WEBSITE)
                .status(LeadStatus.NEW)
                .priority(LeadPriority.LOW)
                .interestedServiceCode("GST_MONTHLY")
                .build());

        // 1. Search by name query
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .param("search", "rohan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Rohan Sharma"));

        // 2. Search by businessName
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .param("search", "logistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Priya Patel"));

        // 3. Filter by status
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .param("status", "QUALIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Rohan Sharma"));

        // 4. Filter by priority
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .param("priority", "LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Priya Patel"));
    }

    @Test
    @DisplayName("Should strictly enforce tenant isolation across organizations")
    void testTenantIsolation() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Lead created in Org A
        PracticeLeadEntity leadA = leadRepository.save(PracticeLeadEntity.builder()
                .organizationId(orgA.getId())
                .leadType(LeadType.INDIVIDUAL)
                .name("Org A Confidential Lead")
                .email("secret@orga.com")
                .phone("9999999999")
                .source(LeadSource.REFERRAL)
                .status(LeadStatus.NEW)
                .priority(LeadPriority.CRITICAL)
                .build());

        // Org B lists leads -> Should return 0 elements
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // Org B tries to access Org A lead by ID -> Should return 404 (ResourceNotFoundException)
        mockMvc.perform(get("/api/v1/leads/" + leadA.getId())
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should allow Practitioner role access even if permissions are resolved via role")
    void testPractitionerRoleAccess() throws Exception {
        RoleEntity practitionerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTITIONER")
                        .name("Practitioner")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        TenantContext.setTenantId(orgA.getId());

        UserEntity practitionerUser = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("practitioner." + UUID.randomUUID() + "@orga.com")
                .firstName("Jane")
                .lastName("Practitioner")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(practitionerRole))
                .build());

        String practitionerToken = jwtTokenProvider.generateAccessToken(
                practitionerUser.getId(),
                orgA.getId(),
                practitionerUser.getEmail(),
                Set.of("PRACTITIONER"),
                Set.of() // empty authorities, role-based access
        );

        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
