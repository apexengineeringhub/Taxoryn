package com.taxoryn.module.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.dto.CreateGstRegistrationRequest;
import com.taxoryn.module.gst.dto.GstVerifyGstinRequest;
import com.taxoryn.module.gst.dto.UpdateGstRegistrationRequest;
import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import com.taxoryn.module.gst.repository.GstRegistrationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GstRegistrationIntegrationTest {

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
    private GstRegistrationRepository gstRegistrationRepository;

    @Autowired
    private GovernmentConnectionService govConnectionService;

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
                .organizationId(orgA.getId())
                .email("partner." + UUID.randomUUID() + "@singhania.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Aditya")
                .lastName("Singhania")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "GST_DELETE", "CLIENT_VIEW")
        );

        ClientEntity client = ClientEntity.builder()
                .displayName("Titanium Logistics Pvt Ltd")
                .legalName("Titanium Logistics Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCT1234A")
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(client);

        // Tenant B setup
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants - " + UUID.randomUUID())
                .legalName("Apex Tax Consultants LLP")
                .email("admin." + UUID.randomUUID() + "@apex.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Apex")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE")
        );

        TenantContext.setTenantId(orgA.getId());
        GovConnectionDto connA = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Maharashtra GST Gateway")
                .build());
        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("mumbai_gst_***")
                .rawSecret("SecretMumbai123")
                .build());
        govConnectionService.activateConnection(connA.getId());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        gstRegistrationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create and Retrieve GST Registration successfully")
    void testCreateAndGetGstRegistration() throws Exception {
        CreateGstRegistrationRequest request = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .gstin("27AABCT1234A1Z5")
                .legalName("Titanium Logistics Private Limited")
                .tradeName("Titanium Freight")
                .registrationType(GstRegistrationType.REGULAR)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .registrationDate(LocalDate.of(2025, 4, 1))
                .stateCode("27")
                .jurisdiction("Mumbai Central Ward")
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();

        String response = mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gstin").value("27AABCT1234A1Z5"))
                .andExpect(jsonPath("$.data.clientName").value("Titanium Logistics Pvt Ltd"))
                .andExpect(jsonPath("$.data.registrationType").value("REGULAR"))
                .andExpect(jsonPath("$.data.filingFrequency").value("MONTHLY"))
                .andReturn().getResponse().getContentAsString();

        String registrationId = objectMapper.readTree(response).path("data").path("id").asText();

        // Get by ID
        mockMvc.perform(get("/api/v1/gst/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstin").value("27AABCT1234A1Z5"));

        // Get by Client ID
        mockMvc.perform(get("/api/v1/gst/registrations/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gstin").value("27AABCT1234A1Z5"));
    }

    @Test
    @DisplayName("Support Multiple GST Registrations for a Client across different states")
    void testMultipleGstRegistrationsPerClient() throws Exception {
        CreateGstRegistrationRequest reg1 = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .gstin("27AABCT1234A1Z5")
                .legalName("Titanium Logistics Pvt Ltd")
                .stateCode("27")
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();

        CreateGstRegistrationRequest reg2 = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .gstin("29AABCT1234A1Z1")
                .legalName("Titanium Logistics Pvt Ltd (Karnataka Branch)")
                .stateCode("29")
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/gst/registrations/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Reject Duplicate GSTIN within the same organization tenant")
    void testRejectDuplicateGstin() throws Exception {
        CreateGstRegistrationRequest reg1 = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .gstin("27AABCT1234A1Z5")
                .build();

        mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg1)))
                .andExpect(status().isCreated());

        // Duplicate GSTIN in same org -> 409 Conflict
        mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg1)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Validate GSTIN Format")
    void testValidateGstinFormat() throws Exception {
        CreateGstRegistrationRequest invalidReg = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .gstin("INVALID_GSTIN_123")
                .build();

        mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update and Delete GST Registration")
    void testUpdateAndDeleteGstRegistration() throws Exception {
        CreateGstRegistrationRequest request = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .gstin("27AABCT1234A1Z5")
                .tradeName("Initial Trade Name")
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();

        String response = mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String registrationId = objectMapper.readTree(response).path("data").path("id").asText();

        // Update
        UpdateGstRegistrationRequest updateReq = UpdateGstRegistrationRequest.builder()
                .tradeName("Updated Trade Name")
                .filingFrequency(GstFilingFrequency.QUARTERLY)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/gst/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeName").value("Updated Trade Name"))
                .andExpect(jsonPath("$.data.filingFrequency").value("QUARTERLY"));

        // Delete
        mockMvc.perform(delete("/api/v1/gst/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/gst/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Multi-Tenant Isolation: Tenant B cannot access Tenant A GST registration")
    void testMultiTenantIsolation() throws Exception {
        CreateGstRegistrationRequest request = CreateGstRegistrationRequest.builder()
                .clientId(clientA.getId())
                .gstin("27AABCT1234A1Z5")
                .build();

        String response = mockMvc.perform(post("/api/v1/gst/registrations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String registrationId = objectMapper.readTree(response).path("data").path("id").asText();

        // Tenant B attempt -> 404
        mockMvc.perform(get("/api/v1/gst/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify GSTIN Taxpayer registration via Government Integration Framework endpoint")
    void testVerifyGstinEndpoint() throws Exception {
        GstVerifyGstinRequest req = GstVerifyGstinRequest.builder()
                .gstin("27AAAPL1234C1ZV")
                .build();

        mockMvc.perform(post("/api/v1/gst/registrations/verify")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.gstin").value("27AAAPL1234C1ZV"))
                .andExpect(jsonPath("$.data.legalName").value("Apex Enterprises Private Limited"))
                .andExpect(jsonPath("$.data.tradeName").value("Apex Solutions"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.stateCode").value("27"));
    }

    @Test
    @DisplayName("Verify GSTIN with invalid format returns 400 Bad Request")
    void testVerifyGstinInvalidFormatEndpoint() throws Exception {
        GstVerifyGstinRequest req = GstVerifyGstinRequest.builder()
                .gstin("INVALID_123")
                .build();

        mockMvc.perform(post("/api/v1/gst/registrations/verify")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify GSTIN with NOT_FOUND simulation returns 200 with valid=false")
    void testVerifyGstinNotFoundEndpoint() throws Exception {
        GstVerifyGstinRequest req = GstVerifyGstinRequest.builder()
                .gstin("27AAAPL1234C1ZV")
                .options(java.util.Map.of("mockOutcome", "NOT_FOUND"))
                .build();

        mockMvc.perform(post("/api/v1/gst/registrations/verify")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("NOT_FOUND"));
    }
}
