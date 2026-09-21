package com.taxoryn.module.capability.model;

/**
 * Standard product capabilities available within Taxoryn.
 *
 * NOTE: ProductCapability represents functional modules, experience profiles,
 * and commercial feature entitlements. It is COMPLETELY DECOUPLED from authorization.
 * Security authorization remains strictly governed by:
 * Tenant Boundary + Security Role + Permission + Client Access Scope.
 */
public enum ProductCapability {
    CLIENT_MANAGEMENT,
    GST_COMPLIANCE,
    ITR_COMPLIANCE,
    TDS_COMPLIANCE,
    COMPLIANCE_CALENDAR,
    TASK_MANAGEMENT,
    DOCUMENT_MANAGEMENT,
    DOCUMENT_REQUESTS,
    TAX_NOTICE_MANAGEMENT,
    BILLING_INVOICING,
    CENTRAL_REPORTING,
    TEAM_MANAGEMENT,
    CLIENT_PORTAL,
    ADVANCED_ANALYTICS
}
