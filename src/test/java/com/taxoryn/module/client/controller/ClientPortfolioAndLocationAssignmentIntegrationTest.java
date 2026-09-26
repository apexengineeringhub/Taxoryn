package com.taxoryn.module.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.client.dto.AssignClientLocationRequest;
import com.taxoryn.module.client.dto.AssignClientUserRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientAssignmentRole;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientLocationAssignmentRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.entity.UserLocationEntity;
import com.taxoryn.module.user.repository.UserLocationRepository;
import com.taxoryn.module.user.repository.UserRepository;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientPortfolioAndLocationAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientLocationAssignmentRepository clientLocationAssignmentRepository;

    @Autowired
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locMumbaiOrgA;
    private LocationEntity locBangaloreOrgA;
    private LocationEntity locDelhiOrgB;

    private UserEntity adminUserOrgA;
    private UserEntity staffUserOrgA;
    private UserEntity reviewerUserOrgA;
    private UserEntity adminUserOrgB;

    private String adminTokenOrgA;
    private String staffTokenOrgA;
    private String adminTokenOrgB;

    private RoleEntity practiceAdminRole;
    private RoleEntity staffRole;

    @BeforeEach
    void setUp() {
        clientUserAssignmentRepository.deleteAll();
        clientLocationAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();

        practiceAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTICE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTICE_ADMIN")
                        .name("Practice Administrator")
                        .isSystemRole(true)
                        .build()));

        staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Junior Staff")
                        .isSystemRole(true)
                        .build()));

        // Org A (Business tier)
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Practice")
                .legalName("Apex CA Practice LLP")
                .email("admin@apexca.com")
                .organizationType(OrganizationType.FIRM)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgA.getId())
                .plan(SubscriptionPlan.BUSINESS)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        // Org B (Starter tier)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Solutions")
                .legalName("Beta Tax Solutions")
                .email("admin@betatax.com")
                .organizationType(OrganizationType.SOLO)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgB.getId())
                .plan(SubscriptionPlan.STARTER)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        // Locations for Org A
        LocationEntity locM = LocationEntity.builder()
                .name("Mumbai Head Office")
                .code("MUM-01")
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        locM.setOrganizationId(orgA.getId());
        locMumbaiOrgA = locationRepository.save(locM);

        LocationEntity locB = LocationEntity.builder()
                .name("Bangalore Branch")
                .code("BLR-01")
                .city("Bangalore")
                .state("Karnataka")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        locB.setOrganizationId(orgA.getId());
        locBangaloreOrgA = locationRepository.save(locB);

        // Location for Org B
        LocationEntity locD = LocationEntity.builder()
                .name("Delhi Office")
                .code("DEL-01")
                .city("New Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        locD.setOrganizationId(orgB.getId());
        locDelhiOrgB = locationRepository.save(locD);

        // Admin User for Org A
        adminUserOrgA = UserEntity.builder()
                .email("admin@apexca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Aditi")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceAdminRole)))
                .build();
        adminUserOrgA.setOrganizationId(orgA.getId());
        adminUserOrgA = userRepository.save(adminUserOrgA);

        // Staff User for Org A
        staffUserOrgA = UserEntity.builder()
                .email("staff@apexca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Ravi")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build();
        staffUserOrgA.setOrganizationId(orgA.getId());
        staffUserOrgA = userRepository.save(staffUserOrgA);

        // Reviewer User for Org A
        reviewerUserOrgA = UserEntity.builder()
                .email("reviewer@apexca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Suresh")
                .lastName("Rao")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build();
        reviewerUserOrgA.setOrganizationId(orgA.getId());
        reviewerUserOrgA = userRepository.save(reviewerUserOrgA);

        // Admin User for Org B
        adminUserOrgB = UserEntity.builder()
                .email("admin@betatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Vikram")
                .lastName("Singh")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceAdminRole)))
                .build();
        adminUserOrgB.setOrganizationId(orgB.getId());
        adminUserOrgB = userRepository.save(adminUserOrgB);

        // Tokens
        adminTokenOrgA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserOrgA.getId(), orgA.getId(), "admin@apexca.com",
                Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "ORGANIZATION_VIEW", "ORGANIZATION_UPDATE")
        );

        staffTokenOrgA = "Bearer " + jwtTokenProvider.generateAccessToken(
                staffUserOrgA.getId(), orgA.getId(), "staff@apexca.com",
                Set.of("ROLE_STAFF", "STAFF"),
                Set.of("CLIENT_VIEW", "ORGANIZATION_VIEW")
        );

        adminTokenOrgB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserOrgB.getId(), orgB.getId(), "admin@betatax.com",
                Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "ORGANIZATION_VIEW", "ORGANIZATION_UPDATE")
        );
    }

    @Test
    @DisplayName("Admin creates client with clientCode and duplicate clientCode in same org is rejected")
    void testClientCreationWithClientCodeAndUniqueValidation() throws Exception {
        CreateClientRequest req1 = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .clientCode("CLI-001")
                .displayName("Infosys Limited")
                .legalName("Infosys Technologies Ltd")
                .pan("AAACI1234F")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientCode").value("CLI-001"))
                .andExpect(jsonPath("$.data.displayName").value("Infosys Limited"));

        // Attempt duplicate clientCode in Org A -> 409 Conflict
        CreateClientRequest reqDuplicate = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .clientCode("CLI-001")
                .displayName("Duplicate Code Client")
                .pan("BBBCI1234F")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDuplicate)))
                .andExpect(status().isConflict());

        // Same clientCode in Org B should succeed (Tenant isolation)
        CreateClientRequest reqOrgB = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .clientCode("CLI-001")
                .displayName("Infosys Branch Org B")
                .pan("AAACI1234F")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqOrgB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientCode").value("CLI-001"));
    }

    @Test
    @DisplayName("Admin manages client location assignments, primary location toggling, and removal")
    void testClientLocationAssignmentAndPrimaryToggling() throws Exception {
        // Create client in Org A
        CreateClientRequest req = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Wipro Limited")
                .build();

        String clientResp = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID clientId = UUID.fromString(objectMapper.readTree(clientResp).get("data").get("id").asText());

        // 1. Assign Mumbai as primary location
        AssignClientLocationRequest assignMumbai = AssignClientLocationRequest.builder()
                .locationId(locMumbaiOrgA.getId())
                .primaryLocation(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/locations")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignMumbai)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationId").value(locMumbaiOrgA.getId().toString()))
                .andExpect(jsonPath("$.data.isPrimaryLocation").value(true));

        // 2. Assign Bangalore as secondary location
        AssignClientLocationRequest assignBangalore = AssignClientLocationRequest.builder()
                .locationId(locBangaloreOrgA.getId())
                .primaryLocation(false)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/locations")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignBangalore)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationId").value(locBangaloreOrgA.getId().toString()))
                .andExpect(jsonPath("$.data.isPrimaryLocation").value(false));

        // 3. List client locations -> 2 assignments
        mockMvc.perform(get("/api/v1/clients/" + clientId + "/locations")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 4. Toggle Bangalore as primary location
        mockMvc.perform(put("/api/v1/clients/" + clientId + "/locations/" + locBangaloreOrgA.getId() + "/primary")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locBangaloreOrgA.getId().toString()))
                .andExpect(jsonPath("$.data.isPrimaryLocation").value(true));

        // Verify in DB that Mumbai is no longer primary
        var mumbaiAssignment = clientLocationAssignmentRepository
                .findByOrganizationIdAndClientIdAndLocationId(orgA.getId(), clientId, locMumbaiOrgA.getId())
                .orElseThrow();
        assertThat(mumbaiAssignment.isPrimaryLocation()).isFalse();

        // 5. Remove Mumbai location assignment
        mockMvc.perform(delete("/api/v1/clients/" + clientId + "/locations/" + locMumbaiOrgA.getId())
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients/" + clientId + "/locations")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].locationId").value(locBangaloreOrgA.getId().toString()));
    }

    @Test
    @DisplayName("Cross-tenant location assignment is rejected: Org A assigning Org B location")
    void testCrossTenantClientLocationAssignmentRejection() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Rohan Joshi")
                .build();

        String clientResp = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID clientId = UUID.fromString(objectMapper.readTree(clientResp).get("data").get("id").asText());

        AssignClientLocationRequest crossTenantReq = AssignClientLocationRequest.builder()
                .locationId(locDelhiOrgB.getId())
                .primaryLocation(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/locations")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossTenantReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin manages client user portfolio assignments, roles, primary responsible toggling, and removal")
    void testClientUserPortfolioAssignmentAndPrimaryToggling() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Tata Consultancy Services")
                .build();

        String clientResp = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID clientId = UUID.fromString(objectMapper.readTree(clientResp).get("data").get("id").asText());

        // 1. Assign Staff user as PRIMARY with primaryResponsible = true
        AssignClientUserRequest assignStaff = AssignClientUserRequest.builder()
                .userId(staffUserOrgA.getId())
                .assignmentRole(ClientAssignmentRole.PRIMARY)
                .primaryResponsible(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/users")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignStaff)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(staffUserOrgA.getId().toString()))
                .andExpect(jsonPath("$.data.assignmentRole").value("PRIMARY"))
                .andExpect(jsonPath("$.data.isPrimaryResponsible").value(true));

        // 2. Assign Reviewer user as REVIEWER with primaryResponsible = false
        AssignClientUserRequest assignReviewer = AssignClientUserRequest.builder()
                .userId(reviewerUserOrgA.getId())
                .assignmentRole(ClientAssignmentRole.REVIEWER)
                .primaryResponsible(false)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/users")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReviewer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(reviewerUserOrgA.getId().toString()))
                .andExpect(jsonPath("$.data.assignmentRole").value("REVIEWER"))
                .andExpect(jsonPath("$.data.isPrimaryResponsible").value(false));

        // 3. List client users -> 2 assignments
        mockMvc.perform(get("/api/v1/clients/" + clientId + "/users")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 4. Set Reviewer user as primary responsible
        mockMvc.perform(put("/api/v1/clients/" + clientId + "/users/" + reviewerUserOrgA.getId() + "/primary")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(reviewerUserOrgA.getId().toString()))
                .andExpect(jsonPath("$.data.isPrimaryResponsible").value(true));

        // Verify Staff user is no longer primary responsible
        var staffAssignment = clientUserAssignmentRepository
                .findByOrganizationIdAndClientIdAndUserId(orgA.getId(), clientId, staffUserOrgA.getId())
                .orElseThrow();
        assertThat(staffAssignment.isPrimaryResponsible()).isFalse();

        // 5. Remove Staff user assignment
        mockMvc.perform(delete("/api/v1/clients/" + clientId + "/users/" + staffUserOrgA.getId())
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients/" + clientId + "/users")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId").value(reviewerUserOrgA.getId().toString()));
    }

    @Test
    @DisplayName("Cross-tenant user portfolio assignment is rejected: Org A assigning Org B user")
    void testCrossTenantClientUserAssignmentRejection() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Kavita Nair")
                .build();

        String clientResp = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID clientId = UUID.fromString(objectMapper.readTree(clientResp).get("data").get("id").asText());

        AssignClientUserRequest crossReq = AssignClientUserRequest.builder()
                .userId(adminUserOrgB.getId())
                .assignmentRole(ClientAssignmentRole.PRIMARY)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + clientId + "/users")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Client visibility honors location scope and user portfolio assignments")
    void testLocationAndPortfolioScopedAccessFiltering() throws Exception {
        // Create 2 clients in Org A:
        // Client BLR assigned to Bangalore location
        ClientEntity clientBlr = ClientEntity.builder()
                .displayName("Bangalore Client Corp")
                .clientType(ClientType.PRIVATE_LIMITED)
                .locationId(locBangaloreOrgA.getId())
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientBlr.setOrganizationId(orgA.getId());
        clientBlr = clientRepository.save(clientBlr);

        com.taxoryn.module.client.entity.ClientLocationAssignmentEntity locBlrAssign =
                com.taxoryn.module.client.entity.ClientLocationAssignmentEntity.builder()
                        .clientId(clientBlr.getId())
                        .locationId(locBangaloreOrgA.getId())
                        .primaryLocation(true)
                        .active(true)
                        .assignedAt(Instant.now())
                        .build();
        locBlrAssign.setOrganizationId(orgA.getId());
        clientLocationAssignmentRepository.save(locBlrAssign);

        // Client MUM assigned to Mumbai location
        ClientEntity clientMum = ClientEntity.builder()
                .displayName("Mumbai Client Corp")
                .clientType(ClientType.PRIVATE_LIMITED)
                .locationId(locMumbaiOrgA.getId())
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientMum.setOrganizationId(orgA.getId());
        clientMum = clientRepository.save(clientMum);

        com.taxoryn.module.client.entity.ClientLocationAssignmentEntity locMumAssign =
                com.taxoryn.module.client.entity.ClientLocationAssignmentEntity.builder()
                        .clientId(clientMum.getId())
                        .locationId(locMumbaiOrgA.getId())
                        .primaryLocation(true)
                        .active(true)
                        .assignedAt(Instant.now())
                        .build();
        locMumAssign.setOrganizationId(orgA.getId());
        clientLocationAssignmentRepository.save(locMumAssign);

        // Assign Staff user ONLY to Bangalore location
        UserLocationEntity staffLoc = UserLocationEntity.builder()
                .id(new UserLocationEntity.UserLocationId(staffUserOrgA.getId(), locBangaloreOrgA.getId()))
                .organizationId(orgA.getId())
                .build();
        userLocationRepository.save(staffLoc);

        // 1. Staff user queries clients -> Sees Bangalore Client, NOT Mumbai Client
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Bangalore Client Corp"));

        // 2. Staff user attempts to access Mumbai Client by ID -> 403 Forbidden
        mockMvc.perform(get("/api/v1/clients/" + clientMum.getId())
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isForbidden());

        // 3. Explicitly assign Staff user to Mumbai Client via ClientUserAssignment (Portfolio scope)
        com.taxoryn.module.client.entity.ClientUserAssignmentEntity portfolioAssign =
                com.taxoryn.module.client.entity.ClientUserAssignmentEntity.builder()
                        .clientId(clientMum.getId())
                        .userId(staffUserOrgA.getId())
                        .assignmentRole(ClientAssignmentRole.SUPPORTING)
                        .primaryResponsible(false)
                        .active(true)
                        .assignedAt(Instant.now())
                        .build();
        portfolioAssign.setOrganizationId(orgA.getId());
        clientUserAssignmentRepository.save(portfolioAssign);

        // 4. Staff user queries clients again -> Now sees BOTH Bangalore Client (via location) and Mumbai Client (via portfolio)!
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // Staff user accesses Mumbai Client by ID -> 200 OK
        mockMvc.perform(get("/api/v1/clients/" + clientMum.getId())
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Mumbai Client Corp"));

        // 5. Cross-tenant isolation: Org B admin querying Org A client by ID -> 404 / 403
        mockMvc.perform(get("/api/v1/clients/" + clientMum.getId())
                        .header("Authorization", adminTokenOrgB))
                .andExpect(status().isNotFound());
    }
}
