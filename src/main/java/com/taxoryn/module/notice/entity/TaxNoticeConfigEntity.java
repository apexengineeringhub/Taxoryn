package com.taxoryn.module.notice.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.notice.enums.NoticePriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tax_notice_configurations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxNoticeConfigEntity extends TenantAuditableEntity {

    // --- Workflow Configuration ---
    @Column(name = "response_review_required", nullable = false)
    @Builder.Default
    private boolean responseReviewRequired = true;

    @Column(name = "partner_approval_required", nullable = false)
    @Builder.Default
    private boolean partnerApprovalRequired = false;

    @Column(name = "hearing_tracking_enabled", nullable = false)
    @Builder.Default
    private boolean hearingTrackingEnabled = true;

    @Column(name = "response_submission_tracking_enabled", nullable = false)
    @Builder.Default
    private boolean responseSubmissionTrackingEnabled = true;

    // --- Deadline Defaults ---
    @Column(name = "default_response_due_days", nullable = false)
    @Builder.Default
    private int defaultResponseDueDays = 30;

    @Column(name = "reminder_days_before_due", nullable = false)
    @Builder.Default
    private int reminderDaysBeforeDue = 7;

    @Column(name = "escalation_days_after_due", nullable = false)
    @Builder.Default
    private int escalationDaysAfterDue = 2;

    // --- Assignment & Automation ---
    @Column(name = "auto_create_response_task", nullable = false)
    @Builder.Default
    private boolean autoCreateResponseTask = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_priority", nullable = false, length = 50)
    @Builder.Default
    private NoticePriority defaultPriority = NoticePriority.MEDIUM;

    @Column(name = "assignment_required", nullable = false)
    @Builder.Default
    private boolean assignmentRequired = false;

    // --- Notifications ---
    @Column(name = "notify_on_assignment", nullable = false)
    @Builder.Default
    private boolean notifyOnAssignment = true;

    @Column(name = "notify_on_due_soon", nullable = false)
    @Builder.Default
    private boolean notifyOnDueSoon = true;

    @Column(name = "notify_on_overdue", nullable = false)
    @Builder.Default
    private boolean notifyOnOverdue = true;

    @Column(name = "notify_on_submission", nullable = false)
    @Builder.Default
    private boolean notifyOnSubmission = true;

    @Column(name = "notify_on_hearing", nullable = false)
    @Builder.Default
    private boolean notifyOnHearing = true;

    // --- Dashboard Widgets ---
    @Column(name = "show_due_soon", nullable = false)
    @Builder.Default
    private boolean showDueSoon = true;

    @Column(name = "show_overdue", nullable = false)
    @Builder.Default
    private boolean showOverdue = true;

    @Column(name = "show_awaiting_response", nullable = false)
    @Builder.Default
    private boolean showAwaitingResponse = true;

    @Column(name = "show_awaiting_hearing", nullable = false)
    @Builder.Default
    private boolean showAwaitingHearing = true;

    @Column(name = "show_awaiting_order", nullable = false)
    @Builder.Default
    private boolean showAwaitingOrder = true;

    // --- Customization Flag ---
    @Column(name = "is_customized", nullable = false)
    @Builder.Default
    private boolean customized = true;
}
