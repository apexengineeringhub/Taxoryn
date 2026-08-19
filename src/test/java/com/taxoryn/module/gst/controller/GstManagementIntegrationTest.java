package com.taxoryn.module.gst.controller;

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
import com.taxoryn.module.gst.dto.BatchGenerateFilingsRequest;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.SaveGstMonthlySummaryRequest;
import com.taxoryn.module.gst.dto.UpdateGstFilingStatusRequest;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstMonthlySummaryRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GstManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GstProfileRepository gstProfileRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private GstMonthlySummaryRepository gstMonthlySummaryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String adminToken1;
    private EmployeeEntity employeeRahul;
    private ClientEntity clientAbcTraders;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        gstMonthlySummaryRepository.deleteAll();
        gstReturnFilingRepository.deleteAll();
        gstProfileRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .email("admin@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Tax Advisory")
                .email("admin@globaltax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());

        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("GST_CREATE", "GST_VIEW", "GST_UPDATE", "GST_DELETE")
        );

        // 2. Create Employee "Rahul"
        employeeRahul = EmployeeEntity.builder()
                .employeeCode("EMP-007")
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@apextax.com")
                .department("GST & Indirect Tax")
                .designation("GST Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRahul = employeeRepository.save(employeeRahul);

        // 3. Create Client "ABC Traders"
        clientAbcTraders = ClientEntity.builder()
                .clientType(ClientType.PROPRIETORSHIP)
                .displayName("ABC Traders")
                .legalName("ABC Traders Proprietorship")
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .email("contact@abctraders.com")
                .city("Mumbai")
                .state("Maharashtra")
                .assignedEmployeeId(employeeRahul.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientAbcTraders = clientRepository.save(clientAbcTraders);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Register GST profile for client")
    void testRegisterGstProfile() throws Exception {
        CreateGstProfileRequest request = CreateGstProfileRequest.builder()
                .clientId(clientAbcTraders.getId())
                .gstin("27AAACB1234D1Z5")
                .tradeName("ABC Traders")
                .gstType(GstType.REGULAR)
                .filingFrequency(FilingFrequency.MONTHLY)
                .assignedEmployeeId(employeeRahul.getId())
                .build();

        mockMvc.perform(post("/api/v1/gst/profiles")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gstin").value("27AAACB1234D1Z5"))
                .andExpect(jsonPath("$.data.clientName").value("ABC Traders"))
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Rahul Sharma"));
    }

    @Test
    @DisplayName("2. Schedule return filing and submit with GST Portal ARN")
    void testCreateAndSubmitFilingWithArn() throws Exception {
        TenantContext.setTenantId(org1.getId());
        var profile = gstProfileRepository.save(com.taxoryn.module.gst.entity.GstProfileEntity.builder()
                .clientId(clientAbcTraders.getId())
                .gstin("27AAACB1234D1Z5")
                .tradeName("ABC Traders")
                .assignedEmployeeId(employeeRahul.getId())
                .build());
        TenantContext.clear();

        // Create Filing
        CreateGstReturnFilingRequest filingReq = CreateGstReturnFilingRequest.builder()
                .gstProfileId(profile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("2026-08")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 9, 11))
                .build();

        String response = mockMvc.perform(post("/api/v1/gst/filings")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.returnType").value("GSTR1"))
                .andExpect(jsonPath("$.data.filingStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        String filingId = objectMapper.readTree(response).path("data").path("id").asText();

        // Update status to FILED with ARN
        UpdateGstFilingStatusRequest statusReq = UpdateGstFilingStatusRequest.builder()
                .filingStatus(GstFilingStatus.FILED)
                .filingDate(LocalDate.of(2026, 9, 10))
                .acknowledgementNumber("AA2708260012345")
                .totalTaxableValue(new BigDecimal("1500000.00"))
                .totalTaxLiability(new BigDecimal("270000.00"))
                .build();

        mockMvc.perform(patch("/api/v1/gst/filings/" + filingId + "/status")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filingStatus").value("FILED"))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("AA2708260012345"));
    }

    @Test
    @DisplayName("3. Batch generate filings for practice")
    void testBatchGenerateFilings() throws Exception {
        TenantContext.setTenantId(org1.getId());
        gstProfileRepository.save(com.taxoryn.module.gst.entity.GstProfileEntity.builder()
                .clientId(clientAbcTraders.getId())
                .gstin("27AAACB1234D1Z5")
                .tradeName("ABC Traders")
                .gstType(GstType.REGULAR)
                .assignedEmployeeId(employeeRahul.getId())
                .build());
        TenantContext.clear();

        BatchGenerateFilingsRequest batchReq = BatchGenerateFilingsRequest.builder()
                .returnPeriod("2026-08")
                .financialYear("2026-27")
                .returnTypes(List.of(GstReturnType.GSTR1, GstReturnType.GSTR3B))
                .gstr1DueDate(LocalDate.of(2026, 9, 11))
                .gstr3bDueDate(LocalDate.of(2026, 9, 20))
                .build();

        mockMvc.perform(post("/api/v1/gst/filings/batch-generate")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("4. GST Workload Dashboard matches exact executive & employee scenario")
    void testGetWorkloadDashboardScenario() throws Exception {
        TenantContext.setTenantId(org1.getId());
        try {
            var profile = gstProfileRepository.save(com.taxoryn.module.gst.entity.GstProfileEntity.builder()
                    .clientId(clientAbcTraders.getId())
                    .gstin("27AAACB1234D1Z5")
                    .tradeName("ABC Traders")
                    .gstType(GstType.REGULAR)
                    .assignedEmployeeId(employeeRahul.getId())
                    .build());

            // GSTR-1 Pending
            gstReturnFilingRepository.save(com.taxoryn.module.gst.entity.GstReturnFilingEntity.builder()
                    .gstProfileId(profile.getId())
                    .clientId(clientAbcTraders.getId())
                    .returnType(GstReturnType.GSTR1)
                    .returnPeriod("2026-08")
                    .financialYear("2026-27")
                    .dueDate(LocalDate.of(2026, 9, 11))
                    .filingStatus(GstFilingStatus.PENDING)
                    .assignedEmployeeId(employeeRahul.getId())
                    .build());

            // GSTR-3B Pending
            gstReturnFilingRepository.save(com.taxoryn.module.gst.entity.GstReturnFilingEntity.builder()
                    .gstProfileId(profile.getId())
                    .clientId(clientAbcTraders.getId())
                    .returnType(GstReturnType.GSTR3B)
                    .returnPeriod("2026-08")
                    .financialYear("2026-27")
                    .dueDate(LocalDate.of(2026, 9, 20))
                    .filingStatus(GstFilingStatus.PENDING)
                    .assignedEmployeeId(employeeRahul.getId())
                    .build());

            // ITC: 1,25,000, Liability: 82,000
            gstMonthlySummaryRepository.save(GstMonthlySummaryEntity.builder()
                    .gstProfileId(profile.getId())
                    .clientId(clientAbcTraders.getId())
                    .period("2026-08")
                    .financialYear("2026-27")
                    .itcNetClaimed(new BigDecimal("125000.00"))
                    .netTaxLiability(new BigDecimal("82000.00"))
                    .build());
        } finally {
            TenantContext.clear();
        }

        // Test Workload Dashboard endpoint
        mockMvc.perform(get("/api/v1/gst/dashboard/workload")
                        .param("period", "2026-08")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period").value("2026-08"))
                .andExpect(jsonPath("$.data.periodLabel").value("August 2026"))
                .andExpect(jsonPath("$.data.totalGstClients").value(1))
                .andExpect(jsonPath("$.data.gstr1PendingCount").value(1))
                .andExpect(jsonPath("$.data.gstr3bPendingCount").value(1))
                .andExpect(jsonPath("$.data.totalItcTracked").value(125000.00))
                .andExpect(jsonPath("$.data.totalTaxLiability").value(82000.00))
                .andExpect(jsonPath("$.data.clients[0].clientName").value("ABC Traders"))
                .andExpect(jsonPath("$.data.clients[0].gstin").value("27AAACB1234D1Z5"))
                .andExpect(jsonPath("$.data.clients[0].gstr1Status").value("PENDING"))
                .andExpect(jsonPath("$.data.clients[0].gstr3bStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.clients[0].itc").value(125000.00))
                .andExpect(jsonPath("$.data.clients[0].taxLiability").value(82000.00))
                .andExpect(jsonPath("$.data.clients[0].dueDate").value("2026-09-20"))
                .andExpect(jsonPath("$.data.clients[0].assignedTo").value("Rahul Sharma"));
    }
}
