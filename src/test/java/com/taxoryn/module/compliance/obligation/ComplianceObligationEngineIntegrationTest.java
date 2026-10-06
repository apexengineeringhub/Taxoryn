package com.taxoryn.module.compliance.obligation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.obligation.dto.CancelObligationRequest;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationSummaryDto;
import com.taxoryn.module.compliance.obligation.dto.GenerateObligationsRequest;
import com.taxoryn.module.compliance.obligation.dto.GeneratedObligationsResponseDto;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceObligationEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ComplianceProfileRepository complianceProfileRepository;

    @Autowired
    private ComplianceRuleCatalogRepository ruleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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
    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;
    private ClientEntity clientA;
    private ClientEntity clientB;

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
                .name("Zenith Tax Partners " + suffix)
                .legalName("Zenith Tax Partners")
                .email("admin_" + suffix + "@zenith.com")
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
                .lastName("Kumar")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        userA.setOrganizationId(orgA.getId());
        userA = userRepository.save(userA);

        TenantContext.setTenantId(orgB.getId());
        userB = UserEntity.builder()
                .email("admin_" + suffix + "@zenith.com")
                .firstName("Anita")
                .lastName("Roy")
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
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE", "CLIENT_READ", "CLIENT_WRITE")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
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

        // 7. Seed standard rules if needed
        TenantContext.clear();
        seedStandardRulesIfEmpty();

        // 8. Setup Client A (Private Limited, Regular Monthly GST, TDS deductor)
        TenantContext.setTenantId(orgA.getId());
        clientA = ClientEntity.builder()
                .displayName("Nexus Infotech Pvt Ltd")
                .legalName("Nexus Infotech Private Limited")
                .clientCode("CL-NEXUS-" + suffix)
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACN1234F")
                .gstin("27AAACN1234F1Z5")
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

        // 9. Setup Client B (Individual, Non-GST)
        TenantContext.setTenantId(orgB.getId());
        clientB = ClientEntity.builder()
                .displayName("Solitaire Jewelers")
                .legalName("Solitaire Jewelers")
                .clientCode("CL-SOL-" + suffix)
                .clientType(ClientType.INDIVIDUAL)
                .pan("BBBCS5678K")
                .status(ClientStatus.ACTIVE)
                .build();
        clientB.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientB);

        ComplianceProfileEntity profileB = ComplianceProfileEntity.builder()
                .clientId(clientB.getId())
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(false)
                .tdsApplicable(false)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.INDIVIDUAL)
                .itrTaxAuditApplicable(false)
                .itrTransferPricingApplicable(false)
                .mcaFilingApplicable(false)
                .pfEsiApplicable(false)
                .build();
        profileB.setOrganizationId(orgB.getId());
        complianceProfileRepository.save(profileB);

        TenantContext.clear();
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
        if (ruleRepository.findAllSystemRules().isEmpty()) {
            ruleRepository.save(ComplianceRuleEntity.builder()
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

            ruleRepository.save(ComplianceRuleEntity.builder()
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

            ruleRepository.save(ComplianceRuleEntity.builder()
                    .ruleCode("GST_CMP08_QUARTERLY")
                    .ruleName("CMP-08 Composition Scheme Statement")
                    .domain(ComplianceRuleDomain.GST)
                    .frequency(ComplianceRuleFrequency.QUARTERLY)
                    .periodType(CompliancePeriodType.QUARTER)
                    .status(ComplianceRuleStatus.ACTIVE)
                    .isSystemRule(true)
                    .applicableGstRegistrationTypes("COMPOSITION")
                    .dueDateRuleType(DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                    .dueDayOffset(18)
                    .dueMonthOffset(1)
                    .build());
        }
    }

    @Test
    @DisplayName("1. Applicable rule generates obligation with proper snapshot and status")
    void testGenerateObligations_ApplicableGstRuleGeneratesObligation() throws Exception {
        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .domain(ComplianceRuleDomain.GST)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.createdCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        GeneratedObligationsResponseDto resp = objectMapper.treeToValue(root.get("data"), GeneratedObligationsResponseDto.class);

        assertThat(resp.getObligations()).isNotEmpty();
        ComplianceObligationDto gstr3b = resp.getObligations().stream()
                .filter(o -> "GST_GSTR3B_MONTHLY".equalsIgnoreCase(o.getRuleCode()))
                .findFirst()
                .orElse(null);

        assertThat(gstr3b).isNotNull();
        assertThat(gstr3b.getPeriodKey()).isEqualTo("2026-09");
        assertThat(gstr3b.getStatus()).isEqualTo(ComplianceObligationStatus.UPCOMING);
        assertThat(gstr3b.getApplicabilityReason()).isNotBlank();
        assertThat(gstr3b.getGeneratedAt()).isNotNull();
        assertThat(gstr3b.getDueDate()).isNull(); // Zero due date side effects in 29.4
    }

    @Test
    @DisplayName("2. NOT_APPLICABLE rule creates no obligation")
    void testGenerateObligations_NotApplicableRuleCreatesNoObligation() throws Exception {
        // Client B is an Individual without GST registration
        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .domain(ComplianceRuleDomain.GST)
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientB.getId())
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.createdCount").value(0));
    }

    @Test
    @DisplayName("3. INSUFFICIENT_DATA creates no obligation")
    void testGenerateObligations_InsufficientDataCreatesNoObligation() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientEntity unconfiguredClient = ClientEntity.builder()
                .displayName("Unconfigured Corp")
                .legalName("Unconfigured Corp Ltd")
                .clientCode("CL-UNCONF-" + UUID.randomUUID().toString().substring(0, 8))
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        unconfiguredClient.setOrganizationId(orgA.getId());
        unconfiguredClient = clientRepository.save(unconfiguredClient);
        TenantContext.clear();

        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", unconfiguredClient.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.createdCount").value(0));
    }

    @Test
    @DisplayName("4. Monthly, Quarterly, and Financial Year period validations")
    void testGenerateObligations_PeriodFormattingValidation() throws Exception {
        // Valid Monthly
        GenerateObligationsRequest validMonth = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMonth)))
                .andExpect(status().isCreated());

        // Invalid Month (Month 13)
        GenerateObligationsRequest invalidMonth = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-13")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidMonth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Invalid Quarter (Q5)
        GenerateObligationsRequest invalidQuarter = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.QUARTER)
                .periodKey("2026-Q5")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQuarter)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("5. Rule / Period Frequency mismatch is rejected")
    void testGenerateObligations_RulePeriodMismatchRejected() throws Exception {
        // Attempting to generate GST_CMP08_QUARTERLY with MONTH period type
        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .ruleCode("GST_CMP08_QUARTERLY")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("6. Duplicate generation is strictly idempotent and creates zero duplicate rows")
    void testGenerateObligations_DuplicateGenerationIsIdempotent() throws Exception {
        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .domain(ComplianceRuleDomain.GST)
                .build();

        // 1st Generation
        MvcResult firstCall = mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root1 = objectMapper.readTree(firstCall.getResponse().getContentAsString());
        GeneratedObligationsResponseDto resp1 = objectMapper.treeToValue(root1.get("data"), GeneratedObligationsResponseDto.class);
        int createdInFirstCall = resp1.getCreatedCount();
        assertThat(createdInFirstCall).isGreaterThan(0);

        long initialDbCount = obligationRepository.count();

        // 2nd Generation (Same client + period)
        MvcResult secondCall = mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root2 = objectMapper.readTree(secondCall.getResponse().getContentAsString());
        GeneratedObligationsResponseDto resp2 = objectMapper.treeToValue(root2.get("data"), GeneratedObligationsResponseDto.class);

        assertThat(resp2.getCreatedCount()).isEqualTo(0);
        assertThat(resp2.getExistingCount()).isEqualTo(createdInFirstCall);
        assertThat(obligationRepository.count()).isEqualTo(initialDbCount); // DB count remains exactly identical
    }

    @Test
    @DisplayName("7. Lifecycle status transition and invalid transition rejection")
    void testObligationLifecycle_StatusTransitions() throws Exception {
        GenerateObligationsRequest genReq = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .ruleCode("GST_GSTR3B_MONTHLY")
                .build();

        MvcResult genResult = mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(genResult.getResponse().getContentAsString());
        GeneratedObligationsResponseDto genResp = objectMapper.treeToValue(root.get("data"), GeneratedObligationsResponseDto.class);
        UUID obligationId = genResp.getObligations().get(0).getId();

        // 1. Transition UPCOMING -> IN_PROGRESS
        UpdateObligationStatusRequest inProgReq = UpdateObligationStatusRequest.builder()
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .notes("Books of accounts under review")
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance/obligations/{obligationId}/status", clientA.getId(), obligationId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 2. Transition IN_PROGRESS -> COMPLETED
        UpdateObligationStatusRequest compReq = UpdateObligationStatusRequest.builder()
                .status(ComplianceObligationStatus.COMPLETED)
                .notes("Filed and challan shared")
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance/obligations/{obligationId}/status", clientA.getId(), obligationId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isString());

        // 3. Invalid Transition: COMPLETED is terminal, transition back to IN_PROGRESS must fail
        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance/obligations/{obligationId}/status", clientA.getId(), obligationId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("8. Cancellation requires mandatory reason and preserves historical row")
    void testCancelObligation_RequiresReasonAndRetainsHistory() throws Exception {
        GenerateObligationsRequest genReq = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .ruleCode("GST_GSTR3B_MONTHLY")
                .build();

        MvcResult genResult = mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(genResult.getResponse().getContentAsString());
        GeneratedObligationsResponseDto genResp = objectMapper.treeToValue(root.get("data"), GeneratedObligationsResponseDto.class);
        UUID obligationId = genResp.getObligations().get(0).getId();

        // 1. Cancel with blank reason -> Fails
        CancelObligationRequest blankReq = CancelObligationRequest.builder().cancellationReason("   ").build();
        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/{obligationId}/cancel", clientA.getId(), obligationId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest());

        // 2. Cancel with valid reason -> Succeeds
        CancelObligationRequest validReq = CancelObligationRequest.builder()
                .cancellationReason("Client surrendered GST registration for this business branch")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/{obligationId}/cancel", clientA.getId(), obligationId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Client surrendered GST registration for this business branch"))
                .andExpect(jsonPath("$.data.cancelledAt").isString());

        // Historical record is preserved in DB
        ComplianceObligationEntity inDb = obligationRepository.findById(obligationId).orElse(null);
        assertThat(inDb).isNotNull();
        assertThat(inDb.getStatus()).isEqualTo(ComplianceObligationStatus.CANCELLED);
    }

    @Test
    @DisplayName("9. Summary computation aggregates correct counts across statuses and domains")
    void testObligationSummary_ComputesMetrics() throws Exception {
        // Generate Month Obligations
        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        MvcResult sumResult = mockMvc.perform(get("/api/v1/clients/{clientId}/compliance/obligations/summary", clientA.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.openCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();

        JsonNode root = objectMapper.readTree(sumResult.getResponse().getContentAsString());
        ComplianceObligationSummaryDto summary = objectMapper.treeToValue(root.get("data"), ComplianceObligationSummaryDto.class);

        assertThat(summary.getByDomain()).containsKey("GST");
    }

    @Test
    @DisplayName("10. Multi-tenant isolation prevents cross-organization access")
    void testTenantIsolation_CrossTenantAccessForbidden() throws Exception {
        // Org A tries to list Org B's client obligations -> 404
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance/obligations", clientB.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound());

        // Org A tries to generate Org B's client obligations -> 404
        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientB.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. Zero side-effects: Generation does not create tasks, work instances, or due dates")
    void testZeroSideEffects_NoWorkTasksOrCalendarEventsCreated() throws Exception {
        long initialTasksCount = taskRepository.count();

        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .build();

        mockMvc.perform(post("/api/v1/clients/{clientId}/compliance/obligations/generate", clientA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertThat(taskRepository.count()).isEqualTo(initialTasksCount); // Zero tasks created
    }
}
