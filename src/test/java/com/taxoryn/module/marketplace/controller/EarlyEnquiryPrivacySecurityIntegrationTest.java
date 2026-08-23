package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.marketplace.dto.CreateMarketplaceLeadRequest;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EarlyEnquiryPrivacySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MarketplaceProfileRepository profileRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private CustomerTaxRequirementRepository requirementRepository;

    @Autowired
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    @Autowired
    private MarketplaceLeadRepository leadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity practiceOrg;
    private MarketplaceProfileEntity practiceProfile;
    private UserEntity practiceUser;
    private String practiceToken;

    private UserEntity customerUser;
    private MarketplaceCustomerProfileEntity customerProfile;
    private String customerToken;

    private TaxServiceCategoryEntity taxCategory;
    private TaxServiceEntity itrService;
    private CustomerTaxRequirementEntity privateRequirement;

    @BeforeEach
    void setUp() {
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ROLE_ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .name("Practice Administrator")
                        .code("ROLE_ORG_ADMIN")
                        .description("Practice Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        RoleEntity customerRole = roleRepository.findByCodeAndIsSystemRoleTrue("ROLE_MARKETPLACE_CUSTOMER").orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .name("Marketplace Customer")
                        .code("ROLE_MARKETPLACE_CUSTOMER")
                        .description("Marketplace Customer")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // 1. Setup Practice & Organization
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        practiceOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants " + suffix)
                .legalName("Apex Tax Consultants LLP " + suffix)
                .email("contact." + suffix + "@apextax.com")
                .phone("+91 98765 43210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practiceUser = userRepository.save(UserEntity.builder()
                .email("practitioner." + suffix + "@apextax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Suresh")
                .lastName("Iyer")
                .organizationId(practiceOrg.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build());

        practiceProfile = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg.getId())
                .slug("apex-tax-" + suffix)
                .displayName("Apex Tax Consultants")
                .bio("Professional Chartered Accountants")
                .isPublished(true)
                .visibilityStatus(MarketplaceProfileEntity.VisibilityStatus.PUBLIC)
                .build());

        practiceToken = jwtTokenProvider.generateAccessToken(
                practiceUser.getId(),
                practiceOrg.getId(),
                practiceUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE", "CLIENT_VIEW")
        );

        // 2. Setup Controlled Tax Service
        taxCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("DIRECT_TAX_" + suffix)
                .name("Direct Tax & ITR")
                .sortOrder(1)
                .isActive(true)
                .build());

        itrService = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(taxCategory.getId())
                .code("INCOME_TAX_RETURN_" + suffix)
                .name("Income Tax Return (ITR) Filing")
                .sortOrder(1)
                .isActive(true)
                .build());

        // 3. Setup Customer User & Private Tax Requirement
        customerUser = userRepository.save(UserEntity.builder()
                .email("vikram." + suffix + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Vikram")
                .lastName("Aditya")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        customerProfile = customerProfileRepository.save(MarketplaceCustomerProfileEntity.builder()
                .userId(customerUser.getId())
                .customerType(MarketplaceCustomerProfileEntity.CustomerType.INDIVIDUAL)
                .firstName("Vikram")
                .lastName("Aditya")
                .displayName("Vikram Aditya")
                .email("vikram.aditya@taxoryn.com")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .status(MarketplaceCustomerProfileEntity.CustomerProfileStatus.ACTIVE)
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(
                customerUser.getId(),
                null,
                customerUser.getEmail(),
                Set.of("ROLE_MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        // Private source of truth with sensitive financial details
        privateRequirement = requirementRepository.save(CustomerTaxRequirementEntity.builder()
                .customerId(customerProfile.getId())
                .taxServiceId(itrService.getId())
                .customerType(CustomerTaxpayerType.SALARIED)
                .financialYear("2025-26")
                .description("Private customer notes: My salary: 35 lakh with PAN ABCDE1234F and bank account 9876543210123. Capital gains: 8 lakh.")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .status(TaxRequirementStatus.SUBMITTED)
                .build());
    }

    @Test
    @DisplayName("Submitting public inquiry referencing private requirement should sanitize sensitive Level 3/4 data")
    void shouldSubmitInquiryAndMaskLevel3SensitiveDataFromPractice() throws Exception {
        CreateMarketplaceLeadRequest request = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .taxRequirementId(privateRequirement.getId())
                .clientName("Vikram Aditya")
                .clientEmail("vikram.aditya@taxoryn.com")
                .clientPhone("+919876543210")
                .earlyEnquiryMessage("Seeking guidance for ITR filing after job change with salary: 35 lakh and PAN ABCDE1234F.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Practice accesses Level 2 Early Enquiries API
        mockMvc.perform(get("/api/v1/practice/marketplace/enquiries")
                        .header("Authorization", "Bearer " + practiceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].service.code").value(itrService.getCode()))
                .andExpect(jsonPath("$.data.content[0].service.name").value("Income Tax Return (ITR) Filing"))
                .andExpect(jsonPath("$.data.content[0].financialYear").value("2025-26"))
                .andExpect(jsonPath("$.data.content[0].financialYearDisplay").value("FY 2025-26"))
                .andExpect(jsonPath("$.data.content[0].customerType").value("SALARIED"))
                .andExpect(jsonPath("$.data.content[0].privacyLevel").value("LEVEL_2_EARLY_ENQUIRY"))
                .andExpect(jsonPath("$.data.content[0].maskedEmail").value("v***a@taxoryn.com"))
                .andExpect(jsonPath("$.data.content[0].maskedPhone").value("+91******3210"))
                // Sensitive identifiers must be redacted:
                .andExpect(jsonPath("$.data.content[0].requirementSummary", not(containsString("ABCDE1234F"))))
                .andExpect(jsonPath("$.data.content[0].requirementSummary", not(containsString("35 lakh"))))
                .andExpect(jsonPath("$.data.content[0].requirementSummary", containsString("[PROTECTED-PAN]")))
                .andExpect(jsonPath("$.data.content[0].requirementSummary", containsString("[FINANCIAL-DISCLOSURE-PROTECTED]")));
    }

    @Test
    @DisplayName("Practice leads pipeline should mask PAN, GSTIN, and private contact info during early stage")
    void shouldMaskSensitiveDataInLeadsEndpoint() throws Exception {
        CreateMarketplaceLeadRequest request = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .taxRequirementId(privateRequirement.getId())
                .clientName("Vikram Aditya")
                .clientEmail("vikram.aditya@taxoryn.com")
                .clientPhone("+919876543210")
                .earlyEnquiryMessage("Need help with return filing.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/practice/marketplace/leads")
                        .header("Authorization", "Bearer " + practiceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].pan").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].gstin").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].clientEmail").value("v***a@taxoryn.com"))
                .andExpect(jsonPath("$.data.content[0].clientPhone").value("+91******3210"));
    }

    @Test
    @DisplayName("Unauthenticated requests to early enquiries should be rejected with 401")
    void shouldRejectUnauthenticatedEnquiriesAccess() throws Exception {
        mockMvc.perform(get("/api/v1/practice/marketplace/enquiries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Practices must not be able to directly query customer private tax requirements")
    void shouldPreventPracticeFromAccessingCustomerTaxRequirementsDirectly() throws Exception {
        mockMvc.perform(get("/api/v1/customer/tax-requirements/" + privateRequirement.getId())
                        .header("Authorization", "Bearer " + practiceToken))
                .andExpect(status().isForbidden());
    }
}
