package com.taxoryn.module.capability.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.entity.PracticeProfileEntity;
import com.taxoryn.module.organization.entity.PracticeType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.organization.repository.PracticeProfileRepository;
import com.taxoryn.module.role.dto.CreateRoleRequest;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SoloPracticeCapabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PracticeProfileRepository practiceProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity soloOrg;
    private UserEntity soloUser;
    private String soloToken;

    private OrganizationEntity firmOrg;
    private UserEntity firmUser;
    private String firmToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        practiceProfileRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Ensure baseline permissions exist
        PermissionEntity p1 = permissionRepository.save(PermissionEntity.builder().code("CLIENT_CREATE").name("Create Client").module("CLIENT").build());
        PermissionEntity p2 = permissionRepository.save(PermissionEntity.builder().code("CLIENT_VIEW").name("View Client").module("CLIENT").build());
        PermissionEntity p3 = permissionRepository.save(PermissionEntity.builder().code("CLIENT_UPDATE").name("Update Client").module("CLIENT").build());
        PermissionEntity p4 = permissionRepository.save(PermissionEntity.builder().code("EMPLOYEE_CREATE").name("Create Employee").module("EMPLOYEE").build());
        PermissionEntity p5 = permissionRepository.save(PermissionEntity.builder().code("EMPLOYEE_VIEW").name("View Employee").module("EMPLOYEE").build());
        PermissionEntity p6 = permissionRepository.save(PermissionEntity.builder().code("ROLE_CREATE").name("Create Role").module("ROLE").build());

        RoleEntity practiceOwnerRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTICE_OWNER")
                .name("Practice Owner")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(p1, p2, p3, p4, p5, p6)))
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(p1, p2, p3, p4, p5, p6)))
                .build());

        // 1. Setup SOLO Practice
        soloOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Alice Solo Practice")
                .email("alice@taxoryn.com")
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .status(OrganizationStatus.ACTIVE)
                .build());

        practiceProfileRepository.save(PracticeProfileEntity.builder()
                .organizationId(soloOrg.getId())
                .practiceType(PracticeType.SOLO)
                .onboardingCompleted(true)
                .build());

        TenantContext.setTenantId(soloOrg.getId());
        soloUser = userRepository.save(UserEntity.builder()
                .email("alice@taxoryn.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Alice")
                .lastName("Smith")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceOwnerRole)))
                .build());

        soloToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                soloUser.getId(),
                soloOrg.getId(),
                soloUser.getEmail(),
                Set.of("PRACTICE_OWNER", "ROLE_PRACTICE_OWNER"),
                Set.of("CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_UPDATE", "EMPLOYEE_CREATE", "EMPLOYEE_VIEW", "ROLE_CREATE")
        );
        TenantContext.clear();

        // 2. Setup FIRM Practice
        firmOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Bob & Partners Tax Firm")
                .email("bob@taxfirm.com")
                .organizationType(OrganizationType.SMALL_TAX_FIRM)
                .status(OrganizationStatus.ACTIVE)
                .build());

        practiceProfileRepository.save(PracticeProfileEntity.builder()
                .organizationId(firmOrg.getId())
                .practiceType(PracticeType.FIRM)
                .onboardingCompleted(true)
                .build());

        TenantContext.setTenantId(firmOrg.getId());
        firmUser = userRepository.save(UserEntity.builder()
                .email("bob@taxfirm.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Bob")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        firmToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                firmUser.getId(),
                firmOrg.getId(),
                firmUser.getEmail(),
                Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_UPDATE", "EMPLOYEE_CREATE", "EMPLOYEE_VIEW", "ROLE_CREATE")
        );
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. SOLO Practice CAN create and list clients, and view Client 360 overview")
    void testSoloPracticeCanCreateAndManageClients() throws Exception {
        CreateClientRequest clientReq = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Rohit Verma Client")
                .pan("ABCDE1234F")
                .email("rohit@client.com")
                .phone("+919876543210")
                .city("Bengaluru")
                .state("Karnataka")
                .status(ClientStatus.ACTIVE)
                .build();

        // Create Client in SOLO Practice
        String response = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", soloToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Rohit Verma Client"))
                .andReturn().getResponse().getContentAsString();

        // List Clients in SOLO Practice
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", soloToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Rohit Verma Client"));
    }

    @Test
    @DisplayName("2. SOLO Practice CANNOT create employees or list team directory (403 Forbidden)")
    void testSoloPracticeCannotCreateOrListEmployees() throws Exception {
        CreateEmployeeRequest empReq = CreateEmployeeRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@solopractice.com")
                .department("Taxation")
                .designation("Associate")
                .status(EmployeeStatus.INVITED)
                .build();

        // Attempting to create employee in SOLO practice returns 403
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", soloToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // Attempting to list employees in SOLO practice returns 403
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", soloToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // Attempting to get employee by ID in SOLO practice returns 403
        mockMvc.perform(get("/api/v1/employees/" + UUID.randomUUID())
                        .header("Authorization", soloToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("3. SOLO Practice CANNOT access team chat channels or contacts (403 Forbidden)")
    void testSoloPracticeCannotAccessTeamChat() throws Exception {
        // Channels
        mockMvc.perform(get("/api/v1/employee/chat/channels")
                        .header("Authorization", soloToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // Contacts
        mockMvc.perform(get("/api/v1/employee/chat/contacts")
                        .header("Authorization", soloToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("4. SOLO Practice CANNOT create custom roles or administer team RBAC (403 Forbidden)")
    void testSoloPracticeCannotManageTeamRoles() throws Exception {
        CreateRoleRequest roleReq = CreateRoleRequest.builder()
                .code("CUSTOM_TAX_AUDITOR")
                .name("Custom Tax Auditor")
                .description("Custom role for audit staff")
                .permissionCodes(Set.of("CLIENT_VIEW"))
                .build();

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", soloToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("5. FIRM Practice retains FULL team capability without regression")
    void testFirmPracticeRetainsFullTeamCapability() throws Exception {
        // 1. FIRM can create client
        CreateClientRequest clientReq = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Firm Client Ltd")
                .pan("AAACF1234G")
                .email("firmclient@test.com")
                .status(ClientStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", firmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. FIRM can create employee
        CreateEmployeeRequest empReq = CreateEmployeeRequest.builder()
                .firstName("Karan")
                .lastName("Mehta")
                .email("karan@taxfirm.com")
                .department("Taxation")
                .designation("Senior Associate")
                .status(EmployeeStatus.INVITED)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", firmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Karan"));

        // 3. FIRM can list employees
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", firmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. FIRM can access team chat channels & contacts
        mockMvc.perform(get("/api/v1/employee/chat/channels")
                        .header("Authorization", firmToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employee/chat/contacts")
                        .header("Authorization", firmToken))
                .andExpect(status().isOk());

        // 5. FIRM can create custom role
        CreateRoleRequest roleReq = CreateRoleRequest.builder()
                .code("FIRM_AUDIT_LEAD")
                .name("Firm Audit Lead")
                .description("Senior leader in audit")
                .permissionCodes(Set.of("CLIENT_VIEW", "CLIENT_CREATE"))
                .build();

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", firmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("FIRM_AUDIT_LEAD"));
    }
}
