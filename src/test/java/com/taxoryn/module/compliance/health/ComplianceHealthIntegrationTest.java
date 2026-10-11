package com.taxoryn.module.compliance.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.health.dto.ClientComplianceHealthDto;
import com.taxoryn.module.compliance.health.dto.PracticeComplianceHealthSummaryDto;
import com.taxoryn.module.compliance.health.model.ComplianceActionCode;
import com.taxoryn.module.compliance.health.model.ComplianceHealthStatus;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSeverity;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSignalCode;
import com.taxoryn.module.compliance.health.service.ComplianceHealthService;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.profile.entity.ComplianceProfileEntity;
import com.taxoryn.module.compliance.profile.repository.ComplianceProfileRepository;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused Integration Test Suite for Phase 29.8: Compliance Health & Intelligence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 29.8 — Compliance Health & Intelligence Integration Tests")
public class ComplianceHealthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ComplianceObligationRepository obligationRepository;
    @Autowired private ComplianceRuleCatalogRepository ruleRepository;
    @Autowired private ComplianceProfileRepository profileRepository;
    @Autowired private OrganizationModuleRepository organizationModuleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ComplianceHealthService healthService;

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

        // Setup Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Health TestOrg A " + suffix)
                .email("admin_" + suffix + "@healtha.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());
        TenantContext.clear();

        RoleEntity roleA = roleRepository.save(RoleEntity.builder()
                .code("ROLE_HEALTH_ADMIN_" + suffix)
                .name("Health Admin")
                .permissions(new HashSet<>())
                .build());

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin_user_" + suffix + "@healtha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Anil")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(roleA)))
                .build());

        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE")
        );

        TenantContext.setTenantId(orgA.getId());
        clientA = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.COMPANY)
                .displayName("HealthCorp Solutions " + suffix)
                .legalName("HealthCorp Solutions Private Limited")
                .pan("AAACH" + suffix.substring(0, 4).toUpperCase() + "A")
                .status(ClientStatus.ACTIVE)
                .build());
        TenantContext.clear();

        // Setup Tenant B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Health TestOrg B " + suffix)
                .email("admin_" + suffix + "@healthb.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());
        TenantContext.clear();

        RoleEntity roleB = roleRepository.save(RoleEntity.builder()
                .code("ROLE_HEALTH_B_" + suffix)
                .name("Health Admin B")
                .permissions(new HashSet<>())
                .build());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin_user_" + suffix + "@healthb.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Sunil")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(roleB)))
                .build());

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "COMPLIANCE_VIEW")
        );

        TenantContext.setTenantId(orgB.getId());
        clientB = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Client B OrgB " + suffix)
                .legalName("Client B OrgB Individual")
                .pan("BBBCB" + suffix.substring(0, 4).toUpperCase() + "B")
                .status(ClientStatus.ACTIVE)
                .build());
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        TenantContext.clear();
        obligationRepository.deleteAll();
        profileRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-01: Empty client returns deterministic NO_DATA status without errors")
    void testEmptyClientReturnsNoData() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.healthStatus").value("NO_DATA"))
                .andExpect(jsonPath("$.data.healthScore").value(100))
                .andExpect(jsonPath("$.data.totalObligations").value(0))
                .andExpect(jsonPath("$.data.openObligations").value(0))
                .andExpect(jsonPath("$.data.overdueCount").value(0));
    }

    @Test
    @DisplayName("TC-02: Overdue obligation triggers OVERDUE_OBLIGATION risk signal and reduces health score")
    void testOverdueObligationTriggersRiskSignal() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        TenantContext.setTenantId(orgA.getId());
        ComplianceObligationEntity overdueObl = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .ruleCode("GST_GSTR3B_MONTHLY")
                .ruleNameSnapshot("GSTR-3B Monthly Return")
                .domain(ComplianceRuleDomain.GST)
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-08")
                .statutoryDueDate(LocalDate.of(2026, 9, 20))
                .status(ComplianceObligationStatus.UPCOMING)
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthScore").value(65)) // 100 - 25 (overdue) - 10 (work missing)
                .andExpect(jsonPath("$.data.healthStatus").value("AT_RISK"))
                .andExpect(jsonPath("$.data.overdueCount").value(1))
                .andExpect(jsonPath("$.data.workNotGeneratedCount").value(1))
                .andExpect(jsonPath("$.data.risks[0].signalCode").value("OVERDUE_OBLIGATION"))
                .andExpect(jsonPath("$.data.risks[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.recommendations[0].actionCode").value("GENERATE_WORK"));
    }

    @Test
    @DisplayName("TC-03: Due today obligation triggers DUE_TODAY signal with HIGH severity")
    void testDueTodayObligationTriggersSignal() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        TenantContext.setTenantId(orgA.getId());
        obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .ruleCode("GST_GSTR1_MONTHLY")
                .ruleNameSnapshot("GSTR-1 Outward Supplies")
                .domain(ComplianceRuleDomain.GST)
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .statutoryDueDate(LocalDate.of(2026, 10, 10))
                .workInstanceId(UUID.randomUUID()) // Work already generated
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dueTodayCount").value(1))
                .andExpect(jsonPath("$.data.overdueCount").value(0))
                .andExpect(jsonPath("$.data.healthScore").value(90)) // 100 - 10 (due today)
                .andExpect(jsonPath("$.data.healthStatus").value("EXCELLENT"))
                .andExpect(jsonPath("$.data.risks[0].signalCode").value("DUE_TODAY"));
    }

    @Test
    @DisplayName("TC-04: Completed and cancelled obligations are excluded from active health penalties")
    void testCompletedAndCancelledObligationsExcluded() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        TenantContext.setTenantId(orgA.getId());
        // Completed past obligation
        obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .ruleCode("GST_GSTR3B_MONTHLY")
                .ruleNameSnapshot("GSTR-3B Monthly Return")
                .domain(ComplianceRuleDomain.GST)
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-07")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .status(ComplianceObligationStatus.COMPLETED)
                .build());

        // Cancelled past obligation
        obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .ruleCode("TDS_26Q_QUARTERLY")
                .ruleNameSnapshot("TDS 26Q Non Salary")
                .domain(ComplianceRuleDomain.TDS)
                .periodType(CompliancePeriodType.QUARTER)
                .periodKey("2026-27-Q1")
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .status(ComplianceObligationStatus.CANCELLED)
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalObligations").value(2))
                .andExpect(jsonPath("$.data.openObligations").value(0))
                .andExpect(jsonPath("$.data.overdueCount").value(0))
                .andExpect(jsonPath("$.data.healthScore").value(100))
                .andExpect(jsonPath("$.data.healthStatus").value("EXCELLENT"));
    }

    @Test
    @DisplayName("TC-05: Missing statutory due date generates MISSING_DUE_DATE signal and recommendation")
    void testMissingDueDateTriggersSignal() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        TenantContext.setTenantId(orgA.getId());
        obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .ruleCode("CUSTOM_COMPLIANCE_RULE")
                .ruleNameSnapshot("Custom Rule Without Due Date")
                .domain(ComplianceRuleDomain.OTHER)
                .periodType(CompliancePeriodType.MONTH)
                .periodKey("2026-09")
                .statutoryDueDate(null)
                .dueDate(null)
                .workInstanceId(UUID.randomUUID())
                .status(ComplianceObligationStatus.UPCOMING)
                .build());
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missingDueDateCount").value(1))
                .andExpect(jsonPath("$.data.risks[0].signalCode").value("MISSING_DUE_DATE"))
                .andExpect(jsonPath("$.data.recommendations[0].actionCode").value("CALCULATE_DUE_DATE"));
    }

    @Test
    @DisplayName("TC-06: Tenant isolation strictly enforced: Org-B user cannot view Org-A client health")
    void testTenantIsolationEnforced() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenB) // Org-B token querying Org-A client
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-07: Practice-wide compliance health aggregates metrics across all tenant clients")
    void testPracticeComplianceHealth() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        TenantContext.setTenantId(orgA.getId());
        // Obligation for Client A in Org A
        obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .ruleCode("GST_GSTR3B_MONTHLY")
                .domain(ComplianceRuleDomain.GST)
                .periodKey("2026-08")
                .statutoryDueDate(LocalDate.of(2026, 9, 20))
                .status(ComplianceObligationStatus.UPCOMING)
                .build());
        TenantContext.clear();

        TenantContext.setTenantId(orgB.getId());
        // Obligation for Client B in Org B
        obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientB.getId())
                .ruleCode("TDS_26Q_QUARTERLY")
                .domain(ComplianceRuleDomain.TDS)
                .periodKey("2026-27-Q2")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceObligationStatus.UPCOMING)
                .build());
        TenantContext.clear();

        // Org A practice health
        mockMvc.perform(get("/api/v1/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(1))
                .andExpect(jsonPath("$.data.totalOverdueObligations").value(1))
                .andExpect(jsonPath("$.data.criticalClientsCount").value(0))
                .andExpect(jsonPath("$.data.atRiskClientsCount").value(1));

        // Org B practice health (zero overdue)
        mockMvc.perform(get("/api/v1/compliance/health")
                        .header("Authorization", tokenB)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(1))
                .andExpect(jsonPath("$.data.totalOverdueObligations").value(0));
    }

    @Test
    @DisplayName("TC-08: Health evaluation is strictly read-only and causes zero write mutations")
    void testReadOnlyEvaluationLeavesDatabaseUnchanged() throws Exception {
        LocalDate refDate = LocalDate.of(2026, 10, 10);

        long countBefore = obligationRepository.count();

        mockMvc.perform(get("/api/v1/clients/" + clientA.getId() + "/compliance/health")
                        .header("Authorization", tokenA)
                        .param("referenceDate", refDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long countAfter = obligationRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
