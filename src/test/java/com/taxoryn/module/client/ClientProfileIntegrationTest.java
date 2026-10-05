package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.Client360Dto;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientProfileCompletenessDto;
import com.taxoryn.module.client.dto.ClientProfileDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientProfileRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientContextService;
import com.taxoryn.module.client.service.ClientProfileCompletenessEvaluator;
import com.taxoryn.module.client.service.ClientService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientContextService clientContextService;

    @Autowired
    private ClientProfileCompletenessEvaluator completenessEvaluator;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity adminA;
    private UserEntity adminB;
    private String tokenA;
    private String tokenB;
    private RoleEntity adminRole;

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_ORG_ADMIN")
                .name("Practice Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory Services " + UUID.randomUUID())
                .email("apex." + UUID.randomUUID() + "@advisory.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Horizon Tax Partners " + UUID.randomUUID())
                .email("horizon." + UUID.randomUUID() + "@tax.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin." + UUID.randomUUID() + "@apex.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Apex")
                .lastName("Principal")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        adminB = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin." + UUID.randomUUID() + "@horizon.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Horizon")
                .lastName("Principal")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminA.getId(), tenantA.getId(), adminA.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_WRITE")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminB.getId(), tenantB.getId(), adminB.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_WRITE")
        );

        com.taxoryn.core.security.SecurityUser principalA = com.taxoryn.core.security.SecurityUser.builder()
                .userId(adminA.getId())
                .organizationId(tenantA.getId())
                .email(adminA.getEmail())
                .roles(Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"))
                .permissions(Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_WRITE"))
                .enabled(true)
                .build();

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principalA, null, principalA.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        TenantContext.setTenantId(tenantA.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Phase 28.2 - HUF Entity classification and business profile creation")
    void testHufClientCreationAndProfile() {
        CreateClientRequest request = CreateClientRequest.builder()
                .displayName("Sharma HUF Family Estate")
                .legalName("Sharma Hindu Undivided Family")
                .clientType(ClientType.HUF)
                .pan("AABHS1234D")
                .email("karta@sharmahuf.test")
                .phone("+919876543210")
                .city("Jaipur")
                .state("Rajasthan")
                .stateCode("08")
                .pincode("302001")
                .businessActivity("Real Estate & Asset Management")
                .industry("Property & Wealth Management")
                .businessScale("MEDIUM")
                .build();

        ClientDto created = clientService.createClient(request);

        assertThat(created).isNotNull();
        assertThat(created.getClientType()).isEqualTo(ClientType.HUF);
        assertThat(created.getPan()).isEqualTo("AABHS1234D");
        assertThat(created.getStateCode()).isEqualTo("08");
        assertThat(created.getBusinessActivity()).isEqualTo("Real Estate & Asset Management");
        assertThat(created.getIndustry()).isEqualTo("Property & Wealth Management");
        assertThat(created.getBusinessScale()).isEqualTo("MEDIUM");
        assertThat(created.getCompleteness()).isNotNull();
        assertThat(created.getCompleteness().getCompletionPercentage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Phase 28.2 - Deterministic Profile Completeness Evaluation across 5 sections")
    void testProfileCompletenessEvaluation() {
        // Step 1: Minimal client (only identity)
        CreateClientRequest minimalReq = CreateClientRequest.builder()
                .displayName("Minimal Incomplete Enterprise")
                .clientType(ClientType.COMPANY)
                .build();

        ClientDto minimalClient = clientService.createClient(minimalReq);
        ClientProfileCompletenessDto minimalCompleteness = clientService.getProfileCompleteness(minimalClient.getId());

        assertThat(minimalCompleteness.isComplete()).isFalse();
        assertThat(minimalCompleteness.getCompletionPercentage()).isEqualTo(20); // 1 of 5
        assertThat(minimalCompleteness.getCompletedSections()).containsExactly("IDENTITY");
        assertThat(minimalCompleteness.getMissingSections()).containsExactlyInAnyOrder("CONTACT", "ADDRESS", "STATUTORY", "BUSINESS");

        // Step 2: Full client (all 5 sections)
        CreateClientRequest fullReq = CreateClientRequest.builder()
                .displayName("Apex Global Technologies Pvt Ltd")
                .legalName("Apex Global Technologies Private Limited")
                .tradeName("Apex Global Tech")
                .clientType(ClientType.COMPANY)
                .pan("AAACA1234C")
                .gstin("27AAACA1234C1Z5")
                .tan("MUMA12345B")
                .cin("U72200MH2020PTC123456")
                .email("finance@apextech.test")
                .phone("+919876543210")
                .addressLine1("Tower B, Tech Park")
                .city("Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("400001")
                .country("India")
                .businessActivity("Software Development & IT SaaS")
                .industry("Information Technology")
                .businessScale("LARGE")
                .build();

        ClientDto fullClient = clientService.createClient(fullReq);
        ClientProfileCompletenessDto fullCompleteness = clientService.getProfileCompleteness(fullClient.getId());

        assertThat(fullCompleteness.isComplete()).isTrue();
        assertThat(fullCompleteness.getCompletionPercentage()).isEqualTo(100);
        assertThat(fullCompleteness.getCompletedSections()).containsExactlyInAnyOrder("IDENTITY", "CONTACT", "ADDRESS", "STATUTORY", "BUSINESS");
        assertThat(fullCompleteness.getMissingSections()).isEmpty();
    }

    @Test
    @DisplayName("Phase 28.2 - Profile Update and Normalization")
    void testUpdateClientProfileAndNormalization() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Initial Enterprise")
                .clientType(ClientType.LLP)
                .build();

        ClientDto created = clientService.createClient(req);

        UpdateClientProfileRequest updateReq = UpdateClientProfileRequest.builder()
                .legalName("   Updated Enterprise LLP   ")
                .tradeName("  Enterprise Solutions  ")
                .clientType(ClientType.LLP)
                .pan("aabcl1234k") // lowercase - should normalize to uppercase
                .gstin("27aabcl1234k1z5") // lowercase - should normalize to uppercase
                .tan("mumc12345a") // lowercase - should normalize to uppercase
                .email("   INFO@ENTERPRISE.TEST   ") // uppercase - should normalize to lowercase
                .phone("  9876543210  ")
                .city("Pune")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("411001")
                .country("India")
                .businessActivity("Management Consultancy")
                .industry("Professional Services")
                .businessScale("MEDIUM")
                .build();

        ClientProfileDto updatedProfile = clientService.updateClientProfile(created.getId(), updateReq);

        assertThat(updatedProfile.getLegalName()).isEqualTo("Updated Enterprise LLP");
        assertThat(updatedProfile.getTradeName()).isEqualTo("Enterprise Solutions");
        assertThat(updatedProfile.getPan()).isEqualTo("AABCL1234K");
        assertThat(updatedProfile.getGstin()).isEqualTo("27AABCL1234K1Z5");
        assertThat(updatedProfile.getTan()).isEqualTo("MUMC12345A");
        assertThat(updatedProfile.getEmail()).isEqualTo("info@enterprise.test");
        assertThat(updatedProfile.getPhone()).isEqualTo("9876543210");
        assertThat(updatedProfile.getBusinessActivity()).isEqualTo("Management Consultancy");
        assertThat(updatedProfile.getIndustry()).isEqualTo("Professional Services");
        assertThat(updatedProfile.getBusinessScale()).isEqualTo("MEDIUM");
        assertThat(updatedProfile.getCompleteness().isComplete()).isTrue();
    }

    @Test
    @DisplayName("Phase 28.2 - Statutory Identifier format validation")
    void testStatutoryFormatValidation() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Validation Test Corp")
                .clientType(ClientType.COMPANY)
                .build();

        ClientDto created = clientService.createClient(req);

        // Invalid PAN format -> 400 Bad Request
        mockMvc.perform(put("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateClientProfileRequest.builder()
                                .pan("INVALID_PAN")
                                .build())))
                .andExpect(status().isBadRequest());

        // Invalid GSTIN format -> 400 Bad Request
        mockMvc.perform(put("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateClientProfileRequest.builder()
                                .gstin("INVALID_GSTIN")
                                .build())))
                .andExpect(status().isBadRequest());

        // Invalid TAN format -> 400 Bad Request
        mockMvc.perform(put("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateClientProfileRequest.builder()
                                .tan("INVALID_TAN")
                                .build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Phase 28.2 - REST Endpoints: GET /profile, PUT /profile, GET /profile/completeness with tenant isolation")
    void testProfileRestEndpointsAndTenantIsolation() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Apex Core Client")
                .legalName("Apex Core Client Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AAACA9999Z")
                .email("core@apex.test")
                .businessActivity("Cloud Infrastructure")
                .industry("IT")
                .businessScale("ENTERPRISE")
                .build();

        ClientDto created = clientService.createClient(req);

        // 1. GET /profile (Tenant A) -> 200 OK
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(created.getId().toString()))
                .andExpect(jsonPath("$.data.pan").value("AAACA9999Z"))
                .andExpect(jsonPath("$.data.businessActivity").value("Cloud Infrastructure"))
                .andExpect(jsonPath("$.data.businessScale").value("ENTERPRISE"));

        // 2. GET /profile/completeness (Tenant A) -> 200 OK
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/profile/completeness")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completionPercentage").isNumber());

        // 3. PUT /profile (Tenant A) -> 200 OK
        UpdateClientProfileRequest updateReq = UpdateClientProfileRequest.builder()
                .legalName("Apex Core Client Private Limited (Updated)")
                .clientType(ClientType.COMPANY)
                .pan("AAACA9999Z")
                .businessActivity("AI & Cloud Infrastructure")
                .industry("Software")
                .businessScale("ENTERPRISE")
                .build();

        mockMvc.perform(put("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.legalName").value("Apex Core Client Private Limited (Updated)"))
                .andExpect(jsonPath("$.data.businessActivity").value("AI & Cloud Infrastructure"));

        // 4. Tenant B attempts GET /profile -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 5. Tenant B attempts PUT /profile -> 404 NOT FOUND
        mockMvc.perform(put("/api/v1/clients/" + created.getId() + "/profile")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // 6. Tenant B attempts GET /profile/completeness -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/profile/completeness")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Phase 28.2 - ClientContext and Client360 reflect profile fields and completeness")
    void testClientContextAndClient360Enrichment() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Comprehensive Portfolio Client")
                .legalName("Comprehensive Portfolio Client Ltd")
                .clientType(ClientType.COMPANY)
                .pan("AAACP5555M")
                .email("portfolio@client.test")
                .city("Bengaluru")
                .state("Karnataka")
                .stateCode("29")
                .pincode("560001")
                .businessActivity("Fintech Platform")
                .industry("Financial Services")
                .businessScale("LARGE")
                .build();

        ClientDto created = clientService.createClient(req);

        // Context summary enrichment
        ClientContextSummaryDto context = clientContextService.requireClientContext(created.getId());
        assertThat(context.getBusinessActivity()).isEqualTo("Fintech Platform");
        assertThat(context.getIndustry()).isEqualTo("Financial Services");
        assertThat(context.getBusinessScale()).isEqualTo("LARGE");
        assertThat(context.getStateCode()).isEqualTo("29");
        assertThat(context.getCity()).isEqualTo("Bengaluru");
        assertThat(context.getCompleteness()).isNotNull();
        assertThat(context.getCompleteness().isComplete()).isTrue();

        // 360 enrichment
        Client360Dto client360 = clientService.getClient360(created.getId());
        assertThat(client360.getProfile()).isNotNull();
        assertThat(client360.getProfile().getBusinessActivity()).isEqualTo("Fintech Platform");
        assertThat(client360.getCompleteness()).isNotNull();
        assertThat(client360.getCompleteness().isComplete()).isTrue();
    }
}
