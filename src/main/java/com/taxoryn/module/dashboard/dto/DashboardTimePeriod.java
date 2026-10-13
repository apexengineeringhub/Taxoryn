package com.taxoryn.module.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DashboardTimePeriod {
    TODAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_QUARTER,
    LAST_QUARTER,
    THIS_YEAR,
    LAST_YEAR,
    CUSTOM,
    ALL_TIME;

    @JsonCreator
    public static DashboardTimePeriod fromString(String value) {
        if (value == null || value.isBlank()) {
            return ALL_TIME;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (DashboardTimePeriod p : values()) {
            if (p.name().equalsIgnoreCase(normalized)) {
                return p;
            }
        }
        return ALL_TIME;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
