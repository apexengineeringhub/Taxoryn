package com.taxoryn.module.tds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
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
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
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
public class TdsModuleEntitlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

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
    private TdsProfileEntity tdsProfile;
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
                .name("TDS Entitlement Test Firm - " + UUID.randomUUID())
                .legalName("TDS Entitlement Test Firm LLP")
                .email("admin." + UUID.randomUUID() + "@tdsentitlement.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Bengaluru HQ")
                .code("BLR-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin." + UUID.randomUUID() + "@tdsentitlement.com")
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
                Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Acme Global Services")
                .legalName("Acme Global Services Pvt Ltd")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AABCA1234F")
                .tan("BLRA12345A")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        TdsProfileEntity profile = TdsProfileEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .tan("BLRA12345A")
                .deductorType(DeductorType.COMPANY)
                .branchDivisionName("Main Branch")
                .status(TdsProfileStatus.ACTIVE)
                .active(true)
                .build();
        profile.setOrganizationId(org.getId());
        tdsProfile = tdsProfileRepository.save(profile);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        organizationModuleRepository.deleteAll();
        tdsProfileRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("When TDS Module is disabled: TDS endpoints return 403 and Client 360 suppresses TDS data")
    void testTdsModuleDisabledBehavior() throws Exception {
        // Explicitly disable TDS module for this organization
        OrganizationModuleEntity modDisabled = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.TDS)
                .enabled(false)
                .build();
        modDisabled.setOrganizationId(org.getId());
        organizationModuleRepository.save(modDisabled);

        // 1. TDS Profile endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tds/profiles/" + tdsProfile.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 2. TDS Workspace endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tds/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 3. Client 360 -> 200 OK, but tdsProfile must be null (suppressed, zero data leakage)
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Acme Global Services"))
                .andExpect(jsonPath("$.data.tdsProfile").doesNotExist());
    }

    @Test
    @DisplayName("When TDS Module is enabled: TDS endpoints are accessible and Client 360 includes TDS profile")
    void testTdsModuleEnabledBehavior() throws Exception {
        // Explicitly enable TDS module for this organization
        OrganizationModuleEntity modEnabled = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.TDS)
                .enabled(true)
                .build();
        modEnabled.setOrganizationId(org.getId());
        organizationModuleRepository.save(modEnabled);

        // 1. TDS Profile endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/tds/profiles/" + tdsProfile.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tan").value("BLRA12345A"));

        // 2. TDS Workspace endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/tds/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.tan").value("BLRA12345A"));

        // 3. Client 360 -> 200 OK with tdsProfile populated
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Acme Global Services"))
                .andExpect(jsonPath("$.data.tdsProfile.tan").value("BLRA12345A"));
    }
}
