package com.taxoryn.module.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gst.entity.GstRegistrationEntity;
import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import com.taxoryn.module.gst.repository.GstRegistrationRepository;
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
public class GstModuleEntitlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private GstRegistrationRepository gstRegistrationRepository;

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
    private GstRegistrationEntity gstReg;
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
                .name("Entitlement Test Firm - " + UUID.randomUUID())
                .legalName("Entitlement Test Firm LLP")
                .email("admin." + UUID.randomUUID() + "@entitlement.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Pune HQ")
                .code("PUN-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Pune")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin." + UUID.randomUUID() + "@entitlement.com")
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
                Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Alpha Traders Pvt Ltd")
                .legalName("Alpha Traders Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCA1234A")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        GstRegistrationEntity reg = GstRegistrationEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .gstin("27AABCA1234A1Z2")
                .legalName("Alpha Traders Private Limited")
                .tradeName("Alpha Traders")
                .registrationType(GstRegistrationType.REGULAR)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .stateCode("27")
                .build();
        reg.setOrganizationId(org.getId());
        gstReg = gstRegistrationRepository.save(reg);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        organizationModuleRepository.deleteAll();
        gstRegistrationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("When GST Module is disabled: GST endpoints return 403 and Client 360 suppresses GST data")
    void testGstModuleDisabledBehavior() throws Exception {
        // Explicitly disable GST module for this organization
        OrganizationModuleEntity modDisabled = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.GST)
                .enabled(false)
                .build();
        modDisabled.setOrganizationId(org.getId());
        organizationModuleRepository.save(modDisabled);

        // 1. Gst Registration endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstReg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 2. Gst Workspace endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/gst/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 3. Client 360 -> 200 OK, but gstRegistrations must be null (suppressed, no data leakage)
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Alpha Traders Pvt Ltd"))
                .andExpect(jsonPath("$.data.gstRegistrations").doesNotExist());
    }

    @Test
    @DisplayName("When GST Module is enabled: GST endpoints are accessible and Client 360 includes GST registrations")
    void testGstModuleEnabledBehavior() throws Exception {
        // Explicitly enable GST module for this organization
        OrganizationModuleEntity modEnabled = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.GST)
                .enabled(true)
                .build();
        modEnabled.setOrganizationId(org.getId());
        organizationModuleRepository.save(modEnabled);

        // 1. Gst Registration endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstReg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstin").value("27AABCA1234A1Z2"));

        // 2. Gst Workspace endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/gst/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrations[0].gstin").value("27AABCA1234A1Z2"));

        // 3. Client 360 -> 200 OK with gstRegistrations populated
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Alpha Traders Pvt Ltd"))
                .andExpect(jsonPath("$.data.gstRegistrations[0].gstin").value("27AABCA1234A1Z2"));
    }
}
