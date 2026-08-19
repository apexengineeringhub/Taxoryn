package com.taxoryn.module.itr.controller;

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
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.RecordItrFilingRequest;
import com.taxoryn.module.itr.dto.UpdateItrStatusRequest;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItrManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItrProfileRepository itrProfileRepository;

    @Autowired
    private ItrReturnRepository itrReturnRepository;

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
    private EmployeeEntity employeeVikram;
    private ClientEntity clientAnand;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        itrReturnRepository.deleteAll();
        itrProfileRepository.deleteAll();
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
                Set.of("ITR_CREATE", "ITR_VIEW", "ITR_UPDATE", "ITR_DELETE")
        );

        // 2. Create Employee Vikram
        employeeVikram = EmployeeEntity.builder()
                .employeeCode("EMP-002")
                .firstName("Vikram")
                .lastName("Sharma")
                .email("vikram@apextax.com")
                .department("Direct Tax")
                .designation("Tax Consultant")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeVikram = employeeRepository.save(employeeVikram);

        // 3. Create Client Anand Joshi
        clientAnand = ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Anand Ramesh Joshi")
                .legalName("Anand Ramesh Joshi")
                .pan("ABCPJ9876M")
                .email("anand.joshi@gmail.com")
                .city("Mumbai")
                .state("Maharashtra")
                .assignedEmployeeId(employeeVikram.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientAnand = clientRepository.save(clientAnand);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Create ITR Profile for Client")
    void testCreateItrProfile() throws Exception {
        CreateItrProfileRequest request = CreateItrProfileRequest.builder()
                .clientId(clientAnand.getId())
                .pan("ABCPJ9876M")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .assignedEmployeeId(employeeVikram.getId())
                .build();

        mockMvc.perform(post("/api/v1/itr/profiles")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pan").value("ABCPJ9876M"))
                .andExpect(jsonPath("$.data.clientName").value("Anand Ramesh Joshi"))
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Vikram Sharma"));
    }

    @Test
    @DisplayName("2. Create ITR Return and progress status workflow")
    void testItrReturnWorkflow() throws Exception {
        CreateItrReturnRequest returnReq = CreateItrReturnRequest.builder()
                .clientId(clientAnand.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .assignedEmployeeId(employeeVikram.getId())
                .notes("Form 16 received from employer")
                .build();

        String response = mockMvc.perform(post("/api/v1/itr/returns")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assessmentYear").value("2026-27"))
                .andExpect(jsonPath("$.data.status").value("DOCUMENTS_PENDING"))
                .andReturn().getResponse().getContentAsString();

        String returnId = objectMapper.readTree(response).path("data").path("id").asText();

        // 1. DOCUMENTS_PENDING -> DATA_ENTRY
        mockMvc.perform(patch("/api/v1/itr/returns/" + returnId + "/status")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItrStatusRequest(ItrStatus.DATA_ENTRY, "AIS & 26AS reconciled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DATA_ENTRY"));

        // 2. DATA_ENTRY -> UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/itr/returns/" + returnId + "/status")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItrStatusRequest(ItrStatus.UNDER_REVIEW, "Draft computation prepared for senior review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        // 3. UNDER_REVIEW -> READY_TO_FILE
        mockMvc.perform(patch("/api/v1/itr/returns/" + returnId + "/status")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItrStatusRequest(ItrStatus.READY_TO_FILE, "Client approved computation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_TO_FILE"));
    }

    @Test
    @DisplayName("3. Record e-Filing submission details & ACK number")
    void testRecordFilingDetails() throws Exception {
        TenantContext.setTenantId(org1.getId());
        var ret = itrReturnRepository.save(com.taxoryn.module.itr.entity.ItrReturnEntity.builder()
                .clientId(clientAnand.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .status(ItrStatus.READY_TO_FILE)
                .assignedEmployeeId(employeeVikram.getId())
                .build());
        TenantContext.clear();

        RecordItrFilingRequest filingReq = RecordItrFilingRequest.builder()
                .filingDate(LocalDate.of(2026, 7, 28))
                .acknowledgementNumber("123456789012345")
                .verificationDate(LocalDate.of(2026, 7, 28))
                .notes("Filed successfully and e-verified with Aadhaar OTP")
                .build();

        mockMvc.perform(post("/api/v1/itr/returns/" + ret.getId() + "/filing-details")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("123456789012345"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("4. Query upcoming and overdue ITR returns")
    void testUpcomingAndOverdueReturns() throws Exception {
        TenantContext.setTenantId(org1.getId());
        try {
            // Overdue return (due in the past)
            itrReturnRepository.save(com.taxoryn.module.itr.entity.ItrReturnEntity.builder()
                    .clientId(clientAnand.getId())
                    .assessmentYear("2024-25")
                    .financialYear("2023-24")
                    .itrType(ItrType.ITR_1)
                    .taxpayerType(TaxpayerType.INDIVIDUAL)
                    .dueDate(LocalDate.now().minusDays(10))
                    .status(ItrStatus.DOCUMENTS_PENDING)
                    .build());

            // Upcoming return (due in the future)
            itrReturnRepository.save(com.taxoryn.module.itr.entity.ItrReturnEntity.builder()
                    .clientId(clientAnand.getId())
                    .assessmentYear("2026-27")
                    .financialYear("2025-26")
                    .itrType(ItrType.ITR_1)
                    .taxpayerType(TaxpayerType.INDIVIDUAL)
                    .dueDate(LocalDate.now().plusDays(15))
                    .status(ItrStatus.DATA_ENTRY)
                    .build());
        } finally {
            TenantContext.clear();
        }

        // Test Overdue endpoint
        mockMvc.perform(get("/api/v1/itr/returns/overdue")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].assessmentYear").value("2024-25"));

        // Test Upcoming endpoint
        mockMvc.perform(get("/api/v1/itr/returns/upcoming")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].assessmentYear").value("2026-27"));
    }

    @Test
    @DisplayName("5. ITR Workload Dashboard aggregates metrics")
    void testGetItrWorkloadDashboard() throws Exception {
        TenantContext.setTenantId(org1.getId());
        try {
            itrReturnRepository.save(com.taxoryn.module.itr.entity.ItrReturnEntity.builder()
                    .clientId(clientAnand.getId())
                    .assessmentYear("2026-27")
                    .financialYear("2025-26")
                    .itrType(ItrType.ITR_1)
                    .taxpayerType(TaxpayerType.INDIVIDUAL)
                    .dueDate(LocalDate.now().plusDays(20))
                    .status(ItrStatus.UNDER_REVIEW)
                    .assignedEmployeeId(employeeVikram.getId())
                    .build());
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/v1/itr/dashboard/workload")
                        .param("assessmentYear", "2026-27")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assessmentYear").value("2026-27"))
                .andExpect(jsonPath("$.data.totalReturns").value(1))
                .andExpect(jsonPath("$.data.underReviewCount").value(1))
                .andExpect(jsonPath("$.data.returns[0].clientName").value("Anand Ramesh Joshi"))
                .andExpect(jsonPath("$.data.returns[0].assignedTo").value("Vikram Sharma"));
    }
}
