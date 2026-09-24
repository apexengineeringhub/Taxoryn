package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.organization.entity.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxNoticeConfigDto {

    private UUID id;
    private UUID organizationId;
    private OrganizationType organizationType;
    private boolean isCustomized;

    // --- Workflow Configuration ---
    private boolean responseReviewRequired;
    private boolean partnerApprovalRequired;
    private boolean hearingTrackingEnabled;
    private boolean responseSubmissionTrackingEnabled;

    // --- Deadline Defaults ---
    private int defaultResponseDueDays;
    private int reminderDaysBeforeDue;
    private int escalationDaysAfterDue;

    // --- Assignment & Automation ---
    private boolean autoCreateResponseTask;
    private NoticePriority defaultPriority;
    private boolean assignmentRequired;

    // --- Notifications ---
    private boolean notifyOnAssignment;
    private boolean notifyOnDueSoon;
    private boolean notifyOnOverdue;
    private boolean notifyOnSubmission;
    private boolean notifyOnHearing;

    // --- Dashboard Widgets ---
    private boolean showDueSoon;
    private boolean showOverdue;
    private boolean showAwaitingResponse;
    private boolean showAwaitingHearing;
    private boolean showAwaitingOrder;

    // --- Metadata ---
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
