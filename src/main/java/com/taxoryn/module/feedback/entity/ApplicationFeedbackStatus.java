package com.taxoryn.module.feedback.entity;

/** Internal lifecycle for application feedback. Customer flows do not change this value. */
public enum ApplicationFeedbackStatus {
    NEW,
    UNDER_REVIEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REJECTED
}
