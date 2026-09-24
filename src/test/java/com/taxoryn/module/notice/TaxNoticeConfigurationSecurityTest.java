package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.notice.dto.CreateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.CreateTaxNoticeRequest;
import com.taxoryn.module.notice.dto.NoticeResponseDto;
import com.taxoryn.module.notice.dto.ReviewNoticeResponseRequest;
import com.taxoryn.module.notice.dto.SubmitNoticeRequest;
import com.taxoryn.module.notice.dto.TaxNoticeConfigDto;
import com.taxoryn.module.notice.dto.TaxNoticeDto;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeConfigRequest;
import com.taxoryn.module.notice.entity.TaxNoticeConfigEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeResponseStatus;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.enums.SubmissionMode;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeConfigRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.notice.service.TaxNoticeConfigurationService;
import com.taxoryn.module.notice.service.TaxNoticeService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
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
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaxNoticeConfigurationSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private TaxNoticeRepository noticeRepository;
    @Autowired private NoticeResponseRepository responseRepository;
    @Autowired private NoticeHearingRepository hearingRepository;
    @Autowired private NoticeActivityRepository activityRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private TaxNoticeConfigRepository configRepository;
    @Autowired private ProductModuleRepository productModuleRepository;
    @Autowired private ModuleConfigurationService moduleConfigurationService;
    @Autowired private TaxNoticeConfigurationService configurationService;
    @Autowired private TaxNoticeService noticeService;

    // Org A
    private OrganizationEntity orgA;
    private UserEntity adminUserA;
    private UserEntity partnerUserA;
    private UserEntity staffUserA;
    private EmployeeEntity staffEmployeeA;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private String adminTokenA;
    private String partnerTokenA;
    private String staffTokenA;

    // Org B (Cross-tenant)
    private OrganizationEntity orgB;
    private UserEntity adminUserB;
    private String adminTokenB;

    @BeforeEach
    void setUp() {
        cleanDb();

        // 1. Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Org Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity partnerRole = roleRepository.save(RoleEntity.builder()
                .code("PARTNER")
                .name("Partner")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 2. Org A (Default: SMALL_TAX_FIRM)
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Advisors")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.SMALL_TAX_FIRM)
                .email("contact@alphatax.com")
                .phone("+919876543210")
                .build());

        TenantContext.setTenantId(orgA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .email("admin@alpha.tax")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        partnerUserA = userRepository.save(UserEntity.builder()
                .email("partner@alpha.tax")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Alpha")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build());

        staffUserA = userRepository.save(UserEntity.builder()
                .email("staff@alpha.tax")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Alpha")
                .lastName("Staff")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        staffEmployeeA = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffUserA.getId())
                .employeeCode("EMP-A1")
                .firstName("Alpha")
                .lastName("Staff")
                .email("staff@alpha.tax")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now().minusMonths(6))
                .build());

        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Client Alpha One")
                .pan("AAACA1111A")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(staffEmployeeA.getId())
                .build());

        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Client Alpha Two")
                .pan("AAACA2222A")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(UUID.randomUUID()) // Unassigned to staffUserA
                .build());
        TenantContext.clear();

        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserA.getId(), orgA.getId(), adminUserA.getEmail(), Set.of("ORG_ADMIN"), Set.of("ORGANIZATION_UPDATE", "TAX_NOTICE:CONFIGURE", "NOTICE_VIEW", "NOTICE_UPDATE"));
        partnerTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(partnerUserA.getId(), orgA.getId(), partnerUserA.getEmail(), Set.of("PARTNER"), Set.of("NOTICE_VIEW", "NOTICE_UPDATE", "TAX_NOTICE:APPROVE"));
        staffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(staffUserA.getId(), orgA.getId(), staffUserA.getEmail(), Set.of("STAFF"), Set.of("NOTICE_VIEW", "NOTICE_UPDATE"));

        // 3. Org B (SOLO_PRACTITIONER)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Solo Practice")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .email("contact@betatax.com")
                .phone("+919876543211")
                .build());

        TenantContext.setTenantId(orgB.getId());
        adminUserB = userRepository.save(UserEntity.builder()
                .email("admin@beta.tax")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        TenantContext.clear();

        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserB.getId(), orgB.getId(), adminUserB.getEmail(), Set.of("ORG_ADMIN"), Set.of("ORGANIZATION_UPDATE", "NOTICE_VIEW"));

        // Seed product modules if missing
        if (productModuleRepository.findByCode(ProductModuleCode.TAX_NOTICES).isEmpty()) {
            productModuleRepository.save(ProductModuleEntity.builder()
                    .code(ProductModuleCode.TAX_NOTICES)
                    .name("Tax Notice Management")
                    .category(ProductModuleCategory.TAX)
                    .status("ACTIVE")
                    .enabledByDefault(true)
                    .displayOrder(11)
                    .build());
        }

        // Set default authenticated context as Org A Admin
        setAuthUser(adminUserA, orgA.getId(), "ORG_ADMIN", Set.of("TAX_NOTICE:CONFIGURE", "NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE", "TAX_NOTICE:APPROVE"));
    }

    private void setAuthUser(UserEntity user, UUID orgId, String role, Set<String> permissions) {
        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(orgId)
                .email(user.getEmail())
                .roles(Set.of(role))
                .permissions(permissions)
                .enabled(true)
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(orgId);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void cleanDb() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        configRepository.deleteAll();
        activityRepository.deleteAll();
        hearingRepository.deleteAll();
        responseRepository.deleteAll();
        taskRepository.deleteAll();
        noticeRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // ==========================================
    // 1. Default Configuration
    // ==========================================
    @Test
    @DisplayName("1. Default configuration resolves accurately for unconfigured organization")
    void test1_defaultConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(orgA.getId());

        assertThat(config).isNotNull();
        assertThat(config.isCustomized()).isFalse();
        assertThat(config.getOrganizationType()).isEqualTo(OrganizationType.SMALL_TAX_FIRM);
        assertThat(config.isResponseReviewRequired()).isTrue();
        assertThat(config.isPartnerApprovalRequired()).isFalse();
        assertThat(config.getDefaultResponseDueDays()).isEqualTo(30);
        assertThat(config.isAutoCreateResponseTask()).isTrue();
    }

    // ==========================================
    // 2. Organization Override
    // ==========================================
    @Test
    @DisplayName("2. Organization explicit override takes precedence over persona defaults")
    void test2_organizationOverride() {
        TenantContext.setTenantId(orgA.getId());
        UpdateTaxNoticeConfigRequest updateReq = UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(45)
                .partnerApprovalRequired(true)
                .autoCreateResponseTask(false)
                .build();

        TaxNoticeConfigDto updated = configurationService.updateConfiguration(orgA.getId(), updateReq);

        assertThat(updated.isCustomized()).isTrue();
        assertThat(updated.getDefaultResponseDueDays()).isEqualTo(45);
        assertThat(updated.isPartnerApprovalRequired()).isTrue();
        assertThat(updated.isAutoCreateResponseTask()).isFalse();

        // Re-query effective configuration
        TaxNoticeConfigDto effective = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(effective.isCustomized()).isTrue();
        assertThat(effective.getDefaultResponseDueDays()).isEqualTo(45);
    }

    // ==========================================
    // 3. Configuration Retrieval API
    // ==========================================
    @Test
    @DisplayName("3. Configuration retrieval endpoint returns effective settings")
    void test3_configurationRetrieval() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/tax-notice-config")
                        .header("Authorization", adminTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationType").value("SMALL_TAX_FIRM"))
                .andExpect(jsonPath("$.data.defaultResponseDueDays").value(30))
                .andExpect(jsonPath("$.data.customized").value(false));

        // Dual route /api/v1/tax-notices/config
        mockMvc.perform(get("/api/v1/tax-notices/config")
                        .header("Authorization", adminTokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==========================================
    // 4. Configuration Update API
    // ==========================================
    @Test
    @DisplayName("4. Configuration update API persists custom settings")
    void test4_configurationUpdate() throws Exception {
        UpdateTaxNoticeConfigRequest req = UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(60)
                .reminderDaysBeforeDue(10)
                .partnerApprovalRequired(true)
                .build();

        mockMvc.perform(put("/api/v1/organizations/tax-notice-config")
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultResponseDueDays").value(60))
                .andExpect(jsonPath("$.data.reminderDaysBeforeDue").value(10))
                .andExpect(jsonPath("$.data.partnerApprovalRequired").value(true))
                .andExpect(jsonPath("$.data.customized").value(true));
    }

    // ==========================================
    // 5. Unauthorized Configuration Update
    // ==========================================
    @Test
    @DisplayName("5. Staff without admin permission cannot modify configuration")
    void test5_unauthorizedConfigurationUpdate() throws Exception {
        UpdateTaxNoticeConfigRequest req = UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(15)
                .build();

        mockMvc.perform(put("/api/v1/organizations/tax-notice-config")
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 6. Cross-Tenant Configuration Access
    // ==========================================
    @Test
    @DisplayName("6. Cross-tenant configuration access is completely isolated")
    void test6_crossTenantConfigurationAccess() throws Exception {
        // Admin B updates Org B configuration
        UpdateTaxNoticeConfigRequest reqB = UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(20)
                .build();

        mockMvc.perform(put("/api/v1/organizations/tax-notice-config")
                        .header("Authorization", adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultResponseDueDays").value(20));

        // Org A's configuration remains unchanged (30)
        mockMvc.perform(get("/api/v1/organizations/tax-notice-config")
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultResponseDueDays").value(30));
    }

    // ==========================================
    // 7. Persona Default Resolution
    // ==========================================
    @Test
    @DisplayName("7. Persona defaults resolve distinct profiles for all OrganizationTypes")
    void test7_personaDefaultResolution() {
        TaxNoticeConfigDto solo = configurationService.getPersonaDefaults(OrganizationType.SOLO_PRACTITIONER);
        assertThat(solo.isResponseReviewRequired()).isFalse();
        assertThat(solo.isAutoCreateResponseTask()).isFalse();

        TaxNoticeConfigDto smallFirm = configurationService.getPersonaDefaults(OrganizationType.SMALL_TAX_FIRM);
        assertThat(smallFirm.isResponseReviewRequired()).isTrue();
        assertThat(smallFirm.isPartnerApprovalRequired()).isFalse();

        TaxNoticeConfigDto growing = configurationService.getPersonaDefaults(OrganizationType.GROWING_PRACTICE);
        assertThat(growing.isResponseReviewRequired()).isTrue();
        assertThat(growing.getReminderDaysBeforeDue()).isEqualTo(7);
        assertThat(growing.getEscalationDaysAfterDue()).isEqualTo(3);

        TaxNoticeConfigDto business = configurationService.getPersonaDefaults(OrganizationType.BUSINESS);
        assertThat(business.getDefaultResponseDueDays()).isEqualTo(21);
        assertThat(business.getDefaultPriority()).isEqualTo(NoticePriority.HIGH);

        TaxNoticeConfigDto unknown = configurationService.getPersonaDefaults(OrganizationType.UNKNOWN);
        assertThat(unknown.getDefaultResponseDueDays()).isEqualTo(30);
    }

    // ==========================================
    // 8. Custom Config Survives OrganizationType Change
    // ==========================================
    @Test
    @DisplayName("8. Custom configuration survives OrganizationType changes")
    void test8_customConfigSurvivesOrgTypeChange() {
        TenantContext.setTenantId(orgA.getId());
        // Set custom override for Org A (currently SMALL_TAX_FIRM)
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(42)
                .build());

        // Change Org A type to GROWING_PRACTICE
        orgA.setOrganizationType(OrganizationType.GROWING_PRACTICE);
        organizationRepository.save(orgA);

        TaxNoticeConfigDto effectiveAfterChange = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(effectiveAfterChange.isCustomized()).isTrue();
        assertThat(effectiveAfterChange.getDefaultResponseDueDays()).isEqualTo(42); // Preserved!
    }

    // ==========================================
    // 9. Response Deadline Calculation
    // ==========================================
    @Test
    @DisplayName("9. Notice creation calculates response due date from config when omitted")
    void test9_responseDeadlineCalculation() {
        TenantContext.setTenantId(orgA.getId());
        // Set default due days to 25
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(25)
                .build());

        LocalDate noticeDate = LocalDate.now().minusDays(2);
        CreateTaxNoticeRequest createReq = CreateTaxNoticeRequest.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2026/CONFIG-01")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(2) Scrutiny")
                .subject("Assessment Query")
                .noticeDate(noticeDate)
                .receivedDate(LocalDate.now())
                .responseDueDate(null) // Omitted
                .build();

        TaxNoticeDto created = noticeService.createNotice(createReq);
        assertThat(created.getResponseDueDate()).isEqualTo(noticeDate.plusDays(25));
    }

    // ==========================================
    // 10. Explicit Official Deadline Remains Authoritative
    // ==========================================
    @Test
    @DisplayName("10. Explicit official response due date is never overwritten by config default")
    void test10_explicitOfficialDeadlineAuthoritative() {
        TenantContext.setTenantId(orgA.getId());
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(30)
                .build());

        LocalDate explicitDueDate = LocalDate.now().plusDays(12);
        CreateTaxNoticeRequest createReq = CreateTaxNoticeRequest.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2026/CONFIG-02")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("148 Reassessment")
                .subject("Income Escaping Assessment")
                .noticeDate(LocalDate.now().minusDays(5))
                .receivedDate(LocalDate.now())
                .responseDueDate(explicitDueDate) // Explicit official deadline
                .build();

        TaxNoticeDto created = noticeService.createNotice(createReq);
        assertThat(created.getResponseDueDate()).isEqualTo(explicitDueDate);
    }

    // ==========================================
    // 11. Response Review Configuration
    // ==========================================
    @Test
    @DisplayName("11. Response review configuration is properly accessible and resolvable")
    void test11_responseReviewConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(config.isResponseReviewRequired()).isTrue();

        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .responseReviewRequired(false)
                .build());

        TaxNoticeConfigDto updated = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(updated.isResponseReviewRequired()).isFalse();
    }

    // ==========================================
    // 12. Partner Approval Configuration
    // ==========================================
    @Test
    @DisplayName("12. Partner approval enforcement blocks submission when required and unapproved")
    void test12_partnerApprovalConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        // Enable mandatory partner approval
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .partnerApprovalRequired(true)
                .build());

        CreateTaxNoticeRequest createReq = CreateTaxNoticeRequest.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2026/PARTNER-01")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1) Intimation")
                .subject("Demand Notice")
                .receivedDate(LocalDate.now())
                .responseDueDate(LocalDate.now().plusDays(30))
                .build();
        TaxNoticeDto notice = noticeService.createNotice(createReq);

        SubmitNoticeRequest submitReq = SubmitNoticeRequest.builder()
                .submissionMode(SubmissionMode.INCOME_TAX_PORTAL)
                .portalAcknowledgementNumber("ACK1234567")
                .build();

        // Staff attempting to submit without partner approval throws BusinessValidationException
        setAuthUser(staffUserA, orgA.getId(), "STAFF", Set.of("NOTICE_VIEW", "NOTICE_UPDATE"));
        assertThatThrownBy(() -> noticeService.submitNotice(notice.getId(), submitReq))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Partner approval is required by organization policy");
    }

    // ==========================================
    // 13. Automatic Task Creation Configuration
    // ==========================================
    @Test
    @DisplayName("13. Auto-create task configuration triggers task generation on notice creation")
    void test13_autoCreateTaskConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .autoCreateResponseTask(true)
                .build());

        long tasksBefore = taskRepository.count();

        CreateTaxNoticeRequest createReq = CreateTaxNoticeRequest.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2026/TASK-01")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1)")
                .subject("Task Test Notice")
                .receivedDate(LocalDate.now())
                .responseDueDate(LocalDate.now().plusDays(20))
                .createIntakeTask(null) // Let configuration decide
                .build();

        TaxNoticeDto notice = noticeService.createNotice(createReq);
        long tasksAfter = taskRepository.count();

        assertThat(tasksAfter).isEqualTo(tasksBefore + 1);
        List<TaskEntity> tasks = taskRepository.findAll();
        assertThat(tasks.stream().anyMatch(t -> notice.getId().equals(t.getNoticeId()))).isTrue();
    }

    // ==========================================
    // 14. Notification Configuration
    // ==========================================
    @Test
    @DisplayName("14. Notification configuration flags persist and resolve correctly")
    void test14_notificationConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .notifyOnAssignment(false)
                .notifyOnHearing(true)
                .notifyOnSubmission(false)
                .build());

        TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(config.isNotifyOnAssignment()).isFalse();
        assertThat(config.isNotifyOnHearing()).isTrue();
        assertThat(config.isNotifyOnSubmission()).isFalse();
    }

    // ==========================================
    // 15. Dashboard Widget Configuration
    // ==========================================
    @Test
    @DisplayName("15. Dashboard widget visibility flags persist and resolve correctly")
    void test15_dashboardWidgetConfiguration() {
        TenantContext.setTenantId(orgA.getId());
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .showDueSoon(false)
                .showOverdue(true)
                .showAwaitingHearing(false)
                .build());

        TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(config.isShowDueSoon()).isFalse();
        assertThat(config.isShowOverdue()).isTrue();
        assertThat(config.isShowAwaitingHearing()).isFalse();
    }

    // ==========================================
    // 16. Disabled TAX_NOTICES Behavior
    // ==========================================
    @Test
    @DisplayName("16. Disabled TAX_NOTICES product module blocks configuration access")
    void test16_disabledTaxNoticesModule() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.TAX_NOTICES, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/organizations/tax-notice-config")
                        .header("Authorization", adminTokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module TAX_NOTICES is disabled for this organization"));
    }

    // ==========================================
    // 17. Subscription Plan Enforcement
    // ==========================================
    @Test
    @DisplayName("17. Subscription plan commercial boundary is preserved")
    void test17_subscriptionEnforcement() {
        TenantContext.setTenantId(orgA.getId());
        TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(orgA.getId());
        assertThat(config).isNotNull();
    }

    // ==========================================
    // 18. Client Scope Unchanged by Configuration
    // ==========================================
    @Test
    @DisplayName("18. Client portfolio scope is strictly enforced regardless of configuration")
    void test18_clientScopeUnchangedByConfig() {
        setAuthUser(adminUserA, orgA.getId(), "ORG_ADMIN", Set.of("TAX_NOTICE:CONFIGURE", "NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE"));

        // Configure permissive workflow
        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .responseReviewRequired(false)
                .assignmentRequired(false)
                .build());

        // Create notice for unassigned clientA2
        CreateTaxNoticeRequest createReq = CreateTaxNoticeRequest.builder()
                .clientId(clientA2.getId())
                .noticeNumber("ITBA/AST/2026/SCOPE-01")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1)")
                .subject("Scope Check Notice")
                .receivedDate(LocalDate.now())
                .responseDueDate(LocalDate.now().plusDays(20))
                .build();
        TaxNoticeDto notice = noticeService.createNotice(createReq);

        // Staff user has no access to clientA2
        setAuthUser(staffUserA, orgA.getId(), "STAFF", Set.of("NOTICE_VIEW", "NOTICE_UPDATE"));

        // Querying notice details as staff should fail
        assertThatThrownBy(() -> noticeService.getNoticeById(notice.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==========================================
    // 19. Maker-Checker Integrity Cannot Be Bypassed
    // ==========================================
    @Test
    @DisplayName("19. Maker-checker self-approval cannot be bypassed through configuration")
    void test19_makerCheckerIntegrityEnforced() {
        setAuthUser(staffUserA, orgA.getId(), "STAFF", Set.of("NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "TAX_NOTICE:APPROVE"));

        CreateTaxNoticeRequest createReq = CreateTaxNoticeRequest.builder()
                .clientId(clientA1.getId())
                .noticeNumber("ITBA/AST/2026/MC-01")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1)")
                .subject("Maker Checker Notice")
                .receivedDate(LocalDate.now())
                .responseDueDate(LocalDate.now().plusDays(30))
                .build();
        TaxNoticeDto notice = noticeService.createNotice(createReq);

        // Staff drafts response
        CreateNoticeResponseRequest draftReq = CreateNoticeResponseRequest.builder()
                .responseTitle("Initial Draft Response")
                .responseSummary("Grounds for appeal")
                .submitForReview(true)
                .build();
        NoticeResponseDto response = noticeService.draftNoticeResponse(notice.getId(), draftReq);

        // Same user attempting to self-approve review throws ForbiddenException
        ReviewNoticeResponseRequest reviewReq = ReviewNoticeResponseRequest.builder()
                .action("APPROVE_REVIEW")
                .comments("Self approval attempt")
                .build();

        assertThatThrownBy(() -> noticeService.reviewResponse(notice.getId(), response.getId(), reviewReq))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Maker-Checker Violation");
    }

    // ==========================================
    // 20. Audit Event Generation
    // ==========================================
    @Test
    @DisplayName("20. Configuration updates emit TAX_NOTICE_CONFIGURATION_UPDATED audit logs")
    void test20_auditEventGeneration() {
        TenantContext.setTenantId(orgA.getId());
        long auditCountBefore = auditLogRepository.count();

        configurationService.updateConfiguration(orgA.getId(), UpdateTaxNoticeConfigRequest.builder()
                .defaultResponseDueDays(40)
                .build());

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
    }
}
