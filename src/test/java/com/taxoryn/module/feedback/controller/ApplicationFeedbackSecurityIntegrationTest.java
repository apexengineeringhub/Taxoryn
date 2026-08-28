package com.taxoryn.module.feedback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationFeedbackSecurityIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private ApplicationFeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity practiceA;
    private OrganizationEntity practiceB;

    private UserEntity customerUser;
    private UserEntity practitionerA;
    private UserEntity practitionerB;
    private UserEntity employeeA;
    private UserEntity employeeB;
    private UserEntity inactiveEmployeeA;

    private String customerToken;
    private String practitionerTokenA;
    private String practitionerTokenB;
    private String employeeTokenA;
    private String employeeTokenB;
    private String inactiveEmployeeTokenA;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Roles
        RoleEntity customerRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_MARKETPLACE_CUSTOMER")
                .name("Marketplace Customer")
                .permissions(new HashSet<>())
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_ORG_ADMIN")
                .name("Practice Admin")
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_STAFF")
                .name("Practice Staff")
                .permissions(new HashSet<>())
                .build());

        // 2. Organizations / Practices
        practiceA = organizationRepository.save(OrganizationEntity.builder()
                .name("Sharma & Associates CA")
                .email("contact@sharmaca.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practiceB = organizationRepository.save(OrganizationEntity.builder()
                .name("Verma & Co CPAs")
                .email("info@vermacpa.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        // 3. Customer User
        customerUser = userRepository.save(UserEntity.builder()
                .email("customer@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Rohan")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());
        customerToken = jwtTokenProvider.generateAccessToken(customerUser.getId(), null, customerUser.getEmail(), Set.of("ROLE_MARKETPLACE_CUSTOMER"), Set.of());

        // 4. Practitioner Users
        practitionerA = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .email("admin@sharmaca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Anil")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build());
        practitionerTokenA = jwtTokenProvider.generateAccessToken(practitionerA.getId(), practiceA.getId(), practitionerA.getEmail(), Set.of("ROLE_ORG_ADMIN"), Set.of());

        practitionerB = userRepository.save(UserEntity.builder()
                .organizationId(practiceB.getId())
                .email("admin@vermacpa.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Deepak")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build());
        practitionerTokenB = jwtTokenProvider.generateAccessToken(practitionerB.getId(), practiceB.getId(), practitionerB.getEmail(), Set.of("ROLE_ORG_ADMIN"), Set.of());

        // 5. Employee Users
        employeeA = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .email("staff@sharmaca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Suresh")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());
        EmployeeEntity activeEmpA = EmployeeEntity.builder()
                .userId(employeeA.getId())
                .employeeCode("EMP-A1")
                .firstName("Suresh")
                .lastName("Patel")
                .email(employeeA.getEmail())
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        activeEmpA.setOrganizationId(practiceA.getId());
        employeeRepository.save(activeEmpA);
        employeeTokenA = jwtTokenProvider.generateAccessToken(employeeA.getId(), practiceA.getId(), employeeA.getEmail(), Set.of("ROLE_STAFF"), Set.of());

        employeeB = userRepository.save(UserEntity.builder()
                .organizationId(practiceB.getId())
                .email("staff@vermacpa.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Kavita")
                .lastName("Nair")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());
        EmployeeEntity activeEmpB = EmployeeEntity.builder()
                .userId(employeeB.getId())
                .employeeCode("EMP-B1")
                .firstName("Kavita")
                .lastName("Nair")
                .email(employeeB.getEmail())
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        activeEmpB.setOrganizationId(practiceB.getId());
        employeeRepository.save(activeEmpB);
        employeeTokenB = jwtTokenProvider.generateAccessToken(employeeB.getId(), practiceB.getId(), employeeB.getEmail(), Set.of("ROLE_STAFF"), Set.of());

        // 6. Inactive Employee User in Practice A
        inactiveEmployeeA = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .email("inactive@sharmaca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Vikram")
                .lastName("Singh")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());
        EmployeeEntity inactEmp = EmployeeEntity.builder()
                .userId(inactiveEmployeeA.getId())
                .employeeCode("EMP-A2")
                .firstName("Vikram")
                .lastName("Singh")
                .email(inactiveEmployeeA.getEmail())
                .status(EmployeeEntity.EmployeeStatus.INACTIVE)
                .build();
        inactEmp.setOrganizationId(practiceA.getId());
        employeeRepository.save(inactEmp);
        inactiveEmployeeTokenA = jwtTokenProvider.generateAccessToken(inactiveEmployeeA.getId(), practiceA.getId(), inactiveEmployeeA.getEmail(), Set.of("ROLE_STAFF"), Set.of());
    }

    @Test
    @DisplayName("Unauthenticated request to feedback endpoints is rejected with 401")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateApplicationFeedbackRequest.builder().build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer can submit feedback: sets actorType CUSTOMER, contextType CUSTOMER_PORTAL, practiceId NULL")
    void shouldAllowCustomerToSubmitFeedback() throws Exception {
        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.APPLICATION_EXPERIENCE)
                .rating(5)
                .title("Great customer onboarding")
                .description("The wizard is smooth and simple.")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Feedback-Page", "/marketplace/customer/dashboard")
                        .header("X-Feedback-Feature", "MARKETPLACE_CUSTOMER_FEEDBACK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Great customer onboarding"))
                .andExpect(jsonPath("$.message").value("Thank you for your feedback. Your input helps us improve Taxoryn."));

        List<ApplicationFeedbackEntity> feedbackList = feedbackRepository.findAll();
        assertThat(feedbackList).hasSize(1);
        ApplicationFeedbackEntity entity = feedbackList.get(0);
        assertThat(entity.getUserId()).isEqualTo(customerUser.getId());
        assertThat(entity.getActorType()).isEqualTo(ApplicationFeedbackActorType.CUSTOMER);
        assertThat(entity.getPracticeId()).isNull();
        assertThat(entity.getContextType()).isEqualTo(ApplicationFeedbackContextType.CUSTOMER_PORTAL);
        assertThat(entity.getPage()).isEqualTo("/marketplace/customer/dashboard");
        assertThat(entity.getFeature()).isEqualTo("MARKETPLACE_CUSTOMER_FEEDBACK");
    }

    @Test
    @DisplayName("Practitioner can submit feedback: automatically captures PRACTITIONER actorType, Practice A ID, and PRACTICE_PORTAL")
    void shouldAllowPractitionerToSubmitFeedback() throws Exception {
        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.EMPLOYEE_MANAGEMENT)
                .rating(4)
                .title("Bulk staff assign")
                .description("Need one-click bulk staff reassignment across clients.")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + practitionerTokenA)
                        .header("X-Feedback-Page", "/team")
                        .header("X-Feedback-Feature", "PRACTICE_PORTAL_FEEDBACK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Bulk staff assign"));

        List<ApplicationFeedbackEntity> feedbackList = feedbackRepository.findAll();
        assertThat(feedbackList).hasSize(1);
        ApplicationFeedbackEntity entity = feedbackList.get(0);
        assertThat(entity.getUserId()).isEqualTo(practitionerA.getId());
        assertThat(entity.getActorType()).isEqualTo(ApplicationFeedbackActorType.PRACTITIONER);
        assertThat(entity.getPracticeId()).isEqualTo(practiceA.getId());
        assertThat(entity.getContextType()).isEqualTo(ApplicationFeedbackContextType.PRACTICE_PORTAL);
    }

    @Test
    @DisplayName("Practice employee can submit feedback: automatically captures PRACTICE_EMPLOYEE actorType, Practice A ID, and PRACTICE_PORTAL")
    void shouldAllowPracticeEmployeeToSubmitFeedback() throws Exception {
        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.EXPERIENCE)
                .category(ApplicationFeedbackCategory.TASKS)
                .rating(5)
                .title("Daily task board")
                .description("The task board layout is very helpful for staff.")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + employeeTokenA)
                        .header("X-Feedback-Page", "/tasks")
                        .header("X-Feedback-Feature", "PRACTICE_PORTAL_FEEDBACK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Daily task board"));

        List<ApplicationFeedbackEntity> feedbackList = feedbackRepository.findAll();
        assertThat(feedbackList).hasSize(1);
        ApplicationFeedbackEntity entity = feedbackList.get(0);
        assertThat(entity.getUserId()).isEqualTo(employeeA.getId());
        assertThat(entity.getActorType()).isEqualTo(ApplicationFeedbackActorType.PRACTICE_EMPLOYEE);
        assertThat(entity.getPracticeId()).isEqualTo(practiceA.getId());
        assertThat(entity.getContextType()).isEqualTo(ApplicationFeedbackContextType.PRACTICE_PORTAL);
    }

    @Test
    @DisplayName("Inactive employee is blocked from submitting feedback")
    void shouldBlockInactiveEmployee() throws Exception {
        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.OTHER)
                .title("Inactive test")
                .description("Should not be allowed")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + inactiveEmployeeTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Only active practice employees can submit feedback")));
    }

    @Test
    @DisplayName("Category validation prevents actor from using unauthorized categories")
    void shouldValidateCategoryForActorType() throws Exception {
        // Customer trying practitioner category EMPLOYEE_MANAGEMENT -> 400
        CreateApplicationFeedbackRequest reqCust = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.EMPLOYEE_MANAGEMENT)
                .title("Customer feedback")
                .description("Invalid category")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCust)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Selected feedback category is not available for your account type")));

        // Employee trying practitioner-only BILLING category -> 400
        CreateApplicationFeedbackRequest reqEmp = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.BILLING)
                .title("Staff billing")
                .description("Invalid category for staff")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + employeeTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqEmp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Selected feedback category is not available for your account type")));
    }

    @Test
    @DisplayName("Multi-tenant isolation: Practitioner in Practice A cannot see feedback from Practice B or other practitioners")
    void shouldEnforceTenantIsolationInListing() throws Exception {
        // Feedback by Practitioner A (Practice A)
        feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(practitionerA.getId())
                .practiceId(practiceA.getId())
                .actorType(ApplicationFeedbackActorType.PRACTITIONER)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.REPORTS)
                .title("Practitioner A Feedback")
                .description("Secret practice A feedback")
                .build());

        // Feedback by Employee A (Practice A)
        feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(employeeA.getId())
                .practiceId(practiceA.getId())
                .actorType(ApplicationFeedbackActorType.PRACTICE_EMPLOYEE)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.EXPERIENCE)
                .category(ApplicationFeedbackCategory.TASKS)
                .title("Employee A Feedback")
                .description("Employee private feedback")
                .build());

        // Feedback by Practitioner B (Practice B)
        feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(practitionerB.getId())
                .practiceId(practiceB.getId())
                .actorType(ApplicationFeedbackActorType.PRACTITIONER)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.BILLING)
                .title("Practitioner B Feedback")
                .description("Practice B notes")
                .build());

        // Feedback by Customer
        feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(customerUser.getId())
                .practiceId(null)
                .actorType(ApplicationFeedbackActorType.CUSTOMER)
                .contextType(ApplicationFeedbackContextType.CUSTOMER_PORTAL)
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.APPLICATION_EXPERIENCE)
                .title("Customer Feedback")
                .description("Customer notes")
                .build());

        // Practitioner A gets own feedback in Practice A only
        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + practitionerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Practitioner A Feedback"));

        // Employee A gets own feedback in Practice A only
        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + employeeTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Employee A Feedback"));

        // Practitioner B gets own feedback in Practice B only
        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + practitionerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Practitioner B Feedback"));

        // Customer gets own customer feedback only
        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Customer Feedback"));
    }
}
