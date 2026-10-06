package com.taxoryn.module.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Priority level of an intelligence attention signal")
public enum SignalPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}
