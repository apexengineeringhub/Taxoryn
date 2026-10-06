package com.taxoryn.module.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category of timeline events for client activity tracking")
public enum TimelineEventCategory {
    CLIENT,
    PROFILE,
    SERVICE,
    CONTACT,
    BRANCH,
    RELATIONSHIP,
    ENGAGEMENT,
    WORK,
    DOCUMENT,
    COMPLIANCE,
    BILLING,
    PAYMENT,
    GOVERNMENT,
    COMMUNICATION,
    SYSTEM
}
