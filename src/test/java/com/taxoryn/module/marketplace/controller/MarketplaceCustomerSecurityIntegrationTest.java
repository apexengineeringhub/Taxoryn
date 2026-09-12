package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.marketplace.dto.CreateMarketplaceLeadRequest;
import com.taxoryn.module.marketplace.dto.RegisterCustomerRequest;
import com.taxoryn.module.marketplace.dto.UpdateCustomerProfileRequest;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerProfileStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerType;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.dto.BookConsultationRequest;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity;
import com.taxoryn.module.marketplace.repository.MarketplaceConsultationRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceCustomerProfileRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceCustomerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    @Autowired
    private MarketplaceProfileRepository practiceProfileRepository;

    @Autowired
    private MarketplaceLeadRepository leadRepository;

    @Autowired
    private MarketplaceConsultationRepository consultationRepository;

    @Autowired
    private com.taxoryn.module.authentication.repository.CustomerEmailVerificationTokenRepository customerEmailVerificationTokenRepository;

    private MarketplaceProfileEntity publishedPracticeProfile;

    @BeforeEach
    void setUp() {
        OrganizationEntity org = organizationRepository.findAll().stream().findFirst().orElseGet(() -> {
            OrganizationEntity newOrg = OrganizationEntity.builder()
                    .name("Test Practice Org")
                    .legalName("Test Practice Org LLP")
                    .email("test.practice." + UUID.randomUUID() + "@taxoryn.com")
                    .pan("ABCDE1234F")
                    .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                    .subscriptionPlan(OrganizationEntity.SubscriptionPlan.PROFESSIONAL)
                    .build();
            return organizationRepository.save(newOrg);
        });

        publishedPracticeProfile = practiceProfileRepository.findByOrganizationId(org.getId()).orElseGet(() -> {
            MarketplaceProfileEntity p = MarketplaceProfileEntity.builder()
                    .organizationId(org.getId())
                    .slug("test-practice-" + UUID.randomUUID().toString().substring(0, 6))
                    .displayName("Test Practice Tax Experts")
                    .bio("Experienced Tax and GST Consultants")
                    .city("Delhi")
                    .state("Delhi")
                    .isPublished(true)
                    .visibilityStatus(VisibilityStatus.PUBLIC)
                    .build();
            return practiceProfileRepository.save(p);
        });
    }

    @Test
    @DisplayName("Scenario 1: Customer registers successfully, becomes immediately authenticated with tokens & cookie, manages profile, and accesses customer dashboard")
    void testCustomerLifecycle_Success() throws Exception {
        String testEmail = "cust.test." + System.currentTimeMillis() + "@taxoryn.test";

        RegisterCustomerRequest registerReq = RegisterCustomerRequest.builder()
                .firstName("Aditi")
                .lastName("Sharma")
                .email(testEmail)
                .phone("9876543210")
                .password("SecureCustPass123!")
                .customerType(CustomerType.INDIVIDUAL)
                .city("Gurgaon")
                .state("Haryana")
                .pincode("122001")
                .preferredLanguage("English")
                .build();

        // 1. Register Customer API (created in ACTIVE status with immediate JWT and HttpOnly refresh cookie)
        MvcResult registerResult = mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.customer.email").value(testEmail))
                .andExpect(jsonPath("$.data.customer.displayName").value("Aditi Sharma"))
                .andExpect(jsonPath("$.data.customer.profileCompleteness.percentage").value(100))
                .andReturn();

        String directAuthToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Verify account is immediately active in database
        UserEntity registeredUser = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(registeredUser.getStatus()).isEqualTo(UserEntity.UserStatus.ACTIVE);

        // 2. Access with direct token returned from registration
        String loginToken = directAuthToken;

        // 3. Get Customer Profile
        mockMvc.perform(get("/api/v1/marketplace/customer/profile")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testEmail))
                .andExpect(jsonPath("$.data.displayName").value("Aditi Sharma"));

        // 4. Update Customer Profile
        UpdateCustomerProfileRequest updateReq = UpdateCustomerProfileRequest.builder()
                .displayName("Aditi S. (Corporate)")
                .customerType(CustomerType.BUSINESS)
                .businessName("Sharma Tech Labs")
                .city("Noida")
                .state("Uttar Pradesh")
                .pincode("201301")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/customer/profile")
                        .header("Authorization", "Bearer " + loginToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Aditi S. (Corporate)"))
                .andExpect(jsonPath("$.data.customerType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.businessName").value("Sharma Tech Labs"))
                .andExpect(jsonPath("$.data.city").value("Noida"));

        // 5. Submit Public Lead while authenticated as Customer (attaches customerId)
        CreateMarketplaceLeadRequest leadReq = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(publishedPracticeProfile.getId())
                .clientName("Aditi Sharma")
                .clientEmail(testEmail)
                .clientPhone("9876543210")
                .city("Noida")
                .serviceCategory("ITR Filing")
                .requirementDescription("Need urgent corporate filing advice")
                .build();

        MvcResult leadResult = mockMvc.perform(post("/api/v1/marketplace/leads")
                        .header("Authorization", "Bearer " + loginToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String leadIdStr = objectMapper.readTree(leadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MarketplaceLeadEntity savedLead = leadRepository.findById(UUID.fromString(leadIdStr)).orElseThrow();
        assertThat(savedLead.getCustomerId()).isNotNull();

        // 6. Access Customer Dashboard
        mockMvc.perform(get("/api/v1/marketplace/customer/dashboard")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRequests").value(1))
                .andExpect(jsonPath("$.data.recentLeads[0].serviceCategory").value("ITR Filing"));

        // 7. Access Customer Leads Endpoint
        mockMvc.perform(get("/api/v1/marketplace/customer/leads")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(leadIdStr));
    }

    @Test
    @DisplayName("Scenario 2: Unauthenticated user cannot access customer profile or dashboard")
    void testUnauthenticatedCustomerEndpoints_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/customer/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/marketplace/customer/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/marketplace/customer/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 3: Customer cannot access practice management internal endpoints")
    void testCustomerCannotAccessPracticeInternalApis() throws Exception {
        String testEmail = "cust.practice.barrier." + System.currentTimeMillis() + "@taxoryn.test";

        RegisterCustomerRequest registerReq = RegisterCustomerRequest.builder()
                .firstName("Karan")
                .email(testEmail)
                .password("SecureCustPass123!")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Activate customer
        UserEntity registeredUser = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        registeredUser.setStatus(UserEntity.UserStatus.ACTIVE);
        userRepository.save(registeredUser);
        customerProfileRepository.findByUserId(registeredUser.getId()).ifPresent(p -> {
            p.setStatus(CustomerProfileStatus.ACTIVE);
            customerProfileRepository.save(p);
        });

        // Log in to get customer JWT
        LoginRequest loginReq = LoginRequest.builder()
                .email(testEmail)
                .password("SecureCustPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Attempt to access practice client management API as a customer
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // Attempt to access practice billing API as a customer
        mockMvc.perform(get("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // Attempt to access practice GST profiles API as a customer
        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 4: Book Consultation preserves authenticated customer ownership and prevents double booking of same slot")
    void testBookConsultation_AtomicSlotRevalidation_And_PreventDoubleBooking() throws Exception {
        String testEmail = "cust.booking." + System.currentTimeMillis() + "@taxoryn.test";

        RegisterCustomerRequest registerReq = RegisterCustomerRequest.builder()
                .firstName("Aditya")
                .lastName("Kapoor")
                .email(testEmail)
                .phone("9876501234")
                .password("SecureCustPass123!")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest loginReq = LoginRequest.builder()
                .email(testEmail)
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        java.time.LocalDate bookingDate = java.time.LocalDate.now().plusDays(5);
        String slotTime = "15:00";

        BookConsultationRequest bookingReq = BookConsultationRequest.builder()
                .marketplaceProfileId(publishedPracticeProfile.getId())
                .clientName("Aditya Kapoor")
                .clientEmail(testEmail)
                .clientPhone("9876501234")
                .topic("Startup Angel Tax Exemption Advisory")
                .consultationMode(MarketplaceConsultationEntity.ConsultationMode.VIDEO)
                .bookingDate(bookingDate)
                .startTime(slotTime)
                .endTime("15:30")
                .notes("Need advice regarding section 56(2)(viib)")
                .build();

        // 1. Initial booking succeeds with authenticated customer token
        MvcResult bookResult = mockMvc.perform(post("/api/v1/marketplace/consultations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientName").value("Aditya Kapoor"))
                .andExpect(jsonPath("$.data.startTime").value(slotTime))
                .andReturn();

        UUID bookingId = UUID.fromString(objectMapper.readTree(bookResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Verify IDOR ownership derived from SecurityContext
        MarketplaceConsultationEntity consultationInDb = consultationRepository.findById(bookingId).orElseThrow();
        assertThat(consultationInDb.getCustomerId()).isEqualTo(user.getId());
        assertThat(consultationInDb.getClientEmail()).isEqualTo(testEmail);

        // 2. Second booking for the exact same slot & date must be rejected by atomic slot revalidation
        BookConsultationRequest conflictingBooking = BookConsultationRequest.builder()
                .marketplaceProfileId(publishedPracticeProfile.getId())
                .clientName("Another Customer")
                .clientEmail("another@taxoryn.test")
                .clientPhone("9811002233")
                .topic("General Tax Discussion")
                .consultationMode(MarketplaceConsultationEntity.ConsultationMode.VIDEO)
                .bookingDate(bookingDate)
                .startTime(slotTime)
                .endTime("15:30")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("This slot is no longer available")));
    }

    @Test
    @DisplayName("Scenario 5: Slot Availability endpoint accurately reflects booked vs available slots")
    void testGetProfileSlotAvailability_ReturnsAccurateAvailability() throws Exception {
        java.time.LocalDate bookingDate = java.time.LocalDate.now().plusDays(7);

        // Query initial availability
        MvcResult initResult = mockMvc.perform(get("/api/v1/marketplace/profiles/" + publishedPracticeProfile.getId() + "/availability")
                        .param("date", bookingDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableSlots").isArray())
                .andReturn();

        assertThat(initResult.getResponse().getContentAsString()).contains("11:30");

        // Book slot 11:30
        BookConsultationRequest bookingReq = BookConsultationRequest.builder()
                .marketplaceProfileId(publishedPracticeProfile.getId())
                .clientName("Slot Tester")
                .clientEmail("slot.tester@taxoryn.test")
                .clientPhone("9899001122")
                .topic("Income Tax Notice")
                .consultationMode(MarketplaceConsultationEntity.ConsultationMode.VIDEO)
                .bookingDate(bookingDate)
                .startTime("11:30")
                .endTime("12:00")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated());

        // Query availability again: 11:30 must be in bookedSlots and NOT in availableSlots
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + publishedPracticeProfile.getId() + "/availability")
                        .param("date", bookingDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookedSlots[0]").value("11:30"))
                .andExpect(jsonPath("$.data.availableSlots", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("11:30"))))
                .andExpect(jsonPath("$.data.availableSlots", org.hamcrest.Matchers.hasItem("10:00")));
    }
}
