package com.taxoryn.module.compliance.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.compliance.rule.dto.ComplianceRuleCatalogSummaryDto;
import com.taxoryn.module.compliance.rule.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.rule.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.dto.UpdateComplianceRuleRequest;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taxoryn.TaxorynApplication;

@SpringBootTest(classes = TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceRuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceRuleCatalogRepository complianceRuleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

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

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        complianceRuleRepository.findAll().stream()
                .filter(r -> r.getOrganizationId() != null)
                .forEach(complianceRuleRepository::delete);
        organizationModuleRepository.deleteAll();
        userRepository.deleteAll();
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

        // 7. Ensure standard system rules are seeded if not present
        TenantContext.clear();
        complianceRuleRepository.findAll().stream()
                .filter(r -> r.getOrganizationId() != null)
                .forEach(complianceRuleRepository::delete);
        seedStandardRulesIfEmpty();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        complianceRuleRepository.findAll().stream()
                .filter(r -> r.getOrganizationId() != null)
                .forEach(complianceRuleRepository::delete);
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
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(11)
                    .dueMonthOffset(1)
                    .dueDateDescription("11th of following month")
                    .defaultWorkTemplateCode("TEMPLATE_GST_MONTHLY_FILING")
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
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(20)
                    .dueMonthOffset(1)
                    .dueDateDescription("20th of following month")
                    .defaultWorkTemplateCode("TEMPLATE_GST_MONTHLY_FILING")
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
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_QUARTER_END_MONTH)
                    .dueDayOffset(31)
                    .dueMonthOffset(1)
                    .dueDateDescription("31st of month following quarter end")
                    .defaultWorkTemplateCode("TEMPLATE_TDS_QUARTERLY_RETURN")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("ITR_TAX_AUDIT_44AB")
                    .ruleName("Tax Audit Report Filing")
                    .domain(ComplianceRuleDomain.STATUTORY_AUDIT)
                    .frequency(ComplianceRuleFrequency.ANNUAL)
                    .periodType(CompliancePeriodType.ASSESSMENT_YEAR)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Income Tax Act, 1961")
                    .statutorySection("Section 44AB")
                    .statutoryFormCode("FORM_3CA_3CD")
                    .dueDateRuleType(DueDateRuleType.FIXED_DATE_IN_YEAR)
                    .fixedMonth(9)
                    .fixedDay(30)
                    .dueDateDescription("30th September of Assessment Year")
                    .defaultWorkTemplateCode("TEMPLATE_TAX_AUDIT_3CA_3CD")
                    .build());

            complianceRuleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("MCA_AOC4_ANNUAL")
                    .ruleName("AOC-4 Financial Statements Filing")
                    .domain(ComplianceRuleDomain.MCA_ROC)
                    .frequency(ComplianceRuleFrequency.ANNUAL)
                    .periodType(CompliancePeriodType.FINANCIAL_YEAR)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .statutoryAct("Companies Act, 2013")
                    .statutorySection("Section 137")
                    .statutoryFormCode("AOC-4")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(30)
                    .dueMonthOffset(7)
                    .dueDateDescription("Within 30 days of AGM (typically 30th October)")
                    .defaultWorkTemplateCode("TEMPLATE_MCA_AOC4_FILING")
                    .build());
        }
    }

    @Test
    @DisplayName("1. Master Catalog: Should list standard system compliance rules")
    void testGetAllRules_ReturnsSeededSystemRules() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<ComplianceRuleDto> rules = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<ComplianceRuleDto>>() {}
        );

        assertThat(rules).isNotEmpty();
        assertThat(rules.size()).isGreaterThanOrEqualTo(5);

        // Verify key statutory rules exist
        assertThat(rules).anyMatch(r -> "GST_GSTR1_MONTHLY".equals(r.getRuleCode()) && r.isSystemRule());
        assertThat(rules).anyMatch(r -> "GST_GSTR3B_MONTHLY".equals(r.getRuleCode()) && r.isSystemRule());
        assertThat(rules).anyMatch(r -> "TDS_26Q_QUARTERLY".equals(r.getRuleCode()) && r.isSystemRule());
        assertThat(rules).anyMatch(r -> "ITR_TAX_AUDIT_44AB".equals(r.getRuleCode()) && r.isSystemRule());
        assertThat(rules).anyMatch(r -> "MCA_AOC4_ANNUAL".equals(r.getRuleCode()) && r.isSystemRule());
    }

    @Test
    @DisplayName("2. Domain & Frequency Filtering: Should filter rules by GST domain and MONTHLY frequency")
    void testGetRulesByDomainAndFrequency_FiltersCorrectly() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .param("domain", "GST")
                        .param("frequency", "MONTHLY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<ComplianceRuleDto> rules = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<ComplianceRuleDto>>() {}
        );

        assertThat(rules).isNotEmpty();
        assertThat(rules).allMatch(r -> r.getDomain() == ComplianceRuleDomain.GST);
        assertThat(rules).allMatch(r -> r.getFrequency() == ComplianceRuleFrequency.MONTHLY);
    }

    @Test
    @DisplayName("3. Get Rule by ID & by Code: Should retrieve detailed rule specification")
    void testGetRuleByIdAndCode_Success() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .param("search", "GSTR3B")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<ComplianceRuleDto> rules = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                new TypeReference<List<ComplianceRuleDto>>() {}
        );

        assertThat(rules).isNotEmpty();
        ComplianceRuleDto target = rules.getFirst();

        // Query by ID
        MvcResult idResult = mockMvc.perform(get("/api/v1/compliance/rules/" + target.getId())
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        ComplianceRuleDto byId = objectMapper.readValue(idResult.getResponse().getContentAsString(), ComplianceRuleDto.class);
        assertThat(byId.getRuleCode()).isEqualTo(target.getRuleCode());
        assertThat(byId.getStatutoryAct()).isEqualTo("CGST Act, 2017");
        assertThat(byId.getStatutorySection()).isEqualTo("Section 39(1)");

        // Query by Code
        MvcResult codeResult = mockMvc.perform(get("/api/v1/compliance/rules/by-code/" + target.getRuleCode())
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        ComplianceRuleDto byCode = objectMapper.readValue(codeResult.getResponse().getContentAsString(), ComplianceRuleDto.class);
        assertThat(byCode.getId()).isEqualTo(target.getId());
    }

    @Test
    @DisplayName("4. Custom Rule Creation: Should create a practice-specific compliance rule")
    void testCreateCustomRule_Success() throws Exception {
        CreateComplianceRuleRequest request = CreateComplianceRuleRequest.builder()
                .ruleCode("APEX_CLIENT_MONTHLY_MIS")
                .ruleName("Apex Monthly Client MIS Reporting")
                .domain(ComplianceRuleDomain.OTHER)
                .frequency(ComplianceRuleFrequency.MONTHLY)
                .periodType(CompliancePeriodType.MONTH)
                .description("Practice standard monthly accounting & MIS review requirement.")
                .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                .dueDayOffset(10)
                .dueMonthOffset(1)
                .dueDateDescription("10th of following month")
                .defaultWorkTemplateCode("TEMPLATE_MONTHLY_MIS")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ComplianceRuleDto created = objectMapper.readValue(createResult.getResponse().getContentAsString(), ComplianceRuleDto.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getRuleCode()).isEqualTo("APEX_CLIENT_MONTHLY_MIS");
        assertThat(created.isSystemRule()).isFalse();
        assertThat(created.getOrganizationId()).isEqualTo(orgA.getId());
        assertThat(created.getStatus()).isEqualTo(ComplianceRuleStatus.ACTIVE);
    }

    @Test
    @DisplayName("5. Tenant Isolation: Practice B cannot view or update Practice A's custom rule")
    void testTenantIsolation_CannotAccessOrModifyOtherTenantCustomRule() throws Exception {
        // Create custom rule for Tenant A
        CreateComplianceRuleRequest requestA = CreateComplianceRuleRequest.builder()
                .ruleCode("APEX_CONFIDENTIAL_AUDIT")
                .ruleName("Apex Internal Peer Review")
                .domain(ComplianceRuleDomain.STATUTORY_AUDIT)
                .frequency(ComplianceRuleFrequency.ANNUAL)
                .periodType(CompliancePeriodType.FINANCIAL_YEAR)
                .build();

        MvcResult resultA = mockMvc.perform(post("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isCreated())
                .andReturn();

        ComplianceRuleDto createdA = objectMapper.readValue(resultA.getResponse().getContentAsString(), ComplianceRuleDto.class);

        // Tenant B attempting to read Tenant A's rule by ID -> 404
        mockMvc.perform(get("/api/v1/compliance/rules/" + createdA.getId())
                        .header("Authorization", adminTokenB)
                        .header("X-Tenant-Id", orgB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Tenant B attempting to update Tenant A's rule -> 404
        UpdateComplianceRuleRequest updateRequest = UpdateComplianceRuleRequest.builder()
                .ruleName("Compromised Rule Name")
                .build();

        mockMvc.perform(put("/api/v1/compliance/rules/" + createdA.getId())
                        .header("Authorization", adminTokenB)
                        .header("X-Tenant-Id", orgB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Standard Rule Immutability: System rules cannot be updated or deleted by tenants")
    void testSystemRule_Immutable_CannotUpdateOrDelete() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/compliance/rules/by-code/GST_GSTR3B_MONTHLY")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        ComplianceRuleDto systemRule = objectMapper.readValue(result.getResponse().getContentAsString(), ComplianceRuleDto.class);
        assertThat(systemRule.isSystemRule()).isTrue();

        // Attempting to update a system rule -> 400 Bad Request
        UpdateComplianceRuleRequest updateRequest = UpdateComplianceRuleRequest.builder()
                .ruleName("Modified System Rule Name")
                .build();

        mockMvc.perform(put("/api/v1/compliance/rules/" + systemRule.getId())
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        // Attempting to delete a system rule -> 400 Bad Request
        mockMvc.perform(delete("/api/v1/compliance/rules/" + systemRule.getId())
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Duplicate Rule Code: Should prevent creating custom rule with duplicate code")
    void testDuplicateRuleCode_ThrowsConflict() throws Exception {
        CreateComplianceRuleRequest request = CreateComplianceRuleRequest.builder()
                .ruleCode("CUSTOM_DUPLICATE_CODE")
                .ruleName("First Registration")
                .domain(ComplianceRuleDomain.GST)
                .frequency(ComplianceRuleFrequency.MONTHLY)
                .periodType(CompliancePeriodType.MONTH)
                .build();

        mockMvc.perform(post("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate code within same tenant -> 409 Conflict
        mockMvc.perform(post("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Attempt to shadow system rule code -> 409 Conflict
        CreateComplianceRuleRequest shadowSystemRequest = CreateComplianceRuleRequest.builder()
                .ruleCode("GST_GSTR3B_MONTHLY")
                .ruleName("Shadowed GSTR-3B")
                .domain(ComplianceRuleDomain.GST)
                .frequency(ComplianceRuleFrequency.MONTHLY)
                .periodType(CompliancePeriodType.MONTH)
                .build();

        mockMvc.perform(post("/api/v1/compliance/rules")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shadowSystemRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("8. Catalog Summary: Returns accurate count metrics across domains")
    void testCatalogSummary_MetricsAccurate() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/compliance/rules/summary")
                        .header("Authorization", adminTokenA)
                        .header("X-Tenant-Id", orgA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        ComplianceRuleCatalogSummaryDto summary = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ComplianceRuleCatalogSummaryDto.class
        );

        assertThat(summary.getTotalRules()).isGreaterThanOrEqualTo(5);
        assertThat(summary.getActiveRules()).isGreaterThanOrEqualTo(5);
        assertThat(summary.getSystemRules()).isGreaterThanOrEqualTo(5);
        assertThat(summary.getRulesByDomain()).containsKey(ComplianceRuleDomain.GST);
        assertThat(summary.getRulesByDomain().get(ComplianceRuleDomain.GST)).isGreaterThanOrEqualTo(2L);
    }
}
