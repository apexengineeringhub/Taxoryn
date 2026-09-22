package com.taxoryn.module.notice.mapper;

import com.taxoryn.module.notice.dto.TaxNoticeConfigDto;
import com.taxoryn.module.notice.entity.TaxNoticeConfigEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import org.springframework.stereotype.Component;

@Component
public class TaxNoticeConfigMapper {

    public TaxNoticeConfigDto toDto(TaxNoticeConfigEntity entity, OrganizationType orgType) {
        if (entity == null) {
            return null;
        }

        return TaxNoticeConfigDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .organizationType(orgType)
                .isCustomized(entity.isCustomized())
                .responseReviewRequired(entity.isResponseReviewRequired())
                .partnerApprovalRequired(entity.isPartnerApprovalRequired())
                .hearingTrackingEnabled(entity.isHearingTrackingEnabled())
                .responseSubmissionTrackingEnabled(entity.isResponseSubmissionTrackingEnabled())
                .defaultResponseDueDays(entity.getDefaultResponseDueDays())
                .reminderDaysBeforeDue(entity.getReminderDaysBeforeDue())
                .escalationDaysAfterDue(entity.getEscalationDaysAfterDue())
                .autoCreateResponseTask(entity.isAutoCreateResponseTask())
                .defaultPriority(entity.getDefaultPriority())
                .assignmentRequired(entity.isAssignmentRequired())
                .notifyOnAssignment(entity.isNotifyOnAssignment())
                .notifyOnDueSoon(entity.isNotifyOnDueSoon())
                .notifyOnOverdue(entity.isNotifyOnOverdue())
                .notifyOnSubmission(entity.isNotifyOnSubmission())
                .notifyOnHearing(entity.isNotifyOnHearing())
                .showDueSoon(entity.isShowDueSoon())
                .showOverdue(entity.isShowOverdue())
                .showAwaitingResponse(entity.isShowAwaitingResponse())
                .showAwaitingHearing(entity.isShowAwaitingHearing())
                .showAwaitingOrder(entity.isShowAwaitingOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
