package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeRisk;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaxNoticeModuleEntitlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaxNoticeRepository noticeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModuleConfigurationService moduleConfigurationService;

    @Autowired
    private com.taxoryn.module.moduleconfig.repository.ProductModuleRepository productModuleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity organization;
    private UserEntity adminUser;
    private ClientEntity client;
    private TaxNoticeEntity notice;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDb();

        if (productModuleRepository.findByCode(ProductModuleCode.TAX_NOTICES).isEmpty()) {
            productModuleRepository.save(com.taxoryn.module.moduleconfig.entity.ProductModuleEntity.builder()
                    .code(ProductModuleCode.TAX_NOTICES)
                    .name("Tax Notice Management")
                    .description("Assessment notices, hearing schedules, and response drafting.")
                    .category(com.taxoryn.module.moduleconfig.model.ProductModuleCategory.BUSINESS)
                    .status("ACTIVE")
                    .enabledByDefault(true)
                    .displayOrder(11)
                    .build());
        }

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Entitlement Test Firm")
                .email("admin@entitlementfirm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .name("Admin Role")
                .code("ORG_ADMIN")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Secure123!"))
                .firstName("Firm")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        client = clientRepository.save(ClientEntity.builder()
                .displayName("Entitlement Client Ltd")
                .legalName("Entitlement Client Limited")
                .pan("ENTIT1234K")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        notice = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(client.getId())
                .noticeNumber("NOT-ENTIT-" + UUID.randomUUID().toString().substring(0, 6))
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("Assessment Notice")
                .subject("Notice regarding AY 2025-26")
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.HIGH)
                .riskLevel(NoticeRisk.MEDIUM)
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(10))
                .build());

        Set<String> perms = Set.of("NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE", "CLIENT_VIEW");
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(adminUser.getId(), organization.getId(), adminUser.getEmail(), Set.of("ORG_ADMIN"), perms);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
        TenantContext.clear();
    }

    private void cleanDb() {
        TenantContext.clear();
        noticeRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private void setSecurityContext() {
        SecurityUser securityUser = SecurityUser.builder()
                .userId(adminUser.getId())
                .organizationId(organization.getId())
                .email(adminUser.getEmail())
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE", "CLIENT_VIEW", "MODULE_CONFIGURE"))
                .enabled(true)
                .build();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(organization.getId());
    }

    @Test
    @DisplayName("Module Entitlement: When TAX_NOTICES is disabled, endpoints return 403 and Client360 suppresses notice data")
    void testModuleDisabledReturns403AndSuppressesClient360() throws Exception {
        // Disable TAX_NOTICES module
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.TAX_NOTICES, false);

        // Notice endpoints return 403 Forbidden
        mockMvc.perform(get("/api/v1/notices")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/tax-notices/" + notice.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // Client 360 suppresses taxNotices field (returns null, no data leakage)
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taxNotices").value(nullValue()));

        // Enable TAX_NOTICES module
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.TAX_NOTICES, true);

        // Notice endpoints return 200 OK
        mockMvc.perform(get("/api/v1/notices")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // Client 360 returns taxNotices
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taxNotices", hasSize(1)))
                .andExpect(jsonPath("$.data.taxNotices[0].id").value(notice.getId().toString()));
    }
}
