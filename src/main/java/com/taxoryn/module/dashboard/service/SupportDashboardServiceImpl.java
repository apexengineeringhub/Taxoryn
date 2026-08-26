package com.taxoryn.module.dashboard.service;

import com.taxoryn.module.dashboard.dto.SupportDashboardDto;
import com.taxoryn.module.dashboard.dto.SupportDashboardDto.RecentSupportActivityDto;
import com.taxoryn.module.dashboard.dto.SupportDashboardDto.SupportAttentionItemDto;
import com.taxoryn.module.dashboard.dto.SupportDashboardDto.SupportKpisDto;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackEntity;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackPriority;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackStatus;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupportDashboardServiceImpl implements SupportDashboardService {

    private final ApplicationFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public SupportDashboardDto getSupportOverview() {
        List<ApplicationFeedbackEntity> allFeedback = feedbackRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Set<ApplicationFeedbackStatus> openStatuses = Set.of(
                ApplicationFeedbackStatus.NEW,
                ApplicationFeedbackStatus.UNDER_REVIEW,
                ApplicationFeedbackStatus.ASSIGNED,
                ApplicationFeedbackStatus.IN_PROGRESS,
                ApplicationFeedbackStatus.ESCALATED
        );

        long openCases = allFeedback.stream()
                .filter(f -> openStatuses.contains(f.getStatus()))
                .count();

        long waitingForCustomer = allFeedback.stream()
                .filter(f -> f.getStatus() == ApplicationFeedbackStatus.UNDER_REVIEW)
                .count();

        long highPriority = allFeedback.stream()
                .filter(f -> openStatuses.contains(f.getStatus()) &&
                        (f.getPriority() == ApplicationFeedbackPriority.HIGH || f.getPriority() == ApplicationFeedbackPriority.CRITICAL))
                .count();

        long unresolvedFeedback = openCases;

        long resolvedThisMonth = allFeedback.stream()
                .filter(f -> f.getStatus() == ApplicationFeedbackStatus.RESOLVED || f.getStatus() == ApplicationFeedbackStatus.CLOSED)
                .count();

        SupportKpisDto kpis = SupportKpisDto.builder()
                .openCases(openCases)
                .waitingForCustomer(waitingForCustomer)
                .highPriority(highPriority)
                .unresolvedFeedback(unresolvedFeedback)
                .resolvedThisMonth(resolvedThisMonth)
                .build();

        // Build actionable Attention Items
        List<SupportAttentionItemDto> attentionItems = new ArrayList<>();

        // 1. High Priority items
        allFeedback.stream()
                .filter(f -> openStatuses.contains(f.getStatus()) &&
                        (f.getPriority() == ApplicationFeedbackPriority.HIGH || f.getPriority() == ApplicationFeedbackPriority.CRITICAL))
                .limit(4)
                .forEach(f -> attentionItems.add(SupportAttentionItemDto.builder()
                        .id(f.getId().toString())
                        .title(f.getTitle())
                        .description("Priority: " + f.getPriority() + " • Status: " + f.getStatus() + " • Category: " + f.getCategory())
                        .priority(f.getPriority().name())
                        .status(f.getStatus().name())
                        .actionTarget("/admin/feedback")
                        .actionLabel("Review →")
                        .build()));

        // 2. Waiting for Customer items
        allFeedback.stream()
                .filter(f -> f.getStatus() == ApplicationFeedbackStatus.UNDER_REVIEW)
                .limit(3)
                .forEach(f -> attentionItems.add(SupportAttentionItemDto.builder()
                        .id(f.getId().toString())
                        .title(f.getTitle())
                        .description("Waiting for customer response • Category: " + f.getCategory())
                        .priority(f.getPriority().name())
                        .status(f.getStatus().name())
                        .actionTarget("/admin/feedback")
                        .actionLabel("Follow Up →")
                        .build()));

        // 3. Escalated feedback items
        allFeedback.stream()
                .filter(f -> f.getStatus() == ApplicationFeedbackStatus.ESCALATED)
                .limit(3)
                .forEach(f -> attentionItems.add(SupportAttentionItemDto.builder()
                        .id(f.getId().toString())
                        .title(f.getTitle())
                        .description("Escalated to engineering • Team: " + (f.getAssignedTeam() != null ? f.getAssignedTeam() : "PLATFORM"))
                        .priority("CRITICAL")
                        .status(f.getStatus().name())
                        .actionTarget("/admin/feedback")
                        .actionLabel("Check Status →")
                        .build()));

        // Build Recent Support Activity
        List<RecentSupportActivityDto> recentActivities = new ArrayList<>();
        allFeedback.stream()
                .limit(10)
                .forEach(f -> {
                    String title = "Feedback " + f.getStatus().name().toLowerCase().replace('_', ' ');
                    String description = f.getTitle() + " (" + f.getCategory() + ")";
                    String actor = f.getActorType() != null ? f.getActorType().name() : "USER";
                    String target = f.getFeature() != null ? f.getFeature() : "Taxoryn Platform";

                    recentActivities.add(RecentSupportActivityDto.builder()
                            .id(f.getId().toString())
                            .title(title)
                            .description(description)
                            .actor(actor)
                            .target(target)
                            .timestamp(f.getUpdatedAt() != null ? f.getUpdatedAt() : f.getCreatedAt())
                            .status(f.getStatus().name())
                            .severity(f.getPriority().name())
                            .navigationTarget("/admin/feedback")
                            .build());
                });

        return SupportDashboardDto.builder()
                .kpis(kpis)
                .supportAttention(attentionItems)
                .recentActivity(recentActivities)
                .build();
    }
}
