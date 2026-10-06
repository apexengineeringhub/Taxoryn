package com.taxoryn.module.compliance.duedate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.duedate.dto.DueDateCalculationResult;
import com.taxoryn.module.compliance.duedate.model.DueDateCalculationStatus;
import com.taxoryn.module.compliance.duedate.service.ComplianceDueDateService;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.obligation.dto.GenerateObligationsRequest;
import com.taxoryn.module.compliance.obligation.service.ComplianceObligationService;
import com.taxoryn.module.compliance.period.model.CompliancePeriod;
import com.taxoryn.module.compliance.period.service.CompliancePeriodService;
import com.taxoryn.module.compliance.profile.entity.ComplianceProfileEntity;
import com.taxoryn.module.compliance.profile.model.ComplianceFilingFrequency;
import com.taxoryn.module.compliance.profile.model.ComplianceGstRegistrationType;
import com.taxoryn.module.compliance.profile.model.ComplianceItrCategory;
import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import com.taxoryn.module.compliance.profile.model.ComplianceTdsDeductorCategory;
import com.taxoryn.module.compliance.profile.repository.ComplianceProfileRepository;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import com.taxoryn.module.compliance.rule.repository.ComplianceRuleCatalogRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CompliancePeriodAndDueDateIntegrationTest {

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
    private ComplianceProfileRepository complianceProfileRepository;

    @Autowired
    private ComplianceRuleCatalogRepository ruleRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CompliancePeriodService periodService;

    @Autowired
    private ComplianceDueDateService dueDateService;

    @Autowired
    private ComplianceObligationService obligationService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;
    private ClientEntity clientA;

    @BeforeEach
    void setUp() {
        cleanUp();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory LLP " + suffix)
                .legalName("Apex Chartered Accountants LLP")
                .email("admin_" + suffix + "@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup Tenant B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beacon Tax Partners " + suffix)
                .legalName("Beacon Tax Partners")
                .email("admin_" + suffix + "@beacontax.com")
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
                .email("admin_" + suffix + "@apexca.com")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        userA.setOrganizationId(orgA.getId());
        userA = userRepository.save(userA);

        TenantContext.setTenantId(orgB.getId());
        userB = UserEntity.builder()
                .email("admin_" + suffix + "@beacontax.com")
                .firstName("Vikram")
                .lastName("Seth")
                .passwordHash(passwordEncoder.encode("Password@123"))
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
                Set.of("CLIENT_VIEW", "CLIENT_EDIT", "COMPLIANCE_VIEW", "COMPLIANCE_MANAGE", "CLIENT_READ", "CLIENT_WRITE")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_EDIT", "COMPLIANCE_VIEW", "COMPLIANCE_MANAGE", "CLIENT_READ", "CLIENT_WRITE")
        );

        // 6. Enable CLIENTS module for both tenants
        TenantContext.setTenantId(orgA.getId());
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        TenantContext.setTenantId(orgB.getId());
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        // 7. Setup Client A (Company, Regular Monthly GST, TDS deductor)
        TenantContext.setTenantId(orgA.getId());
        clientA = ClientEntity.builder()
                .displayName("Alpha Traders Private Limited")
                .legalName("Alpha Traders Private Limited")
                .clientCode("CL-ALPHA-" + suffix)
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACA1234F")
                .gstin("27AAACA1234F1Z5")
                .status(ClientStatus.ACTIVE)
                .build();
        clientA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clientA);

        ComplianceProfileEntity profileA = ComplianceProfileEntity.builder()
                .clientId(clientA.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .gstEinvoiceApplicable(true)
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsDeductorCategory(ComplianceTdsDeductorCategory.COMPANY)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.COMPANY)
                .itrTaxAuditApplicable(true)
                .itrTransferPricingApplicable(false)
                .advanceTaxApplicable(true)
                .mcaFilingApplicable(true)
                .pfEsiApplicable(true)
                .build();
        profileA.setOrganizationId(orgA.getId());
        complianceProfileRepository.save(profileA);

        TenantContext.clear();
        seedStandardRulesIfEmpty();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        try {
            obligationRepository.deleteAll();
            complianceProfileRepository.deleteAll();
            clientRepository.deleteAll();
            organizationModuleRepository.deleteAll();
            userRepository.deleteAll();
            roleRepository.deleteAll();
            organizationRepository.deleteAll();
        } catch (Exception ignored) {
        }
    }

    private void seedStandardRulesIfEmpty() {
        saveSystemRuleIfNotExists("GST_GSTR1_MONTHLY", "GSTR-1 Monthly Return", ComplianceRuleDomain.GST,
                ComplianceRuleFrequency.MONTHLY, CompliancePeriodType.MONTH, DueDateRuleType.DAY_OF_FOLLOWING_MONTH,
                11, 11, 1, null, null, "11th of following month", "REGULAR", "MONTHLY", null, null, null);

        saveSystemRuleIfNotExists("GST_GSTR3B_MONTHLY", "GSTR-3B Monthly Return & Tax Payment", ComplianceRuleDomain.GST,
                ComplianceRuleFrequency.MONTHLY, CompliancePeriodType.MONTH, DueDateRuleType.DAY_OF_FOLLOWING_MONTH,
                20, 20, 1, null, null, "20th of following month", "REGULAR", "MONTHLY", null, null, null);

        saveSystemRuleIfNotExists("GST_GSTR1_QUARTERLY_QRMP", "GSTR-1 Quarterly (QRMP) Return", ComplianceRuleDomain.GST,
                ComplianceRuleFrequency.QUARTERLY, CompliancePeriodType.QUARTER, DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH,
                13, 13, 1, null, null, "13th of month following quarter", "REGULAR", "QUARTERLY", null, null, null);

        saveSystemRuleIfNotExists("GST_CMP08_QUARTERLY", "CMP-08 Quarterly Challan-cum-Statement", ComplianceRuleDomain.GST,
                ComplianceRuleFrequency.QUARTERLY, CompliancePeriodType.QUARTER, DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH,
                18, 18, 1, null, null, "18th of month following quarter", "COMPOSITION", "QUARTERLY", null, null, null);

        saveSystemRuleIfNotExists("GST_GSTR9_ANNUAL", "GSTR-9 Annual GST Return", ComplianceRuleDomain.GST,
                ComplianceRuleFrequency.ANNUAL, CompliancePeriodType.FINANCIAL_YEAR, DueDateRuleType.FIXED_DATE_IN_YEAR,
                31, null, null, 12, 31, "31st December following financial year", "REGULAR", "ANNUAL", null, null, null);

        saveSystemRuleIfNotExists("TDS_CHALLAN_281_MONTHLY", "Monthly TDS / TCS Deposit Challan ITNS 281", ComplianceRuleDomain.TDS,
                ComplianceRuleFrequency.MONTHLY, CompliancePeriodType.MONTH, DueDateRuleType.DAY_OF_FOLLOWING_MONTH,
                7, 7, 1, null, null, "7th of following month", null, null, true, null, null);

        saveSystemRuleIfNotExists("TDS_26Q_QUARTERLY", "Form 26Q Quarterly Non-Salary TDS Return", ComplianceRuleDomain.TDS,
                ComplianceRuleFrequency.QUARTERLY, CompliancePeriodType.QUARTER, DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH,
                31, 31, 1, null, null, "31st of month following quarter", null, null, true, null, null);

        saveSystemRuleIfNotExists("ITR_NON_AUDIT_ANNUAL", "ITR Filing (Non-Audit Cases)", ComplianceRuleDomain.INCOME_TAX,
                ComplianceRuleFrequency.ANNUAL, CompliancePeriodType.ASSESSMENT_YEAR, DueDateRuleType.FIXED_DATE_IN_YEAR,
                31, null, null, 7, 31, "31st July of Assessment Year", null, null, null, false, false);

        saveSystemRuleIfNotExists("ITR_AUDIT_ANNUAL", "ITR Filing (Corporate & Tax Audit Cases)", ComplianceRuleDomain.INCOME_TAX,
                ComplianceRuleFrequency.ANNUAL, CompliancePeriodType.ASSESSMENT_YEAR, DueDateRuleType.FIXED_DATE_IN_YEAR,
                31, null, null, 10, 31, "31st October of Assessment Year", null, null, null, true, false);

        saveSystemRuleIfNotExists("ITR_TAX_AUDIT_44AB", "Tax Audit Report Form 3CA/3CB-3CD", ComplianceRuleDomain.INCOME_TAX,
                ComplianceRuleFrequency.ANNUAL, CompliancePeriodType.ASSESSMENT_YEAR, DueDateRuleType.FIXED_DATE_IN_YEAR,
                30, null, null, 9, 30, "30th September of Assessment Year", null, null, null, true, false);

        saveSystemRuleIfNotExists("MCA_AOC4_ANNUAL", "MCA Form AOC-4 Financial Statements Filing", ComplianceRuleDomain.MCA_ROC,
                ComplianceRuleFrequency.ANNUAL, CompliancePeriodType.FINANCIAL_YEAR, DueDateRuleType.FIXED_DATE_IN_YEAR,
                29, null, null, 10, 29, "29th October following financial year", null, null, null, null, true);
    }

    private void saveSystemRuleIfNotExists(
            String ruleCode, String ruleName, ComplianceRuleDomain domain,
            ComplianceRuleFrequency frequency, CompliancePeriodType periodType,
            DueDateRuleType dueDateRuleType, Integer dueDay, Integer dueDayOffset,
            Integer dueMonthOffset, Integer fixedMonth, Integer fixedDay,
            String dueDateDescription, String gstRegTypes, String gstFrequencies,
            Boolean requiresTds, Boolean requiresTaxAudit, Boolean requiresMca) {
        if (ruleRepository.findByRuleCodeAndOrganizationIdIsNull(ruleCode).isEmpty()) {
            ruleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode(ruleCode)
                    .ruleName(ruleName)
                    .domain(domain)
                    .frequency(frequency)
                    .periodType(periodType)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .dueDateRuleType(dueDateRuleType)
                    .dueDay(dueDay)
                    .dueDayOffset(dueDayOffset)
                    .dueMonthOffset(dueMonthOffset)
                    .fixedMonth(fixedMonth)
                    .fixedDay(fixedDay)
                    .dueDateDescription(dueDateDescription)
                    .applicableGstRegistrationTypes(gstRegTypes)
                    .applicableFilingFrequencies(gstFrequencies)
                    .requiresTdsDeductor(requiresTds)
                    .requiresTaxAudit(requiresTaxAudit)
                    .requiresMcaFiling(requiresMca)
                    .build());
        }
    }

    // =========================================================================
    // 1. Period Engine Tests
    // =========================================================================

    @Test
    @DisplayName("Period Engine: Resolve Monthly Period (September 2026)")
    void testResolveMonthlyPeriod() {
        CompliancePeriod period = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2026-09");

        assertThat(period).isNotNull();
        assertThat(period.getPeriodType()).isEqualTo(CompliancePeriodType.MONTH);
        assertThat(period.getPeriodKey()).isEqualTo("2026-09");
        assertThat(period.getStartDate()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 1));
        assertThat(period.getEndDate()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 30));
        assertThat(period.getFinancialYear()).isEqualTo("2026-27");
        assertThat(period.getAssessmentYear()).isEqualTo("2027-28");
        assertThat(period.getDisplayLabel()).isEqualTo("September 2026");
    }

    @Test
    @DisplayName("Period Engine: Resolve Monthly Period in Q4 (February 2027)")
    void testResolveMonthlyPeriodQ4() {
        CompliancePeriod period = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2027-02");

        assertThat(period).isNotNull();
        assertThat(period.getStartDate()).isEqualTo(LocalDate.of(2027, Month.FEBRUARY, 1));
        assertThat(period.getEndDate()).isEqualTo(LocalDate.of(2027, Month.FEBRUARY, 28));
        assertThat(period.getFinancialYear()).isEqualTo("2026-27");
        assertThat(period.getAssessmentYear()).isEqualTo("2027-28");
    }

    @Test
    @DisplayName("Period Engine: Resolve Quarterly Periods (Q1 to Q4 FY 2026-27)")
    void testResolveQuarterlyPeriods() {
        CompliancePeriod q1 = periodService.resolvePeriod(CompliancePeriodType.QUARTER, "2026-27-Q1");
        assertThat(q1.getStartDate()).isEqualTo(LocalDate.of(2026, Month.APRIL, 1));
        assertThat(q1.getEndDate()).isEqualTo(LocalDate.of(2026, Month.JUNE, 30));
        assertThat(q1.getFinancialYear()).isEqualTo("2026-27");
        assertThat(q1.getAssessmentYear()).isEqualTo("2027-28");

        CompliancePeriod q2 = periodService.resolvePeriod(CompliancePeriodType.QUARTER, "2026-27-Q2");
        assertThat(q2.getStartDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 1));
        assertThat(q2.getEndDate()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 30));

        CompliancePeriod q4 = periodService.resolvePeriod(CompliancePeriodType.QUARTER, "2026-27-Q4");
        assertThat(q4.getStartDate()).isEqualTo(LocalDate.of(2027, Month.JANUARY, 1));
        assertThat(q4.getEndDate()).isEqualTo(LocalDate.of(2027, Month.MARCH, 31));
        assertThat(q4.isQ4()).isTrue();
    }

    @Test
    @DisplayName("Period Engine: Resolve Financial Year and Assessment Year")
    void testResolveFinancialAndAssessmentYear() {
        CompliancePeriod fy = periodService.resolvePeriod(CompliancePeriodType.FINANCIAL_YEAR, "2026-27");
        assertThat(fy.getStartDate()).isEqualTo(LocalDate.of(2026, Month.APRIL, 1));
        assertThat(fy.getEndDate()).isEqualTo(LocalDate.of(2027, Month.MARCH, 31));
        assertThat(fy.getFinancialYear()).isEqualTo("2026-27");
        assertThat(fy.getAssessmentYear()).isEqualTo("2027-28");

        CompliancePeriod ay = periodService.resolvePeriod(CompliancePeriodType.ASSESSMENT_YEAR, "2027-28");
        assertThat(ay.getStartDate()).isEqualTo(LocalDate.of(2026, Month.APRIL, 1));
        assertThat(ay.getEndDate()).isEqualTo(LocalDate.of(2027, Month.MARCH, 31));
        assertThat(ay.getFinancialYear()).isEqualTo("2026-27");
        assertThat(ay.getAssessmentYear()).isEqualTo("2027-28");
    }

    // =========================================================================
    // 2. Due Date Calculation Engine Tests
    // =========================================================================

    @Test
    @DisplayName("Due Date: Strategy DAY_OF_FOLLOWING_MONTH (GSTR-1, GSTR-3B, TDS Challan 281)")
    void testDayOfFollowingMonthCalculation() {
        CompliancePeriod sep2026 = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2026-09");

        // GSTR-1: 11th of following month -> 2026-10-11
        ComplianceRuleEntity gstr1 = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("GST_GSTR1_MONTHLY").orElseThrow();
        DueDateCalculationResult res1 = dueDateService.calculateDueDate(gstr1, sep2026, LocalDate.now());
        assertThat(res1.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(res1.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 11));
        assertThat(res1.getDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 11));

        // GSTR-3B: 20th of following month -> 2026-10-20
        ComplianceRuleEntity gstr3b = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("GST_GSTR3B_MONTHLY").orElseThrow();
        DueDateCalculationResult res3b = dueDateService.calculateDueDate(gstr3b, sep2026, LocalDate.now());
        assertThat(res3b.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(res3b.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 20));

        // TDS Challan 281: 7th of following month -> 2026-10-07
        ComplianceRuleEntity tds281 = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("TDS_CHALLAN_281_MONTHLY").orElseThrow();
        DueDateCalculationResult resTds = dueDateService.calculateDueDate(tds281, sep2026, LocalDate.now());
        assertThat(resTds.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resTds.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 7));
    }

    @Test
    @DisplayName("Due Date: Strategy DAY_OF_FOLLOWING_QUARTER_END_MONTH (TDS 26Q, GSTR-1 QRMP)")
    void testDayOfFollowingQuarterCalculation() {
        CompliancePeriod q2 = periodService.resolvePeriod(CompliancePeriodType.QUARTER, "2026-27-Q2");

        // Form 26Q (ending Sep 30): 31st of month following quarter -> 2026-10-31
        ComplianceRuleEntity tds26q = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("TDS_26Q_QUARTERLY").orElseThrow();
        DueDateCalculationResult res26q = dueDateService.calculateDueDate(tds26q, q2, LocalDate.now());
        assertThat(res26q.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(res26q.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 31));

        // GSTR-1 QRMP Q1 (ending Jun 30): 13th of month following quarter -> 2026-07-13
        CompliancePeriod q1 = periodService.resolvePeriod(CompliancePeriodType.QUARTER, "2026-27-Q1");
        ComplianceRuleEntity gstr1Qrmp = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("GST_GSTR1_QUARTERLY_QRMP").orElseThrow();
        DueDateCalculationResult resQrmp = dueDateService.calculateDueDate(gstr1Qrmp, q1, LocalDate.now());
        assertThat(resQrmp.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resQrmp.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 13));
    }

    @Test
    @DisplayName("Due Date: Strategy FIXED_DATE_IN_YEAR for ITR Assessment Year")
    void testFixedDateInAssessmentYear() {
        CompliancePeriod ay2026_27 = periodService.resolvePeriod(CompliancePeriodType.ASSESSMENT_YEAR, "2026-27");

        // ITR Non-Audit: 31st July of AY -> 2026-07-31
        ComplianceRuleEntity itrNonAudit = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("ITR_NON_AUDIT_ANNUAL").orElseThrow();
        DueDateCalculationResult resNonAudit = dueDateService.calculateDueDate(itrNonAudit, ay2026_27, LocalDate.now());
        assertThat(resNonAudit.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resNonAudit.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 31));

        // ITR Audit: 31st October of AY -> 2026-10-31
        ComplianceRuleEntity itrAudit = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("ITR_AUDIT_ANNUAL").orElseThrow();
        DueDateCalculationResult resAudit = dueDateService.calculateDueDate(itrAudit, ay2026_27, LocalDate.now());
        assertThat(resAudit.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resAudit.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 31));

        // Tax Audit 44AB: 30th September of AY -> 2026-09-30
        ComplianceRuleEntity taxAudit = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("ITR_TAX_AUDIT_44AB").orElseThrow();
        DueDateCalculationResult resTaxAudit = dueDateService.calculateDueDate(taxAudit, ay2026_27, LocalDate.now());
        assertThat(resTaxAudit.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resTaxAudit.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 30));
    }

    @Test
    @DisplayName("Due Date: Strategy FIXED_DATE_IN_YEAR for Annual Financial Year Filings (GSTR-9, AOC-4)")
    void testFixedDateInFinancialYear() {
        CompliancePeriod fy2025_26 = periodService.resolvePeriod(CompliancePeriodType.FINANCIAL_YEAR, "2025-26");

        // GSTR-9: 31st December following FY -> 2026-12-31
        ComplianceRuleEntity gstr9 = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("GST_GSTR9_ANNUAL").orElseThrow();
        DueDateCalculationResult resGstr9 = dueDateService.calculateDueDate(gstr9, fy2025_26, LocalDate.now());
        assertThat(resGstr9.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resGstr9.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.DECEMBER, 31));

        // MCA AOC-4: 29th October following FY -> 2026-10-29
        ComplianceRuleEntity aoc4 = ruleRepository.findByRuleCodeAndOrganizationIdIsNull("MCA_AOC4_ANNUAL").orElseThrow();
        DueDateCalculationResult resAoc4 = dueDateService.calculateDueDate(aoc4, fy2025_26, LocalDate.now());
        assertThat(resAoc4.getStatus()).isEqualTo(DueDateCalculationStatus.CALCULATED);
        assertThat(resAoc4.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 29));
    }

    @Test
    @DisplayName("Due Date: Month-end Clamping & Leap Year Handling (Safe Date Construction)")
    void testMonthEndClampingAndLeapYear() {
        // Test clamping day 31 to 30 for November
        ComplianceRuleEntity customNovRule = ComplianceRuleEntity.builder()
                .ruleCode("CUSTOM_NOV_CLAMP")
                .ruleName("Custom November Rule")
                .domain(ComplianceRuleDomain.OTHER)
                .frequency(ComplianceRuleFrequency.MONTHLY)
                .periodType(CompliancePeriodType.MONTH)
                .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                .dueDayOffset(31)
                .dueMonthOffset(1)
                .build();

        CompliancePeriod oct2026 = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2026-10");
        DueDateCalculationResult resNov = dueDateService.calculateDueDate(customNovRule, oct2026, LocalDate.now());
        // October + 1 month = November, which has 30 days. Clamped to Nov 30.
        assertThat(resNov.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.NOVEMBER, 30));

        // Test clamping day 31 to 29 for February in leap year (2028)
        CompliancePeriod jan2028 = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2028-01");
        DueDateCalculationResult resFebLeap = dueDateService.calculateDueDate(customNovRule, jan2028, LocalDate.now());
        assertThat(resFebLeap.getStatutoryDueDate()).isEqualTo(LocalDate.of(2028, Month.FEBRUARY, 29));

        // Test clamping day 31 to 28 for February in non-leap year (2027)
        CompliancePeriod jan2027 = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2027-01");
        DueDateCalculationResult resFebNonLeap = dueDateService.calculateDueDate(customNovRule, jan2027, LocalDate.now());
        assertThat(resFebNonLeap.getStatutoryDueDate()).isEqualTo(LocalDate.of(2027, Month.FEBRUARY, 28));
    }

    @Test
    @DisplayName("Due Date: Validity Window Enforcement (OUT_OF_EFFECTIVE_RANGE)")
    void testValidityWindowEnforcement() {
        ComplianceRuleEntity expiredRule = ComplianceRuleEntity.builder()
                .ruleCode("CUSTOM_EXPIRED_RULE")
                .ruleName("Expired Compliance Rule")
                .domain(ComplianceRuleDomain.GST)
                .frequency(ComplianceRuleFrequency.MONTHLY)
                .periodType(CompliancePeriodType.MONTH)
                .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                .dueDayOffset(20)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .build();

        CompliancePeriod sep2026 = periodService.resolvePeriod(CompliancePeriodType.MONTH, "2026-09");
        DueDateCalculationResult res = dueDateService.calculateDueDate(expiredRule, sep2026, LocalDate.now());

        assertThat(res.getStatus()).isEqualTo(DueDateCalculationStatus.OUT_OF_EFFECTIVE_RANGE);
        assertThat(res.getStatutoryDueDate()).isNull();
    }

    // =========================================================================
    // 3. Obligation Engine + Due Date Integration Tests
    // =========================================================================

    @Test
    @DisplayName("Obligation Generation: Authoritative Statutory Due Dates Auto-Populated")
    void testObligationGenerationAutoPopulatesDueDates() {
        TenantContext.setTenantId(orgA.getId());

        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .build();

        var response = obligationService.generateObligations(clientA.getId(), req);
        assertThat(response.getObligations()).isNotEmpty();

        // Verify GSTR-3B obligation
        var gstr3bOpt = response.getObligations().stream()
                .filter(o -> "GST_GSTR3B_MONTHLY".equalsIgnoreCase(o.getRuleCode()))
                .findFirst();
        assertThat(gstr3bOpt).isPresent();
        var gstr3b = gstr3bOpt.get();
        assertThat(gstr3b.getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 20));
        assertThat(gstr3b.getDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 20));
        assertThat(gstr3b.getDueDateCalculationStatus()).isEqualTo("CALCULATED");
        assertThat(gstr3b.getPeriodLabel()).isEqualTo("September 2026");
        assertThat(gstr3b.getFinancialYear()).isEqualTo("2026-27");
        assertThat(gstr3b.getAssessmentYear()).isEqualTo("2027-28");

        // Verify GSTR-1 obligation
        var gstr1Opt = response.getObligations().stream()
                .filter(o -> "GST_GSTR1_MONTHLY".equalsIgnoreCase(o.getRuleCode()))
                .findFirst();
        assertThat(gstr1Opt).isPresent();
        assertThat(gstr1Opt.get().getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 11));

        // Verify TDS Challan 281 obligation
        var tdsOpt = response.getObligations().stream()
                .filter(o -> "TDS_CHALLAN_281_MONTHLY".equalsIgnoreCase(o.getRuleCode()))
                .findFirst();
        assertThat(tdsOpt).isPresent();
        assertThat(tdsOpt.get().getStatutoryDueDate()).isEqualTo(LocalDate.of(2026, Month.OCTOBER, 7));

        // Verify zero side-effects on tasks
        assertThat(taskRepository.count()).isEqualTo(0);
    }

    // =========================================================================
    // 4. REST Controller & Security Tests
    // =========================================================================

    @Test
    @DisplayName("REST API: GET obligation due-date details and explanation")
    void testGetObligationDueDateApi() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        var obligationDto = obligationService.createOrGetSingleObligation(
                clientA.getId(), "GST_GSTR3B_MONTHLY", CompliancePeriodType.MONTH, "2026-09");

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/obligations/" + obligationDto.getId() + "/due-date")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ruleCode").value("GST_GSTR3B_MONTHLY"))
                .andExpect(jsonPath("$.data.statutoryDueDate").value("2026-10-20"))
                .andExpect(jsonPath("$.data.calculationStatus").value("CALCULATED"))
                .andExpect(jsonPath("$.data.strategy").value("DAY_OF_FOLLOWING_MONTH"));
    }

    @Test
    @DisplayName("REST API: POST recalculate obligation due-date")
    void testRecalculateObligationDueDateApi() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        var obligationDto = obligationService.createOrGetSingleObligation(
                clientA.getId(), "GST_GSTR3B_MONTHLY", CompliancePeriodType.MONTH, "2026-09");

        mockMvc.perform(post("/api/v1/clients/" + clientA.getId() + "/compliance/obligations/" + obligationDto.getId() + "/due-date/recalculate")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statutoryDueDate").value("2026-10-20"))
                .andExpect(jsonPath("$.data.dueDateCalculationStatus").value("CALCULATED"));
    }

    @Test
    @DisplayName("REST API: Preview due date calculation without persistence")
    void testPreviewDueDateApi() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/due-dates/preview")
                        .header("Authorization", tokenA)
                        .param("ruleCode", "ITR_NON_AUDIT_ANNUAL")
                        .param("periodType", "ASSESSMENT_YEAR")
                        .param("periodKey", "2026-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statutoryDueDate").value("2026-07-31"))
                .andExpect(jsonPath("$.data.status").value("CALCULATED"))
                .andExpect(jsonPath("$.data.strategy").value("FIXED_DATE_IN_YEAR"));
    }

    @Test
    @DisplayName("Security & Tenant Isolation: Cross-tenant due-date access returns 404")
    void testCrossTenantDueDateAccessForbidden() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        var obligationDto = obligationService.createOrGetSingleObligation(
                clientA.getId(), "GST_GSTR3B_MONTHLY", CompliancePeriodType.MONTH, "2026-09");

        // Attempt to access primary org obligation using secondary org token
        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/obligations/" + obligationDto.getId() + "/due-date")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }
}
