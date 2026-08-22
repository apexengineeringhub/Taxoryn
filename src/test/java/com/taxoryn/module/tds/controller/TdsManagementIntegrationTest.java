package com.taxoryn.module.tds.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.tds.dto.CreateTdsProfileRequest;
import com.taxoryn.module.tds.dto.CreateTdsReturnRequest;
import com.taxoryn.module.tds.dto.TdsComputationRequest;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TdsManagementIntegrationTest {

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
    private PermissionRepository permissionRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

    @Autowired
    private TdsReturnRepository tdsReturnRepository;

    @Autowired
    private TdsChallanRepository tdsChallanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity organization;
    private UserEntity adminUser;
    private String adminToken;
    private ClientEntity client;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Test Practice Firm")
                .email("tds-test-" + System.currentTimeMillis() + "@practice.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        List<String> codes = List.of(
                "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_DELETE"
        );
        Set<PermissionEntity> perms = new HashSet<>();
        for (String c : codes) {
            PermissionEntity p = permissionRepository.findByCode(c)
                    .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                            .code(c)
                            .name(c)
                            .module("TDS")
                            .build()));
            perms.add(p);
        }

        RoleEntity role = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Admin")
                .isSystemRole(true)
                .permissions(perms)
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + System.currentTimeMillis() + "@tdspractice.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Tds")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                organization.getId(),
                adminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_DELETE")
        );

        client = clientRepository.save(ClientEntity.builder()
                .displayName("TDS Client Corp")
                .legalName("TDS Client Corporation Pvt Ltd")
                .pan("ABCDE1234F")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build());
        client.setOrganizationId(organization.getId());
        client = clientRepository.save(client);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/tds/profiles - should register a new TAN profile")
    void testCreateProfileEndpoint() throws Exception {
        CreateTdsProfileRequest request = CreateTdsProfileRequest.builder()
                .clientId(client.getId())
                .tan("BLRP99887A")
                .deductorType(DeductorType.COMPANY)
                .responsiblePersonName("Sanjay Verma")
                .responsiblePersonPan("ABCPS9876K")
                .build();

        mockMvc.perform(post("/api/v1/tds/profiles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tan").value("BLRP99887A"))
                .andExpect(jsonPath("$.data.responsiblePersonName").value("Sanjay Verma"));
    }

    @Test
    @DisplayName("POST /api/v1/tds/calculator/compute - should compute statutory TDS with rates and late fee")
    void testTdsCalculatorEndpoint() throws Exception {
        TdsComputationRequest req = TdsComputationRequest.builder()
                .sectionCode("194J")
                .amount(new BigDecimal("100000.00"))
                .deducteeType(com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.DeducteeType.NON_COMPANY)
                .validPanProvided(true)
                .build();

        mockMvc.perform(post("/api/v1/tds/calculator/compute")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionCode").value("194J"))
                .andExpect(jsonPath("$.data.effectiveRate").value(10.00))
                .andExpect(jsonPath("$.data.totalTaxDeducted").value(10000.00))
                .andExpect(jsonPath("$.data.netPayableToDeductee").value(90000.00));
    }

    @Test
    @DisplayName("GET /api/v1/tds/calculator/rates - should return master section rates catalog")
    void testGetSectionRatesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/tds/calculator/rates")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
