package com.taxoryn.module.itr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ItrModuleEntitlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ItrProfileRepository itrProfileRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity loc;
    private UserEntity user;
    private ClientEntity client;
    private ItrProfileEntity itrProfile;
    private String token;

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

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR Entitlement Test Firm - " + UUID.randomUUID())
                .legalName("ITR Entitlement Test Firm LLP")
                .email("admin." + UUID.randomUUID() + "@itrentitlement.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("BOM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin." + UUID.randomUUID() + "@itrentitlement.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Vikram Malhotra")
                .legalName("Vikram Malhotra")
                .clientType(ClientType.INDIVIDUAL)
                .pan("ABCDE1234F")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        ItrProfileEntity profile = ItrProfileEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .pan("ABCDE1234F")
                .taxpayerType(ItrProfileEntity.TaxpayerType.INDIVIDUAL)
                .residentialStatus(ItrProfileEntity.ResidentialStatus.RESIDENT)
                .defaultItrType(ItrProfileEntity.ItrType.ITR_1)
                .applicableReturnType(ItrProfileEntity.ItrType.ITR_1)
                .defaultAssessmentYear("2026-27")
                .assessmentCategory("REGULAR")
                .active(true)
                .build();
        profile.setOrganizationId(org.getId());
        itrProfile = itrProfileRepository.save(profile);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        organizationModuleRepository.deleteAll();
        itrProfileRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("When ITR Module is disabled: ITR endpoints return 403 and Client 360 suppresses ITR data")
    void testItrModuleDisabledBehavior() throws Exception {
        // Explicitly disable ITR module for this organization
        OrganizationModuleEntity modDisabled = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.ITR)
                .enabled(false)
                .build();
        modDisabled.setOrganizationId(org.getId());
        organizationModuleRepository.save(modDisabled);

        // 1. ITR Profile endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/itr/profiles/" + itrProfile.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 2. ITR Workspace endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/itr/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 3. Client 360 -> 200 OK, but itrProfile must be null (suppressed, zero data leakage)
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Vikram Malhotra"))
                .andExpect(jsonPath("$.data.itrProfile").doesNotExist());
    }

    @Test
    @DisplayName("When ITR Module is enabled: ITR endpoints are accessible and Client 360 includes ITR profile")
    void testItrModuleEnabledBehavior() throws Exception {
        // Explicitly enable ITR module for this organization
        OrganizationModuleEntity modEnabled = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.ITR)
                .enabled(true)
                .build();
        modEnabled.setOrganizationId(org.getId());
        organizationModuleRepository.save(modEnabled);

        // 1. ITR Profile endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/itr/profiles/" + itrProfile.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pan").value("ABCDE1234F"));

        // 2. ITR Workspace endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/itr/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.pan").value("ABCDE1234F"));

        // 3. Client 360 -> 200 OK with itrProfile populated
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Vikram Malhotra"))
                .andExpect(jsonPath("$.data.itrProfile.pan").value("ABCDE1234F"));
    }
}
