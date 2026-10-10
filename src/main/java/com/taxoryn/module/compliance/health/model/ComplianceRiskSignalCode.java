package com.taxoryn.module.compliance.health.model;

/**
 * Standardized codes for explainable risk signals detected by Compliance Intelligence.
 */
public enum ComplianceRiskSignalCode {
    OVERDUE_OBLIGATION,
    DUE_TODAY,
    DUE_SOON,
    WORK_NOT_GENERATED,
    NO_WORK_TEMPLATE,
    INSUFFICIENT_APPLICABILITY_DATA,
    MISSING_COMPLIANCE_PROFILE,
    MISSING_DUE_DATE
}
