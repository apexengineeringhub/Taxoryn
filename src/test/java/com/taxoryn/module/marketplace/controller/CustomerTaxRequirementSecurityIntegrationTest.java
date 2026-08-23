package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.marketplace.dto.CreateTaxRequirementRequest;
import com.taxoryn.module.marketplace.dto.UpdateTaxRequirementRequest;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.repository.*;
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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerTaxRequirementSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    @Autowired
    private CustomerTaxRequirementRepository requirementRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity userA;
    private UserEntity userB;
    private MarketplaceCustomerProfileEntity profileA;
    private MarketplaceCustomerProfileEntity profileB;
    private String tokenA;
    private String tokenB;
    private TaxServiceCategoryEntity directTaxCategory;
    private TaxServiceEntity itrService;
    private TaxServiceEntity inactiveService;

    @BeforeEach
    void setUp() {
        requirementRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        customerProfileRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Roles
        RoleEntity customerRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_MARKETPLACE_CUSTOMER")
                .name("Marketplace Customer")
                .permissions(new HashSet<>())
                .build());

        // 2. Tax Service Category & Services
        directTaxCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("DIRECT_TAX")
                .name("Direct Tax & ITR")
                .sortOrder(1)
                .isActive(true)
                .build());

        itrService = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(directTaxCategory.getId())
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return (ITR) Filing")
                .description("Annual tax filing for individuals and businesses")
                .isActive(true)
                .sortOrder(1)
                .build());

        inactiveService = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(directTaxCategory.getId())
                .code("LEGACY_WEALTH_TAX")
                .name("Legacy Wealth Tax Assessment")
                .isActive(false)
                .sortOrder(99)
                .build());

        // 3. Customer A
        userA = userRepository.save(UserEntity.builder()
                .email("customerA@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Rahul")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        profileA = customerProfileRepository.save(MarketplaceCustomerProfileEntity.builder()
                .userId(userA.getId())
                .customerType(MarketplaceCustomerProfileEntity.CustomerType.INDIVIDUAL)
                .firstName("Rahul")
                .lastName("Sharma")
                .displayName("Rahul Sharma")
                .email("customerA@taxoryn.com")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .status(MarketplaceCustomerProfileEntity.CustomerProfileStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(userA.getId(), null, userA.getEmail(), Set.of("ROLE_MARKETPLACE_CUSTOMER"), Set.of());

        // 4. Customer B
        userB = userRepository.save(UserEntity.builder()
                .email("customerB@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Priya")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        profileB = customerProfileRepository.save(MarketplaceCustomerProfileEntity.builder()
                .userId(userB.getId())
                .customerType(MarketplaceCustomerProfileEntity.CustomerType.INDIVIDUAL)
                .firstName("Priya")
                .lastName("Patel")
                .displayName("Priya Patel")
                .email("customerB@taxoryn.com")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .status(MarketplaceCustomerProfileEntity.CustomerProfileStatus.ACTIVE)
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(userB.getId(), null, userB.getEmail(), Set.of("ROLE_MARKETPLACE_CUSTOMER"), Set.of());
    }

    @Test
    @DisplayName("Unauthenticated requests to customer tax requirements should be rejected with 401")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/customer/tax-requirements"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/customer/tax-requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateTaxRequirementRequest.builder().build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer A can create a draft requirement, update it, and submit it")
    void shouldExecuteRequirementLifecycleForOwner() throws Exception {
        CreateTaxRequirementRequest createReq = CreateTaxRequirementRequest.builder()
                .taxServiceId(itrService.getId())
                .customerType(CustomerTaxpayerType.SALARIED)
                .financialYear("FY 2025-26")
                .description("Switched jobs in October, have multiple Form 16 documents")
                .city("Bengaluru")
                .state("Karnataka")
                .build();

        // 1. Create DRAFT requirement
        String createResponse = mockMvc.perform(post("/api/v1/customer/tax-requirements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.financialYear").value("2025-26"))
                .andExpect(jsonPath("$.data.financialYearDisplay").value("FY 2025-26"))
                .andExpect(jsonPath("$.data.customerType").value("SALARIED"))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.cancellable").value(true))
                .andReturn().getResponse().getContentAsString();

        String requirementId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // 2. Update DRAFT requirement
        UpdateTaxRequirementRequest updateReq = UpdateTaxRequirementRequest.builder()
                .description("Updated: Also have foreign dividend income and capital gains")
                .financialYear("2025-26")
                .build();

        mockMvc.perform(put("/api/v1/customer/tax-requirements/" + requirementId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated: Also have foreign dividend income and capital gains"));

        // 3. Submit DRAFT requirement -> transitions to SUBMITTED
        mockMvc.perform(post("/api/v1/customer/tax-requirements/" + requirementId + "/submit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.editable").value(false))
                .andExpect(jsonPath("$.data.cancellable").value(true));

        // 4. Attempting to update SUBMITTED requirement should be rejected
        mockMvc.perform(put("/api/v1/customer/tax-requirements/" + requirementId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Only requirements in DRAFT status can be modified")));
    }

    @Test
    @DisplayName("Customer B cannot access or mutate Customer A's tax requirement (Strict IDOR protection)")
    void shouldPreventIdorAcrossCustomers() throws Exception {
        // Customer A creates draft requirement
        CustomerTaxRequirementEntity entityA = requirementRepository.save(CustomerTaxRequirementEntity.builder()
                .customerId(profileA.getId())
                .taxServiceId(itrService.getId())
                .status(TaxRequirementStatus.DRAFT)
                .customerType(CustomerTaxpayerType.SALARIED)
                .financialYear("2025-26")
                .description("Customer A private tax notes")
                .build());

        UUID reqIdA = entityA.getId();

        // Customer B attempts to GET Customer A's requirement -> 404
        mockMvc.perform(get("/api/v1/customer/tax-requirements/" + reqIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Customer B attempts to UPDATE Customer A's requirement -> 404
        UpdateTaxRequirementRequest updateReq = UpdateTaxRequirementRequest.builder()
                .description("Hacked description by B")
                .build();
        mockMvc.perform(put("/api/v1/customer/tax-requirements/" + reqIdA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Customer B attempts to SUBMIT Customer A's requirement -> 404
        mockMvc.perform(post("/api/v1/customer/tax-requirements/" + reqIdA + "/submit")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Customer B attempts to CANCEL Customer A's requirement -> 404
        mockMvc.perform(post("/api/v1/customer/tax-requirements/" + reqIdA + "/cancel")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Requirements list is strictly partitioned by customer identity")
    void shouldPartitionListByCustomer() throws Exception {
        // Customer A requirement
        requirementRepository.save(CustomerTaxRequirementEntity.builder()
                .customerId(profileA.getId())
                .taxServiceId(itrService.getId())
                .status(TaxRequirementStatus.DRAFT)
                .financialYear("2025-26")
                .build());

        // Customer B requirement
        requirementRepository.save(CustomerTaxRequirementEntity.builder()
                .customerId(profileB.getId())
                .taxServiceId(itrService.getId())
                .status(TaxRequirementStatus.SUBMITTED)
                .financialYear("2024-25")
                .build());

        // Customer A list -> 1 item (A's item only)
        mockMvc.perform(get("/api/v1/customer/tax-requirements")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].financialYear").value("2025-26"));

        // Customer B list -> 1 item (B's item only)
        mockMvc.perform(get("/api/v1/customer/tax-requirements")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].financialYear").value("2024-25"));
    }

    @Test
    @DisplayName("Should reject creating requirement for inactive tax service")
    void shouldRejectInactiveService() throws Exception {
        CreateTaxRequirementRequest request = CreateTaxRequirementRequest.builder()
                .taxServiceId(inactiveService.getId())
                .financialYear("2025-26")
                .build();

        mockMvc.perform(post("/api/v1/customer/tax-requirements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot select inactive tax service")));
    }

    @Test
    @DisplayName("Should retrieve standard financial years list")
    void shouldGetFinancialYears() throws Exception {
        mockMvc.perform(get("/api/v1/customer/tax-requirements/financial-years")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].code").isNotEmpty())
                .andExpect(jsonPath("$.data[0].label").value(startsWith("FY ")));
    }
}
