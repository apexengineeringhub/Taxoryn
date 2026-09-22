package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.notice.dto.CreateTaxNoticeRequest;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxNoticeVisibilityAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaxNoticeRepository noticeRepository;

    @Autowired
    private NoticeResponseRepository responseRepository;

    @Autowired
    private NoticeHearingRepository hearingRepository;

    @Autowired
    private NoticeActivityRepository activityRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ModuleConfigurationService moduleConfigurationService;

    // Org A
    private OrganizationEntity orgA;
    private UserEntity adminUserA;
    private UserEntity staffUserA;
    private EmployeeEntity staffEmployeeA;
    private UserEntity otherStaffUserA;
    private EmployeeEntity otherStaffEmployeeA;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private String adminTokenA;
    private String staffTokenA;
    private String otherStaffTokenA;

    // Org B
    private OrganizationEntity orgB;
    private UserEntity adminUserB;
    private ClientEntity clientB;
    private String adminTokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        // 1. Setup Org A (Growing Practice)
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Associates")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.GROWING_PRACTICE)
                .email("contact@alphatax.com")
                .phone("+919876543210")
                .build());

        // 2. Setup Org B (Small Firm)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Consultants")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.SMALL_TAX_FIRM)
                .email("contact@betatax.com")
                .phone("+919876543211")
                .build());

        // Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Practice Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        Set<String> noticeViewPermissions = Set.of("NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE");

        // Org A Users & Employees
        TenantContext.setTenantId(orgA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .email("admin@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserA.getId(), orgA.getId(), adminUserA.getEmail(), Set.of("ORG_ADMIN"), noticeViewPermissions);

        staffUserA = userRepository.save(UserEntity.builder()
                .email("staff1@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Staff")
                .lastName("One")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());
        staffEmployeeA = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffUserA.getId())
                .employeeCode("EMP-A1")
                .firstName("Staff")
                .lastName("One")
                .email("staff1@alphatax.com")
                .designation("Article Assistant")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now().minusMonths(6))
                .build());
        staffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(staffUserA.getId(), orgA.getId(), staffUserA.getEmail(), Set.of("STAFF"), noticeViewPermissions);

        otherStaffUserA = userRepository.save(UserEntity.builder()
                .email("staff2@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Staff")
                .lastName("Two")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());
        otherStaffEmployeeA = employeeRepository.save(EmployeeEntity.builder()
                .userId(otherStaffUserA.getId())
                .employeeCode("EMP-A2")
                .firstName("Staff")
                .lastName("Two")
                .email("staff2@alphatax.com")
                .designation("Tax Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now().minusMonths(4))
                .build());
        otherStaffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(otherStaffUserA.getId(), orgA.getId(), otherStaffUserA.getEmail(), Set.of("STAFF"), noticeViewPermissions);

        // Client A1 (Assigned to Staff 1)
        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Client Alpha One")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .pan("ABCDE1234F")
                .email("client1@alphatax.com")
                .assignedEmployeeId(staffEmployeeA.getId())
                .build());

        // Client A2 (Unassigned)
        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Client Alpha Two")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("BCDEF2345G")
                .email("client2@alphatax.com")
                .build());

        // Org B User & Client
        TenantContext.setTenantId(orgB.getId());

        adminUserB = userRepository.save(UserEntity.builder()
                .email("admin@betatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserB.getId(), orgB.getId(), adminUserB.getEmail(), Set.of("ORG_ADMIN"), noticeViewPermissions);

        clientB = clientRepository.save(ClientEntity.builder()
                .displayName("Client Beta Corp")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("CDEFG3456H")
                .email("client@betatax.com")
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        TenantContext.clear();
    }

    private void cleanDatabase() {
        TenantContext.clear();
        taskRepository.deleteAll();
        activityRepository.deleteAll();
        hearingRepository.deleteAll();
        responseRepository.deleteAll();
        noticeRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("1. TAX_NOTICES is present in Catalog and enabled by default for organizations")
    void testTaxNoticesCatalogAndDefaultConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        boolean isEnabled = moduleConfigurationService.isModuleEnabled(ProductModuleCode.TAX_NOTICES);
        assertThat(isEnabled).isTrue();
    }

    @Test
    @DisplayName("2. Dual-route API: Both /api/v1/tax-notices and /api/v1/notices successfully list notices")
    void testDualRouteNoticeListing() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        TaxNoticeEntity notice = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2024/001")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1) Intimation")
                .section("143(1)")
                .subject("Discrepancy in AIS vs ITR")
                .assessmentYear("2024-25")
                .receivedDate(LocalDate.now().minusDays(5))
                .responseDueDate(LocalDate.now().plusDays(25))
                .status(NoticeStatus.NOTICE_RECEIVED)
                .priority(NoticePriority.HIGH)
                .demandAmount(new BigDecimal("150000.00"))
                .build());
        TenantContext.clear();

        // 1. Query /api/v1/tax-notices
        mockMvc.perform(get("/api/v1/tax-notices")
                        .header("Authorization", adminTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(notice.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].noticeNumber").value("ITBA/AST/2024/001"));

        // 2. Query legacy alias /api/v1/notices
        mockMvc.perform(get("/api/v1/notices")
                        .header("Authorization", adminTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(notice.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].noticeNumber").value("ITBA/AST/2024/001"));
    }

    @Test
    @DisplayName("3. Cross-Tenant Security: Tenant B cannot access Tenant A's notices by direct ID or list")
    void testCrossTenantIsolation() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        TaxNoticeEntity noticeA = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2024/SEC-01")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("148 Reassessment")
                .section("148")
                .subject("Escaped Assessment Scrutiny")
                .assessmentYear("2021-22")
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(28))
                .status(NoticeStatus.NOTICE_RECEIVED)
                .priority(NoticePriority.CRITICAL)
                .build());
        TenantContext.clear();

        // Tenant B direct ID lookup on Tenant A's notice returns 404
        mockMvc.perform(get("/api/v1/tax-notices/" + noticeA.getId())
                        .header("Authorization", adminTokenB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Tenant B list query returns 0 results
        mockMvc.perform(get("/api/v1/tax-notices")
                        .header("Authorization", adminTokenB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("4. Portfolio Scope: Staff user can only see and access notices for assigned clients")
    void testClientPortfolioScopeEnforcement() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Notice on Client A1 (Staff 1 is assigned)
        TaxNoticeEntity noticeA1 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2024/A1")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(2) Scrutiny")
                .section("143(2)")
                .subject("Detailed Scrutiny Notice")
                .assessmentYear("2023-24")
                .receivedDate(LocalDate.now().minusDays(3))
                .responseDueDate(LocalDate.now().plusDays(27))
                .status(NoticeStatus.UNDER_REVIEW)
                .priority(NoticePriority.HIGH)
                .build());

        // Notice on Client A2 (Staff 1 is NOT assigned)
        TaxNoticeEntity noticeA2 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA2.getId())
                .noticeNumber("GST/DRC-01/2024/A2")
                .department(NoticeDepartment.GST)
                .noticeType("DRC-01 SCN")
                .section("73")
                .subject("ITC Mismatch Notice")
                .assessmentYear("2023-24")
                .receivedDate(LocalDate.now().minusDays(1))
                .responseDueDate(LocalDate.now().plusDays(29))
                .status(NoticeStatus.NOTICE_RECEIVED)
                .priority(NoticePriority.MEDIUM)
                .build());

        TenantContext.clear();

        // Staff 1 can access notice A1 (Client A1 in portfolio)
        mockMvc.perform(get("/api/v1/tax-notices/" + noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noticeA1.getId().toString()));

        // Staff 1 cannot access notice A2 (Client A2 not in portfolio) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/tax-notices/" + noticeA2.getId())
                        .header("Authorization", staffTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Staff 1 list only returns notice A1
        mockMvc.perform(get("/api/v1/tax-notices")
                        .header("Authorization", staffTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(noticeA1.getId().toString()));

        // Staff 2 has no assigned clients, list returns 0
        mockMvc.perform(get("/api/v1/tax-notices")
                        .header("Authorization", otherStaffTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("5. Task Assignment Independence: Task assignment on notice does NOT grant notice access without client portfolio")
    void testTaskAssignmentDoesNotGrantNoticeAccess() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Notice on Client A2 (Staff 1 does NOT have Client A2 in portfolio)
        TaxNoticeEntity noticeA2 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA2.getId())
                .noticeNumber("GST/DRC-01/TASK-ISOLATION")
                .department(NoticeDepartment.GST)
                .noticeType("DRC-01 SCN")
                .section("73")
                .subject("GST Mismatch Task Test")
                .assessmentYear("2023-24")
                .receivedDate(LocalDate.now().minusDays(1))
                .responseDueDate(LocalDate.now().plusDays(29))
                .status(NoticeStatus.NOTICE_RECEIVED)
                .priority(NoticePriority.MEDIUM)
                .build());

        // Assign a task for this notice to Staff 1
        taskRepository.save(TaskEntity.builder()
                .clientId(clientA2.getId())
                .noticeId(noticeA2.getId())
                .assignedTo(staffEmployeeA.getId())
                .title("Draft reply for DRC-01")
                .taskCategory(TaskEntity.TaskCategory.NOTICE)
                .priority(TaskEntity.TaskPriority.HIGH)
                .status(TaskEntity.TaskStatus.TODO)
                .dueDate(LocalDate.now().plusDays(7))
                .build());

        TenantContext.clear();

        // Direct notice lookup MUST still fail (403 Forbidden) because Client A2 is not in Staff 1's portfolio scope
        mockMvc.perform(get("/api/v1/tax-notices/" + noticeA2.getId())
                        .header("Authorization", staffTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Client / Organization Invariant: Cannot create notice for client of another organization")
    void testClientOrgInvariantOnCreate() throws Exception {
        CreateTaxNoticeRequest request = CreateTaxNoticeRequest.builder()
                .clientId(clientB.getId()) // Client belongs to Org B
                .noticeNumber("ITBA/MALICIOUS/001")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1) Intimation")
                .section("143(1)")
                .subject("Cross-tenant injection attempt")
                .receivedDate(LocalDate.now())
                .responseDueDate(LocalDate.now().plusDays(30))
                .priority(NoticePriority.MEDIUM)
                .build();

        // Attempt from Org A admin should fail (404 Resource Not Found)
        mockMvc.perform(post("/api/v1/tax-notices")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7. Assigned Employee / Organization Invariant: Cannot assign employee of another organization")
    void testAssignedEmployeeOrgInvariant() throws Exception {
        CreateTaxNoticeRequest request = CreateTaxNoticeRequest.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/VALID/001")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1) Intimation")
                .section("143(1)")
                .subject("Valid Notice with invalid employee")
                .receivedDate(LocalDate.now())
                .responseDueDate(LocalDate.now().plusDays(30))
                .assignedEmployeeId(UUID.randomUUID()) // Non-existent or foreign employee
                .priority(NoticePriority.MEDIUM)
                .build();

        // Should reject with 404
        mockMvc.perform(post("/api/v1/tax-notices")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8. OrganizationType Independence: Changing Org Type does not alter authorization checks")
    void testOrganizationTypeSecurityIndependence() throws Exception {
        // Change Org A type to SOLO_PRACTITIONER
        orgA.setOrganizationType(OrganizationType.SOLO_PRACTITIONER);
        organizationRepository.save(orgA);

        TenantContext.setTenantId(orgA.getId());
        TaxNoticeEntity noticeA1 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/SOLO/001")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1)")
                .section("143(1)")
                .subject("Notice under solo type")
                .assessmentYear("2024-25")
                .receivedDate(LocalDate.now().minusDays(1))
                .responseDueDate(LocalDate.now().plusDays(29))
                .status(NoticeStatus.NOTICE_RECEIVED)
                .priority(NoticePriority.MEDIUM)
                .build());
        TenantContext.clear();

        // Admin still has access
        mockMvc.perform(get("/api/v1/tax-notices/" + noticeA1.getId())
                        .header("Authorization", adminTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Foreign Org B still gets 404
        mockMvc.perform(get("/api/v1/tax-notices/" + noticeA1.getId())
                        .header("Authorization", adminTokenB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
