package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.dto.SendEnquiryMessageRequest;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnquiryMessagingIntegrationTest {

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
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MarketplaceProfileRepository profileRepository;

    @Autowired
    private MarketplaceLeadRepository leadRepository;

    @Autowired
    private MarketplaceEnquiryMessageRepository enquiryMessageRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private OrganizationEntity practiceOrg;
    private UserEntity practiceAdminUser;
    private EmployeeEntity practiceEmployee;
    private String practiceAdminToken;

    private OrganizationEntity otherPracticeOrg;
    private UserEntity otherAdminUser;
    private String otherAdminToken;

    private UserEntity customerUser;
    private String customerToken;

    private UserEntity otherCustomerUser;
    private String otherCustomerToken;

    private MarketplaceProfileEntity practiceProfile;
    private MarketplaceLeadEntity activeEnquiry;

    @BeforeEach
    void setUp() {
        enquiryMessageRepository.deleteAll();
        leadRepository.deleteAll();
        profileRepository.deleteAll();

        // 1. Setup Permissions & Roles
        PermissionEntity pView = permissionRepository.findByCode("MARKETPLACE_VIEW")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_VIEW").name("View").module("MARKETPLACE").build()));
        PermissionEntity pWrite = permissionRepository.findByCode("MARKETPLACE_WRITE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_WRITE").name("Write").module("MARKETPLACE").build()));

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        orgAdminRole.setPermissions(new HashSet<>(Set.of(pView, pWrite)));
        roleRepository.save(orgAdminRole);

        RoleEntity customerRole = roleRepository.findByCodeAndIsSystemRoleTrue("MARKETPLACE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("MARKETPLACE_CUSTOMER").name("Customer").isSystemRole(true).build()));

        // 2. Practice Org & Users
        practiceOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Messaging CA LLP " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Apex Messaging CA LLP")
                .email("admin@apexmsg" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .phone("+91 98200 11223")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .pan("AABCL1234F")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practiceAdminUser = UserEntity.builder()
                .email("admin@apexmsg" + UUID.randomUUID().toString().substring(0, 6) + ".com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Apex")
                .lastName("Admin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        practiceAdminUser.setOrganizationId(practiceOrg.getId());
        practiceAdminUser = userRepository.save(practiceAdminUser);

        practiceEmployee = EmployeeEntity.builder()
                .userId(practiceAdminUser.getId())
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .firstName("Rajesh")
                .lastName("Sharma")
                .email("rajesh@apexmsg.com")
                .designation("Senior Tax Manager")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        practiceEmployee.setOrganizationId(practiceOrg.getId());
        practiceEmployee = employeeRepository.save(practiceEmployee);

        practiceAdminToken = jwtTokenProvider.generateAccessToken(
                practiceAdminUser.getId(),
                practiceOrg.getId(),
                practiceAdminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        // 3. Other Practice Org
        otherPracticeOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Msg Firm " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Other Msg Firm LLP")
                .email("other@othermsg" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .phone("+91 98200 99887")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .pan("AABCO9988F")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        otherAdminUser = UserEntity.builder()
                .email("admin@othermsg" + UUID.randomUUID().toString().substring(0, 6) + ".com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Other")
                .lastName("Admin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        otherAdminUser.setOrganizationId(otherPracticeOrg.getId());
        otherAdminUser = userRepository.save(otherAdminUser);

        otherAdminToken = jwtTokenProvider.generateAccessToken(
                otherAdminUser.getId(),
                otherPracticeOrg.getId(),
                otherAdminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        // 4. Customers
        customerUser = userRepository.save(UserEntity.builder()
                .email("client" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Priya")
                .lastName("Sharma")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(
                customerUser.getId(),
                null,
                customerUser.getEmail(),
                Set.of("MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        otherCustomerUser = userRepository.save(UserEntity.builder()
                .email("client" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Arun")
                .lastName("Patel")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build());

        otherCustomerToken = jwtTokenProvider.generateAccessToken(
                otherCustomerUser.getId(),
                null,
                otherCustomerUser.getEmail(),
                Set.of("MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        // 5. Practice Profile
        practiceProfile = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg.getId())
                .slug("apex-messaging-advisors-" + UUID.randomUUID().toString().substring(0, 6))
                .displayName("Apex Messaging Advisors")
                .headline("Corporate Tax & Compliance")
                .bio("Professional CA practice for corporate taxation.")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(10)
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .averageRating(new BigDecimal("4.80"))
                .totalReviews(5)
                .verificationStatus(VerificationStatus.VERIFIED)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .isPublished(true)
                .build());

        // 6. Active Enquiry
        activeEnquiry = leadRepository.save(MarketplaceLeadEntity.builder()
                .referenceNumber("ENQ-2026-MSG1")
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .clientName("Priya Sharma")
                .clientEmail(customerUser.getEmail())
                .clientPhone("+919876543210")
                .serviceCategory("INCOME_TAX")
                .financialYear("2026-27")
                .customerType(CustomerTaxpayerType.SALARIED)
                .earlyEnquiryMessage("I need assistance with capital gains computation and overseas tax credit filing.")
                .enquiryStatus(EnquiryStatus.ACCEPTED)
                .assignedEmployeeId(practiceEmployee.getId())
                .build());
    }

    @Test
    @DisplayName("Bi-directional secure messaging between customer and practice with read receipts")
    void testBiDirectionalSecureMessaging() throws Exception {
        // Step 1: Customer sends a message on the enquiry
        SendEnquiryMessageRequest custMsg = SendEnquiryMessageRequest.builder()
                .messageBody("Hello Rajesh, I have uploaded my Form 16 and bank statements for capital gains review.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custMsg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderType").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.senderName").value("Priya Sharma"))
                .andExpect(jsonPath("$.data.isReadByCustomer").value(true))
                .andExpect(jsonPath("$.data.isReadByPractice").value(false));

        // Step 2: Practice retrieves the thread and marks read
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/lifecycle-enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + practiceAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceNumber").value("ENQ-2026-MSG1"))
                .andExpect(jsonPath("$.data.unreadCountForPractice").value(1))
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[0].messageBody", containsString("uploaded my Form 16")));

        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/{id}/messages/read", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + practiceAdminToken))
                .andExpect(status().isOk());

        // Step 3: Practice sends reply message
        SendEnquiryMessageRequest pracMsg = SendEnquiryMessageRequest.builder()
                .messageBody("Thank you Priya. We have received your documents and are computing Schedule TR and FSI.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pracMsg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderType").value("PRACTICE_USER"))
                .andExpect(jsonPath("$.data.isReadByPractice").value(true))
                .andExpect(jsonPath("$.data.isReadByCustomer").value(false));

        // Step 4: Customer checks thread, sees 2 messages, and marks read
        mockMvc.perform(get("/api/v1/marketplace/customer/enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCountForCustomer").value(1))
                .andExpect(jsonPath("$.data.messages", hasSize(2)));

        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/{id}/messages/read", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Verify unread count is now 0 for customer
        mockMvc.perform(get("/api/v1/marketplace/customer/enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCountForCustomer").value(0));
    }

    @Test
    @DisplayName("Multi-tenant isolation: unauthorized customer and practice cannot access messages")
    void testMultiTenantIsolationOnMessages() throws Exception {
        // Other customer cannot get or send messages
        mockMvc.perform(get("/api/v1/marketplace/customer/enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isNotFound());

        // Other practice cannot get or send messages
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/lifecycle-enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Terminal enquiry status prevents sending new messages")
    void testTerminalEnquiryPreventsSendingMessages() throws Exception {
        activeEnquiry.setEnquiryStatus(EnquiryStatus.CANCELLED);
        leadRepository.save(activeEnquiry);

        SendEnquiryMessageRequest msg = SendEnquiryMessageRequest.builder()
                .messageBody("Trying to send message on cancelled enquiry")
                .build();

        // Customer attempt -> Bad Request
        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msg)))
                .andExpect(status().isBadRequest());

        // Practice attempt -> Bad Request
        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/{id}/messages", activeEnquiry.getId())
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msg)))
                .andExpect(status().isBadRequest());
    }
}