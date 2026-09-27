package com.taxoryn.module.engagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EngagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locA;
    private UserEntity userA;
    private UserEntity userB;
    private ClientEntity clientA;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Kothari & Associates - " + UUID.randomUUID())
                .legalName("Kothari & Associates LLP")
                .email("admin." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        LocationEntity loc = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(orgA.getId());
        locA = locationRepository.save(loc);

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("partner." + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Kothari")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_WRITE")
        );

        ClientEntity client = ClientEntity.builder()
                .displayName("Nexus Retail Pvt Ltd")
                .legalName("Nexus Retail Technologies Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .pan("AABCN1234A")
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(client);

        TenantContext.clear();

        // Tenant B (Isolation)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Firm - " + UUID.randomUUID())
                .legalName("Beta Firm LLP")
                .email("admin." + UUID.randomUUID() + "@beta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@beta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Engagement, retrieve by ID, update details and update status")
    void testCreateGetUpdateEngagement() throws Exception {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .engagementCode("ENG-GST-2026")
                .name("Annual GST Compliance FY 2026-27")
                .description("Complete monthly GSTR-1, GSTR-3B filings and annual reconciliation")
                .status(EngagementStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .assignedUserId(userA.getId())
                .notes("Monthly retainer mandate")
                .build();

        String response = mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Annual GST Compliance FY 2026-27"))
                .andExpect(jsonPath("$.data.engagementCode").value("ENG-GST-2026"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        UUID engagementId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/engagements/" + engagementId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientName").value("Nexus Retail Pvt Ltd"))
                .andExpect(jsonPath("$.data.locationName").value("Mumbai HQ"))
                .andExpect(jsonPath("$.data.assignedUserName").value(userA.getFullName()));

        // Update Engagement
        UpdateEngagementRequest updateReq = UpdateEngagementRequest.builder()
                .name("Comprehensive GST & ITC Advisory FY 2026-27")
                .description("Expanded scope to cover Rule 37A supplier ITC reconciliation")
                .build();

        mockMvc.perform(put("/api/v1/engagements/" + engagementId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Comprehensive GST & ITC Advisory FY 2026-27"));

        // Update Status
        UpdateEngagementStatusRequest statusReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.COMPLETED)
                .notes("All FY 2026-27 returns filed and annual return completed")
                .build();

        mockMvc.perform(patch("/api/v1/engagements/" + engagementId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Retrieve Engagements by Client ID for Client 360")
    void testGetEngagementsByClientId() throws Exception {
        CreateEngagementRequest req1 = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .name("Engagement 1 - GST")
                .build();

        CreateEngagementRequest req2 = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .name("Engagement 2 - TDS")
                .build();

        mockMvc.perform(post("/api/v1/engagements").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/engagements").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/engagements/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Tenant Isolation: Tenant B cannot access Tenant A engagement")
    void testTenantIsolation() throws Exception {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .name("Confidential Client Mandate")
                .build();

        String response = mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID engagementId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Tenant B attempts to read -> 404
        mockMvc.perform(get("/api/v1/engagements/" + engagementId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
