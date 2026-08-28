package com.taxoryn.module.feedback.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.feedback.dto.ApplicationFeedbackDto;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationFeedbackServiceImpl implements ApplicationFeedbackService {

    private final ApplicationFeedbackRepository feedbackRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ApplicationFeedbackDto createFeedback(CreateApplicationFeedbackRequest request, String page, String feature) {
        FeedbackActorContext actor = resolveActorContext();
        validateCategoryForActor(request.getCategory(), actor.actorType());
        ApplicationFeedbackEntity feedback = ApplicationFeedbackEntity.builder()
                .userId(actor.userId())
                .actorType(actor.actorType())
                .practiceId(actor.practiceId())
                .contextType(actor.contextType())
                .type(request.getType())
                .category(request.getCategory())
                .rating(request.getRating())
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .page(normalizeContext(page, 500))
                .feature(normalizeContext(feature, 100))
                .source("WEB")
                .status(ApplicationFeedbackStatus.NEW)
                .priority(ApplicationFeedbackPriority.MEDIUM)
                .build();

        ApplicationFeedbackEntity saved = feedbackRepository.save(feedback);
        // Deliberately log metadata only: feedback text can contain personal information.
        auditService.logEvent("APPLICATION_FEEDBACK_CREATED", "APPLICATION_FEEDBACK", saved.getId().toString(), null,
                "Application feedback submitted (actorType=" + saved.getActorType() + ", type=" + saved.getType() + ", category=" + saved.getCategory() + ")");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationFeedbackDto> getMyFeedback(Pageable pageable) {
        FeedbackActorContext actor = resolveActorContext();
        if (actor.practiceId() == null) {
            return PagedResponse.of(feedbackRepository.findByUserIdOrderByCreatedAtDesc(actor.userId(), pageable), this::toDto);
        }
        return PagedResponse.of(feedbackRepository.findByUserIdAndPracticeIdOrderByCreatedAtDesc(actor.userId(), actor.practiceId(), pageable), this::toDto);
    }

    private ApplicationFeedbackDto toDto(ApplicationFeedbackEntity feedback) {
        return ApplicationFeedbackDto.builder()
                .id(feedback.getId()).type(feedback.getType()).category(feedback.getCategory())
                .rating(feedback.getRating()).title(feedback.getTitle()).description(feedback.getDescription())
                .createdAt(feedback.getCreatedAt()).build();
    }

    private String normalizeContext(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private FeedbackActorContext resolveActorContext() {
        SecurityUser user = SecurityUtils.requireCurrentUser();
        Set<String> roles = user.getRoles() == null ? Set.of() : user.getRoles();
        if (hasRole(roles, "MARKETPLACE_CUSTOMER")) {
            return new FeedbackActorContext(user.getUserId(), ApplicationFeedbackActorType.CUSTOMER, null, ApplicationFeedbackContextType.CUSTOMER_PORTAL);
        }
        UUID practiceId = user.getOrganizationId();
        if (practiceId == null) throw new BusinessValidationException("Feedback is available only to an authenticated customer or practice member");
        if (hasAnyRole(roles, "ORG_ADMIN", "PARTNER", "CA_PARTNER", "PRACTITIONER")) {
            return new FeedbackActorContext(user.getUserId(), ApplicationFeedbackActorType.PRACTITIONER, practiceId, ApplicationFeedbackContextType.PRACTICE_PORTAL);
        }
        EmployeeEntity employee = employeeRepository.findByOrganizationIdAndUserId(practiceId, user.getUserId())
                .orElseThrow(() -> new BusinessValidationException("Authenticated user is not a member of this practice"));
        if (employee.getStatus() != EmployeeEntity.EmployeeStatus.ACTIVE) throw new BusinessValidationException("Only active practice employees can submit feedback");
        return new FeedbackActorContext(user.getUserId(), ApplicationFeedbackActorType.PRACTICE_EMPLOYEE, practiceId, ApplicationFeedbackContextType.PRACTICE_PORTAL);
    }

    private boolean hasRole(Set<String> roles, String expected) { return roles.contains(expected) || roles.contains("ROLE_" + expected); }
    private boolean hasAnyRole(Set<String> roles, String... expected) {
        for (String role : expected) if (hasRole(roles, role)) return true;
        return false;
    }

    private void validateCategoryForActor(ApplicationFeedbackCategory category, ApplicationFeedbackActorType actorType) {
        Set<ApplicationFeedbackCategory> allowed = switch (actorType) {
            case CUSTOMER -> Set.of(ApplicationFeedbackCategory.APPLICATION_EXPERIENCE, ApplicationFeedbackCategory.PRACTICE_SEARCH, ApplicationFeedbackCategory.PRACTICE_PROFILE, ApplicationFeedbackCategory.CUSTOMER_PROFILE, ApplicationFeedbackCategory.TAX_SERVICE, ApplicationFeedbackCategory.REQUIREMENT, ApplicationFeedbackCategory.MATCHING, ApplicationFeedbackCategory.ENQUIRY, ApplicationFeedbackCategory.REVIEWS, ApplicationFeedbackCategory.ACCOUNT, ApplicationFeedbackCategory.PERFORMANCE, ApplicationFeedbackCategory.OTHER);
            case PRACTITIONER -> Set.of(ApplicationFeedbackCategory.PRACTICE_PROFILE, ApplicationFeedbackCategory.PRACTICE_LOCATIONS, ApplicationFeedbackCategory.EMPLOYEE_MANAGEMENT, ApplicationFeedbackCategory.CUSTOMER_MANAGEMENT, ApplicationFeedbackCategory.ENQUIRIES, ApplicationFeedbackCategory.TAX_SERVICES, ApplicationFeedbackCategory.MARKETPLACE, ApplicationFeedbackCategory.CUSTOMER_MATCHING, ApplicationFeedbackCategory.NOTIFICATIONS, ApplicationFeedbackCategory.DOCUMENTS, ApplicationFeedbackCategory.REPORTS, ApplicationFeedbackCategory.BILLING, ApplicationFeedbackCategory.PERFORMANCE, ApplicationFeedbackCategory.OTHER);
            case PRACTICE_EMPLOYEE -> Set.of(ApplicationFeedbackCategory.CUSTOMER_MANAGEMENT, ApplicationFeedbackCategory.ENQUIRIES, ApplicationFeedbackCategory.CUSTOMER_REQUIREMENTS, ApplicationFeedbackCategory.TAX_SERVICES, ApplicationFeedbackCategory.DOCUMENTS, ApplicationFeedbackCategory.TASKS, ApplicationFeedbackCategory.NOTIFICATIONS, ApplicationFeedbackCategory.PRACTICE_OPERATIONS, ApplicationFeedbackCategory.SEARCH, ApplicationFeedbackCategory.PERFORMANCE, ApplicationFeedbackCategory.OTHER);
        };
        if (!allowed.contains(category)) throw new BusinessValidationException("Selected feedback category is not available for your account type");
    }

    private record FeedbackActorContext(UUID userId, ApplicationFeedbackActorType actorType, UUID practiceId, ApplicationFeedbackContextType contextType) { }
}
