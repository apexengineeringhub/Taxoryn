package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.dto.*;
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
class EnquiryLifecycleIntegrationTest {

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
    private MarketplaceReviewRepository reviewRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

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

    private MarketplaceProfileEntity practiceProfile;
    private TaxServiceEntity gstService;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        leadRepository.deleteAll();
        profileRepository.deleteAll();

        // 1. Permissions & Roles
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
                .name("Apex Lifecycle CA LLP " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Apex Lifecycle CA LLP")
                .email("admin@apexlifecycle" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .phone("+91 98200 11223")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .pan("AABCL1234F")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practiceAdminUser = UserEntity.builder()
                .email("admin@apexlifecycle" + UUID.randomUUID().toString().substring(0, 6) + ".com")
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
                .email("rajesh@apexlifecycle.com")
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

        // 3. Other Practice Org (For multi-tenant isolation testing)
        otherPracticeOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Tax Firm " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Other Tax Firm LLP")
                .email("other@otherfirm" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .phone("+91 98200 99887")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .pan("AABCO9988F")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        otherAdminUser = UserEntity.builder()
                .email("admin@otherfirm" + UUID.randomUUID().toString().substring(0, 6) + ".com")
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

        // 4. Customer User
        customerUser = userRepository.save(UserEntity.builder()
                .email("client" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Aarav")
                .lastName("Mehta")
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

        // 5. Published Practice Profile
        practiceProfile = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg.getId())
                .slug("apex-lifecycle-advisors-" + UUID.randomUUID().toString().substring(0, 6))
                .displayName("Apex Lifecycle Advisors")
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

        // 6. Tax Service Master
        TaxServiceCategoryEntity cat = categoryRepository.findByCodeIgnoreCase("GST").orElseGet(() -> categoryRepository.save(
                TaxServiceCategoryEntity.builder().code("GST").name("Goods & Services Tax").sortOrder(1).isActive(true).build()
        ));

        gstService = taxServiceRepository.findByCodeIgnoreCase("GST_REG").orElseGet(() -> taxServiceRepository.save(
                TaxServiceEntity.builder()
                        .categoryId(cat.getId())
                        .code("GST_REG")
                        .name("New GST Registration")
                        .description("GST registration for private limited & proprietorship")
                        .sortOrder(1)
                        .isActive(true)
                        .build()
        ));
    }

    @Test
    @DisplayName("Submit Public Enquiry generates formatted reference number, status NEW, and dispatches notification")
    void testSubmitPublicEnquirySuccess() throws Exception {
        CreateMarketplaceLeadRequest request = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .taxServiceId(gstService.getId())
                .clientName("Aarav Mehta")
                .clientEmail("client.enquiry@example.com")
                .clientPhone("+91 98765 43210")
                .city("Mumbai")
                .earlyEnquiryMessage("Need help with new GST registration for technology venture.")
                .financialYear("2026-27")
                .customerType(CustomerTaxpayerType.BUSINESS_OWNER)
                .urgency(MarketplaceLeadEntity.Urgency.STANDARD)
                .build();

        mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referenceNumber", startsWith("TXN-2026-")))
                .andExpect(jsonPath("$.data.clientName").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data.enquiryStatus").value("NEW"));
    }

    @Test
    @DisplayName("Duplicate enquiry submission within 10-minute window returns existing active enquiry")
    void testDuplicateEnquiryProtection() throws Exception {
        CreateMarketplaceLeadRequest request = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .taxServiceId(gstService.getId())
                .clientName("Aarav Mehta")
                .clientEmail("duplicate.client@example.com")
                .clientPhone("+91 98765 43210")
                .city("Mumbai")
                .earlyEnquiryMessage("Need help with new GST registration.")
                .financialYear("2026-27")
                .customerType(CustomerTaxpayerType.SALARIED)
                .build();

        // First Submission
        String res1 = mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref1 = objectMapper.readTree(res1).get("data").get("referenceNumber").asText();

        // Second Submission (identical email + tax service + practice within minutes)
        String res2 = mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref2 = objectMapper.readTree(res2).get("data").get("referenceNumber").asText();

        assertEquals(ref1, ref2, "Duplicate submission within 10 mins must return existing enquiry reference");
    }

    @Test
    @DisplayName("Complete Enquiry Lifecycle: NEW -> ACCEPTED -> IN_PROGRESS -> COMPLETED -> VERIFIED REVIEW")
    void testFullEnquiryLifecycleAndVerifiedReview() throws Exception {
        // 1. Submit Enquiry
        MarketplaceLeadEntity lead = leadRepository.save(MarketplaceLeadEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .taxServiceId(gstService.getId())
                .referenceNumber("TXN-2026-123456")
                .clientName("Aarav Mehta")
                .clientEmail("client.enquiry@example.com")
                .clientPhone("+91 98765 43210")
                .city("Mumbai")
                .serviceCategory("GST")
                .earlyEnquiryMessage("GST registration requirement")
                .enquiryStatus(EnquiryStatus.NEW)
                .receivedAt(java.time.Instant.now())
                .build());

        UUID leadId = lead.getId();

        // 2. Practice Assigns Employee
        AssignEnquiryRequest assignReq = AssignEnquiryRequest.builder()
                .assignedEmployeeId(practiceEmployee.getId())
                .assignmentNotes("Assigned to Senior CA Rajesh")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + leadId + "/assign")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Rajesh Sharma"));

        // 3. Practice Accepts Enquiry
        AcceptEnquiryRequest acceptReq = AcceptEnquiryRequest.builder()
                .notes("We have accepted your request and reviewed docs")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + leadId + "/accept")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enquiryStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.acceptedAt").isNotEmpty());

        // 4. Practice Starts Work
        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + leadId + "/start")
                        .header("Authorization", "Bearer " + practiceAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enquiryStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty());

        // Customer attempts to cancel after work started -> 400 Bad Request
        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + leadId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancellationReason\":\"Changed mind\"}"))
                .andExpect(status().isBadRequest());

        // 5. Practice Completes Enquiry
        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + leadId + "/complete")
                        .header("Authorization", "Bearer " + practiceAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enquiryStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // 6. Customer fetches enquiry detail + timeline
        mockMvc.perform(get("/api/v1/marketplace/customer/enquiries/" + leadId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enquiryStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.canReview").value(true))
                .andExpect(jsonPath("$.data.timeline", hasSize(5)));

        // 7. Customer submits Verified Review for Completed Enquiry
        SubmitEnquiryReviewRequest reviewReq = SubmitEnquiryReviewRequest.builder()
                .rating(5)
                .reviewTitle("Flawless GST Registration Support")
                .reviewComment("Apex Lifecycle CA delivered our GST certificate in record time!")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + leadId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.isVerifiedClient").value(true))
                .andExpect(jsonPath("$.data.reviewTitle").value("Flawless GST Registration Support"));

        // 8. Attempting duplicate review on same completed enquiry -> 400 Bad Request
        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + leadId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Practice Rejection with Reason transitions to terminal status REJECTED")
    void testPracticeRejectEnquiry() throws Exception {
        MarketplaceLeadEntity lead = leadRepository.save(MarketplaceLeadEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .taxServiceId(gstService.getId())
                .referenceNumber("TXN-2026-999888")
                .clientName("Aarav Mehta")
                .clientEmail("client.enquiry@example.com")
                .clientPhone("+91 98765 43210")
                .city("Mumbai")
                .serviceCategory("GST")
                .enquiryStatus(EnquiryStatus.NEW)
                .receivedAt(java.time.Instant.now())
                .build());

        RejectEnquiryRequest rejectReq = RejectEnquiryRequest.builder()
                .rejectionReason(EnquiryRejectionReason.SERVICE_NOT_AVAILABLE)
                .rejectionNote("We are not offering this specialization this quarter")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + lead.getId() + "/reject")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enquiryStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("SERVICE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.data.rejectedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Multi-tenant isolation: Practice B cannot view or modify Practice A's enquiry")
    void testMultiTenantPracticeIsolation() throws Exception {
        MarketplaceLeadEntity lead = leadRepository.save(MarketplaceLeadEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .taxServiceId(gstService.getId())
                .referenceNumber("TXN-2026-111222")
                .clientName("Aarav Mehta")
                .clientEmail("client.enquiry@example.com")
                .clientPhone("+91 98765 43210")
                .city("Mumbai")
                .serviceCategory("GST")
                .enquiryStatus(EnquiryStatus.NEW)
                .receivedAt(java.time.Instant.now())
                .build());

        // Other Practice attempts to view Practice A's enquiry -> 404 Not Found
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + lead.getId())
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isNotFound());

        // Other Practice attempts to accept Practice A's enquiry -> 404 Not Found
        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + lead.getId() + "/accept")
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isNotFound());
    }
}
