package com.taxoryn.module.notice.enums;

public enum NoticeStatus {
    // Standard Lifecycle Statuses
    NOTICE_RECEIVED,
    NOTICE_REGISTERED,
    ASSIGNED,
    UNDER_REVIEW,
    RESPONSE_PREPARATION,
    HEARING,
    SUBMITTED,
    AWAITING_ORDER,
    RESOLVED,
    FOLLOW_UP,
    CLOSED,

    // Extended / Workflow Aliases
    RECEIVED,
    INFO_REQUESTED,
    RESPONSE_DRAFTING,
    INTERNAL_REVIEW,
    PARTNER_APPROVED,
    HEARING_SCHEDULED,
    DEMAND_DROPPED,
    APPEAL_FILED
}

