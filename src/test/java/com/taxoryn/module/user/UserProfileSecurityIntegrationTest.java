package com.taxoryn.module.user;

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
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.dto.UpdateClientPortalProfileRequest;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.dto.UpdateUserProfileRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;

    private UserEntity employeeUser1;
    private String employeeToken1;
    private EmployeeEntity employee1;

    private UserEntity employeeUser2;
    private String employeeToken2;
    private EmployeeEntity employee2;

    private UserEntity clientUser1;
    private String clientToken1;
    private ClientEntity client1;

    // Standard valid PNG image bytes
    private static final byte[] VALID_PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    // Standard valid JPEG image bytes
    private static final byte[] VALID_JPEG_BYTES = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x60, 0x00, 0x60, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xD9
    };

    // Standard valid GIF image bytes (GIF89a)
    private static final byte[] VALID_GIF_BYTES = new byte[] {
            'G', 'I', 'F', '8', '9', 'a',
            0x01, 0x00, 0x01, 0x00, (byte) 0x80, 0x00, 0x00,
            0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
            0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
    };

    // Standard valid WEBP image bytes
    private static final byte[] VALID_WEBP_BYTES = new byte[] {
            'R', 'I', 'F', 'F',
            0x14, 0x00, 0x00, 0x00,
            'W', 'E', 'B', 'P',
            'V', 'P', '8', ' ',
            0x08, 0x00, 0x00, 0x00,
            0x30, 0x01, 0x00, (byte) 0x9D, 0x01, 0x2A, 0x01, 0x00
    };

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        employeeRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organizations
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Advisors")
                .email("admin@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Global Consulting")
                .email("admin@zenith.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Roles
        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Staff")
                .permissions(new HashSet<>())
                .isSystemRole(true)
                .build());

        RoleEntity clientRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client User")
                .permissions(new HashSet<>())
                .isSystemRole(true)
                .build());

        // 3. Employee 1 (Org 1)
        TenantContext.setTenantId(org1.getId());
        employeeUser1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("alice@apextax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alice")
                .lastName("Smith")
                .phone("9876543210")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        employee1 = employeeRepository.save(EmployeeEntity.builder()
                .userId(employeeUser1.getId())
                .employeeCode("EMP-001")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@apextax.com")
                .phone("9876543210")
                .department("Tax Advisory")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .build());

        employeeToken1 = jwtTokenProvider.generateAccessToken(
                employeeUser1.getId(),
                org1.getId(),
                employeeUser1.getEmail(),
                Set.of("STAFF"),
                Set.of()
        );

        // 4. Employee 2 (Org 2)
        TenantContext.setTenantId(org2.getId());
        employeeUser2 = userRepository.save(UserEntity.builder()
                .organizationId(org2.getId())
                .email("bob@zenith.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Bob")
                .lastName("Jones")
                .phone("9876543211")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        employee2 = employeeRepository.save(EmployeeEntity.builder()
                .userId(employeeUser2.getId())
                .employeeCode("EMP-002")
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@zenith.com")
                .phone("9876543211")
                .department("Audit")
                .designation("Associate")
                .status(EmployeeStatus.ACTIVE)
                .build());

        employeeToken2 = jwtTokenProvider.generateAccessToken(
                employeeUser2.getId(),
                org2.getId(),
                employeeUser2.getEmail(),
                Set.of("STAFF"),
                Set.of()
        );

        // 5. Client User 1 (Org 1)
        TenantContext.setTenantId(org1.getId());
        client1 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Enterprises")
                .legalName("Alpha Private Limited")
                .pan("ABCDE1234F")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .email("contact@alphacorp.com")
                .phone("9123456789")
                .build());

        clientUser1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .clientId(client1.getId())
                .email("contact@alphacorp.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        clientToken1 = jwtTokenProvider.generateAccessToken(
                clientUser1.getId(),
                org1.getId(),
                client1.getId(),
                clientUser1.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Self-service: User can get own profile and avatar URL")
    void testGetMyProfile_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@apextax.com"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"));
    }

    @Test
    @DisplayName("Self-service: Updating User profile syncs Employee record in same tenant")
    void testUpdateMyProfile_SyncsEmployee() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("Alice-Updated")
                .lastName("Smith-Updated")
                .phone("9998887776")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice-Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Smith-Updated"))
                .andExpect(jsonPath("$.data.phone").value("9998887776"));

        // Verify database persistence and synchronization with EmployeeEntity
        UserEntity updatedUser = userRepository.findById(employeeUser1.getId()).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("Alice-Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("Smith-Updated");
        assertThat(updatedUser.getPhone()).isEqualTo("9998887776");

        EmployeeEntity updatedEmp = employeeRepository.findByOrganizationIdAndUserId(org1.getId(), employeeUser1.getId()).orElseThrow();
        assertThat(updatedEmp.getFirstName()).isEqualTo("Alice-Updated");
        assertThat(updatedEmp.getLastName()).isEqualTo("Smith-Updated");
        assertThat(updatedEmp.getPhone()).isEqualTo("9998887776");
    }

    @Test
    @DisplayName("Self-service: Employee can get and update own profile via /employees/me")
    void testEmployeeMeEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/employees/me")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-001"))
                .andExpect(jsonPath("$.data.department").value("Tax Advisory"));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("Alice-Emp")
                .lastName("Smith-Emp")
                .phone("9876543299")
                .build();

        mockMvc.perform(put("/api/v1/employees/me")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice-Emp"))
                .andExpect(jsonPath("$.data.lastName").value("Smith-Emp"));
    }

    @Test
    @DisplayName("Profile Photo Upload: Valid PNG upload sets avatar URL and updates user and employee")
    void testUploadAvatar_ValidPNG_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").isNotEmpty());

        // Verify Employee also received avatarUrl
        EmployeeEntity emp = employeeRepository.findByOrganizationIdAndUserId(org1.getId(), employeeUser1.getId()).orElseThrow();
        assertThat(emp.getAvatarUrl()).isNotEmpty();
    }

    @Test
    @DisplayName("Profile Photo Security: Reject non-image files with spoofed MIME type (Magic Bytes Validation)")
    void testUploadAvatar_SpoofedMimeType_FailsValidation() throws Exception {
        // Text payload spoofed as image/png
        MockMultipartFile fakePng = new MockMultipartFile(
                "file",
                "evil.png",
                "image/png",
                "echo 'Malicious Executable Payload'".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(fakePng)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Profile Photo Security: Reject files exceeding 5MB limit")
    void testUploadAvatar_ExceedsSizeLimit_FailsValidation() throws Exception {
        byte[] oversizedBytes = new byte[6 * 1024 * 1024]; // 6MB
        System.arraycopy(VALID_PNG_BYTES, 0, oversizedBytes, 0, VALID_PNG_BYTES.length);

        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                oversizedBytes
        );

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(largeFile)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Self-service: User can delete own avatar")
    void testDeleteAvatar_Success() throws Exception {
        // First upload
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", VALID_JPEG_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar").file(file).header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        // Then delete
        mockMvc.perform(delete("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UserEntity user = userRepository.findById(employeeUser1.getId()).orElseThrow();
        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("Client Portal: Client can update own contact details and upload avatar")
    void testClientPortalProfileAndAvatar() throws Exception {
        UpdateClientPortalProfileRequest updateReq = UpdateClientPortalProfileRequest.builder()
                .displayName("Alpha Global Solutions")
                .phone("9888877777")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .build();

        mockMvc.perform(put("/api/v1/portal/profile")
                        .header("Authorization", "Bearer " + clientToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Alpha Global Solutions"))
                .andExpect(jsonPath("$.data.city").value("Mumbai"));

        // Upload Client avatar
        MockMultipartFile file = new MockMultipartFile("file", "client-avatar.png", "image/png", VALID_PNG_BYTES);
        mockMvc.perform(multipart("/api/v1/portal/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + clientToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").isNotEmpty());
    }

    @Test
    @DisplayName("Tenant Isolation: User from Org 2 cannot access or stream avatar of Org 1")
    void testTenantIsolation_CrossTenantAccess_Forbidden() throws Exception {
        // Upload avatar for employee1 (Org 1)
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", VALID_PNG_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        // Employee 2 (Org 2) attempts to access avatar of Employee 1 (Org 1) -> 404 Not Found (tenant scoped)
        mockMvc.perform(get("/api/v1/users/" + employeeUser1.getId() + "/avatar")
                        .header("Authorization", "Bearer " + employeeToken2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security & Privacy: Avatar streaming endpoints enforce private cache-control and security headers")
    void testAvatarStreaming_CachePrivacyAndSecurityHeaders() throws Exception {
        // Upload avatar for employee1
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", VALID_PNG_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        // 1. Check /users/me/avatar headers
        mockMvc.perform(get("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "private, no-cache, no-store, must-revalidate"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Pragma", "no-cache"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Expires", "0"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(VALID_PNG_BYTES));

        // 2. Check /employees/me/avatar headers
        mockMvc.perform(get("/api/v1/employees/me/avatar")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "private, no-cache, no-store, must-revalidate"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Pragma", "no-cache"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Expires", "0"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));

        // 3. Upload client avatar and check /portal/profile/avatar headers
        MockMultipartFile clientFile = new MockMultipartFile("file", "client.png", "image/png", VALID_PNG_BYTES);
        mockMvc.perform(multipart("/api/v1/portal/profile/avatar")
                        .file(clientFile)
                        .header("Authorization", "Bearer " + clientToken1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/portal/profile/avatar")
                        .header("Authorization", "Bearer " + clientToken1))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "private, no-cache, no-store, must-revalidate"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Pragma", "no-cache"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Expires", "0"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));
    }

    @Test
    @DisplayName("MIME Correctness: Avatar endpoints dynamically detect and serve correct MIME type for PNG, JPEG, GIF, WEBP")
    void testAvatarStreaming_DynamicMimeTypeDetection() throws Exception {
        // 1. JPEG
        MockMultipartFile jpegFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(jpegFile)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(VALID_JPEG_BYTES));

        // 2. GIF
        MockMultipartFile gifFile = new MockMultipartFile("file", "anim.gif", "image/gif", VALID_GIF_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(gifFile)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/gif"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(VALID_GIF_BYTES));

        // 3. WEBP
        MockMultipartFile webpFile = new MockMultipartFile("file", "graphic.webp", "image/webp", VALID_WEBP_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(webpFile)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/webp"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(VALID_WEBP_BYTES));
    }

    @Test
    @DisplayName("Security: Untrusted avatarUrl in profile update DTO is ignored and does not mutate avatar storage key")
    void testUntrustedAvatarUrl_IgnoredInProfileUpdate() throws Exception {
        // Upload genuine avatar first
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", VALID_PNG_BYTES);
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk());

        UserEntity userBefore = userRepository.findById(employeeUser1.getId()).orElseThrow();
        String authoritativeAvatarKey = userBefore.getAvatarUrl();
        assertThat(authoritativeAvatarKey).isNotNull();

        // Attempt untrusted avatar URL injection via PUT /users/me
        UpdateUserProfileRequest userRequest = UpdateUserProfileRequest.builder()
                .firstName("Alice-Hacked")
                .avatarUrl("https://attacker.com/malicious-avatar.png")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Alice-Hacked"));

        UserEntity userAfter = userRepository.findById(employeeUser1.getId()).orElseThrow();
        assertThat(userAfter.getAvatarUrl()).isEqualTo(authoritativeAvatarKey);

        // Attempt untrusted avatar URL injection via PUT /employees/me
        UpdateUserProfileRequest empRequest = UpdateUserProfileRequest.builder()
                .firstName("Alice-Emp-Hacked")
                .avatarUrl("tenants/org_foreign/avatars/stolen.png")
                .build();

        mockMvc.perform(put("/api/v1/employees/me")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empRequest)))
                .andExpect(status().isOk());

        EmployeeEntity empAfter = employeeRepository.findByOrganizationIdAndUserId(org1.getId(), employeeUser1.getId()).orElseThrow();
        assertThat(empAfter.getAvatarUrl()).isEqualTo(authoritativeAvatarKey);

        // Attempt untrusted avatar URL injection via PUT /portal/profile
        UpdateClientPortalProfileRequest clientReq = UpdateClientPortalProfileRequest.builder()
                .displayName("Legit Corp")
                .avatarUrl("https://malicious.site/cross-tenant-avatar.jpg")
                .build();

        mockMvc.perform(put("/api/v1/portal/profile")
                        .header("Authorization", "Bearer " + clientToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientReq)))
                .andExpect(status().isOk());

        ClientEntity clientAfter = clientRepository.findById(client1.getId()).orElseThrow();
        assertThat(clientAfter.getAvatarUrl()).isNull(); // Wasn't uploaded, so must remain null despite payload
    }

    @Test
    @DisplayName("Security: Unauthenticated requests to /me endpoints are rejected with 401/403")
    void testUnauthenticatedAccess_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/portal/profile"))
                .andExpect(status().isUnauthorized());
    }
}
