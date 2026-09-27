package com.taxoryn.module.tds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.tds.dto.CreateTdsProfileRequest;
import com.taxoryn.module.tds.dto.UpdateTdsProfileRequest;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TracesStatus;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TdsProfileIntegrationTest {

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
    private TdsProfileRepository tdsProfileRepository;

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
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Verma & Associates - " + UUID.randomUUID())
                .legalName("Verma & Associates LLP")
                .email("admin." + UUID.randomUUID() + "@verma.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity loc = LocationEntity.builder()
                .name("Bengaluru HQ")
                .code("BLR-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(orgA.getId());
        locA = locationRepository.save(loc);

        userA = userRepository.save(UserEntity.builder()
                .email("partner." + UUID.randomUUID() + "@verma.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Aditya")
                .lastName("Verma")
                .organizationId(orgA.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_DELETE", "CLIENT_VIEW")
        );

        ClientEntity client = ClientEntity.builder()
                .displayName("Nexus Infotech Pvt Ltd")
                .legalName("Nexus Infotech Private Limited")
                .pan("AABCN1234F")
                .tan("BLRN12345A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(client);

        // Tenant B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Mehta & Co - " + UUID.randomUUID())
                .legalName("Mehta & Co CA")
                .email("admin." + UUID.randomUUID() + "@mehta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .email("partner." + UUID.randomUUID() + "@mehta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Karan")
                .lastName("Mehta")
                .organizationId(orgB.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "CLIENT_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        TenantContext.clear();
        tdsProfileRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("TDS Profile CRUD: Successfully create, retrieve, and update TDS profile")
    void testTdsProfileCrud() throws Exception {
        CreateTdsProfileRequest request = CreateTdsProfileRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .tan("BLRN12345A")
                .deductorType(DeductorType.COMPANY)
                .branchDivisionName("Main Branch")
                .responsiblePersonName("Ramesh Kumar")
                .responsiblePersonPan("ABCPR1234G")
                .responsiblePersonDesignation("Finance Manager")
                .responsiblePersonEmail("ramesh@nexus.com")
                .responsiblePersonMobile("9876543210")
                .tracesUsername("NEXUS_TRACES")
                .build();

        String response = mockMvc.perform(post("/api/v1/tds/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tan").value("BLRN12345A"))
                .andExpect(jsonPath("$.data.clientName").value("Nexus Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.locationName").value("Bengaluru HQ"))
                .andExpect(jsonPath("$.data.deductorType").value("COMPANY"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn().getResponse().getContentAsString();

        String profileId = objectMapper.readTree(response).path("data").path("id").asText();

        // Get by Profile ID
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tan").value("BLRN12345A"))
                .andExpect(jsonPath("$.data.responsiblePersonName").value("Ramesh Kumar"));

        // Get by Client ID
        mockMvc.perform(get("/api/v1/tds/profiles/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tan").value("BLRN12345A"))
                .andExpect(jsonPath("$.data.tracesUsername").value("NEXUS_TRACES"));

        // Update Profile
        UpdateTdsProfileRequest updateReq = UpdateTdsProfileRequest.builder()
                .locationId(locA.getId())
                .deductorType(DeductorType.COMPANY)
                .branchDivisionName("Bengaluru Main Branch")
                .responsiblePersonName("Ramesh Kumar Updated")
                .responsiblePersonPan("ABCPR1234G")
                .responsiblePersonDesignation("Chief Financial Officer")
                .responsiblePersonEmail("cfo@nexus.com")
                .responsiblePersonMobile("9876543211")
                .tracesUsername("NEXUS_TRACES_V2")
                .tracesStatus(TracesStatus.REGISTERED_ACTIVE)
                .status(TdsProfileStatus.ACTIVE)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/tds/profiles/" + profileId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responsiblePersonDesignation").value("Chief Financial Officer"))
                .andExpect(jsonPath("$.data.tracesUsername").value("NEXUS_TRACES_V2"));
    }

    @Test
    @DisplayName("Create TDS Profile: Automatically reuse TAN from Client Master when omitted")
    void testCreateTdsProfileReusingClientTan() throws Exception {
        CreateTdsProfileRequest request = CreateTdsProfileRequest.builder()
                .clientId(clientA.getId())
                .deductorType(DeductorType.COMPANY)
                .responsiblePersonName("Suresh Rao")
                .build();

        mockMvc.perform(post("/api/v1/tds/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tan").value("BLRN12345A"))
                .andExpect(jsonPath("$.data.clientName").value("Nexus Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.locationName").value("Bengaluru HQ"));
    }

    @Test
    @DisplayName("Duplicate TAN: Rejects registering duplicate TDS profile with same TAN in same tenant")
    void testDuplicateTanRejectionInSameTenant() throws Exception {
        CreateTdsProfileRequest request1 = CreateTdsProfileRequest.builder()
                .clientId(clientA.getId())
                .tan("BLRN12345A")
                .deductorType(DeductorType.COMPANY)
                .build();

        mockMvc.perform(post("/api/v1/tds/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Attempt duplicate creation for another client with same TAN
        ClientEntity client2 = ClientEntity.builder()
                .displayName("Second Company Pvt Ltd")
                .legalName("Second Company Private Limited")
                .pan("AABCS9999K")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        client2.setOrganizationId(orgA.getId());
        client2 = clientRepository.save(client2);

        CreateTdsProfileRequest request2 = CreateTdsProfileRequest.builder()
                .clientId(client2.getId())
                .tan("BLRN12345A")
                .deductorType(DeductorType.COMPANY)
                .build();

        mockMvc.perform(post("/api/v1/tds/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Multi-Tenant Isolation: Tenant B cannot access Tenant A TDS profile")
    void testMultiTenantIsolation() throws Exception {
        CreateTdsProfileRequest request = CreateTdsProfileRequest.builder()
                .clientId(clientA.getId())
                .tan("BLRN12345A")
                .deductorType(DeductorType.COMPANY)
                .build();

        String response = mockMvc.perform(post("/api/v1/tds/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String profileId = objectMapper.readTree(response).path("data").path("id").asText();

        // Tenant B attempt -> 404 Not Found
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
