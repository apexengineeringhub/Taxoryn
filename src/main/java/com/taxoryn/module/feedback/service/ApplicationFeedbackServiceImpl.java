package com.taxoryn.module.feedback.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.feedback.dto.ApplicationFeedbackDto;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApplicationFeedbackServiceImpl implements ApplicationFeedbackService {

    private final ApplicationFeedbackRepository feedbackRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ApplicationFeedbackDto createFeedback(CreateApplicationFeedbackRequest request, String page, String feature) {
        ApplicationFeedbackEntity feedback = ApplicationFeedbackEntity.builder()
                .userId(SecurityUtils.getCurrentUserId())
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
                "Application feedback submitted (type=" + saved.getType() + ", category=" + saved.getCategory() + ")");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationFeedbackDto> getMyFeedback(Pageable pageable) {
        return PagedResponse.of(feedbackRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId(), pageable), this::toDto);
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
}
