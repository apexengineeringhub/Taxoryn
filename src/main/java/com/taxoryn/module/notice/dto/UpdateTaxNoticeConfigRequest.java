package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticePriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaxNoticeConfigRequest {

    // --- Workflow Configuration ---
    private Boolean responseReviewRequired;
    private Boolean partnerApprovalRequired;
    private Boolean hearingTrackingEnabled;
    private Boolean responseSubmissionTrackingEnabled;

    // --- Deadline Defaults ---
    @Min(value = 1, message = "Default response due days must be at least 1")
    @Max(value = 365, message = "Default response due days cannot exceed 365")
    private Integer defaultResponseDueDays;

    @Min(value = 1, message = "Reminder days before due must be at least 1")
    @Max(value = 60, message = "Reminder days before due cannot exceed 60")
    private Integer reminderDaysBeforeDue;

    @Min(value = 0, message = "Escalation days after due cannot be negative")
    @Max(value = 60, message = "Escalation days after due cannot exceed 60")
    private Integer escalationDaysAfterDue;

    // --- Assignment & Automation ---
    private Boolean autoCreateResponseTask;
    private NoticePriority defaultPriority;
    private Boolean assignmentRequired;

    // --- Notifications ---
    private Boolean notifyOnAssignment;
    private Boolean notifyOnDueSoon;
    private Boolean notifyOnOverdue;
    private Boolean notifyOnSubmission;
    private Boolean notifyOnHearing;

    // --- Dashboard Widgets ---
    private Boolean showDueSoon;
    private Boolean showOverdue;
    private Boolean showAwaitingResponse;
    private Boolean showAwaitingHearing;
    private Boolean showAwaitingOrder;
}
