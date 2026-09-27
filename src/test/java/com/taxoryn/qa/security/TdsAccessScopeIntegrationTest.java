package com.taxoryn.qa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientUserAssignmentEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
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
import com.taxoryn.module.user.entity.UserLocationEntity;
import com.taxoryn.module.user.repository.UserLocationRepository;
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
public class TdsAccessScopeIntegrationTest {

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
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;

    private LocationEntity loc1Org1;
    private LocationEntity loc2Org1;

    private UserEntity adminUserOrg1;
    private UserEntity loc1Practitioner;
    private UserEntity clientAStaff;
    private UserEntity adminUserOrg2;

    private String adminTokenOrg1;
    private String loc1PractitionerToken;
    private String clientAStaffToken;
    private String adminTokenOrg2;

    private ClientEntity clientAOrg1; // in loc1Org1, assigned to clientAStaff
    private ClientEntity clientBOrg1; // in loc2Org1, unassigned
    private ClientEntity clientOrg2;

    private TdsProfileEntity profileAOrg1;
    private TdsProfileEntity profileBOrg1;
    private TdsProfileEntity profileOrg2;

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

        RoleEntity practitionerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTITIONER")
                        .name("Practitioner")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Staff")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // --- Organization 1 ---
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory LLP - " + UUID.randomUUID())
                .legalName("Apex Advisory LLP")
                .email("admin." + UUID.randomUUID() + "@apex.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l1 = LocationEntity.builder()
                .name("Bengaluru Central")
                .code("BLR-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l1.setOrganizationId(org1.getId());
        loc1Org1 = locationRepository.save(l1);

        LocationEntity l2 = LocationEntity.builder()
                .name("Mysuru Branch")
                .code("MYS-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mysuru")
                .state("Karnataka")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        l2.setOrganizationId(org1.getId());
        loc2Org1 = locationRepository.save(l2);

        // 1. Firm Admin (Org 1)
        adminUserOrg1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("admin." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Amit")
                .lastName("Shah")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());
        adminTokenOrg1 = jwtTokenProvider.generateAccessToken(
                adminUserOrg1.getId(),
                org1.getId(),
                adminUserOrg1.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "CLIENT_VIEW")
        );

        // 2. Location 1 Practitioner (restricted to loc1Org1)
        loc1Practitioner = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("practitioner." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Prashant")
                .lastName("Joshi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(practitionerRole))
                .build());
        userLocationRepository.save(UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(loc1Practitioner.getId())
                        .locationId(loc1Org1.getId())
                        .build())
                .organizationId(org1.getId())
                .build());
        loc1PractitionerToken = jwtTokenProvider.generateAccessToken(
                loc1Practitioner.getId(),
                org1.getId(),
                loc1Practitioner.getEmail(),
                Set.of("ROLE_PRACTITIONER"),
                Set.of("ROLE_PRACTITIONER", "TDS_VIEW", "CLIENT_VIEW")
        );

        // 3. Client A Staff (restricted to loc1Org1 and assigned only to Client A)
        clientAStaff = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("staff." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Rohan")
                .lastName("Patil")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());
        userLocationRepository.save(UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(clientAStaff.getId())
                        .locationId(loc1Org1.getId())
                        .build())
                .organizationId(org1.getId())
                .build());
        clientAStaffToken = jwtTokenProvider.generateAccessToken(
                clientAStaff.getId(),
                org1.getId(),
                clientAStaff.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "TDS_VIEW", "CLIENT_VIEW")
        );

        // Client A (in loc1Org1)
        ClientEntity ca = ClientEntity.builder()
                .displayName("Alpha Infotech Systems")
                .legalName("Alpha Infotech Systems Pvt Ltd")
                .pan("AABCA1234A")
                .tan("BLRA12345A")
                .clientType(ClientType.COMPANY)
                .locationId(loc1Org1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        ca.setOrganizationId(org1.getId());
        clientAOrg1 = clientRepository.save(ca);

        // Assign clientAStaff to clientAOrg1
        ClientUserAssignmentEntity assignA = ClientUserAssignmentEntity.builder()
                .clientId(clientAOrg1.getId())
                .userId(clientAStaff.getId())
                .active(true)
                .primaryResponsible(true)
                .build();
        assignA.setOrganizationId(org1.getId());
        clientUserAssignmentRepository.save(assignA);

        TdsProfileEntity pa = TdsProfileEntity.builder()
                .clientId(clientAOrg1.getId())
                .locationId(loc1Org1.getId())
                .tan("BLRA12345A")
                .deductorType(DeductorType.COMPANY)
                .branchDivisionName("Main Branch")
                .status(TdsProfileStatus.ACTIVE)
                .active(true)
                .build();
        pa.setOrganizationId(org1.getId());
        profileAOrg1 = tdsProfileRepository.save(pa);

        // Client B (in loc2Org1 - Mysuru)
        ClientEntity cb = ClientEntity.builder()
                .displayName("Beta Logistics")
                .legalName("Beta Logistics LLP")
                .pan("AABCB1234B")
                .tan("MYSB12345B")
                .clientType(ClientType.LLP)
                .locationId(loc2Org1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        cb.setOrganizationId(org1.getId());
        clientBOrg1 = clientRepository.save(cb);

        TdsProfileEntity pb = TdsProfileEntity.builder()
                .clientId(clientBOrg1.getId())
                .locationId(loc2Org1.getId())
                .tan("MYSB12345B")
                .deductorType(DeductorType.LLP)
                .branchDivisionName("Mysuru Hub")
                .status(TdsProfileStatus.ACTIVE)
                .active(true)
                .build();
        pb.setOrganizationId(org1.getId());
        profileBOrg1 = tdsProfileRepository.save(pb);

        // --- Organization 2 ---
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Kothari Associates - " + UUID.randomUUID())
                .legalName("Kothari Associates")
                .email("admin." + UUID.randomUUID() + "@kothari.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserOrg2 = userRepository.save(UserEntity.builder()
                .organizationId(org2.getId())
                .email("admin." + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Vijay")
                .lastName("Kothari")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());
        adminTokenOrg2 = jwtTokenProvider.generateAccessToken(
                adminUserOrg2.getId(),
                org2.getId(),
                adminUserOrg2.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "CLIENT_VIEW")
        );

        ClientEntity cg = ClientEntity.builder()
                .displayName("Gamma Industries")
                .legalName("Gamma Industries Ltd")
                .pan("AABCG1234G")
                .tan("PUNG12345G")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build();
        cg.setOrganizationId(org2.getId());
        clientOrg2 = clientRepository.save(cg);

        TdsProfileEntity pg = TdsProfileEntity.builder()
                .clientId(clientOrg2.getId())
                .tan("PUNG12345G")
                .deductorType(DeductorType.COMPANY)
                .status(TdsProfileStatus.ACTIVE)
                .active(true)
                .build();
        pg.setOrganizationId(org2.getId());
        profileOrg2 = tdsProfileRepository.save(pg);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        TenantContext.clear();
        tdsProfileRepository.deleteAll();
        clientUserAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Firm Admin: Unrestricted access across all locations and clients in tenant")
    void testFirmAdminUnrestrictedAccess() throws Exception {
        // Can access Client A (loc1)
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileAOrg1.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tan").value("BLRA12345A"));

        // Can access Client B (loc2)
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileBOrg1.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tan").value("MYSB12345B"));

        // Can access Workspaces
        mockMvc.perform(get("/api/v1/tds/clients/" + clientAOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tds/clients/" + clientBOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Location Scoping: Practitioner restricted to Location 1 cannot access Location 2 data")
    void testLocationScopingAccess() throws Exception {
        // Location 1 -> 200 OK
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileAOrg1.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isOk());

        // Location 2 -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileBOrg1.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());

        // Workspace Location 2 -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tds/clients/" + clientBOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client Portfolio Scoping: Staff member assigned to Client A cannot access unassigned Client B")
    void testClientPortfolioScopingAccess() throws Exception {
        // Client A -> 200 OK
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileAOrg1.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isOk());

        // Client B -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileBOrg1.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());

        // Client B workspace -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tds/clients/" + clientBOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tenant Isolation: Cross-tenant access is denied (404 Not Found)")
    void testCrossTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/tds/profiles/" + profileOrg2.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tds/clients/" + clientOrg2.getId() + "/workspace")
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isNotFound());
    }
}
