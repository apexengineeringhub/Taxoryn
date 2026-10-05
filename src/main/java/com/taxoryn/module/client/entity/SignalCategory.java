package com.taxoryn.module.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category of client intelligence signal")
public enum SignalCategory {
    ONBOARDING,
    PROFILE,
    CONTACT,
    LOCATION,
    SERVICE,
    LIFECYCLE,
    COMPLIANCE,
    ENGAGEMENT
}
