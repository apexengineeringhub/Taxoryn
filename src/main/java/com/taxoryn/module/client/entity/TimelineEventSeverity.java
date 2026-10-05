package com.taxoryn.module.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Severity/visual prominence level of a timeline event")
public enum TimelineEventSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}
