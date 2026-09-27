package com.taxoryn.module.itr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.UpdateItrProfileRequest;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
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
public class ItrProfileIntegrationTest {

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
    private ItrProfileRepository itrProfileRepository;

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
                .name("Singhania & Partners - " + UUID.randomUUID())
                .legalName("Singhania & Partners LLP")
                .email("admin." + UUID.randomUUID() + "@singhania.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

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
                .email("partner." + UUID.randomUUID() + "@singhania.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Sunil")
                .lastName("Singhania")
                .organizationId(orgA.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity client = ClientEntity.builder()
                .displayName("Titanium Logistics Pvt Ltd")
                .legalName("Titanium Logistics Private Limited")
                .pan("AABCT1234A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(client);

        // Tenant B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Kapoor & Co - " + UUID.randomUUID())
                .legalName("Kapoor & Co CA")
                .email("admin." + UUID.randomUUID() + "@kapoor.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .email("partner." + UUID.randomUUID() + "@kapoor.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Rajesh")
                .lastName("Kapoor")
                .organizationId(orgB.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "CLIENT_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        TenantContext.clear();
        itrProfileRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("ITR Profile CRUD: Successfully create, retrieve, update ITR profile")
    void testItrProfileCrud() throws Exception {
        CreateItrProfileRequest request = CreateItrProfileRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .pan("AABCT1234A")
                .taxpayerType(TaxpayerType.COMPANY)
                .defaultItrType(ItrType.ITR_6)
                .applicableReturnType(ItrType.ITR_6)
                .defaultAssessmentYear("2026-27")
                .assessmentCategory("CORPORATE")
                .residentialStatus(ResidentialStatus.RESIDENT)
                .build();

        String response = mockMvc.perform(post("/api/v1/itr/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pan").value("AABCT1234A"))
                .andExpect(jsonPath("$.data.clientName").value("Titanium Logistics Pvt Ltd"))
                .andExpect(jsonPath("$.data.taxpayerType").value("COMPANY"))
                .andExpect(jsonPath("$.data.defaultItrType").value("ITR_6"))
                .andExpect(jsonPath("$.data.defaultAssessmentYear").value("2026-27"))
                .andReturn().getResponse().getContentAsString();

        String profileId = objectMapper.readTree(response).path("data").path("id").asText();

        // Get by ID
        mockMvc.perform(get("/api/v1/itr/profiles/" + profileId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pan").value("AABCT1234A"))
                .andExpect(jsonPath("$.data.defaultAssessmentYear").value("2026-27"));

        // Get by Client ID
        mockMvc.perform(get("/api/v1/itr/profiles/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pan").value("AABCT1234A"));

        // Update
        UpdateItrProfileRequest updateReq = UpdateItrProfileRequest.builder()
                .taxpayerType(TaxpayerType.COMPANY)
                .defaultItrType(ItrType.ITR_6)
                .applicableReturnType(ItrType.ITR_6)
                .defaultAssessmentYear("2027-28")
                .assessmentCategory("AUDIT_REQUIRED")
                .residentialStatus(ResidentialStatus.RESIDENT)
                .active(true)
                .status(ItrProfileStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/itr/profiles/" + profileId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultAssessmentYear").value("2027-28"))
                .andExpect(jsonPath("$.data.assessmentCategory").value("AUDIT_REQUIRED"));
    }

    @Test
    @DisplayName("Create ITR Profile: Automatically reuse PAN from Client when PAN is omitted in payload")
    void testCreateItrProfileReusingClientPan() throws Exception {
        CreateItrProfileRequest request = CreateItrProfileRequest.builder()
                .clientId(clientA.getId())
                .taxpayerType(TaxpayerType.COMPANY)
                .defaultItrType(ItrType.ITR_6)
                .defaultAssessmentYear("2026-27")
                .build();

        mockMvc.perform(post("/api/v1/itr/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.pan").value("AABCT1234A"))
                .andExpect(jsonPath("$.data.clientName").value("Titanium Logistics Pvt Ltd"));
    }

    @Test
    @DisplayName("Duplicate PAN: Rejects registering duplicate ITR profile with same PAN in same tenant")
    void testDuplicatePanRejectionInSameTenant() throws Exception {
        CreateItrProfileRequest request1 = CreateItrProfileRequest.builder()
                .clientId(clientA.getId())
                .pan("AABCT1234A")
                .taxpayerType(TaxpayerType.COMPANY)
                .build();

        mockMvc.perform(post("/api/v1/itr/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Attempt second registration with same PAN
        CreateItrProfileRequest request2 = CreateItrProfileRequest.builder()
                .displayName("Another Client")
                .pan("AABCT1234A")
                .taxpayerType(TaxpayerType.COMPANY)
                .build();

        mockMvc.perform(post("/api/v1/itr/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Multi-Tenant Isolation: Tenant B cannot access Tenant A ITR profile")
    void testMultiTenantIsolation() throws Exception {
        CreateItrProfileRequest request = CreateItrProfileRequest.builder()
                .clientId(clientA.getId())
                .pan("AABCT1234A")
                .taxpayerType(TaxpayerType.COMPANY)
                .build();

        String response = mockMvc.perform(post("/api/v1/itr/profiles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String profileId = objectMapper.readTree(response).path("data").path("id").asText();

        // Tenant B attempt -> 404 Not Found
        mockMvc.perform(get("/api/v1/itr/profiles/" + profileId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
