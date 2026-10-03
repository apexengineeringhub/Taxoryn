package com.taxoryn.module.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.dsc.entity.DscEntity;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import com.taxoryn.module.dsc.repository.DscRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.udin.entity.UdinEntity;
import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import com.taxoryn.module.udin.repository.UdinRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PracticeDashboardOperationalMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DscRepository dscRepository;

    @Autowired
    private UdinRepository udinRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locA1;
    private UserEntity adminUserA;
    private UserEntity staffUserA;
    private String adminTokenA;
    private String staffTokenA;

    @BeforeEach
    void setUp() {
        tearDown();

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build())
        );

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff Member").isSystemRole(true).build())
        );

        // 1. Organization A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Advisory LLP")
                .legalName("Alpha Tax Advisory Limited Liability Partnership")
                .email("contact." + UUID.randomUUID() + "@alphatax.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Organization B (for tenant isolation check)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta & Associates")
                .legalName("Beta & Associates Chartered Accountants")
                .email("contact." + UUID.randomUUID() + "@betatax.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        LocationEntity l1 = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("MUM")
                .isHeadOffice(true)
                .city("Mumbai")
                .state("Maharashtra")
                .isActive(true)
                .build();
        l1.setOrganizationId(orgA.getId());
        locA1 = locationRepository.save(l1);

        // Admin User Org A
        UserEntity uAdminA = UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@alphatax.test")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        uAdminA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(uAdminA);

        // Staff User Org A
        UserEntity uStaffA = UserEntity.builder()
                .email("staff." + UUID.randomUUID() + "@alphatax.test")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Sunil")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(staffRole)))
                .build();
        uStaffA.setOrganizationId(orgA.getId());
        staffUserA = userRepository.save(uStaffA);

        EmployeeEntity empStaff = EmployeeEntity.builder()
                .userId(staffUserA.getId())
                .employeeCode("EMP-001")
                .firstName("Sunil")
                .lastName("Mehta")
                .email(staffUserA.getEmail())
                .designation("Senior Tax Associate")
                .department("Direct Tax")
                .status(EmployeeStatus.ACTIVE)
                .build();
        empStaff.setOrganizationId(orgA.getId());
        employeeRepository.save(empStaff);

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(), orgA.getId(), locA1.getId(), adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW", "COMPLIANCE_VIEW", "TASK_VIEW", "DOCUMENT_VIEW", "NOTICE_VIEW", "BILLING_VIEW", "REPORT_VIEW")
        );

        staffTokenA = jwtTokenProvider.generateAccessToken(
                staffUserA.getId(), orgA.getId(), locA1.getId(), staffUserA.getEmail(),
                Set.of("STAFF"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW", "COMPLIANCE_VIEW", "TASK_VIEW", "DOCUMENT_VIEW")
        );

        // 3. Seed Clients for Org A
        ClientEntity c1 = ClientEntity.builder()
                .clientType(ClientType.COMPANY)
                .displayName("Acme Global Pvt Ltd")
                .pan("AAACA1234A")
                .status(ClientStatus.ACTIVE)
                .build();
        c1.setOrganizationId(orgA.getId());
        ClientEntity clientA1 = clientRepository.save(c1);

        // 4. Seed Tasks for Org A
        TaskEntity t1 = TaskEntity.builder()
                .clientId(clientA1.getId())
                .assignedTo(empStaff.getUserId())
                .title("Prepare Q3 GST Audit")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        t1.setOrganizationId(orgA.getId());
        taskRepository.save(t1);

        TaskEntity t2 = TaskEntity.builder()
                .clientId(clientA1.getId())
                .assignedTo(empStaff.getUserId())
                .title("ITR-6 Verification")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().minusDays(2))
                .completedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
        t2.setOrganizationId(orgA.getId());
        taskRepository.save(t2);

        TaskEntity t3 = TaskEntity.builder()
                .clientId(clientA1.getId())
                .assignedTo(empStaff.getUserId())
                .title("TDS Correction Statement")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.URGENT)
                .dueDate(LocalDate.now().minusDays(1)) // Overdue!
                .build();
        t3.setOrganizationId(orgA.getId());
        taskRepository.save(t3);

        // 5. Seed DSC for Org A
        DscEntity dsc1 = DscEntity.builder()
                .clientId(clientA1.getId())
                .holderName("Rajesh Sharma")
                .certificateIdentifier("AAAPS1234A")
                .certificateType(DscCertificateType.CLASS_3)
                .issuedDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(18))
                .status(DscStatus.ACTIVE)
                .build();
        dsc1.setOrganizationId(orgA.getId());
        dscRepository.save(dsc1);

        DscEntity dsc2 = DscEntity.builder()
                .clientId(clientA1.getId())
                .holderName("Sunil Mehta")
                .certificateIdentifier("AAAPM5678B")
                .certificateType(DscCertificateType.CLASS_3)
                .issuedDate(LocalDate.now().minusYears(2))
                .expiryDate(LocalDate.now().plusDays(15)) // Expiring soon (<30d)
                .status(DscStatus.ACTIVE)
                .build();
        dsc2.setOrganizationId(orgA.getId());
        dscRepository.save(dsc2);

        // 6. Seed UDIN for Org A
        UdinEntity udin1 = UdinEntity.builder()
                .clientId(clientA1.getId())
                .udin("24012345ABCDEF1234")
                .signatoryMembershipNo("012345")
                .signatoryName("CA Rajesh Sharma")
                .documentType(UdinDocumentType.GST_AUDIT_CERTIFICATE)
                .documentTitle("GST Audit Report Form 9C")
                .generationDate(LocalDate.now().minusDays(5))
                .status(UdinStatus.ACTIVE)
                .verificationStatus(UdinVerificationStatus.VERIFIED)
                .build();
        udin1.setOrganizationId(orgA.getId());
        udinRepository.save(udin1);

        UdinEntity udin2 = UdinEntity.builder()
                .clientId(clientA1.getId())
                .udin("24012345XYZ9876543")
                .signatoryMembershipNo("012345")
                .signatoryName("CA Rajesh Sharma")
                .documentType(UdinDocumentType.TAX_AUDIT_REPORT_3CB_3CD)
                .documentTitle("Tax Audit Form 3CD")
                .generationDate(LocalDate.now().minusDays(1))
                .status(UdinStatus.ACTIVE)
                .verificationStatus(UdinVerificationStatus.NOT_VERIFIED)
                .build();
        udin2.setOrganizationId(orgA.getId());
        udinRepository.save(udin2);

        // 7. Seed Reminders for Org A
        ReminderEntity rem1 = ReminderEntity.builder()
                .title("Follow up for Form 3CA with Acme")
                .reminderType(ReminderType.FOLLOW_UP)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.HIGH)
                .recurrenceType(ReminderRecurrenceType.NONE)
                .scheduledAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .targetUserId(adminUserA.getId())
                .build();
        rem1.setOrganizationId(orgA.getId());
        reminderRepository.save(rem1);

        // 8. Seed Audit Log for Org A
        AuditLogEntity audit1 = AuditLogEntity.builder()
                .organizationId(orgA.getId())
                .userId(adminUserA.getId())
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityName("Acme Global Pvt Ltd")
                .entityId(clientA1.getId().toString())
                .build();
        auditLogRepository.save(audit1);

        // 9. Seed Org B (Tenant Isolation)
        TenantContext.setTenantId(orgB.getId());
        ClientEntity cB1 = ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Beta Client 1")
                .pan("BBBCB1234B")
                .status(ClientStatus.ACTIVE)
                .build();
        cB1.setOrganizationId(orgB.getId());
        ClientEntity clientB1 = clientRepository.save(cB1);

        DscEntity dscB = DscEntity.builder()
                .clientId(clientB1.getId())
                .holderName("Beta Holder")
                .certificateIdentifier("BBBPB1234B")
                .certificateType(DscCertificateType.CLASS_3)
                .issuedDate(LocalDate.now().minusMonths(1))
                .expiryDate(LocalDate.now().plusMonths(23))
                .status(DscStatus.ACTIVE)
                .build();
        dscB.setOrganizationId(orgB.getId());
        dscRepository.save(dscB);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        reminderRepository.deleteAll();
        udinRepository.deleteAll();
        dscRepository.deleteAll();
        taskRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("P0.10: Practice Dashboard Overview Aggregates DSC, UDIN, Reminders and Workload for Admin")
    void testPracticeDashboardOverviewAggregation() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .param("period", "ALL_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                // Clients & Tasks
                .andExpect(jsonPath("$.data.activeClients", is(1)))
                .andExpect(jsonPath("$.data.pendingTasks", is(2))) // TODO + IN_PROGRESS
                .andExpect(jsonPath("$.data.overdueTasks", is(1))) // 1 overdue
                .andExpect(jsonPath("$.data.completedTasks", is(1))) // 1 completed
                // DSC Metrics
                .andExpect(jsonPath("$.data.dsc.total", is(2)))
                .andExpect(jsonPath("$.data.dsc.active", is(1)))
                .andExpect(jsonPath("$.data.dsc.expiringSoon", is(1)))
                // UDIN Metrics
                .andExpect(jsonPath("$.data.udin.totalCount", is(2)))
                .andExpect(jsonPath("$.data.udin.verifiedCount", is(1)))
                .andExpect(jsonPath("$.data.udin.unverifiedCount", is(1)))
                // Reminders
                .andExpect(jsonPath("$.data.reminders.pending", is(1)))
                .andExpect(jsonPath("$.data.reminders.upcoming", is(1)))
                // Workload
                .andExpect(jsonPath("$.data.employeeWorkload", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.employeeWorkload[0].employeeCode", is("EMP-001")))
                // Recent Activity
                .andExpect(jsonPath("$.data.recentActivity", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.recentActivity[0].action", is("CLIENT_CREATED")));
    }

    @Test
    @DisplayName("P0.10: Practice Dashboard Strict Tenant Isolation — Org A Cannot See Org B DSC or UDIN")
    void testPracticeDashboardTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dsc.total", is(2))) // Org A has 2, not 3 (Org B's 1 is excluded)
                .andExpect(jsonPath("$.data.activeClients", is(1))); // Org A has 1 client, not 2
    }

    @Test
    @DisplayName("P0.10: Practice Dashboard Date Filtering Works Correctly")
    void testPracticeDashboardDateFiltering() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeClients", is(1)));
    }
}
