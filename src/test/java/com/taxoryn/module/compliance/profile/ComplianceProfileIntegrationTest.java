package com.taxoryn.module.compliance.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.profile.dto.UpdateComplianceProfileRequest;
import com.taxoryn.module.compliance.profile.entity.ComplianceProfileEntity;
import com.taxoryn.module.compliance.profile.model.ComplianceFilingFrequency;
import com.taxoryn.module.compliance.profile.model.ComplianceGstRegistrationType;
import com.taxoryn.module.compliance.profile.model.ComplianceItrCategory;
import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import com.taxoryn.module.compliance.profile.model.ComplianceTdsDeductorCategory;
import com.taxoryn.module.compliance.profile.repository.ComplianceProfileRepository;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private UserEntity staffUserA;
    private UserEntity adminUserB;
    private String adminTokenA;
    private String staffTokenA;
    private String adminTokenB;

    private ClientEntity clientA;
    private ClientEntity clientB;

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

        // 1. Setup Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory LLP")
                .legalName("Apex Chartered Accountants LLP")
                .email("admin@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup Tenant B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beacon Tax Services Pvt Ltd")
                .legalName("Beacon Tax Services Pvt Ltd")
                .email("admin@beacontax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTICE_STAFF")
                .name("Practice Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Users in Tenant A
        TenantContext.setTenantId(orgA.getId());
        adminUserA = UserEntity.builder()
                .email("admin@apexca.com")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        adminUserA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(adminUserA);

        staffUserA = UserEntity.builder()
                .email("staff@apexca.com")
                .firstName("Pooja")
                .lastName("Verma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build();
        staffUserA.setOrganizationId(orgA.getId());
        staffUserA = userRepository.save(staffUserA);

        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE", "CLIENT_READ", "CLIENT_WRITE")
        );

        staffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                staffUserA.getId(),
                orgA.getId(),
                staffUserA.getEmail(),
                Set.of("PRACTICE_STAFF"),
                Set.of("CLIENT_VIEW", "CLIENT_READ", "COMPLIANCE_VIEW", "COMPLIANCE_READ")
        );

        // Modules in Tenant A
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        clientA = ClientEntity.builder()
                .displayName("Kothari Infotech Pvt Ltd")
                .legalName("Kothari Infotech Private Limited")
                .clientCode("CL-KOT001")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACK1234A")
                .gstin("27AAACK1234A1Z5")
                .tan("PNEK12345D")
                .email("contact@kothari.com")
                .phone("9876543210")
                .city("Pune")
                .state("Maharashtra")
                .stateCode("27")
                .status(ClientStatus.ACTIVE)
                .build();
        clientA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clientA);

        // 5. Setup Tenant B
        TenantContext.setTenantId(orgB.getId());
        adminUserB = UserEntity.builder()
                .email("admin@beacontax.com")
                .firstName("Suresh")
                .lastName("Menon")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        adminUserB.setOrganizationId(orgB.getId());
        adminUserB = userRepository.save(adminUserB);

        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE")
        );

        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        clientB = ClientEntity.builder()
                .displayName("Global Traders LLC")
                .legalName("Global Traders LLC")
                .clientCode("CL-GLO001")
                .clientType(ClientType.COMPANY)
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

    @Test
    @DisplayName("1. GET Unconfigured Client Compliance Profile Returns Default Draft (0% readiness)")
    void testGetUnconfiguredComplianceProfile() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(clientA.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.clientSummary.displayName").value("Kothari Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.clientSummary.pan").value("AAACK1234A"))
                .andExpect(jsonPath("$.data.gstConfig.applicable").value(false))
                .andExpect(jsonPath("$.data.tdsConfig.applicable").value(false))
                .andExpect(jsonPath("$.data.itrConfig.applicable").value(false))
                .andExpect(jsonPath("$.data.completeness.configured").value(false))
                .andExpect(jsonPath("$.data.completeness.readinessScore").value(0))
                .andExpect(jsonPath("$.data.completeness.readinessSummary").value("UNCONFIGURED"));
    }

    @Test
    @DisplayName("2. PUT Compliance Profile Initializes Statutory Facts & 100% Readiness Score")
    void testInitializeComplianceProfile() throws Exception {
        UpdateComplianceProfileRequest request = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                // GST
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .gstCompositionScheme(false)
                .gstEinvoiceApplicable(true)
                .gstEwaybillApplicable(true)
                // TDS
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsDeductorCategory(ComplianceTdsDeductorCategory.COMPANY)
                .tdsLowerDeductionCertificate(false)
                // ITR
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.COMPANY)
                .itrTaxAuditApplicable(true)
                .itrTransferPricingApplicable(false)
                // Other
                .advanceTaxApplicable(true)
                .mcaFilingApplicable(true)
                .professionalTaxApplicable(true)
                .pfEsiApplicable(true)
                .notes("Standard corporate tax profile for Maharashtra registered entity.")
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(clientA.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.gstConfig.applicable").value(true))
                .andExpect(jsonPath("$.data.gstConfig.registrationType").value("REGULAR"))
                .andExpect(jsonPath("$.data.gstConfig.filingFrequency").value("MONTHLY"))
                .andExpect(jsonPath("$.data.gstConfig.einvoiceApplicable").value(true))
                .andExpect(jsonPath("$.data.tdsConfig.applicable").value(true))
                .andExpect(jsonPath("$.data.tdsConfig.filingFrequency").value("QUARTERLY"))
                .andExpect(jsonPath("$.data.tdsConfig.deductorCategory").value("COMPANY"))
                .andExpect(jsonPath("$.data.itrConfig.applicable").value(true))
                .andExpect(jsonPath("$.data.itrConfig.category").value("COMPANY"))
                .andExpect(jsonPath("$.data.itrConfig.taxAuditApplicable").value(true))
                .andExpect(jsonPath("$.data.otherComplianceConfig.advanceTaxApplicable").value(true))
                .andExpect(jsonPath("$.data.otherComplianceConfig.mcaFilingApplicable").value(true))
                .andExpect(jsonPath("$.data.completeness.configured").value(true))
                .andExpect(jsonPath("$.data.completeness.readinessScore").value(100))
                .andExpect(jsonPath("$.data.completeness.readinessSummary").value("READY"));

        // Verify Database entity created
        TenantContext.setTenantId(orgA.getId());
        ComplianceProfileEntity saved = complianceProfileRepository.findByOrganizationIdAndClientId(orgA.getId(), clientA.getId()).orElseThrow();
        assertThat(saved.isGstApplicable()).isTrue();
        assertThat(saved.getGstRegistrationType()).isEqualTo(ComplianceGstRegistrationType.REGULAR);
        assertThat(saved.getGstFilingFrequency()).isEqualTo(ComplianceFilingFrequency.MONTHLY);
        assertThat(saved.isTdsApplicable()).isTrue();
        assertThat(saved.getTdsDeductorCategory()).isEqualTo(ComplianceTdsDeductorCategory.COMPANY);
        assertThat(saved.isItrApplicable()).isTrue();
        assertThat(saved.getItrCategory()).isEqualTo(ComplianceItrCategory.COMPANY);
    }

    @Test
    @DisplayName("3. One Profile Per Client: Repeated PUT Updates Existing Row Idempotently")
    void testOneProfilePerClientEnforcement() throws Exception {
        UpdateComplianceProfileRequest req1 = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.COMPOSITION)
                .gstFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstConfig.registrationType").value("COMPOSITION"));

        UpdateComplianceProfileRequest req2 = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .notes("Switched from composition to regular scheme")
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstConfig.registrationType").value("REGULAR"))
                .andExpect(jsonPath("$.data.notes").value("Switched from composition to regular scheme"));

        // Only 1 row in compliance_profiles table for Tenant A
        TenantContext.setTenantId(orgA.getId());
        long count = complianceProfileRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("4. Tenant Isolation: Cross-Tenant Access Is Rejected (404/403)")
    void testTenantIsolation() throws Exception {
        // Tenant B attempts to read Client A's compliance profile
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenB))
                .andExpect(status().isNotFound());

        // Tenant B attempts to update Client A's compliance profile
        UpdateComplianceProfileRequest req = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. RBAC Enforcement: Staff Can Read But Cannot Update Profile")
    void testRbacPermissions() throws Exception {
        // Staff reads profile -> OK
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", staffTokenA))
                .andExpect(status().isOk());

        // Staff tries to update profile -> 403 FORBIDDEN
        UpdateComplianceProfileRequest req = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Audit Logging: Mutations Are Audited But GET Operations Are Not")
    void testAuditLoggingOnMutations() throws Exception {
        auditLogRepository.deleteAll();

        // 1. GET does NOT generate audit log
        mockMvc.perform(get("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk());

        TenantContext.setTenantId(orgA.getId());
        assertThat(auditLogRepository.count()).isEqualTo(0);

        // 2. PUT (Create) generates COMPLIANCE_PROFILE_CREATED
        UpdateComplianceProfileRequest createReq = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        TenantContext.setTenantId(orgA.getId());
        assertThat(auditLogRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll().get(0).getAction()).isEqualTo("COMPLIANCE_PROFILE_CREATED");

        // 3. PUT (Update) generates COMPLIANCE_PROFILE_UPDATED
        UpdateComplianceProfileRequest updateReq = UpdateComplianceProfileRequest.builder()
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsDeductorCategory(ComplianceTdsDeductorCategory.COMPANY)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        TenantContext.setTenantId(orgA.getId());
        assertThat(auditLogRepository.count()).isEqualTo(2);
        assertThat(auditLogRepository.findAll().get(1).getAction()).isEqualTo("COMPLIANCE_PROFILE_UPDATED");
    }

    @Test
    @DisplayName("7. Guardrail Verification: NO Side-Effect Obligations, Work Items or Tasks Created")
    void testNoSideEffectObligationsOrTasksCreated() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        long obligationsBefore = complianceObligationRepository.count();
        long tasksBefore = taskRepository.count();

        UpdateComplianceProfileRequest req = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsDeductorCategory(ComplianceTdsDeductorCategory.COMPANY)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.COMPANY)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        TenantContext.setTenantId(orgA.getId());
        long obligationsAfter = complianceObligationRepository.count();
        long tasksAfter = taskRepository.count();

        // Must strictly be 0 mutations to obligations and tasks
        assertThat(obligationsAfter).isEqualTo(obligationsBefore);
        assertThat(tasksAfter).isEqualTo(tasksBefore);
    }

    @Test
    @DisplayName("8. Client 360 Integration: GET /api/v1/clients/{clientId}/360 Includes Compliance Profile Summary")
    void testClient360Compatibility() throws Exception {
        // Initialize profile
        UpdateComplianceProfileRequest req = UpdateComplianceProfileRequest.builder()
                .status(ComplianceProfileStatus.ACTIVE)
                .gstApplicable(true)
                .gstRegistrationType(ComplianceGstRegistrationType.REGULAR)
                .gstFilingFrequency(ComplianceFilingFrequency.MONTHLY)
                .tdsApplicable(true)
                .tdsFilingFrequency(ComplianceFilingFrequency.QUARTERLY)
                .tdsDeductorCategory(ComplianceTdsDeductorCategory.COMPANY)
                .itrApplicable(true)
                .itrCategory(ComplianceItrCategory.COMPANY)
                .build();

        mockMvc.perform(put("/api/v1/clients/{clientId}/compliance-profile", clientA.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Verify Client 360 endpoint
        mockMvc.perform(get("/api/v1/clients/{clientId}/360", clientA.getId())
                        .header("Authorization", adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.id").value(clientA.getId().toString()))
                .andExpect(jsonPath("$.data.complianceProfile.gstApplicable").value(true))
                .andExpect(jsonPath("$.data.complianceProfile.gstRegistrationType").value("REGULAR"))
                .andExpect(jsonPath("$.data.complianceProfile.tdsApplicable").value(true))
                .andExpect(jsonPath("$.data.complianceProfile.itrApplicable").value(true))
                .andExpect(jsonPath("$.data.complianceProfile.readinessScore").value(100));
    }
}
