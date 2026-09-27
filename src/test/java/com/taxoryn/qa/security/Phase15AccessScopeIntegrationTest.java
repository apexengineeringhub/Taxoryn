package com.taxoryn.qa.security;

import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.entity.BillingProfileEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.repository.BillingProfileRepository;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientUserAssignmentEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.timetracking.entity.TimeEntryEntity;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import com.taxoryn.module.timetracking.repository.TimeEntryRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Phase15AccessScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private BillingProfileRepository billingProfileRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

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

    private EngagementEntity engagementLoc1ClientA;
    private EngagementEntity engagementLoc2ClientB;
    private EngagementEntity engagementOrg2;

    private TimeEntryEntity timeEntryLoc1ClientA;
    private TimeEntryEntity timeEntryLoc2ClientB;
    private TimeEntryEntity timeEntryOrg2;

    private InvoiceEntity invoiceLoc1ClientA;
    private InvoiceEntity invoiceLoc2ClientB;
    private InvoiceEntity invoiceOrg2;

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
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE")
        );

        // Practitioner restricted to Location 1 only
        loc1Practitioner = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("mumbai-" + UUID.randomUUID() + "@apex.com")
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
                Set.of("ROLE_PRACTITIONER", "CLIENT_VIEW", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "BILLING_VIEW")
        );

        // Staff assigned to Client A only
        clientAStaff = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("staffa-" + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Article")
                .lastName("Staff")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        clientAStaffToken = jwtTokenProvider.generateAccessToken(
                clientAStaff.getId(),
                org1.getId(),
                clientAStaff.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "CLIENT_VIEW", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "BILLING_VIEW")
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
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "TASK_VIEW", "BILLING_VIEW")
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

        // Engagements
        EngagementEntity e1 = EngagementEntity.builder()
                .clientId(clientAOrg1.getId())
                .locationId(loc1Org1.getId())
                .name("Engagement Loc1 Client A")
                .status(EngagementStatus.ACTIVE)
                .build();
        e1.setOrganizationId(org1.getId());
        engagementLoc1ClientA = engagementRepository.save(e1);

        EngagementEntity e2 = EngagementEntity.builder()
                .clientId(clientBOrg1.getId())
                .locationId(loc2Org1.getId())
                .name("Engagement Loc2 Client B")
                .status(EngagementStatus.ACTIVE)
                .build();
        e2.setOrganizationId(org1.getId());
        engagementLoc2ClientB = engagementRepository.save(e2);

        EngagementEntity eOrg2 = EngagementEntity.builder()
                .clientId(clientOrg2.getId())
                .name("Tenant 2 Engagement")
                .status(EngagementStatus.ACTIVE)
                .build();
        eOrg2.setOrganizationId(org2.getId());
        engagementOrg2 = engagementRepository.save(eOrg2);

        // Time Entries
        TimeEntryEntity te1 = TimeEntryEntity.builder()
                .clientId(clientAOrg1.getId())
                .locationId(loc1Org1.getId())
                .userId(adminUserOrg1.getId())
                .entryDate(LocalDate.of(2026, 8, 1))
                .durationMinutes(60)
                .status(TimeEntryStatus.SUBMITTED)
                .build();
        te1.setOrganizationId(org1.getId());
        timeEntryLoc1ClientA = timeEntryRepository.save(te1);

        TimeEntryEntity te2 = TimeEntryEntity.builder()
                .clientId(clientBOrg1.getId())
                .locationId(loc2Org1.getId())
                .userId(adminUserOrg1.getId())
                .entryDate(LocalDate.of(2026, 8, 2))
                .durationMinutes(90)
                .status(TimeEntryStatus.SUBMITTED)
                .build();
        te2.setOrganizationId(org1.getId());
        timeEntryLoc2ClientB = timeEntryRepository.save(te2);

        TimeEntryEntity teOrg2 = TimeEntryEntity.builder()
                .clientId(clientOrg2.getId())
                .userId(adminUserOrg2.getId())
                .entryDate(LocalDate.of(2026, 8, 3))
                .durationMinutes(45)
                .status(TimeEntryStatus.SUBMITTED)
                .build();
        teOrg2.setOrganizationId(org2.getId());
        timeEntryOrg2 = timeEntryRepository.save(teOrg2);

        // Invoices
        InvoiceEntity inv1 = InvoiceEntity.builder()
                .clientId(clientAOrg1.getId())
                .locationId(loc1Org1.getId())
                .invoiceNumber("INV-2026-LOC1")
                .invoiceDate(LocalDate.of(2026, 8, 1))
                .dueDate(LocalDate.of(2026, 8, 15))
                .subtotal(new BigDecimal("1000.00"))
                .tax(new BigDecimal("180.00"))
                .total(new BigDecimal("1180.00"))
                .balanceDue(new BigDecimal("1180.00"))
                .status(InvoiceStatus.DRAFT)
                .build();
        inv1.setOrganizationId(org1.getId());
        invoiceLoc1ClientA = invoiceRepository.save(inv1);

        InvoiceEntity inv2 = InvoiceEntity.builder()
                .clientId(clientBOrg1.getId())
                .locationId(loc2Org1.getId())
                .invoiceNumber("INV-2026-LOC2")
                .invoiceDate(LocalDate.of(2026, 8, 1))
                .dueDate(LocalDate.of(2026, 8, 15))
                .subtotal(new BigDecimal("2000.00"))
                .tax(new BigDecimal("360.00"))
                .total(new BigDecimal("2360.00"))
                .balanceDue(new BigDecimal("2360.00"))
                .status(InvoiceStatus.DRAFT)
                .build();
        inv2.setOrganizationId(org1.getId());
        invoiceLoc2ClientB = invoiceRepository.save(inv2);

        InvoiceEntity invOrg2 = InvoiceEntity.builder()
                .clientId(clientOrg2.getId())
                .invoiceNumber("INV-ORG2-001")
                .invoiceDate(LocalDate.of(2026, 8, 1))
                .dueDate(LocalDate.of(2026, 8, 15))
                .subtotal(new BigDecimal("5000.00"))
                .tax(new BigDecimal("900.00"))
                .total(new BigDecimal("5900.00"))
                .balanceDue(new BigDecimal("5900.00"))
                .status(InvoiceStatus.DRAFT)
                .build();
        invOrg2.setOrganizationId(org2.getId());
        invoiceOrg2 = invoiceRepository.save(invOrg2);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        invoicePaymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        billingProfileRepository.deleteAll();
        timeEntryRepository.deleteAll();
        engagementRepository.deleteAll();
        clientUserAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Tenant Isolation: Tenant 2 cannot access Tenant 1 Engagement, Time Entry, or Invoice")
    void testTenantIsolation() throws Exception {
        // Engagement
        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg2))
                .andExpect(status().isNotFound());

        // Time Entry
        mockMvc.perform(get("/api/v1/time-entries/" + timeEntryLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg2))
                .andExpect(status().isNotFound());

        // Invoice
        mockMvc.perform(get("/api/v1/invoices/" + invoiceLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Location Scope: Practitioner in Location 1 cannot access Location 2 Engagement or Time Entry")
    void testLocationAccessScope() throws Exception {
        // Practitioner in Loc 1 can access Loc 1 Engagement
        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Engagement Loc1 Client A"));

        // Practitioner in Loc 1 CANNOT access Loc 2 Engagement -> 403
        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());

        // Practitioner in Loc 1 can access Loc 1 Time Entry
        mockMvc.perform(get("/api/v1/time-entries/" + timeEntryLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isOk());

        // Practitioner in Loc 1 CANNOT access Loc 2 Time Entry -> 403
        mockMvc.perform(get("/api/v1/time-entries/" + timeEntryLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + loc1PractitionerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client Portfolio Scope: Staff assigned to Client A cannot access Client B data")
    void testClientPortfolioScope() throws Exception {
        // Staff assigned to Client A can access Client A engagement
        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Engagement Loc1 Client A"));

        // Staff assigned to Client A CANNOT access Client B engagement -> 403
        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());

        // Staff assigned to Client A CANNOT access Client B time entry -> 403
        mockMvc.perform(get("/api/v1/time-entries/" + timeEntryLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());

        // Staff assigned to Client A CANNOT access Client B invoice -> 403
        mockMvc.perform(get("/api/v1/invoices/" + invoiceLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + clientAStaffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Firm Admin Scope: Firm Admin has unrestricted access across all locations and clients")
    void testFirmAdminUnrestrictedScope() throws Exception {
        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/engagements/" + engagementLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/invoices/" + invoiceLoc1ClientA.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/invoices/" + invoiceLoc2ClientB.getId())
                        .header("Authorization", "Bearer " + adminTokenOrg1))
                .andExpect(status().isOk());
    }
}
