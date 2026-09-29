package com.taxoryn.module.engagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementAssignmentRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
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
    private ServiceRepository serviceRepository;

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
    private UserEntity reviewerA;
    private UserEntity userB;
    private ClientEntity clientA;
    private ClientEntity clientB;
    private ServiceEntity serviceGST;
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
                .email("preparer." + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Aarav")
                .lastName("Kothari")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        reviewerA = userRepository.save(UserEntity.builder()
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

        serviceGST = serviceRepository.findByServiceCodeAndOrganizationIdIsNull("GST_COMPLIANCE")
                .orElseGet(() -> serviceRepository.save(ServiceEntity.builder()
                        .serviceCode("GST_COMPLIANCE")
                        .serviceName("GST Compliance & Returns")
                        .category(ServiceCategory.GST)
                        .status(ServiceStatus.ACTIVE)
                        .moduleCode("GST")
                        .build()));

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

        ClientEntity clientBeta = ClientEntity.builder()
                .displayName("Beta Client Ltd")
                .clientType(ClientType.COMPANY)
                .pan("BBBCN1234B")
                .status(ClientStatus.ACTIVE)
                .build();
        clientBeta.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientBeta);

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
    @DisplayName("Create Engagement with service, reviewer, priority, and auto-generated code")
    void testCreateGetUpdateEngagement() throws Exception {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .serviceId(serviceGST.getId())
                .locationId(locA.getId())
                .name("Annual GST Compliance FY 2026-27")
                .description("Complete monthly GSTR-1, GSTR-3B filings and annual reconciliation")
                .status(EngagementStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .assignedUserId(userA.getId())
                .reviewerUserId(reviewerA.getId())
                .priority(EngagementPriority.HIGH)
                .notes("Monthly retainer mandate")
                .build();

        String response = mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Annual GST Compliance FY 2026-27"))
                .andExpect(jsonPath("$.data.engagementCode").value(org.hamcrest.Matchers.startsWith("ENG-")))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.serviceCode").value("GST_COMPLIANCE"))
                .andReturn().getResponse().getContentAsString();

        UUID engagementId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/engagements/" + engagementId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientName").value("Nexus Retail Pvt Ltd"))
                .andExpect(jsonPath("$.data.locationName").value("Mumbai HQ"))
                .andExpect(jsonPath("$.data.assignedUserName").value(userA.getFullName()))
                .andExpect(jsonPath("$.data.reviewerUserName").value(reviewerA.getFullName()));

        // Update Engagement
        UpdateEngagementRequest updateReq = UpdateEngagementRequest.builder()
                .name("Comprehensive GST & ITC Advisory FY 2026-27")
                .priority(EngagementPriority.URGENT)
                .build();

        mockMvc.perform(put("/api/v1/engagements/" + engagementId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Comprehensive GST & ITC Advisory FY 2026-27"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));

        // Update Assignment via Patch
        UpdateEngagementAssignmentRequest assignReq = UpdateEngagementAssignmentRequest.builder()
                .reviewerUserId(reviewerA.getId())
                .notes("Reviewer updated")
                .build();

        mockMvc.perform(patch("/api/v1/engagements/" + engagementId + "/assignment")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewerUserId").value(reviewerA.getId().toString()));

        // Update Status: ACTIVE -> ON_HOLD -> ACTIVE -> COMPLETED
        UpdateEngagementStatusRequest holdReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.ON_HOLD)
                .notes("Awaiting client bank statements")
                .build();

        mockMvc.perform(patch("/api/v1/engagements/" + engagementId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_HOLD"));

        UpdateEngagementStatusRequest resumeReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.ACTIVE)
                .build();

        mockMvc.perform(patch("/api/v1/engagements/" + engagementId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        UpdateEngagementStatusRequest completeReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.COMPLETED)
                .notes("All returns filed")
                .build();

        mockMvc.perform(patch("/api/v1/engagements/" + engagementId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("List Engagements with server-side pagination, search, and status filters")
    void testGetEngagementsWithFilters() throws Exception {
        CreateEngagementRequest req1 = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .name("Engagement Alpha - GST")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .build();

        CreateEngagementRequest req2 = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .name("Engagement Beta - TDS")
                .status(EngagementStatus.DRAFT)
                .priority(EngagementPriority.MEDIUM)
                .build();

        mockMvc.perform(post("/api/v1/engagements").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/engagements").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Filter by status ACTIVE
        mockMvc.perform(get("/api/v1/engagements?status=ACTIVE")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Engagement Alpha - GST"));

        // Search by keyword "Beta"
        mockMvc.perform(get("/api/v1/engagements?search=Beta")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Engagement Beta - TDS"));
    }

    @Test
    @DisplayName("Cross-tenant client rejection: Tenant A cannot create engagement using Tenant B client")
    void testCrossTenantClientRejection() throws Exception {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientB.getId()) // Belongs to Tenant B
                .name("Cross Tenant Attempt")
                .build();

        mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-tenant user rejection: Tenant A cannot assign user from Tenant B")
    void testCrossTenantUserRejection() throws Exception {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientA.getId())
                .name("Cross Tenant User Attempt")
                .assignedUserId(userB.getId()) // Belongs to Tenant B
                .build();

        mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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
