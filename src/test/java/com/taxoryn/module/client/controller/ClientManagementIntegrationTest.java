package com.taxoryn.module.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientNoteRepository clientNoteRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

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
    private EmployeeEntity employee1;
    private ClientEntity client1;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        taskRepository.deleteAll();
        clientNoteRepository.deleteAll();
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
                Set.of("CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_DELETE")
        );

        // 2. Create Employee in Org 1
        employee1 = EmployeeEntity.builder()
                .employeeCode("EMP-001")
                .firstName("Vikram")
                .lastName("Sharma")
                .email("vikram@apextax.com")
                .department("Taxation")
                .designation("Senior Manager")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee1 = employeeRepository.save(employee1);

        // 3. Create Sample Client in Org 1
        client1 = ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .legalName("Zenith Information Technologies Private Limited")
                .tradeName("Zenith Software")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .tan("MUMZ12345A")
                .cin("U72200MH2018PTC312345")
                .email("finance@zenithinfo.com")
                .phone("+919811122233")
                .city("Mumbai")
                .state("Maharashtra")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client1 = clientRepository.save(client1);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Create client across various constitution types")
    void testCreateClientAcrossTypes() throws Exception {
        // Create LLP Client
        CreateClientRequest requestLLP = CreateClientRequest.builder()
                .clientType(ClientType.LLP)
                .displayName("Bluecrest Logistics LLP")
                .pan("AAALB5678E")
                .gstin("27AAALB5678E1Z4")
                .email("accounts@bluecrestlog.com")
                .phone("+919811144455")
                .city("Pune")
                .state("Maharashtra")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestLLP)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientType").value("LLP"))
                .andExpect(jsonPath("$.data.displayName").value("Bluecrest Logistics LLP"))
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Vikram Sharma"));

        // Create Individual Client
        CreateClientRequest requestInd = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Anand Ramesh Joshi")
                .pan("ABCPJ9876M")
                .email("anand.joshi@gmail.com")
                .phone("+919811166677")
                .city("Mumbai")
                .state("Maharashtra")
                .status(ClientStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientType").value("INDIVIDUAL"));
    }

    @Test
    @DisplayName("2. List & search clients by keyword, PAN, and filters")
    void testSearchAndFilterClients() throws Exception {
        // Search by keyword 'Zenith'
        mockMvc.perform(get("/api/v1/clients")
                        .param("search", "Zenith")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Zenith Infotech Pvt Ltd"));

        // Filter by PAN 'AAACZ1234D'
        mockMvc.perform(get("/api/v1/clients")
                        .param("pan", "AAACZ1234D")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        // Filter by clientType 'PRIVATE_LIMITED'
        mockMvc.perform(get("/api/v1/clients")
                        .param("clientType", "PRIVATE_LIMITED")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("3. Update client details and assign practitioner")
    void testUpdateAndAssignEmployee() throws Exception {
        UpdateClientRequest updateReq = UpdateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Global Pvt Ltd")
                .legalName("Zenith Information Technologies Private Limited")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .email("cfo@zenithinfo.com")
                .phone("+919811199999")
                .city("Navi Mumbai")
                .state("Maharashtra")
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/clients/" + client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Zenith Infotech Global Pvt Ltd"))
                .andExpect(jsonPath("$.data.city").value("Navi Mumbai"));

        // Reassign Employee endpoint
        AssignClientEmployeeRequest assignReq = new AssignClientEmployeeRequest(employee1.getId());
        mockMvc.perform(put("/api/v1/clients/" + client1.getId() + "/assigned-employee")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedEmployeeId").value(employee1.getId().toString()));
    }

    @Test
    @DisplayName("4. Client 360-Degree Overview returns unified client dashboard")
    void testGetClient360Overview() throws Exception {
        TenantContext.setTenantId(org1.getId());
        try {
            // Add task for client
            taskRepository.save(TaskEntity.builder()
                    .clientId(client1.getId())
                    .title("GSTR-3B Filing")
                    .taskCategory(TaskCategory.GST)
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDate.now().plusDays(5))
                    .build());

            // Add note for client
            clientNoteRepository.save(com.taxoryn.module.client.entity.ClientNoteEntity.builder()
                    .clientId(client1.getId())
                    .noteType(NoteType.MEETING)
                    .title("Audit Alignment")
                    .content("Met with CFO.")
                    .build());
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/overview")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.displayName").value("Zenith Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.statutory.pan").value("AAACZ1234D"))
                .andExpect(jsonPath("$.data.statutory.gstin").value("27AAACZ1234D1Z8"))
                .andExpect(jsonPath("$.data.statutory.isGstActive").value(true))
                .andExpect(jsonPath("$.data.taskSummary.totalTasks").value(1))
                .andExpect(jsonPath("$.data.taskSummary.pendingTasks").value(1))
                .andExpect(jsonPath("$.data.recentNotes.length()").value(1));

        // Also test dual route /api/clients/{id}/overview
        mockMvc.perform(get("/api/clients/" + client1.getId() + "/overview")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Zenith Infotech Pvt Ltd"));
    }

    @Test
    @DisplayName("5. Add and list client communication notes")
    void testClientCommunicationNotes() throws Exception {
        CreateClientNoteRequest noteReq = CreateClientNoteRequest.builder()
                .noteType(NoteType.CALL)
                .title("TDS 26Q Clarification Call")
                .content("Clarified vendor TDS challan mapping.")
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/notes")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("TDS 26Q Clarification Call"));

        // Retrieve notes
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/notes")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("TDS 26Q Clarification Call"));
    }

    @Test
    @DisplayName("6. Archive client & cross-tenant access rejection")
    void testArchiveAndCrossTenantRejection() throws Exception {
        // Archive client
        mockMvc.perform(delete("/api/v1/clients/" + client1.getId())
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk());

        // Cross tenant rejection
        TenantContext.setTenantId(org2.getId());
        UserEntity adminUser2;
        try {
            adminUser2 = userRepository.save(UserEntity.builder()
                    .email("admin@globaltax.com")
                    .passwordHash(passwordEncoder.encode("SecretPass123!"))
                    .firstName("Sanjay")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>())
                    .build());
        } finally {
            TenantContext.clear();
        }

        String org2Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE")
        );

        mockMvc.perform(get("/api/v1/clients/" + client1.getId())
                        .header("Authorization", org2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
