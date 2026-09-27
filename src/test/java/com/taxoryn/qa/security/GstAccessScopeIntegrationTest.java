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
import com.taxoryn.module.gst.entity.GstRegistrationEntity;
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
public class GstAccessScopeIntegrationTest {

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
    private GstRegistrationRepository gstRegistrationRepository;

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

    private ClientEntity clientAOrg1;
    private ClientEntity clientBOrg1;
    private ClientEntity clientOrg2;

    private GstRegistrationEntity gstLoc1ClientA;
    private GstRegistrationEntity gstLoc2ClientB;
    private GstRegistrationEntity gstOrg2;

    @BeforeEach
    void setUp() {
        cleanUp();

        // 1. Setup Tenant 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex GST Advisors - " + UUID.randomUUID())
                .legalName("Apex GST Advisors LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("info-" + UUID.randomUUID() + "@apexgst.in")
                .build());

        // 2. Setup Tenant 2
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Vertex GST - " + UUID.randomUUID())
                .legalName("Vertex GST LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("info-" + UUID.randomUUID() + "@vertexgst.in")
                .build());

        // Locations in Org 1
        LocationEntity loc1 = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc1.setOrganizationId(org1.getId());
        loc1Org1 = locationRepository.save(loc1);

        LocationEntity loc2 = LocationEntity.builder()
                .name("Delhi Branch")
                .code("DEL-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Delhi")
                .state("Delhi")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        loc2.setOrganizationId(org1.getId());
        loc2Org1 = locationRepository.save(loc2);

        // Roles
        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Org Admin")
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

        // Users in Org 1
        adminUserOrg1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("admin-" + UUID.randomUUID() + "@apexgst.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Managing")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminTokenOrg1 = jwtTokenProvider.generateAccessToken(
                adminUserOrg1.getId(),
                org1.getId(),
                adminUserOrg1.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "CLIENT_VIEW")
        );

        // Practitioner restricted to Location 1 only
        loc1Practitioner = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("mumbai-" + UUID.randomUUID() + "@apexgst.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Mumbai")
                .lastName("Lead")
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
                Set.of("ROLE_PRACTITIONER", "GST_VIEW", "CLIENT_VIEW")
        );

        // Staff assigned to Client A only
        clientAStaff = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("staffa-" + UUID.randomUUID() + "@apexgst.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("GST")
                .lastName("Staff")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        clientAStaffToken = jwtTokenProvider.generateAccessToken(
                clientAStaff.getId(),
                org1.getId(),
                clientAStaff.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "GST_VIEW", "CLIENT_VIEW")
        );

        // User in Org 2
        adminUserOrg2 = userRepository.save(UserEntity.builder()
                .organizationId(org2.getId())
                .email("admin-" + UUID.randomUUID() + "@vertexgst.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Vertex")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminTokenOrg2 = jwtTokenProvider.generateAccessToken(
                adminUserOrg2.getId(),
                org2.getId(),
                adminUserOrg2.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "CLIENT_VIEW")
        );

        // Clients
        ClientEntity ca = ClientEntity.builder()
                .displayName("Client Alpha")
                .legalName("Client Alpha Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("ALPHA1234F")
                .locationId(loc1Org1.getId())
                .build();
        ca.setOrganizationId(org1.getId());
        clientAOrg1 = clientRepository.save(ca);

        ClientEntity cb = ClientEntity.builder()
                .displayName("Client Beta")
                .legalName("Client Beta Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("BETAA1234F")
                .locationId(loc2Org1.getId())
                .build();
        cb.setOrganizationId(org1.getId());
        clientBOrg1 = clientRepository.save(cb);

        ClientEntity cg = ClientEntity.builder()
                .displayName("Client Gamma (Tenant 2)")
                .legalName("Client Gamma LLP")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .pan("GAMMA1234F")
                .build();
        cg.setOrganizationId(org2.getId());
        clientOrg2 = clientRepository.save(cg);

        // Assign clientAStaff to clientAOrg1
        ClientUserAssignmentEntity assignment = ClientUserAssignmentEntity.builder()
                .clientId(clientAOrg1.getId())
                .userId(clientAStaff.getId())
                .active(true)
                .primaryResponsible(true)
                .build();
        assignment.setOrganizationId(org1.getId());
        clientUserAssignmentRepository.save(assignment);

        // GST Registrations
        GstRegistrationEntity g1 = GstRegistrationEntity.builder()
                .clientId(clientAOrg1.getId())
                .locationId(loc1Org1.getId())
                .gstin("27ALPHA1234F1Z5")
                .legalName("Client Alpha Pvt Ltd")
                .tradeName("Alpha Retail")
                .registrationType(GstRegistrationType.REGULAR)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();
        g1.setOrganizationId(org1.getId());
        gstLoc1ClientA = gstRegistrationRepository.save(g1);

        GstRegistrationEntity g2 = GstRegistrationEntity.builder()
                .clientId(clientBOrg1.getId())
                .locationId(loc2Org1.getId())
                .gstin("07BETAA1234F1Z3")
                .legalName("Client Beta Pvt Ltd")
                .tradeName("Beta Logistics")
                .registrationType(GstRegistrationType.REGULAR)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();
        g2.setOrganizationId(org1.getId());
        gstLoc2ClientB = gstRegistrationRepository.save(g2);

        GstRegistrationEntity g3 = GstRegistrationEntity.builder()
                .clientId(clientOrg2.getId())
                .gstin("29GAMMA1234F1Z1")
                .legalName("Client Gamma LLP")
                .registrationType(GstRegistrationType.REGULAR)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .build();
        g3.setOrganizationId(org2.getId());
        gstOrg2 = gstRegistrationRepository.save(g3);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        gstRegistrationRepository.deleteAll();
        clientUserAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Multi-Tenant Isolation: Tenant 2 cannot access Tenant 1 GST registration or workspace")
    void testTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg2))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/gst/clients/" + clientAOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + adminTokenOrg2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Location Scope: Practitioner in Loc 1 cannot access Loc 2 GST data")
    void testLocationScope() throws Exception {
        // Practitioner in Loc 1 can access Loc 1 GST Registration
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstin").value("27ALPHA1234F1Z5"));

        // Practitioner in Loc 1 CANNOT access Loc 2 GST Registration -> 403
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());

        // Practitioner in Loc 1 CANNOT access Client B (Loc 2) GST Workspace -> 403
        mockMvc.perform(get("/api/v1/gst/clients/" + clientBOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client Portfolio Scope: Staff assigned to Client A cannot access Client B GST data")
    void testClientPortfolioScope() throws Exception {
        // Staff assigned to Client A can access Client A GST Registration
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstin").value("27ALPHA1234F1Z5"));

        // Staff assigned to Client A CANNOT access Client B GST Registration -> 403
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());

        // Staff assigned to Client A CANNOT access Client B GST Workspace -> 403
        mockMvc.perform(get("/api/v1/gst/clients/" + clientBOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Firm Admin Scope: Firm Admin has unrestricted access across all locations and clients")
    void testFirmAdminUnrestrictedScope() throws Exception {
        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/gst/registrations/" + gstLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/gst/clients/" + clientAOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/gst/clients/" + clientBOrg1.getId() + "/workspace")
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());
    }
}
