package com.taxoryn.module.workflow.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Period classification for client service compliance cycles.
 */
@Getter
@RequiredArgsConstructor
public enum ServicePeriodType {

    MONTHLY("Monthly", "Monthly recurring compliance cycle (e.g. GSTR-1, GSTR-3B)"),
    QUARTERLY("Quarterly", "Quarterly recurring cycle (e.g. TDS Returns, Advance Tax, QRMP)"),
    ANNUAL("Annual", "Annual compliance or tax return cycle (e.g. ITR, Annual GST return)"),
    EVENT_BASED("Event / Case Based", "Single or multi-stage case engagement (e.g. Tax Notice, Appeal, ROC filing)");

    private final String displayName;
    private final String description;
}
