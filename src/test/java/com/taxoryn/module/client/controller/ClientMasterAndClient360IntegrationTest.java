package com.taxoryn.module.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.AssignClientLocationRequest;
import com.taxoryn.module.client.dto.AssignClientUserRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.CreateClientServiceRequest;
import com.taxoryn.module.client.entity.ClientAssignmentRole;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientLocationAssignmentRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientMasterAndClient360IntegrationTest {

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
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    private LocationEntity orgAHeadOffice;
    private LocationEntity orgABranch;
    private LocationEntity orgBHeadOffice;

    private UserEntity orgAAdminUser;
    private UserEntity orgAPractitioner;
    private UserEntity orgAStaffUser;
    private UserEntity orgBAdminUser;

    private String orgAAdminToken;
    private String orgAPractitionerToken;
    private String orgAStaffToken;
    private String orgBAdminToken;

    @BeforeEach
    void setUp() {
        clientServiceRepository.deleteAll();
        clientUserAssignmentRepository.deleteAll();
        clientLocationAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Setup Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTICE_ADMIN")
                .name("Practice Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity practitionerRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTITIONER")
                .name("Practitioner")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 2. Setup Organizations
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Firm")
                .legalName("Apex CA LLP")
                .organizationType(OrganizationType.FIRM)
                .email("contact@apexca.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Tax Advisors")
                .legalName("Zenith Tax Advisors LLP")
                .organizationType(OrganizationType.SOLO)
                .email("contact@zenithtax.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        // 3. Setup Subscriptions
        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgA.getId())
                .plan(SubscriptionPlan.ENTERPRISE)
                .status(SubscriptionEntity.SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgB.getId())
                .plan(SubscriptionPlan.STARTER)
                .status(SubscriptionEntity.SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        // 4. Setup Locations
        orgAHeadOffice = LocationEntity.builder()
                .name("Mumbai Head Office")
                .code("MUM-HO")
                .isHeadOffice(true)
                .isActive(true)
                .city("Mumbai")
                .state("Maharashtra")
                .build();
        orgAHeadOffice.setOrganizationId(orgA.getId());
        orgAHeadOffice = locationRepository.save(orgAHeadOffice);

        orgABranch = LocationEntity.builder()
                .name("Pune Branch")
                .code("PUN-BR")
                .isHeadOffice(false)
                .isActive(true)
                .city("Pune")
                .state("Maharashtra")
                .build();
        orgABranch.setOrganizationId(orgA.getId());
        orgABranch = locationRepository.save(orgABranch);

        orgBHeadOffice = LocationEntity.builder()
                .name("Delhi Head Office")
                .code("DEL-HO")
                .isHeadOffice(true)
                .isActive(true)
                .city("Delhi")
                .state("Delhi")
                .build();
        orgBHeadOffice.setOrganizationId(orgB.getId());
        orgBHeadOffice = locationRepository.save(orgBHeadOffice);

        // 5. Setup Users
        orgAAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin@apexca.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Arun")
                .lastName("Joshi")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        orgAPractitioner = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("practitioner@apexca.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Pooja")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practitionerRole)))
                .build());

        orgAStaffUser = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("staff@apexca.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Rahul")
                .lastName("Shah")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        orgBAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin@zenithtax.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Suresh")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        // Assign practitioner to Pune Branch only
        UserLocationEntity pLoc = UserLocationEntity.builder()
                .id(new UserLocationEntity.UserLocationId(orgAPractitioner.getId(), orgABranch.getId()))
                .organizationId(orgA.getId())
                .build();
        userLocationRepository.save(pLoc);

        // Enable GST, ITR, TDS modules for Org A
        for (ProductModuleCode mod : List.of(ProductModuleCode.GST, ProductModuleCode.ITR, ProductModuleCode.TDS, ProductModuleCode.TAX_NOTICES)) {
            OrganizationModuleEntity modEnt = OrganizationModuleEntity.builder()
                    .moduleCode(mod)
                    .enabled(true)
                    .build();
            modEnt.setOrganizationId(orgA.getId());
            organizationModuleRepository.save(modEnt);
        }

        // Setup JWT Tokens
        orgAAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                orgAAdminUser.getId(), orgA.getId(), orgAAdminUser.getEmail(),
                Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN", "ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE")
        );

        orgAPractitionerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                orgAPractitioner.getId(), orgA.getId(), orgAPractitioner.getEmail(),
                Set.of("ROLE_PRACTITIONER", "PRACTITIONER"),
                Set.of("CLIENT_VIEW", "CLIENT_READ", "CLIENT_UPDATE")
        );

        orgAStaffToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                orgAStaffUser.getId(), orgA.getId(), orgAStaffUser.getEmail(),
                Set.of("ROLE_STAFF", "STAFF"),
                Set.of("CLIENT_VIEW", "CLIENT_READ")
        );

        orgBAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                orgBAdminUser.getId(), orgB.getId(), orgBAdminUser.getEmail(),
                Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN", "ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE")
        );

        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Create client under current organization - Success with auto tenant assignment")
    void testCreateClientUnderCurrentOrganization_Success() throws Exception {
        CreateClientRequest request = CreateClientRequest.builder()
                .displayName("Alpha Traders")
                .legalName("Alpha Traders Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .clientCode("CL-000101")
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .tan("PUNZ12345A")
                .email("accounts@alphatraders.com")
                .phone("+919876543210")
                .addressLine1("101 Commercial Hub")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .country("India")
                .locationId(orgAHeadOffice.getId())
                .status(ClientStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.displayName").value("Alpha Traders"))
                .andExpect(jsonPath("$.data.clientCode").value("CL-000101"))
                .andExpect(jsonPath("$.data.pan").value("ABCDE1234F"))
                .andExpect(jsonPath("$.data.organizationId").value(orgA.getId().toString()));

        assertThat(clientRepository.existsByOrganizationIdAndClientCode(orgA.getId(), "CL-000101")).isTrue();
    }

    @Test
    @DisplayName("2. Client cannot be created without tenant context")
    void testCreateClientWithoutTenantContext_Fails() throws Exception {
        CreateClientRequest request = CreateClientRequest.builder()
                .displayName("Anonymous Client")
                .clientType(ClientType.INDIVIDUAL)
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Client code is unique within organization, permitted across distinct organizations")
    void testClientCodeUniquenessWithinOrganization() throws Exception {
        CreateClientRequest req1 = CreateClientRequest.builder()
                .displayName("OrgA Client")
                .clientType(ClientType.COMPANY)
                .clientCode("CL-DUPLICATE-01")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Duplicate in same org fails
        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isConflict());

        // Same code in Org B succeeds
        CreateClientRequest reqOrgB = CreateClientRequest.builder()
                .displayName("OrgB Client")
                .clientType(ClientType.PROPRIETOR)
                .clientCode("CL-DUPLICATE-01")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", orgBAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqOrgB)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("4. Client from another organization cannot be accessed (Strict Isolation)")
    void testClientFromAnotherOrganizationCannotBeAccessed() throws Exception {
        ClientEntity clientB = ClientEntity.builder()
                .displayName("Foreign OrgB Client")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build();
        clientB.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientB);

        mockMvc.perform(get("/api/v1/clients/{id}", clientB.getId())
                        .header("Authorization", orgAAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Location scope filters client visibility for non-admin practitioners")
    void testLocationScopeFiltersClientVisibility() throws Exception {
        // Client 1 assigned to Mumbai HO
        ClientEntity clientMumbai = ClientEntity.builder()
                .displayName("Mumbai Client")
                .clientType(ClientType.INDIVIDUAL)
                .locationId(orgAHeadOffice.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientMumbai.setOrganizationId(orgA.getId());
        clientMumbai = clientRepository.save(clientMumbai);

        // Client 2 assigned to Pune Branch
        ClientEntity clientPune = ClientEntity.builder()
                .displayName("Pune Client")
                .clientType(ClientType.INDIVIDUAL)
                .locationId(orgABranch.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientPune.setOrganizationId(orgA.getId());
        clientPune = clientRepository.save(clientPune);

        // Practitioner (assigned to Pune only) should only see Pune Client
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", orgAPractitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Pune Client"));

        // Admin sees all clients
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @DisplayName("6. Authorized user can access assigned client via portfolio")
    void testAuthorizedUserCanAccessAssignedClientPortfolio() throws Exception {
        ClientEntity client = ClientEntity.builder()
                .displayName("Special Portfolio Client")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);

        // Explicitly assign staff user to this client
        AssignClientUserRequest assignReq = AssignClientUserRequest.builder()
                .userId(orgAStaffUser.getId())
                .assignmentRole(ClientAssignmentRole.PRIMARY)
                .primaryResponsible(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/users", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated());

        // Staff can now access client details
        mockMvc.perform(get("/api/v1/clients/{clientId}", client.getId())
                        .header("Authorization", orgAStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Special Portfolio Client"));
    }

    @Test
    @DisplayName("7. Unauthorized user cannot access unassigned client")
    void testUnauthorizedUserCannotAccessUnassignedClient() throws Exception {
        ClientEntity client = ClientEntity.builder()
                .displayName("Confidential Corporate Client")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);

        // Staff user with no portfolio assignment and no location assignment is rejected
        mockMvc.perform(get("/api/v1/clients/{clientId}", client.getId())
                        .header("Authorization", orgAStaffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Client service configuration validates organization ownership (rejects foreign user/location)")
    void testClientServiceConfigurationValidatesOrganizationOwnership() throws Exception {
        ClientEntity client = ClientEntity.builder()
                .displayName("Service Bound Client")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);

        // Attempt to configure service using foreign user from Org B
        CreateClientServiceRequest invalidUserReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .responsibleUserId(orgBAdminUser.getId())
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserReq)))
                .andExpect(status().isNotFound());

        // Attempt to configure service using foreign location from Org B
        CreateClientServiceRequest invalidLocReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .locationId(orgBHeadOffice.getId())
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLocReq)))
                .andExpect(status().isNotFound());

        // Valid configuration with Org A user and location succeeds
        CreateClientServiceRequest validReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .responsibleUserId(orgAAdminUser.getId())
                .locationId(orgAHeadOffice.getId())
                .notes("Monthly GSTR-1 and GSTR-3B filings")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.serviceType").value("GST_COMPLIANCE"))
                .andExpect(jsonPath("$.data.responsibleUserId").value(orgAAdminUser.getId().toString()))
                .andExpect(jsonPath("$.data.locationId").value(orgAHeadOffice.getId().toString()));
    }

    @Test
    @DisplayName("9. Client 360 returns expected unified core sections")
    void testClient360ReturnsExpectedCoreSections() throws Exception {
        ClientEntity client = ClientEntity.builder()
                .displayName("Comprehensive 360 Corp")
                .legalName("Comprehensive 360 Corporation Pvt Ltd")
                .clientCode("CL-360-001")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("ABCDE9999Z")
                .gstin("27ABCDE9999Z1Z1")
                .tan("PUNZ99999Z")
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);

        // Assign location
        AssignClientLocationRequest locReq = AssignClientLocationRequest.builder()
                .locationId(orgAHeadOffice.getId())
                .primaryLocation(true)
                .build();
        mockMvc.perform(post("/api/v1/clients/{clientId}/locations", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isCreated());

        // Assign user
        AssignClientUserRequest userReq = AssignClientUserRequest.builder()
                .userId(orgAPractitioner.getId())
                .assignmentRole(ClientAssignmentRole.PRIMARY)
                .primaryResponsible(true)
                .build();
        mockMvc.perform(post("/api/v1/clients/{clientId}/users", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated());

        // Configure service
        CreateClientServiceRequest srvReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.ITR_COMPLIANCE)
                .responsibleUserId(orgAPractitioner.getId())
                .locationId(orgAHeadOffice.getId())
                .build();
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(srvReq)))
                .andExpect(status().isCreated());

        // Query Client 360
        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client.getId())
                        .header("Authorization", orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Comprehensive 360 Corp"))
                .andExpect(jsonPath("$.data.client.clientCode").value("CL-360-001"))
                .andExpect(jsonPath("$.data.identifiers.pan").value("ABCDE9999Z"))
                .andExpect(jsonPath("$.data.identifiers.gstin").value("27ABCDE9999Z1Z1"))
                .andExpect(jsonPath("$.data.identifiers.tan").value("PUNZ99999Z"))
                .andExpect(jsonPath("$.data.primaryLocation.locationId").value(orgAHeadOffice.getId().toString()))
                .andExpect(jsonPath("$.data.locations", hasSize(1)))
                .andExpect(jsonPath("$.data.assignedUsers", hasSize(1)))
                .andExpect(jsonPath("$.data.assignedUsers[0].userId").value(orgAPractitioner.getId().toString()))
                .andExpect(jsonPath("$.data.services", hasSize(1)))
                .andExpect(jsonPath("$.data.services[0].serviceType").value("ITR_COMPLIANCE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("10. Disabled compliance module does not expose or permit service configuration")
    void testDisabledModuleServiceConfigurationRejected() throws Exception {
        // Disable TDS module for Org A
        OrganizationModuleEntity tdsMod = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(orgA.getId(), ProductModuleCode.TDS)
                .orElse(null);
        if (tdsMod != null) {
            tdsMod.setEnabled(false);
            organizationModuleRepository.save(tdsMod);
        }

        ClientEntity client = ClientEntity.builder()
                .displayName("Disabled Module Test Client")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);

        CreateClientServiceRequest tdsReq = CreateClientServiceRequest.builder()
                .serviceType(ClientServiceType.TDS_COMPLIANCE)
                .build();

        // Should be rejected because TDS is disabled in organization settings
        mockMvc.perform(post("/api/v1/clients/{clientId}/services", client.getId())
                        .header("Authorization", orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tdsReq)))
                .andExpect(status().isBadRequest());
    }
}
