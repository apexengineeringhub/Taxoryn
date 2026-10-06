package com.taxoryn.module.compliance.applicability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.TaxorynApplication;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.applicability.dto.ClientComplianceApplicabilityDto;
import com.taxoryn.module.compliance.applicability.dto.ComplianceApplicabilitySummaryDto;
import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceApplicabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceRuleCatalogRepository complianceRuleRepository;

    @Autowired
    private ComplianceProfileRepository complianceProfileRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminUserA;
    private UserEntity adminUserB;
    private String adminTokenA;
    private String adminTokenB;

    private ClientEntity clientRegularGstA;
    private ClientEntity clientQrmpGstA;
    private ClientEntity clientCompositionA;
    private ClientEntity clientCorporateAuditA;
    private ClientEntity clientIndividualNoGstA;
    private ClientEntity clientNoProfileA;
    private ClientEntity clientOrgB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        complianceProfileRepository.deleteAll();
        complianceObligationRepository.deleteAll();
        taskRepository.deleteAll();
        auditLogRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

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
                .name("Beacon Tax Services Pvt Ltd " + suffix)
                .legalName("Beacon Tax Services Pvt Ltd")
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
        adminUserA = UserEntity.builder()
                .email("admin_" + suffix + "@apexca.com")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        adminUserA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(adminUserA);

        TenantContext.setTenantId(orgB.getId());
        adminUserB = UserEntity.builder()
                .email("admin_" + suffix + "@beacontax.com")
                .firstName("Suresh")
                .lastName("Patel")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        adminUserB.setOrganizationId(orgB.getId());
        adminUserB = userRepository.save(adminUserB);

        // 5. Auth Tokens
        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE", "CLIENT_READ", "CLIENT_WRITE")
        );

        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE", "CLIENT_READ", "CLIENT_WRITE")
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

        // 7. Seed Standard Rules if not already present
        TenantContext.clear();
        seedStandardRulesIfEmpty();

        // 8. Create Clients & Compliance Profiles in Tenant A
        TenantContext.setTenantId(orgA.getId());

        // Client 1: Regular Monthly GST + Non-Audit ITR + TDS Deductor + PF/ESI
        clientRegularGstA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .clientCode("CL-APEX-001")
                .displayName("Acme Dynamics Pvt Ltd")
                .legalName("Acme Dynamics Private Limited")
                .pan("AABCA1234F")
                .gstin("27AABCA1234F1Z5")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        complianceProfileRepository.save(ComplianceProfileEntity.builder()
                .clientId(clientRegularGstA.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsDeductorCategory(ComplianceTdsDeductorCategory.COMPANY)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.COMPANY)
                .itrTaxAuditApplicable(false)
                .itrTransferPricingApplicable(false)
                .advanceTaxApplicable(true)
                .mcaFilingApplicable(true)
                .pfEsiApplicable(true)
                .build());

        // Client 2: QRMP Quarterly GST + Non-Audit ITR
        clientQrmpGstA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PARTNERSHIP)
                .clientCode("CL-APEX-002")
                .displayName("Shree Balaji Traders")
                .legalName("Shree Balaji Traders LLP")
                .pan("AABFS9876K")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        complianceProfileRepository.save(ComplianceProfileEntity.builder()
                .clientId(clientQrmpGstA.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsApplicable(false)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.PARTNERSHIP_LLP)
                .itrTaxAuditApplicable(false)
                .build());

        // Client 3: Composition Scheme GST
        clientCompositionA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PROPRIETORSHIP)
                .clientCode("CL-APEX-003")
                .displayName("Krishna Retail Store")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        complianceProfileRepository.save(ComplianceProfileEntity.builder()
                .clientId(clientCompositionA.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.COMPOSITION)
                .gstCompositionScheme(true)
                .gstFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsApplicable(false)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.INDIVIDUAL)
                .itrTaxAuditApplicable(false)
                .build());

        // Client 4: Corporate Tax Audit + Transfer Pricing Assessee
        clientCorporateAuditA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PUBLIC_LIMITED)
                .clientCode("CL-APEX-004")
                .displayName("Global Tech Industries Ltd")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        complianceProfileRepository.save(ComplianceProfileEntity.builder()
                .clientId(clientCorporateAuditA.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.COMPANY)
                .itrTaxAuditApplicable(true)
                .itrTransferPricingApplicable(true)
                .advanceTaxApplicable(true)
                .mcaFilingApplicable(true)
                .build());

        // Client 5: Individual No GST / No TDS (Salaried / Simple ITR)
        clientIndividualNoGstA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .clientCode("CL-APEX-005")
                .displayName("Amitabh Varma")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        complianceProfileRepository.save(ComplianceProfileEntity.builder()
                .clientId(clientIndividualNoGstA.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(false)
                .tdsApplicable(false)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.INDIVIDUAL)
                .itrTaxAuditApplicable(false)
                .mcaFilingApplicable(false)
                .build());

        // Client 6: Unconfigured Client (No compliance profile created)
        clientNoProfileA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .clientCode("CL-APEX-006")
                .displayName("New Unconfigured Client")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        // 9. Setup Client in Tenant B
        TenantContext.setTenantId(orgB.getId());
        clientOrgB = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.COMPANY)
                .clientCode("CL-BEACON-001")
                .displayName("Beacon Client One")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        complianceRuleRepository.findAll().stream()
                .filter(r -> r.getOrganizationId() != null)
                .forEach(complianceRuleRepository::delete);
        complianceProfileRepository.deleteAll();
        complianceObligationRepository.deleteAll();
        taskRepository.deleteAll();
        auditLogRepository.deleteAll();
        clientRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private void seedStandardRulesIfEmpty() {
        if (complianceRuleRepository.findAllSystemRules().isEmpty()) {
            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("GST_GSTR1_MONTHLY")
                    .ruleName("GSTR-1 Monthly Return")
                    .domain(ComplianceRuleDomain.GST)
                    .frequency(ComplianceRuleFrequency.MONTHLY)
                    .periodType(CompliancePeriodType.MONTH)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("CGST Act, 2017")
                    .statutorySection("Section 37(1)")
                    .statutoryFormCode("GSTR-1")
                    .applicableGstRegistrationTypes("REGULAR")
                    .applicableFilingFrequencies("MONTHLY")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(11)
                    .dueMonthOffset(1)
                    .dueDateDescription("11th of following month")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("GST_GSTR3B_MONTHLY")
                    .ruleName("GSTR-3B Monthly Return & Tax Payment")
                    .domain(ComplianceRuleDomain.GST)
                    .frequency(ComplianceRuleFrequency.MONTHLY)
                    .periodType(CompliancePeriodType.MONTH)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("CGST Act, 2017")
                    .statutorySection("Section 39(1)")
                    .statutoryFormCode("GSTR-3B")
                    .applicableGstRegistrationTypes("REGULAR")
                    .applicableFilingFrequencies("MONTHLY")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(20)
                    .dueMonthOffset(1)
                    .dueDateDescription("20th of following month")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("GST_GSTR1_QUARTERLY_QRMP")
                    .ruleName("GSTR-1 Quarterly QRMP Return")
                    .domain(ComplianceRuleDomain.GST)
                    .frequency(ComplianceRuleFrequency.QUARTERLY)
                    .periodType(CompliancePeriodType.QUARTER)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("CGST Act, 2017")
                    .statutorySection("Section 37(1)")
                    .applicableGstRegistrationTypes("REGULAR")
                    .applicableFilingFrequencies("QUARTERLY")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH)
                    .dueDayOffset(13)
                    .dueMonthOffset(1)
                    .dueDateDescription("13th of month following quarter")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("GST_CMP08_QUARTERLY")
                    .ruleName("CMP-08 Composition Statement")
                    .domain(ComplianceRuleDomain.GST)
                    .frequency(ComplianceRuleFrequency.QUARTERLY)
                    .periodType(CompliancePeriodType.QUARTER)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("CGST Act, 2017")
                    .statutorySection("Section 10")
                    .applicableGstRegistrationTypes("COMPOSITION")
                    .applicableFilingFrequencies("QUARTERLY")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH)
                    .dueDayOffset(18)
                    .dueMonthOffset(1)
                    .dueDateDescription("18th of month following quarter")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("TDS_26Q_QUARTERLY")
                    .ruleName("Form 26Q TDS Return (Non-Salary)")
                    .domain(ComplianceRuleDomain.TDS)
                    .frequency(ComplianceRuleFrequency.QUARTERLY)
                    .periodType(CompliancePeriodType.QUARTER)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Income Tax Act, 1961")
                    .statutorySection("Section 200(3)")
                    .statutoryFormCode("FORM_26Q")
                    .requiresTdsDeductor(true)
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH)
                    .dueDayOffset(31)
                    .dueMonthOffset(1)
                    .dueDateDescription("31st of month following quarter end")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("ITR_NON_AUDIT_ANNUAL")
                    .ruleName("ITR Filing (Non-Audit Cases)")
                    .domain(ComplianceRuleDomain.INCOME_TAX)
                    .frequency(ComplianceRuleFrequency.ANNUAL)
                    .periodType(CompliancePeriodType.ASSESSMENT_YEAR)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Income Tax Act, 1961")
                    .statutorySection("Section 139(1)")
                    .requiresTaxAudit(false)
                    .requiresTransferPricing(false)
                    .dueDateRuleType(DueDateRuleType.FIXED_DATE_IN_YEAR)
                    .fixedMonth(7)
                    .fixedDay(31)
                    .dueDateDescription("31st July of Assessment Year")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("ITR_AUDIT_ANNUAL")
                    .ruleName("ITR Filing (Corporate & Tax Audit Cases)")
                    .domain(ComplianceRuleDomain.INCOME_TAX)
                    .frequency(ComplianceRuleFrequency.ANNUAL)
                    .periodType(CompliancePeriodType.ASSESSMENT_YEAR)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Income Tax Act, 1961")
                    .statutorySection("Section 139(1)")
                    .requiresTaxAudit(true)
                    .dueDateRuleType(DueDateRuleType.FIXED_DATE_IN_YEAR)
                    .fixedMonth(10)
                    .fixedDay(31)
                    .dueDateDescription("31st October of Assessment Year")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("ITR_TAX_AUDIT_44AB")
                    .ruleName("Tax Audit Report Filing Form 3CA/3CD")
                    .domain(ComplianceRuleDomain.INCOME_TAX)
                    .frequency(ComplianceRuleFrequency.ANNUAL)
                    .periodType(CompliancePeriodType.ASSESSMENT_YEAR)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Income Tax Act, 1961")
                    .statutorySection("Section 44AB")
                    .requiresTaxAudit(true)
                    .dueDateRuleType(DueDateRuleType.FIXED_DATE_IN_YEAR)
                    .fixedMonth(9)
                    .fixedDay(30)
                    .dueDateDescription("30th September of Assessment Year")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("MCA_AOC4_ANNUAL")
                    .ruleName("MCA Form AOC-4 Financial Statements")
                    .domain(ComplianceRuleDomain.MCA_ROC)
                    .frequency(ComplianceRuleFrequency.ANNUAL)
                    .periodType(CompliancePeriodType.FINANCIAL_YEAR)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Companies Act, 2013")
                    .statutorySection("Section 137")
                    .requiresMcaFiling(true)
                    .dueDateRuleType(DueDateRuleType.FIXED_DATE_IN_YEAR)
                    .fixedMonth(10)
                    .fixedDay(29)
                    .dueDateDescription("29th October")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("PAYROLL_PF_ECR_MONTHLY")
                    .ruleName("Monthly EPFO ECR Return")
                    .domain(ComplianceRuleDomain.PAYROLL_LABOUR)
                    .frequency(ComplianceRuleFrequency.MONTHLY)
                    .periodType(CompliancePeriodType.MONTH)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("EPF Act, 1952")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(15)
                    .dueMonthOffset(1)
                    .dueDateDescription("15th of following month")
                    .build());
        }
    }

    // =========================================================================
    // 1. GST Applicability Tests
    // =========================================================================

    @Test
    @DisplayName("1. GST Regular Monthly Client: GSTR-1 & GSTR-3B Monthly are APPLICABLE; QRMP & Composition are NOT_APPLICABLE")
    void testGstRegularMonthly_ApplicabilityEvaluation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/clients/" + clientRegularGstA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(clientRegularGstA.getId().toString()))
                .andExpect(jsonPath("$.data.summary.applicableCount").isNumber())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode dataNode = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        ClientComplianceApplicabilityDto report = objectMapper.treeToValue(dataNode, ClientComplianceApplicabilityDto.class);

        assertThat(report.getRules()).isNotEmpty();

        Map<String, EvaluatedRuleApplicabilityDto> ruleMap = report.getRules().stream()
                .collect(java.util.stream.Collectors.toMap(EvaluatedRuleApplicabilityDto::getRuleCode, r -> r));

        assertThat(ruleMap.get("GST_GSTR1_MONTHLY").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("GST_GSTR3B_MONTHLY").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("GST_GSTR1_QUARTERLY_QRMP").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
        assertThat(ruleMap.get("GST_CMP08_QUARTERLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("2. GST QRMP Quarterly Client: QRMP return is APPLICABLE; Monthly return is NOT_APPLICABLE")
    void testGstQrmpQuarterly_ApplicabilityEvaluation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/clients/" + clientQrmpGstA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode dataNode = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        ClientComplianceApplicabilityDto report = objectMapper.treeToValue(dataNode, ClientComplianceApplicabilityDto.class);

        Map<String, EvaluatedRuleApplicabilityDto> ruleMap = report.getRules().stream()
                .collect(java.util.stream.Collectors.toMap(EvaluatedRuleApplicabilityDto::getRuleCode, r -> r));

        assertThat(ruleMap.get("GST_GSTR1_QUARTERLY_QRMP").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("GST_GSTR1_MONTHLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
        assertThat(ruleMap.get("GST_CMP08_QUARTERLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("3. GST Composition Client: CMP-08 is APPLICABLE; Regular returns are NOT_APPLICABLE")
    void testGstComposition_ApplicabilityEvaluation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/clients/" + clientCompositionA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode dataNode = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        ClientComplianceApplicabilityDto report = objectMapper.treeToValue(dataNode, ClientComplianceApplicabilityDto.class);

        Map<String, EvaluatedRuleApplicabilityDto> ruleMap = report.getRules().stream()
                .collect(java.util.stream.Collectors.toMap(EvaluatedRuleApplicabilityDto::getRuleCode, r -> r));

        assertThat(ruleMap.get("GST_CMP08_QUARTERLY").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("GST_GSTR1_MONTHLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
        assertThat(ruleMap.get("GST_GSTR3B_MONTHLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
    }

    // =========================================================================
    // 2. TDS & ITR Applicability Tests
    // =========================================================================

    @Test
    @DisplayName("4. TDS & Corporate Audit Assessee: Form 26Q & Tax Audit 44AB are APPLICABLE; Non-Audit ITR is NOT_APPLICABLE")
    void testCorporateAuditClient_ApplicabilityEvaluation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/clients/" + clientCorporateAuditA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode dataNode = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        ClientComplianceApplicabilityDto report = objectMapper.treeToValue(dataNode, ClientComplianceApplicabilityDto.class);

        Map<String, EvaluatedRuleApplicabilityDto> ruleMap = report.getRules().stream()
                .collect(java.util.stream.Collectors.toMap(EvaluatedRuleApplicabilityDto::getRuleCode, r -> r));

        assertThat(ruleMap.get("TDS_26Q_QUARTERLY").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("ITR_AUDIT_ANNUAL").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("ITR_TAX_AUDIT_44AB").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("ITR_NON_AUDIT_ANNUAL").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
        assertThat(ruleMap.get("MCA_AOC4_ANNUAL").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
    }

    @Test
    @DisplayName("5. Individual No GST / No TDS: GST and TDS rules are NOT_APPLICABLE; Non-Audit ITR is APPLICABLE")
    void testIndividualNoGst_ApplicabilityEvaluation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/clients/" + clientIndividualNoGstA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode dataNode = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        ClientComplianceApplicabilityDto report = objectMapper.treeToValue(dataNode, ClientComplianceApplicabilityDto.class);

        Map<String, EvaluatedRuleApplicabilityDto> ruleMap = report.getRules().stream()
                .collect(java.util.stream.Collectors.toMap(EvaluatedRuleApplicabilityDto::getRuleCode, r -> r));

        assertThat(ruleMap.get("GST_GSTR1_MONTHLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
        assertThat(ruleMap.get("TDS_26Q_QUARTERLY").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
        assertThat(ruleMap.get("ITR_NON_AUDIT_ANNUAL").getResult()).isEqualTo(ApplicabilityResultState.APPLICABLE);
        assertThat(ruleMap.get("MCA_AOC4_ANNUAL").getResult()).isEqualTo(ApplicabilityResultState.NOT_APPLICABLE);
    }

    // =========================================================================
    // 3. Insufficient Data & Missing Profile
    // =========================================================================

    @Test
    @DisplayName("6. Unconfigured Client: Returns INSUFFICIENT_DATA with clear explanation")
    void testUnconfiguredClient_ReturnsInsufficientData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/clients/" + clientNoProfileA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.profileConfigured").value(false))
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode dataNode = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        ClientComplianceApplicabilityDto report = objectMapper.treeToValue(dataNode, ClientComplianceApplicabilityDto.class);

        assertThat(report.getSummary().getApplicableCount()).isEqualTo(0);
        assertThat(report.getSummary().getInsufficientDataCount()).isGreaterThanOrEqualTo(5);

        EvaluatedRuleApplicabilityDto ruleRes = report.getRules().get(0);
        assertThat(ruleRes.getResult()).isEqualTo(ApplicabilityResultState.INSUFFICIENT_DATA);
        assertThat(ruleRes.getReason()).containsIgnoringCase("profile");
    }

    // =========================================================================
    // 4. Single Rule & Summary Endpoints
    // =========================================================================

    @Test
    @DisplayName("7. Single Rule Evaluation Endpoint: /api/v1/clients/{clientId}/compliance/applicability/{ruleCode}")
    void testSingleRuleEvaluationEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientRegularGstA.getId() + "/compliance/applicability/GST_GSTR3B_MONTHLY")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleCode").value("GST_GSTR3B_MONTHLY"))
                .andExpect(jsonPath("$.data.result").value("APPLICABLE"))
                .andExpect(jsonPath("$.data.domain").value("GST"));
    }

    @Test
    @DisplayName("8. Compact Summary Endpoint: /api/v1/clients/{clientId}/compliance/applicability/summary")
    void testSummaryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientRegularGstA.getId() + "/compliance/applicability/summary")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(clientRegularGstA.getId().toString()))
                .andExpect(jsonPath("$.data.profileConfigured").value(true))
                .andExpect(jsonPath("$.data.applicableCount").isNumber());
    }

    // =========================================================================
    // 5. Tenant Isolation & Zero Side Effects
    // =========================================================================

    @Test
    @DisplayName("9. Tenant Isolation: Practice B cannot access Practice A's client applicability")
    void testTenantIsolation_CannotAccessOtherTenantClient() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientRegularGstA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenB)
                        .header("X-Tenant-Id", orgB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Zero Side Effects: Applicability evaluation creates NO obligations, tasks, or audit logs")
    void testZeroSideEffects() throws Exception {
        long obligationCountBefore = complianceObligationRepository.count();
        long taskCountBefore = taskRepository.count();
        long auditCountBefore = auditLogRepository.count();

        mockMvc.perform(get("/api/v1/clients/" + clientRegularGstA.getId() + "/compliance/applicability")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long obligationCountAfter = complianceObligationRepository.count();
        long taskCountAfter = taskRepository.count();
        long auditCountAfter = auditLogRepository.count();

        assertThat(obligationCountAfter).isEqualTo(obligationCountBefore);
        assertThat(taskCountAfter).isEqualTo(taskCountBefore);
        assertThat(auditCountAfter).isEqualTo(auditCountBefore);
    }
}
