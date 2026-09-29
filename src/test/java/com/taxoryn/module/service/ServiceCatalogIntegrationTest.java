package com.taxoryn.module.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.service.dto.CreateServiceRequest;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ServiceCatalogIntegrationTest {

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
    private ServiceRepository serviceRepository;

    private OrganizationEntity org;
    private UserEntity user;
    private String token;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        seedDefaultServicesIfMissing();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Sharma & Co - " + UUID.randomUUID())
                .legalName("Sharma & Co LLP")
                .email("admin." + UUID.randomUUID() + "@sharma.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org.getId());

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin." + UUID.randomUUID() + "@sharma.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Aakash")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
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
    @DisplayName("GET /api/v1/services returns seeded catalog with GST, TDS, ITR, Audit, Notice, Advisory services")
    void testGetServicesCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'GST_COMPLIANCE')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'TDS_COMPLIANCE')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'ITR_FILING')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'TAX_AUDIT')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'NOTICE_MANAGEMENT')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'TAX_ADVISORY')]").exists());
    }

    @Test
    @DisplayName("POST /api/v1/services creates custom practice service and retrieves by ID")
    void testCreateAndGetCustomService() throws Exception {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("CUSTOM_ROC_COMPLIANCE_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("Annual ROC Company Secretarial Compliance")
                .description("MGT-7, AOC-4 filing and director KYC")
                .category(ServiceCategory.ADVISORY)
                .build();

        String response = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceName").value("Annual ROC Company Secretarial Compliance"))
                .andExpect(jsonPath("$.data.category").value("ADVISORY"))
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(serviceId.toString()))
                .andExpect(jsonPath("$.data.serviceName").value("Annual ROC Company Secretarial Compliance"));
    }

    private void seedDefaultServicesIfMissing() {
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("GST_COMPLIANCE").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("GST_COMPLIANCE")
                    .serviceName("GST Compliance & Returns")
                    .category(ServiceCategory.GST)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("GST")
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TDS_COMPLIANCE").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("TDS_COMPLIANCE")
                    .serviceName("TDS Compliance & Returns")
                    .category(ServiceCategory.TDS)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("TDS")
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("ITR_FILING").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("ITR_FILING")
                    .serviceName("Income Tax Returns (ITR)")
                    .category(ServiceCategory.ITR)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("ITR")
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TAX_AUDIT").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("TAX_AUDIT")
                    .serviceName("Tax Audit (Form 3CD/3CA/3CB)")
                    .category(ServiceCategory.AUDIT)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("AUDIT")
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("NOTICE_MANAGEMENT").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("NOTICE_MANAGEMENT")
                    .serviceName("Tax Notice & Assessment Management")
                    .category(ServiceCategory.NOTICE)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("NOTICE")
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TAX_ADVISORY").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("TAX_ADVISORY")
                    .serviceName("Tax Advisory & Opinion")
                    .category(ServiceCategory.ADVISORY)
                    .status(ServiceStatus.ACTIVE)
                    .build());
        }
    }
}
