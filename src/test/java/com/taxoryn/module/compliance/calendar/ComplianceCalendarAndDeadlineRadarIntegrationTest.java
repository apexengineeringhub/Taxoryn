package com.taxoryn.module.compliance.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.calendar.dto.ClientComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineRadarDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.model.DeadlineStatus;
import com.taxoryn.module.compliance.calendar.service.ComplianceCalendarService;
import com.taxoryn.module.compliance.calendar.service.ComplianceDeadlineClassifier;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceCalendarAndDeadlineRadarIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ComplianceDeadlineClassifier classifier;

    @Autowired
    private ComplianceCalendarService calendarService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private ClientEntity clientB;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA & Associates " + suffix)
                .legalName("Apex Chartered Accountants LLP")
                .email("admin_" + suffix + "@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup Tenant B (for multi-tenant isolation tests)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Advisory LLP " + suffix)
                .legalName("Global Advisory Partners")
                .email("admin_" + suffix + "@globaladv.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Users in Tenant A & Tenant B
        TenantContext.setTenantId(orgA.getId());
        userA = UserEntity.builder()
                .email("ca.sharma." + suffix + "@apexca.in")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        userA.setOrganizationId(orgA.getId());
        userA = userRepository.save(userA);

        TenantContext.setTenantId(orgB.getId());
        userB = UserEntity.builder()
                .email("adv.patel." + suffix + "@globaladv.in")
                .firstName("Suresh")
                .lastName("Patel")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        userB.setOrganizationId(orgB.getId());
        userB = userRepository.save(userB);

        // 5. Auth Tokens
        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("COMPLIANCE_VIEW", "COMPLIANCE_MANAGE", "CLIENT_VIEW", "CLIENT_MANAGE", "TASK_VIEW", "GST_VIEW", "ITR_VIEW", "TDS_VIEW")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("COMPLIANCE_VIEW", "COMPLIANCE_MANAGE", "CLIENT_VIEW", "CLIENT_MANAGE", "TASK_VIEW", "GST_VIEW", "ITR_VIEW", "TDS_VIEW")
        );

        // Setup Clients
        TenantContext.setTenantId(orgA.getId());
        clientA1 = ClientEntity.builder()
                .displayName("Alpha Tech Solutions Pvt Ltd")
                .legalName("Alpha Tech Solutions Private Limited")
                .clientCode("CL-ALPHA-" + UUID.randomUUID().toString().substring(0, 6))
                .pan("AAACA1234A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        clientA1.setOrganizationId(orgA.getId());
        clientA1 = clientRepository.save(clientA1);

        clientA2 = ClientEntity.builder()
                .displayName("Beta Enterprises")
                .legalName("Beta Enterprises LLP")
                .clientCode("CL-BETA-" + UUID.randomUUID().toString().substring(0, 6))
                .pan("BBBCB5678B")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .build();
        clientA2.setOrganizationId(orgA.getId());
        clientA2 = clientRepository.save(clientA2);
        TenantContext.clear();

        TenantContext.setTenantId(orgB.getId());
        clientB = ClientEntity.builder()
                .displayName("Gamma Trading Co")
                .legalName("Gamma Trading Corporation")
                .clientCode("CL-GAMMA-" + UUID.randomUUID().toString().substring(0, 6))
                .pan("CCCGC9999C")
                .clientType(ClientType.PROPRIETORSHIP)
                .status(ClientStatus.ACTIVE)
                .build();
        clientB.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientB);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ComplianceObligationEntity createObligation(
            UUID orgId,
            UUID clientId,
            String ruleCode,
            ComplianceRuleDomain domain,
            CompliancePeriodType periodType,
            String periodKey,
            LocalDate statutoryDueDate,
            ComplianceObligationStatus status,
            TaskPriority priority
    ) {
        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .ruleCode(ruleCode)
                .ruleVersion(1)
                .ruleNameSnapshot(ruleCode.replace("_", " "))
                .domain(domain)
                .periodType(periodType)
                .periodKey(periodKey)
                .periodLabel(periodKey)
                .statutoryDueDate(statutoryDueDate)
                .dueDate(statutoryDueDate)
                .status(status)
                .priority(priority)
                .title(ruleCode + " - " + periodKey)
                .build();
        ob.setOrganizationId(orgId);
        return obligationRepository.save(ob);
    }

    // =========================================================================
    // 1. Classification Tests
    // =========================================================================

    @Test
    @DisplayName("1. Deterministic Classifier: Classifies OVERDUE correctly with days overdue")
    void testClassifier_Overdue() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20); // Reference: 20 Oct 2026 (Wednesday)

        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 15)) // 5 days past
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        var result = classifier.classify(ob, refDate);
        assertThat(result.status()).isEqualTo(DeadlineStatus.OVERDUE);
        assertThat(result.daysOverdue()).isEqualTo(5);
        assertThat(result.daysRemaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("2. Deterministic Classifier: Classifies DUE_TODAY with zero remaining days")
    void testClassifier_DueToday() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20);

        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 20))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();

        var result = classifier.classify(ob, refDate);
        assertThat(result.status()).isEqualTo(DeadlineStatus.DUE_TODAY);
        assertThat(result.daysRemaining()).isEqualTo(0);
        assertThat(result.daysOverdue()).isEqualTo(0);
    }

    @Test
    @DisplayName("3. Deterministic Classifier: Classifies DUE_TOMORROW with 1 day remaining")
    void testClassifier_DueTomorrow() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20);

        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 21))
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        var result = classifier.classify(ob, refDate);
        assertThat(result.status()).isEqualTo(DeadlineStatus.DUE_TOMORROW);
        assertThat(result.daysRemaining()).isEqualTo(1);
        assertThat(result.daysOverdue()).isEqualTo(0);
    }

    @Test
    @DisplayName("4. Deterministic Classifier: Classifies DUE_WITHIN_3_DAYS for +2 and +3 days")
    void testClassifier_DueWithin3Days() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20); // Tuesday

        ComplianceObligationEntity ob2 = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 22)) // +2 days (Thursday)
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        ComplianceObligationEntity ob3 = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 23)) // +3 days (Friday)
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        var res2 = classifier.classify(ob2, refDate);
        assertThat(res2.status()).isEqualTo(DeadlineStatus.DUE_WITHIN_3_DAYS);
        assertThat(res2.daysRemaining()).isEqualTo(2);

        var res3 = classifier.classify(ob3, refDate);
        assertThat(res3.status()).isEqualTo(DeadlineStatus.DUE_WITHIN_3_DAYS);
        assertThat(res3.daysRemaining()).isEqualTo(3);
    }

    @Test
    @DisplayName("5. Deterministic Classifier: Classifies DUE_THIS_WEEK for end of ISO week (Sunday)")
    void testClassifier_DueThisWeek() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20); // Tuesday, End of week: Sunday 25 Oct

        ComplianceObligationEntity obSunday = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 25)) // Sunday (+5 days)
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        var res = classifier.classify(obSunday, refDate);
        assertThat(res.status()).isEqualTo(DeadlineStatus.DUE_THIS_WEEK);
        assertThat(res.daysRemaining()).isEqualTo(5);
    }

    @Test
    @DisplayName("6. Deterministic Classifier: Classifies UPCOMING for dates past the current ISO week")
    void testClassifier_Upcoming() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20); // Tuesday, End of week: Sunday 25 Oct

        ComplianceObligationEntity obNextMonday = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 26)) // Next Monday (+6 days)
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        var res = classifier.classify(obNextMonday, refDate);
        assertThat(res.status()).isEqualTo(DeadlineStatus.UPCOMING);
        assertThat(res.daysRemaining()).isEqualTo(6);
    }

    @Test
    @DisplayName("7. Deterministic Classifier: Classifies NO_DUE_DATE when due date is null")
    void testClassifier_NoDueDate() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20);

        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .statutoryDueDate(null)
                .dueDate(null)
                .status(ComplianceObligationStatus.UPCOMING)
                .build();

        var res = classifier.classify(ob, refDate);
        assertThat(res.status()).isEqualTo(DeadlineStatus.NO_DUE_DATE);
        assertThat(res.daysRemaining()).isEqualTo(0);
        assertThat(res.daysOverdue()).isEqualTo(0);
    }

    @Test
    @DisplayName("8. Deterministic Classifier: COMPLETED and CANCELLED terminal states are classified as terminal")
    void testClassifier_TerminalStates() {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20);

        ComplianceObligationEntity comp = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 10)) // Past date
                .status(ComplianceObligationStatus.COMPLETED)
                .build();

        ComplianceObligationEntity canc = ComplianceObligationEntity.builder()
                .statutoryDueDate(LocalDate.of(2026, Month.OCTOBER, 10)) // Past date
                .status(ComplianceObligationStatus.CANCELLED)
                .build();

        assertThat(classifier.classify(comp, refDate).status()).isEqualTo(DeadlineStatus.COMPLETED);
        assertThat(classifier.classify(canc, refDate).status()).isEqualTo(DeadlineStatus.CANCELLED);
    }

    // =========================================================================
    // 2. Practice Radar API & Summary Tests
    // =========================================================================

    @Test
    @DisplayName("9. Practice Deadline Radar API: Returns exact aggregate summary counts and domain breakdown without double counting")
    void testGetPracticeDeadlineRadar_AggregateSummaryAndBreakdown() throws Exception {
        LocalDate refDate = LocalDate.of(2026, Month.OCTOBER, 20); // Tuesday

        TenantContext.setTenantId(orgA.getId());
        // Create diverse test obligations in Org A:
        // 1 Overdue (GST)
        createObligation(orgA.getId(), clientA1.getId(), "GST_GSTR1_MONTHLY", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 11), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        // 1 Due Today (GST)
        createObligation(orgA.getId(), clientA1.getId(), "GST_GSTR3B_MONTHLY", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 20), ComplianceObligationStatus.IN_PROGRESS, TaskPriority.URGENT);
        // 1 Due Tomorrow (TDS)
        createObligation(orgA.getId(), clientA1.getId(), "TDS_CHALLAN281_MONTHLY", ComplianceRuleDomain.TDS, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 21), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        // 1 Due Within 3 Days (22 Oct - MCA)
        createObligation(orgA.getId(), clientA2.getId(), "MCA_MGT7_ANNUAL", ComplianceRuleDomain.MCA_ROC, CompliancePeriodType.FINANCIAL_YEAR, "2025-26", LocalDate.of(2026, 10, 22), ComplianceObligationStatus.UPCOMING, TaskPriority.MEDIUM);
        // 1 Due This Week (24 Oct Saturday - ITR)
        createObligation(orgA.getId(), clientA2.getId(), "ITR_NON_AUDIT_ANNUAL", ComplianceRuleDomain.INCOME_TAX, CompliancePeriodType.ASSESSMENT_YEAR, "2026-27", LocalDate.of(2026, 10, 24), ComplianceObligationStatus.UPCOMING, TaskPriority.MEDIUM);
        // 1 Upcoming (31 Oct - ITR)
        createObligation(orgA.getId(), clientA1.getId(), "ITR_COMPANY_AUDIT_ANNUAL", ComplianceRuleDomain.INCOME_TAX, CompliancePeriodType.ASSESSMENT_YEAR, "2026-27", LocalDate.of(2026, 10, 31), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        // 1 No Due Date (OTHER)
        createObligation(orgA.getId(), clientA2.getId(), "CUSTOM_INTERNAL_CHECK", ComplianceRuleDomain.OTHER, CompliancePeriodType.MONTH, "2026-09", null, ComplianceObligationStatus.UPCOMING, TaskPriority.LOW);
        // 1 Completed (Excluded from active radar counts)
        createObligation(orgA.getId(), clientA1.getId(), "GST_OLD_COMPLETED", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-08", LocalDate.of(2026, 9, 20), ComplianceObligationStatus.COMPLETED, TaskPriority.MEDIUM);
        // 1 Cancelled (Excluded from active radar counts)
        createObligation(orgA.getId(), clientA2.getId(), "GST_OLD_CANCELLED", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-08", LocalDate.of(2026, 9, 20), ComplianceObligationStatus.CANCELLED, TaskPriority.MEDIUM);
        TenantContext.clear();

        MvcResult result = mockMvc.perform(get("/api/v1/compliance/calendar/radar")
                        .header("Authorization", tokenA)
                        .param("referenceDate", "2026-10-20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.asOfDate").value("2026-10-20"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        ComplianceDeadlineRadarDto radar = objectMapper.treeToValue(root.get("data"), ComplianceDeadlineRadarDto.class);

        assertThat(radar.getSummary()).isNotNull();
        assertThat(radar.getSummary().getOverdue()).isEqualTo(1);
        assertThat(radar.getSummary().getDueToday()).isEqualTo(1);
        assertThat(radar.getSummary().getDueTomorrow()).isEqualTo(1);
        assertThat(radar.getSummary().getDueWithin3Days()).isEqualTo(1);
        assertThat(radar.getSummary().getDueThisWeek()).isEqualTo(1);
        assertThat(radar.getSummary().getUpcoming()).isEqualTo(1);
        assertThat(radar.getSummary().getNoDueDate()).isEqualTo(1);
        assertThat(radar.getSummary().getTotalActive()).isEqualTo(7);
        assertThat(radar.getSummary().getCompleted()).isEqualTo(1);
        assertThat(radar.getSummary().getCancelled()).isEqualTo(1);

        // Verify Domain Breakdown
        assertThat(radar.getDomainBreakdown()).containsKey(ComplianceRuleDomain.GST);
        assertThat(radar.getDomainBreakdown().get(ComplianceRuleDomain.GST).getOverdue()).isEqualTo(1);
        assertThat(radar.getDomainBreakdown().get(ComplianceRuleDomain.GST).getDueToday()).isEqualTo(1);

        assertThat(radar.getDomainBreakdown()).containsKey(ComplianceRuleDomain.TDS);
        assertThat(radar.getDomainBreakdown().get(ComplianceRuleDomain.TDS).getDueTomorrow()).isEqualTo(1);

        // Verify Top Deadlines sorting
        assertThat(radar.getTopDeadlines()).isNotEmpty();
        assertThat(radar.getTopDeadlines().get(0).getDeadlineStatus()).isEqualTo(DeadlineStatus.OVERDUE);
        assertThat(radar.getTopDeadlines().get(1).getDeadlineStatus()).isEqualTo(DeadlineStatus.DUE_TODAY);
    }

    // =========================================================================
    // 3. Calendar List & Filtering Tests
    // =========================================================================

    @Test
    @DisplayName("10. Calendar Query: Domain filter returns only requested domain")
    void testGetCalendar_DomainFilter() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        createObligation(orgA.getId(), clientA1.getId(), "GST_TEST_FILTER", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 20), ComplianceObligationStatus.UPCOMING, TaskPriority.MEDIUM);
        createObligation(orgA.getId(), clientA1.getId(), "TDS_TEST_FILTER", ComplianceRuleDomain.TDS, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 20), ComplianceObligationStatus.UPCOMING, TaskPriority.MEDIUM);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/compliance/calendar")
                        .header("Authorization", tokenA)
                        .param("domain", "GST")
                        .param("referenceDate", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.[*].domain").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("GST"))));
    }

    @Test
    @DisplayName("11. Calendar Query: Deadline status filter returns only matching deadline bucket")
    void testGetCalendar_DeadlineStatusFilter() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        createObligation(orgA.getId(), clientA1.getId(), "GST_OVERDUE_STATUS", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 10), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        createObligation(orgA.getId(), clientA1.getId(), "GST_FUTURE_STATUS", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 11, 20), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/compliance/calendar")
                        .header("Authorization", tokenA)
                        .param("deadlineStatus", "OVERDUE")
                        .param("referenceDate", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.[*].deadlineStatus").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("OVERDUE"))));
    }

    @Test
    @DisplayName("12. Calendar Query: Maximum date range protection rejects spans exceeding 366 days")
    void testGetCalendar_DateRangeMaxLimit() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/calendar")
                        .header("Authorization", tokenA)
                        .param("from", "2026-01-01")
                        .param("to", "2027-02-01")) // 396 days
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 4. Client 360 Deadline Summary API Tests
    // =========================================================================

    @Test
    @DisplayName("13. Client 360 Deadline Summary API: Returns compact summary and next closest deadline")
    void testGetClientDeadlineSummary_Client360() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        createObligation(orgA.getId(), clientA1.getId(), "GST_GSTR3B_MONTHLY", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 20), ComplianceObligationStatus.UPCOMING, TaskPriority.URGENT);
        createObligation(orgA.getId(), clientA1.getId(), "ITR_AUDIT_ANNUAL", ComplianceRuleDomain.INCOME_TAX, CompliancePeriodType.ASSESSMENT_YEAR, "2026-27", LocalDate.of(2026, 10, 31), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        TenantContext.clear();

        MvcResult result = mockMvc.perform(get("/api/v1/clients/{clientId}/compliance/calendar/summary", clientA1.getId())
                        .header("Authorization", tokenA)
                        .param("referenceDate", "2026-10-20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(clientA1.getId().toString()))
                .andExpect(jsonPath("$.data.clientDisplayName").value(clientA1.getDisplayName()))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        ClientComplianceDeadlineSummaryDto summary = objectMapper.treeToValue(root.get("data"), ClientComplianceDeadlineSummaryDto.class);

        assertThat(summary.getSummary().getDueToday()).isEqualTo(1);
        assertThat(summary.getSummary().getUpcoming()).isEqualTo(1);
        assertThat(summary.getNextDeadline()).isNotNull();
        assertThat(summary.getNextDeadline().getRuleCode()).isEqualTo("GST_GSTR3B_MONTHLY");
        assertThat(summary.getNextDeadline().getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    // =========================================================================
    // 5. Multi-Tenant Isolation & Zero Write Side-Effects
    // =========================================================================

    @Test
    @DisplayName("14. Security & Isolation: Org A cannot view Org B obligations or radar metrics")
    void testTenantIsolation_CrossTenantDataHidden() throws Exception {
        TenantContext.setTenantId(orgB.getId());
        createObligation(orgB.getId(), clientB.getId(), "GST_GAMMA_SECRET", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 20), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        TenantContext.clear();

        // User A querying radar
        MvcResult resA = mockMvc.perform(get("/api/v1/compliance/calendar")
                        .header("Authorization", tokenA)
                        .param("search", "Gamma")
                        .param("referenceDate", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andReturn();

        // User A querying Org B's client calendar directly -> must return 404
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance/calendar/summary", clientB.getId())
                        .header("Authorization", tokenA)
                        .param("referenceDate", "2026-10-20"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Zero Side-Effects: Calendar and Radar queries produce zero database writes or audit writes")
    void testZeroSideEffects_PureReadModel() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        createObligation(orgA.getId(), clientA1.getId(), "GST_SIDE_EFFECT_TEST", ComplianceRuleDomain.GST, CompliancePeriodType.MONTH, "2026-09", LocalDate.of(2026, 10, 20), ComplianceObligationStatus.UPCOMING, TaskPriority.HIGH);
        TenantContext.clear();

        long initialObligations = obligationRepository.count();
        long initialTasks = taskRepository.count();
        long initialAudits = auditLogRepository.count();

        // Execute multiple calendar/radar queries
        mockMvc.perform(get("/api/v1/compliance/calendar/radar").header("Authorization", tokenA)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/compliance/calendar/summary").header("Authorization", tokenA)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/compliance/calendar").header("Authorization", tokenA)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance/calendar/summary", clientA1.getId()).header("Authorization", tokenA)).andExpect(status().isOk());

        assertThat(obligationRepository.count()).isEqualTo(initialObligations);
        assertThat(taskRepository.count()).isEqualTo(initialTasks);
        assertThat(auditLogRepository.count()).isEqualTo(initialAudits);
    }
}
