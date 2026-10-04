package com.taxoryn.module.udin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.udin.dto.CancelUdinRequest;
import com.taxoryn.module.udin.dto.CreateUdinRequest;
import com.taxoryn.module.udin.dto.UpdateUdinRequest;
import com.taxoryn.module.udin.dto.UpdateUdinVerificationRequest;
import com.taxoryn.module.udin.entity.UdinEntity;
import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import com.taxoryn.module.udin.repository.UdinRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UdinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UdinRepository udinRepository;

    private OrganizationEntity orgA;
    private UserEntity userA;
    private String tokenA;

    private OrganizationEntity orgB;
    private UserEntity userB;
    private String tokenB;

    private ClientEntity clientA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // Practice A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Singhal & Associates - " + UUID.randomUUID())
                .legalName("Singhal & Associates LLP")
                .email("admin." + UUID.randomUUID() + "@singhal.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin." + UUID.randomUUID() + "@singhal.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Anand")
                .lastName("Singhal")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_READ", "ORGANIZATION_UPDATE", "ORG_WRITE", "CLIENT_UPDATE")
        );

        TenantContext.setTenantId(orgA.getId());
        ClientEntity clA = ClientEntity.builder()
                .displayName("Zenith Software Pvt Ltd")
                .clientType(ClientEntity.ClientType.COMPANY)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .email("zenith." + UUID.randomUUID() + "@example.com")
                .pan("AAACZ1234E")
                .build();
        clA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clA);
        TenantContext.clear();

        // Practice B (Tenant Isolation)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Kedia & Partners - " + UUID.randomUUID())
                .legalName("Kedia & Partners")
                .email("admin." + UUID.randomUUID() + "@kedia.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@kedia.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Karan")
                .lastName("Kedia")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_READ", "ORGANIZATION_UPDATE", "ORG_WRITE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/udins creates UDIN record and GET /api/v1/udins/{id} retrieves it")
    void testCreateAndGetUdin() throws Exception {
        String testUdin = "24" + String.format("%016d", Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 16);
        CreateUdinRequest request = CreateUdinRequest.builder()
                .udin(testUdin)
                .clientId(clientA.getId())
                .documentType(UdinDocumentType.TAX_AUDIT_REPORT_3CA_3CD)
                .documentTitle("Tax Audit Form 3CA/3CD FY 2023-24")
                .documentDescription("Statutory tax audit under Section 44AB")
                .signatoryName("CA Anand Singhal")
                .signatoryMembershipNo("098765")
                .generationDate(LocalDate.now())
                .notes("Generated for client compliance")
                .build();

        String response = mockMvc.perform(post("/api/v1/udins")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.udin").value(testUdin))
                .andExpect(jsonPath("$.data.documentTitle").value("Tax Audit Form 3CA/3CD FY 2023-24"))
                .andExpect(jsonPath("$.data.signatoryName").value("CA Anand Singhal"))
                .andExpect(jsonPath("$.data.clientName").value("Zenith Software Pvt Ltd"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.verificationStatus").value("NOT_VERIFIED"))
                .andReturn().getResponse().getContentAsString();

        UUID udinId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/udins/" + udinId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(udinId.toString()))
                .andExpect(jsonPath("$.data.udin").value(testUdin));
    }

    @Test
    @DisplayName("Multi-tenant isolation: Practice B cannot access Practice A's UDIN records")
    void testTenantIsolation() throws Exception {
        String testUdin = "24" + String.format("%016d", Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 16);
        CreateUdinRequest request = CreateUdinRequest.builder()
                .udin(testUdin)
                .documentType(UdinDocumentType.NET_WORTH_CERTIFICATE)
                .documentTitle("Confidential Net Worth")
                .signatoryName("CA Anand Singhal")
                .generationDate(LocalDate.now())
                .build();

        String response = mockMvc.perform(post("/api/v1/udins")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID udinId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Practice B attempting to get Practice A's UDIN -> 404 Not Found
        mockMvc.perform(get("/api/v1/udins/" + udinId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Practice B list does not contain Practice A's UDIN
        mockMvc.perform(get("/api/v1/udins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + udinId + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Update Verification Status & Cancel UDIN flow")
    void testVerificationAndCancelFlow() throws Exception {
        String testUdin = "24" + String.format("%016d", Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 16);
        CreateUdinRequest request = CreateUdinRequest.builder()
                .udin(testUdin)
                .documentType(UdinDocumentType.GST_AUDIT_CERTIFICATE)
                .documentTitle("GST Certification")
                .signatoryName("CA Anand Singhal")
                .generationDate(LocalDate.now())
                .build();

        String response = mockMvc.perform(post("/api/v1/udins")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID udinId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // 1. Update verification
        UpdateUdinVerificationRequest verifyReq = UpdateUdinVerificationRequest.builder()
                .verificationStatus(UdinVerificationStatus.VERIFIED)
                .verificationSource("ICAI_PORTAL")
                .verificationRemarks("Matched with ICAI database successfully")
                .build();

        mockMvc.perform(patch("/api/v1/udins/" + udinId + "/verification")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.verificationRemarks").value("Matched with ICAI database successfully"));

        // 2. Cancel UDIN
        CancelUdinRequest cancelReq = CancelUdinRequest.builder()
                .reason("Client modified balance sheet")
                .build();

        mockMvc.perform(post("/api/v1/udins/" + udinId + "/cancel")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.notes").value(org.hamcrest.Matchers.containsString("Client modified balance sheet")));
    }

    @Test
    @DisplayName("GET /api/v1/udins/summary returns metrics")
    void testUdinSummaryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/udins/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").isNumber())
                .andExpect(jsonPath("$.data.activeCount").isNumber())
                .andExpect(jsonPath("$.data.verifiedCount").isNumber())
                .andExpect(jsonPath("$.data.unverifiedCount").isNumber());
    }
}
