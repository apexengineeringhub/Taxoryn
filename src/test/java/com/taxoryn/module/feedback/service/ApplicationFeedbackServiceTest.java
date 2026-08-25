package com.taxoryn.module.feedback.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.feedback.dto.ApplicationFeedbackDto;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationFeedbackServiceTest {

    @Mock
    private ApplicationFeedbackRepository feedbackRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ApplicationFeedbackServiceImpl service;

    private UUID userId;
    private UUID practiceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        practiceId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(UUID uId, UUID orgId, Set<String> roles) {
        SecurityUser user = SecurityUser.builder()
                .userId(uId)
                .organizationId(orgId)
                .email("user@taxoryn.com")
                .password("unused")
                .enabled(true)
                .roles(roles)
                .permissions(Set.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // ==========================================
    // 1. CUSTOMER FEEDBACK TESTS
    // ==========================================

    @Test
    @DisplayName("Customer can submit feedback: sets CUSTOMER actorType, CUSTOMER_PORTAL context, and null practiceId")
    void createsCustomerFeedbackSuccessfully() {
        authenticateUser(userId, null, Set.of("ROLE_MARKETPLACE_CUSTOMER"));

        when(feedbackRepository.save(any(ApplicationFeedbackEntity.class))).thenAnswer(invocation -> {
            ApplicationFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.PERFORMANCE)
                .rating(2)
                .title("  Search is slow  ")
                .description("  A description that must never be copied to application logs.  ")
                .build();

        ApplicationFeedbackDto result = service.createFeedback(request, "/marketplace/customer/feedback", "MARKETPLACE_CUSTOMER_FEEDBACK");

        ArgumentCaptor<ApplicationFeedbackEntity> feedbackCaptor = ArgumentCaptor.forClass(ApplicationFeedbackEntity.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        ApplicationFeedbackEntity saved = feedbackCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getActorType()).isEqualTo(ApplicationFeedbackActorType.CUSTOMER);
        assertThat(saved.getPracticeId()).isNull();
        assertThat(saved.getContextType()).isEqualTo(ApplicationFeedbackContextType.CUSTOMER_PORTAL);
        assertThat(saved.getStatus()).isEqualTo(ApplicationFeedbackStatus.NEW);
        assertThat(saved.getPriority()).isEqualTo(ApplicationFeedbackPriority.MEDIUM);
        assertThat(saved.getTitle()).isEqualTo("Search is slow");
        assertThat(saved.getDescription()).isEqualTo("A description that must never be copied to application logs.");
        assertThat(result).extracting(ApplicationFeedbackDto::getTitle).isEqualTo("Search is slow");

        verify(auditService).logEvent(eq("APPLICATION_FEEDBACK_CREATED"), eq("APPLICATION_FEEDBACK"), anyString(), isNull(),
                argThat(value -> !String.valueOf(value).contains("A description that must never be copied to application logs.")));
    }

    @Test
    @DisplayName("Customer cannot submit feedback with practitioner-only category")
    void rejectsInvalidCategoryForCustomer() {
        authenticateUser(userId, null, Set.of("ROLE_MARKETPLACE_CUSTOMER"));

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.EMPLOYEE_MANAGEMENT) // practitioner category, not allowed for customer
                .title("Add bulk invite")
                .description("Need employee management bulk invite")
                .build();

        assertThatThrownBy(() -> service.createFeedback(request, "/customer/feedback", "CUSTOMER_FEEDBACK"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Selected feedback category is not available for your account type");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Customer can list their own feedback partitioned by userId")
    void listsCustomerFeedback() {
        authenticateUser(userId, null, Set.of("ROLE_MARKETPLACE_CUSTOMER"));
        Pageable pageable = PageRequest.of(0, 10);

        ApplicationFeedbackEntity entity = ApplicationFeedbackEntity.builder()
                .userId(userId)
                .actorType(ApplicationFeedbackActorType.CUSTOMER)
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.APPLICATION_EXPERIENCE)
                .title("Great UX")
                .description("Love the platform")
                .build();
        entity.setId(UUID.randomUUID());

        when(feedbackRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PagedResponse<ApplicationFeedbackDto> response = service.getMyFeedback(pageable);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Great UX");
        verify(feedbackRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
    }

    // ==========================================
    // 2. PRACTITIONER FEEDBACK TESTS
    // ==========================================

    @Test
    @DisplayName("Practitioner can submit feedback: sets PRACTITIONER actorType, PRACTICE_PORTAL context, and practiceId from security context")
    void createsPractitionerFeedbackSuccessfully() {
        authenticateUser(userId, practiceId, Set.of("ROLE_ORG_ADMIN"));

        when(feedbackRepository.save(any(ApplicationFeedbackEntity.class))).thenAnswer(invocation -> {
            ApplicationFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.EMPLOYEE_MANAGEMENT)
                .rating(5)
                .title("Enhanced Staff Roles")
                .description("Please add custom RBAC permissions for article assistants")
                .build();

        ApplicationFeedbackDto result = service.createFeedback(request, "/team", "PRACTICE_PORTAL_FEEDBACK");

        ArgumentCaptor<ApplicationFeedbackEntity> feedbackCaptor = ArgumentCaptor.forClass(ApplicationFeedbackEntity.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        ApplicationFeedbackEntity saved = feedbackCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getActorType()).isEqualTo(ApplicationFeedbackActorType.PRACTITIONER);
        assertThat(saved.getPracticeId()).isEqualTo(practiceId);
        assertThat(saved.getContextType()).isEqualTo(ApplicationFeedbackContextType.PRACTICE_PORTAL);
        assertThat(saved.getStatus()).isEqualTo(ApplicationFeedbackStatus.NEW);
        assertThat(saved.getTitle()).isEqualTo("Enhanced Staff Roles");
        assertThat(result.getTitle()).isEqualTo("Enhanced Staff Roles");

        verify(auditService).logEvent(eq("APPLICATION_FEEDBACK_CREATED"), eq("APPLICATION_FEEDBACK"), anyString(), isNull(),
                contains("actorType=PRACTITIONER"));
    }

    @Test
    @DisplayName("Practitioner with PARTNER role is recognized as PRACTITIONER actor")
    void recognizesPartnerRoleAsPractitioner() {
        authenticateUser(userId, practiceId, Set.of("PARTNER"));

        when(feedbackRepository.save(any(ApplicationFeedbackEntity.class))).thenAnswer(invocation -> {
            ApplicationFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.BILLING)
                .title("Billing statements")
                .description("Would be great to export in PDF")
                .build();

        service.createFeedback(request, "/billing", "BILLING_FEEDBACK");

        ArgumentCaptor<ApplicationFeedbackEntity> feedbackCaptor = ArgumentCaptor.forClass(ApplicationFeedbackEntity.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getActorType()).isEqualTo(ApplicationFeedbackActorType.PRACTITIONER);
        assertThat(feedbackCaptor.getValue().getPracticeId()).isEqualTo(practiceId);
    }

    @Test
    @DisplayName("Practitioner cannot submit feedback with customer-only category")
    void rejectsInvalidCategoryForPractitioner() {
        authenticateUser(userId, practiceId, Set.of("ROLE_ORG_ADMIN"));

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.REQUIREMENT) // Customer category, not in practitioner allowed categories
                .title("Requirement error")
                .description("Invalid category for practice")
                .build();

        assertThatThrownBy(() -> service.createFeedback(request, "/feedback", "FEEDBACK"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Selected feedback category is not available for your account type");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Practitioner listing feedback filters by userId AND practiceId")
    void listsPractitionerFeedbackFilteredByPractice() {
        authenticateUser(userId, practiceId, Set.of("ROLE_ORG_ADMIN"));
        Pageable pageable = PageRequest.of(0, 10);

        ApplicationFeedbackEntity entity = ApplicationFeedbackEntity.builder()
                .userId(userId)
                .practiceId(practiceId)
                .actorType(ApplicationFeedbackActorType.PRACTITIONER)
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.DOCUMENTS)
                .title("Bulk document download")
                .description("Faster bulk download needed")
                .build();
        entity.setId(UUID.randomUUID());

        when(feedbackRepository.findByUserIdAndPracticeIdOrderByCreatedAtDesc(eq(userId), eq(practiceId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PagedResponse<ApplicationFeedbackDto> response = service.getMyFeedback(pageable);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Bulk document download");
        verify(feedbackRepository).findByUserIdAndPracticeIdOrderByCreatedAtDesc(eq(userId), eq(practiceId), eq(pageable));
    }

    // ==========================================
    // 3. PRACTICE EMPLOYEE FEEDBACK TESTS
    // ==========================================

    @Test
    @DisplayName("Active practice employee can submit feedback: sets PRACTICE_EMPLOYEE actorType, PRACTICE_PORTAL context, and practiceId from membership")
    void createsEmployeeFeedbackSuccessfully() {
        authenticateUser(userId, practiceId, Set.of("ROLE_STAFF"));

        EmployeeEntity employee = EmployeeEntity.builder()
                .userId(userId)
                .employeeCode("EMP001")
                .firstName("Ankit")
                .lastName("Verma")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        employee.setOrganizationId(practiceId);

        when(employeeRepository.findByOrganizationIdAndUserId(eq(practiceId), eq(userId)))
                .thenReturn(Optional.of(employee));

        when(feedbackRepository.save(any(ApplicationFeedbackEntity.class))).thenAnswer(invocation -> {
            ApplicationFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.EXPERIENCE)
                .category(ApplicationFeedbackCategory.TASKS)
                .rating(4)
                .title("Task checklist workflow")
                .description("The checklist feature saves a lot of time daily.")
                .build();

        ApplicationFeedbackDto result = service.createFeedback(request, "/tasks", "PRACTICE_PORTAL_FEEDBACK");

        ArgumentCaptor<ApplicationFeedbackEntity> feedbackCaptor = ArgumentCaptor.forClass(ApplicationFeedbackEntity.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        ApplicationFeedbackEntity saved = feedbackCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getActorType()).isEqualTo(ApplicationFeedbackActorType.PRACTICE_EMPLOYEE);
        assertThat(saved.getPracticeId()).isEqualTo(practiceId);
        assertThat(saved.getContextType()).isEqualTo(ApplicationFeedbackContextType.PRACTICE_PORTAL);
        assertThat(saved.getStatus()).isEqualTo(ApplicationFeedbackStatus.NEW);
        assertThat(saved.getTitle()).isEqualTo("Task checklist workflow");
        assertThat(result.getTitle()).isEqualTo("Task checklist workflow");

        verify(auditService).logEvent(eq("APPLICATION_FEEDBACK_CREATED"), eq("APPLICATION_FEEDBACK"), anyString(), isNull(),
                contains("actorType=PRACTICE_EMPLOYEE"));
    }

    @Test
    @DisplayName("Inactive employee cannot submit feedback")
    void rejectsInactiveEmployeeFeedback() {
        authenticateUser(userId, practiceId, Set.of("ROLE_STAFF"));

        EmployeeEntity employee = EmployeeEntity.builder()
                .userId(userId)
                .employeeCode("EMP001")
                .status(EmployeeEntity.EmployeeStatus.INACTIVE)
                .build();
        employee.setOrganizationId(practiceId);

        when(employeeRepository.findByOrganizationIdAndUserId(eq(practiceId), eq(userId)))
                .thenReturn(Optional.of(employee));

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.TASKS)
                .title("Feedback")
                .description("Some text")
                .build();

        assertThatThrownBy(() -> service.createFeedback(request, "/tasks", "TASKS"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Only active practice employees can submit feedback");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("User not belonging to practice employee roster is rejected")
    void rejectsNonMemberEmployeeFeedback() {
        authenticateUser(userId, practiceId, Set.of("ROLE_STAFF"));

        when(employeeRepository.findByOrganizationIdAndUserId(eq(practiceId), eq(userId)))
                .thenReturn(Optional.empty());

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.TASKS)
                .title("Feedback")
                .description("Some text")
                .build();

        assertThatThrownBy(() -> service.createFeedback(request, "/tasks", "TASKS"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Authenticated user is not a member of this practice");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Employee cannot submit feedback with practitioner-only category")
    void rejectsInvalidCategoryForEmployee() {
        authenticateUser(userId, practiceId, Set.of("ROLE_STAFF"));

        EmployeeEntity employee = EmployeeEntity.builder()
                .userId(userId)
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        employee.setOrganizationId(practiceId);

        when(employeeRepository.findByOrganizationIdAndUserId(eq(practiceId), eq(userId)))
                .thenReturn(Optional.of(employee));

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.BILLING) // Practitioner category, not employee category
                .title("Billing issue")
                .description("Staff does not have billing category")
                .build();

        assertThatThrownBy(() -> service.createFeedback(request, "/feedback", "FEEDBACK"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Selected feedback category is not available for your account type");

        verify(feedbackRepository, never()).save(any());
    }

    // ==========================================
    // 4. SECURITY & TENANT ISOLATION TESTS
    // ==========================================

    @Test
    @DisplayName("User without organizationId and without customer role is rejected")
    void rejectsUserWithoutOrgOrCustomerRole() {
        authenticateUser(userId, null, Set.of("ROLE_STAFF"));

        CreateApplicationFeedbackRequest request = CreateApplicationFeedbackRequest.builder()
                .type(ApplicationFeedbackType.GENERAL)
                .category(ApplicationFeedbackCategory.OTHER)
                .title("General question")
                .description("Test")
                .build();

        assertThatThrownBy(() -> service.createFeedback(request, "/feedback", "FEEDBACK"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Feedback is available only to an authenticated customer or practice member");

        verify(feedbackRepository, never()).save(any());
    }
}
