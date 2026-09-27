package com.taxoryn.module.task;

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
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
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
public class WorkItemAccessScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

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

    private OrganizationEntity org1;
    private OrganizationEntity org2;

    private LocationEntity loc1Org1;
    private LocationEntity loc2Org1;

    private UserEntity adminUserOrg1;
    private String adminTokenOrg1;

    private UserEntity loc1Practitioner;
    private String loc1PractitionerToken;

    private UserEntity clientAStaff;
    private String clientAStaffToken;

    private UserEntity adminUserOrg2;
    private String adminTokenOrg2;

    private ClientEntity clientAOrg1;
    private ClientEntity clientBOrg1;
    private ClientEntity clientOrg2;

    private WorkItemEntity workItemLoc1ClientA;
    private WorkItemEntity workItemLoc2ClientB;
    private WorkItemEntity workItemOrg2;

    @BeforeEach
    void setUp() {
        cleanUp();

        // 1. Setup Tenant 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisors - " + UUID.randomUUID())
                .legalName("Apex Advisors LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("info-" + UUID.randomUUID() + "@apexadvisors.in")
                .build());

        // 2. Setup Tenant 2
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Vertex Consulting - " + UUID.randomUUID())
                .legalName("Vertex Consulting LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("info-" + UUID.randomUUID() + "@vertex.in")
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
                .email("admin-" + UUID.randomUUID() + "@apex.com")
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
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE")
        );

        // Practitioner restricted to Location 1 only
        loc1Practitioner = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("mumbai-" + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Mumbai")
                .lastName("Tax Lead")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(practitionerRole))
                .build());

        UserLocationEntity userLoc = UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(loc1Practitioner.getId())
                        .locationId(loc1Org1.getId())
                        .build())
                .organizationId(org1.getId())
                .build();
        userLocationRepository.save(userLoc);

        loc1PractitionerToken = jwtTokenProvider.generateAccessToken(
                loc1Practitioner.getId(),
                org1.getId(),
                loc1Practitioner.getEmail(),
                Set.of("ROLE_PRACTITIONER"),
                Set.of("ROLE_PRACTITIONER", "CLIENT_VIEW", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE")
        );

        // Staff assigned to Client A only
        clientAStaff = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("staffa-" + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Article")
                .lastName("Assistant A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        clientAStaffToken = jwtTokenProvider.generateAccessToken(
                clientAStaff.getId(),
                org1.getId(),
                clientAStaff.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "CLIENT_VIEW", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE")
        );

        // User in Org 2
        adminUserOrg2 = userRepository.save(UserEntity.builder()
                .organizationId(org2.getId())
                .email("admin-" + UUID.randomUUID() + "@vertex.com")
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
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE")
        );

        // Clients
        ClientEntity cA = ClientEntity.builder()
                .displayName("Client Alpha")
                .legalName("Client Alpha Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("ALPHA1234F")
                .locationId(loc1Org1.getId())
                .build();
        cA.setOrganizationId(org1.getId());
        clientAOrg1 = clientRepository.save(cA);

        ClientEntity cB = ClientEntity.builder()
                .displayName("Client Beta")
                .legalName("Client Beta Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("BETAA1234F")
                .locationId(loc2Org1.getId())
                .build();
        cB.setOrganizationId(org1.getId());
        clientBOrg1 = clientRepository.save(cB);

        ClientEntity cG = ClientEntity.builder()
                .displayName("Client Gamma (Tenant 2)")
                .legalName("Client Gamma LLP")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .pan("GAMMA1234F")
                .build();
        cG.setOrganizationId(org2.getId());
        clientOrg2 = clientRepository.save(cG);

        // Assign clientAStaff to clientAOrg1
        ClientUserAssignmentEntity assignment = ClientUserAssignmentEntity.builder()
                .clientId(clientAOrg1.getId())
                .userId(clientAStaff.getId())
                .active(true)
                .primaryResponsible(true)
                .build();
        assignment.setOrganizationId(org1.getId());
        clientUserAssignmentRepository.save(assignment);

        // Work Items
        WorkItemEntity wi1 = WorkItemEntity.builder()
                .title("Audit Report Client A")
                .clientId(clientAOrg1.getId())
                .locationId(loc1Org1.getId())
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.HIGH)
                .build();
        wi1.setOrganizationId(org1.getId());
        workItemLoc1ClientA = workItemRepository.save(wi1);

        WorkItemEntity wi2 = WorkItemEntity.builder()
                .title("GST Compliance Client B")
                .clientId(clientBOrg1.getId())
                .locationId(loc2Org1.getId())
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.MEDIUM)
                .build();
        wi2.setOrganizationId(org1.getId());
        workItemLoc2ClientB = workItemRepository.save(wi2);

        WorkItemEntity wi3 = WorkItemEntity.builder()
                .title("Tenant 2 Work Item")
                .clientId(clientOrg2.getId())
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.LOW)
                .build();
        wi3.setOrganizationId(org2.getId());
        workItemOrg2 = workItemRepository.save(wi3);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        taskRepository.deleteAll();
        workItemRepository.deleteAll();
        clientUserAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Tenant Isolation: User from Tenant 2 cannot view Tenant 1 Work Item")
    void testTenantIsolation() throws Exception {
        // Tenant 2 user tries to access Tenant 1 work item -> 404
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg2))
                .andExpect(status().isNotFound());

        // Tenant 1 user tries to access Tenant 2 work item -> 404
        mockMvc.perform(get("/api/v1/work-items/" + workItemOrg2.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Location Scope: Practitioner in Location 1 cannot access Work Item in Location 2")
    void testLocationAccessScope() throws Exception {
        // Location 1 practitioner can access Location 1 work item
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Audit Report Client A"));

        // Location 1 practitioner CANNOT access Location 2 work item -> 403 Forbidden
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client Portfolio Scope: Staff assigned to Client A cannot access Work Item of Client B")
    void testClientPortfolioScope() throws Exception {
        // Staff assigned to Client A can access Client A work item
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Audit Report Client A"));

        // Staff NOT assigned to Client B CANNOT access Client B work item -> 403 Forbidden
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Firm Admin Scope: Firm Admin has unrestricted access across all locations and clients")
    void testFirmAdminUnrestrictedScope() throws Exception {
        // Firm Admin can access Location 1 work item
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        // Firm Admin can access Location 2 work item
        mockMvc.perform(get("/api/v1/work-items/" + workItemLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());
    }
}
