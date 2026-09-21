package com.taxoryn.module.employee.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.employee.chat.dto.SendEmployeeChatMessageRequest;
import com.taxoryn.module.employee.chat.entity.EmployeeChatChannelEntity;
import com.taxoryn.module.employee.chat.repository.EmployeeChatChannelRepository;
import com.taxoryn.module.employee.chat.repository.EmployeeChatMessageRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeChatSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeChatMessageRepository chatMessageRepository;

    @Autowired
    private EmployeeChatChannelRepository chatChannelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID tenantId;
    private OrganizationEntity organization;

    private EmployeeEntity adminEmp;
    private UserEntity adminUser;
    private String adminToken;

    private EmployeeEntity gstStaffEmp;
    private UserEntity gstStaffUser;
    private String gstStaffToken;

    private EmployeeEntity itrStaffEmp;
    private UserEntity itrStaffUser;
    private String itrStaffToken;

    @BeforeEach
    void setUp() {
        cleanData();

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Chat Security Test Firm")
                .legalName("Chat Security Test Firm LLP")
                .email("admin@chatsec.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());
        tenantId = organization.getId();
        TenantContext.setTenantId(tenantId);

        RoleEntity adminRole = getOrCreateRole("ORG_ADMIN", "Organization Admin");
        RoleEntity staffRole = getOrCreateRole("STAFF", "Practice Staff");

        // 1. Firm Admin
        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantId)
                .email("admin@chatsec.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Super")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        adminEmp = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUser.getId())
                .employeeCode("ADM-001")
                .firstName("Super")
                .lastName("Admin")
                .email(adminUser.getEmail())
                .department("Management")
                .designation("Partner")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        adminEmp.setOrganizationId(tenantId);
        adminEmp = employeeRepository.save(adminEmp);
        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                tenantId,
                adminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("USER_VIEW", "ROLE_READ", "EMPLOYEE_VIEW")
        );

        // 2. GST Staff
        gstStaffUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantId)
                .email("gst.staff@chatsec.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("GST")
                .lastName("Specialist")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        gstStaffEmp = employeeRepository.save(EmployeeEntity.builder()
                .userId(gstStaffUser.getId())
                .employeeCode("GST-001")
                .firstName("GST")
                .lastName("Specialist")
                .email(gstStaffUser.getEmail())
                .department("GST Compliance")
                .designation("Senior GST Associate")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        gstStaffEmp.setOrganizationId(tenantId);
        gstStaffEmp = employeeRepository.save(gstStaffEmp);
        gstStaffToken = jwtTokenProvider.generateAccessToken(
                gstStaffUser.getId(),
                tenantId,
                gstStaffUser.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("GST_VIEW", "TASK_VIEW")
        );

        // 3. ITR Staff (Different Department)
        itrStaffUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantId)
                .email("itr.staff@chatsec.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("ITR")
                .lastName("Auditor")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        itrStaffEmp = employeeRepository.save(EmployeeEntity.builder()
                .userId(itrStaffUser.getId())
                .employeeCode("ITR-001")
                .firstName("ITR")
                .lastName("Auditor")
                .email(itrStaffUser.getEmail())
                .department("Direct Tax & ITR")
                .designation("Tax Auditor")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build());
        itrStaffEmp.setOrganizationId(tenantId);
        itrStaffEmp = employeeRepository.save(itrStaffEmp);
        itrStaffToken = jwtTokenProvider.generateAccessToken(
                itrStaffUser.getId(),
                tenantId,
                itrStaffUser.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ITR_VIEW", "TASK_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        cleanData();
    }

    private void cleanData() {
        TenantContext.clear();
        chatMessageRepository.deleteAll();
        chatChannelRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private RoleEntity getOrCreateRole(String code, String name) {
        return roleRepository.findByCodeAndOrganizationId(code, tenantId).orElseGet(() -> {
            RoleEntity r = RoleEntity.builder()
                    .code(code)
                    .name(name)
                    .isSystemRole(true)
                    .build();
            r.setOrganizationId(tenantId);
            return roleRepository.save(r);
        });
    }

    @Test
    @DisplayName("Admin sees all employees in chat contacts")
    void testAdminSeesAllContacts() throws Exception {
        mockMvc.perform(get("/api/v1/employee/chat/contacts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("Staff sees department peers and admin, but NOT staff from unrelated departments")
    void testStaffSeesScopedContacts() throws Exception {
        mockMvc.perform(get("/api/v1/employee/chat/contacts")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // GST Staff should see Admin and Self/GST peers, but NOT ITR Staff
                .andExpect(jsonPath("$.data[*].employeeId", hasItem(adminEmp.getId().toString())))
                .andExpect(jsonPath("$.data[*].employeeId", not(hasItem(itrStaffEmp.getId().toString()))));
    }

    @Test
    @DisplayName("Direct messaging between Admin and Staff succeeds with read receipt updates")
    void testDirectMessagingFlow() throws Exception {
        SendEmployeeChatMessageRequest request = SendEmployeeChatMessageRequest.builder()
                .messageBody("Hello GST Specialist, please review the Q2 GSTR-1 filings.")
                .recipientEmployeeId(gstStaffEmp.getId())
                .build();

        // 1. Admin sends message to GST Staff
        mockMvc.perform(post("/api/v1/employee/chat/direct/" + gstStaffEmp.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.messageBody").value("Hello GST Specialist, please review the Q2 GSTR-1 filings."))
                .andExpect(jsonPath("$.data.isRead").value(false));

        // 2. GST Staff checks unread count -> 1
        mockMvc.perform(get("/api/v1/employee/chat/unread-count")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 3. GST Staff fetches direct messages with Admin
        mockMvc.perform(get("/api/v1/employee/chat/direct/" + adminEmp.getId() + "/messages")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. GST Staff marks messages as read
        mockMvc.perform(post("/api/v1/employee/chat/direct/" + adminEmp.getId() + "/read")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk());

        // 5. Unread count becomes 0
        mockMvc.perform(get("/api/v1/employee/chat/unread-count")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("Staff sending message to unauthorized department staff is forbidden")
    void testCrossDepartmentMessageForbidden() throws Exception {
        SendEmployeeChatMessageRequest request = SendEmployeeChatMessageRequest.builder()
                .messageBody("Hi ITR colleague, this should be blocked.")
                .recipientEmployeeId(itrStaffEmp.getId())
                .build();

        mockMvc.perform(post("/api/v1/employee/chat/direct/" + itrStaffEmp.getId() + "/messages")
                        .header("Authorization", "Bearer " + gstStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Channel messaging in accessible department channel")
    void testChannelMessaging() throws Exception {
        // Fetch accessible channels for GST staff
        String channelsResponse = mockMvc.perform(get("/api/v1/employee/chat/channels")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(channelsResponse.contains("general"));
        assertTrue(channelsResponse.contains("GST Compliance"));

        EmployeeChatChannelEntity generalChan = chatChannelRepository.findByOrganizationIdAndNameAndIsArchivedFalse(tenantId, "general")
                .orElseThrow();

        SendEmployeeChatMessageRequest chanMsg = SendEmployeeChatMessageRequest.builder()
                .messageBody("Practice tax update posted.")
                .channelId(generalChan.getId())
                .build();

        mockMvc.perform(post("/api/v1/employee/chat/channels/" + generalChan.getId() + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanMsg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.messageBody").value("Practice tax update posted."));

        mockMvc.perform(get("/api/v1/employee/chat/channels/" + generalChan.getId() + "/messages")
                        .header("Authorization", "Bearer " + gstStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
