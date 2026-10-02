package com.taxoryn.module.gst;

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
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GstClientDropdownIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private GstProfileRepository gstProfileRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity practitionerUser1;
    private String adminToken1;
    private String practitionerToken1;
    private EmployeeEntity employee1;
    private EmployeeEntity employee2;
    private ClientEntity gstClient1;
    private ClientEntity gstClient2;
    private ClientEntity noGstClient;
    private ClientEntity blankGstClient;
    private ClientEntity inactiveGstClient;
    private ClientEntity org2Client;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        gstReturnFilingRepository.deleteAll();
        gstProfileRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Practice Firm")
                .email("admin@apexfirm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Foreign Organization")
                .email("admin@foreignfirm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Enable GST and Clients Modules for Org 1
        TenantContext.setTenantId(org1.getId());
        OrganizationModuleEntity gstConfig = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.GST)
                .enabled(true)
                .build();
        gstConfig.setOrganizationId(org1.getId());
        organizationModuleRepository.save(gstConfig);

        OrganizationModuleEntity clientsConfig = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build();
        clientsConfig.setOrganizationId(org1.getId());
        organizationModuleRepository.save(clientsConfig);

        // 3. Setup Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity practitionerRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTITIONER")
                .name("Practitioner")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Create Admin User
        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@apexfirm.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());
        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(adminUser1.getId(), org1.getId(), adminUser1.getEmail(), Set.of("ORG_ADMIN"), Set.of("CLIENT_VIEW", "CLIENT_READ", "GST_VIEW", "GST_CREATE", "GST_WRITE"));

        // 5. Create Employees
        employee1 = EmployeeEntity.builder()
                .employeeCode("EMP-AMIT-001")
                .firstName("Amit")
                .lastName("Kumar")
                .email("amit@apexfirm.com")
                .designation("Senior Tax Manager")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee1.setOrganizationId(org1.getId());
        employee1 = employeeRepository.save(employee1);

        employee2 = EmployeeEntity.builder()
                .employeeCode("EMP-NEHA-002")
                .firstName("Neha")
                .lastName("Patel")
                .email("neha@apexfirm.com")
                .designation("Staff Accountant")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee2.setOrganizationId(org1.getId());
        employee2 = employeeRepository.save(employee2);

        // 6. Create Practitioner User linked to Employee 1
        practitionerUser1 = UserEntity.builder()
                .organizationId(org1.getId())
                .email("amit@apexfirm.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Amit")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practitionerRole)))
                .build();
        practitionerUser1 = userRepository.save(practitionerUser1);

        employee1.setUserId(practitionerUser1.getId());
        employee1 = employeeRepository.save(employee1);

        practitionerToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(practitionerUser1.getId(), org1.getId(), practitionerUser1.getEmail(), Set.of("PRACTITIONER"), Set.of("CLIENT_VIEW", "CLIENT_READ", "GST_VIEW", "GST_CREATE", "GST_WRITE"));

        // 7. Create Clients in Org 1
        // Client 1: Has valid GSTIN, assigned to employee 1, ACTIVE
        gstClient1 = ClientEntity.builder()
                .displayName("ABC Traders")
                .legalName("ABC Traders Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACA1234A")
                .gstin("10ABCDE1234F1Z5")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        gstClient1.setOrganizationId(org1.getId());
        gstClient1 = clientRepository.save(gstClient1);

        // Client 2: Has valid GSTIN, assigned to employee 2, ACTIVE
        gstClient2 = ClientEntity.builder()
                .displayName("Zenith Logistics")
                .legalName("Zenith Logistics LLP")
                .clientType(ClientType.LLP)
                .pan("BBBCB2345B")
                .gstin("27BBBCB2345B1Z8")
                .assignedEmployeeId(employee2.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        gstClient2.setOrganizationId(org1.getId());
        gstClient2 = clientRepository.save(gstClient2);

        // Client 3: No GSTIN (null)
        noGstClient = ClientEntity.builder()
                .displayName("Individual Taxpayer")
                .clientType(ClientType.INDIVIDUAL)
                .pan("CCCCC3456C")
                .gstin(null)
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        noGstClient.setOrganizationId(org1.getId());
        noGstClient = clientRepository.save(noGstClient);

        // Client 4: Blank GSTIN ("   ")
        blankGstClient = ClientEntity.builder()
                .displayName("Blank GST Firm")
                .clientType(ClientType.PROPRIETORSHIP)
                .pan("DDDDD4567D")
                .gstin("   ")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        blankGstClient.setOrganizationId(org1.getId());
        blankGstClient = clientRepository.save(blankGstClient);

        // Client 5: Inactive Client with GSTIN
        inactiveGstClient = ClientEntity.builder()
                .displayName("Closed Enterprise")
                .clientType(ClientType.COMPANY)
                .pan("EEEEE5678E")
                .gstin("29EEEEE5678E1Z9")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.INACTIVE)
                .build();
        inactiveGstClient.setOrganizationId(org1.getId());
        inactiveGstClient = clientRepository.save(inactiveGstClient);

        // 8. Create Client in Org 2 (cross-organization test)
        TenantContext.setTenantId(org2.getId());
        org2Client = ClientEntity.builder()
                .displayName("Foreign Corp")
                .clientType(ClientType.COMPANY)
                .pan("FFFFF6789F")
                .gstin("07FFFFF6789F1Z0")
                .status(ClientStatus.ACTIVE)
                .build();
        org2Client.setOrganizationId(org2.getId());
        org2Client = clientRepository.save(org2Client);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. GST client with GSTIN is returned; clients with null or blank GSTIN are excluded")
    void testGetClientsWithGstinFilter() throws Exception {
        // When hasGstin=true and status=ACTIVE, should only return gstClient1 and gstClient2
        mockMvc.perform(get("/api/v1/clients")
                        .param("hasGstin", "true")
                        .param("status", "ACTIVE")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + gstClient1.getId() + "')].gstin").value("10ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + gstClient2.getId() + "')].gstin").value("27BBBCB2345B1Z8"))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + noGstClient.getId() + "')]").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + blankGstClient.getId() + "')]").doesNotExist());
    }

    @Test
    @DisplayName("2. Inactive client with GSTIN follows status filter business rule")
    void testInactiveClientStatusFiltering() throws Exception {
        // When status=ACTIVE and hasGstin=true, inactive client is excluded
        mockMvc.perform(get("/api/v1/clients")
                        .param("hasGstin", "true")
                        .param("status", "ACTIVE")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + inactiveGstClient.getId() + "')]").doesNotExist());

        // When status=INACTIVE and hasGstin=true, inactive client is returned
        mockMvc.perform(get("/api/v1/clients")
                        .param("hasGstin", "true")
                        .param("status", "INACTIVE")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(inactiveGstClient.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].gstin").value("29EEEEE5678E1Z9"));
    }

    @Test
    @DisplayName("3. Cross-organization client is never returned (Multi-tenant isolation)")
    void testCrossOrganizationIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .param("hasGstin", "true")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + org2Client.getId() + "')]").doesNotExist());
    }

    @Test
    @DisplayName("4. Employee only receives clients within their allowed portfolio")
    void testEmployeePortfolioScope() throws Exception {
        // Practitioner 1 (Amit) is assigned only to gstClient1, noGstClient, blankGstClient, and inactiveGstClient
        // gstClient2 is assigned to Employee 2 (Neha)
        mockMvc.perform(get("/api/v1/clients")
                        .param("hasGstin", "true")
                        .param("status", "ACTIVE")
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(gstClient1.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].displayName").value("ABC Traders"))
                .andExpect(jsonPath("$.data.content[0].gstin").value("10ABCDE1234F1Z5"));
    }

    @Test
    @DisplayName("5. GST module entitlement is enforced on GST filing endpoints")
    void testGstModuleEntitlementEnforced() throws Exception {
        // Schedule individual filing with clientId
        CreateGstReturnFilingRequest request = CreateGstReturnFilingRequest.builder()
                .clientId(gstClient1.getId())
                .returnType(GstReturnType.GSTR3B)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 20))
                .build();

        mockMvc.perform(post("/api/v1/gst/filings")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(gstClient1.getId().toString()))
                .andExpect(jsonPath("$.data.gstin").value("10ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.data.returnType").value("GSTR3B"));

        // Disable GST module for org1 and verify access is blocked
        TenantContext.setTenantId(org1.getId());
        organizationModuleRepository.findByOrganizationIdAndModuleCode(org1.getId(), ProductModuleCode.GST)
                .ifPresent(cfg -> {
                    cfg.setEnabled(false);
                    organizationModuleRepository.save(cfg);
                });
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/gst/filings")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
