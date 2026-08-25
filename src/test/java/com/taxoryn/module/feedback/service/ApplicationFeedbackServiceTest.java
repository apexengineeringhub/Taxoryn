package com.taxoryn.module.feedback.service;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.feedback.dto.ApplicationFeedbackDto;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationFeedbackServiceTest {

    @Mock
    private ApplicationFeedbackRepository feedbackRepository;
    @Mock
    private AuditService auditService;

    private ApplicationFeedbackServiceImpl service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ApplicationFeedbackServiceImpl(feedbackRepository, auditService);
        userId = UUID.randomUUID();
        SecurityUser user = SecurityUser.builder()
                .userId(userId).email("customer@taxoryn.com").password("unused").enabled(true)
                .roles(Set.of("ROLE_MARKETPLACE_CUSTOMER")).permissions(Set.of()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPlatformFeedbackWithInternalDefaultsAndSafeAuditMetadata() {
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

        ArgumentCaptor<ApplicationFeedbackEntity> feedback = ArgumentCaptor.forClass(ApplicationFeedbackEntity.class);
        verify(feedbackRepository).save(feedback.capture());
        assertThat(feedback.getValue().getUserId()).isEqualTo(userId);
        assertThat(feedback.getValue().getStatus()).isEqualTo(ApplicationFeedbackStatus.NEW);
        assertThat(feedback.getValue().getPriority()).isEqualTo(ApplicationFeedbackPriority.MEDIUM);
        assertThat(feedback.getValue().getTitle()).isEqualTo("Search is slow");
        assertThat(feedback.getValue().getDescription()).isEqualTo("A description that must never be copied to application logs.");
        assertThat(result).extracting(ApplicationFeedbackDto::getTitle).isEqualTo("Search is slow");

        verify(auditService).logEvent(eq("APPLICATION_FEEDBACK_CREATED"), eq("APPLICATION_FEEDBACK"), anyString(), isNull(),
                argThat(value -> !String.valueOf(value).contains("A description that must never be copied to application logs.")));
    }
}
