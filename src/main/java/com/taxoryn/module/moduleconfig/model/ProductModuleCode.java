package com.taxoryn.module.moduleconfig.model;

/**
 * Standard product module codes available within the Taxoryn SaaS platform.
 */
public enum ProductModuleCode {
    ORGANIZATION,
    LOCATIONS,
    USERS,
    ROLES,
    SECURITY,
    SUBSCRIPTION,
    MODULE_CONFIG,
    CLIENTS,
    TASKS,
    REMINDERS,
    DOCUMENTS,
    DOCUMENT_REQUESTS,
    CLIENT_PORTAL,
    NOTIFICATIONS,
    AUDIT,
    GST,
    GST_COMPLIANCE,
    ITR,
    ITR_COMPLIANCE,
    TDS,
    TDS_COMPLIANCE,
    TAX_NOTICES,
    TAX_NOTICE_MANAGEMENT,
    BILLING,
    BILLING_PRACTICE_OPERATIONS,
    REPORTS,
    DASHBOARD,
    PRACTICE_DASHBOARD,
    MARKETPLACE,
    GMAIL,
    SELF_ITR;

    public ProductModuleCategory getDefaultCategory() {
        return switch (this) {
            case ORGANIZATION, LOCATIONS, USERS, ROLES, SECURITY, SUBSCRIPTION, MODULE_CONFIG, NOTIFICATIONS, AUDIT ->
                    ProductModuleCategory.CORE;
            case CLIENTS, TASKS, REMINDERS, DOCUMENTS, DOCUMENT_REQUESTS, BILLING, BILLING_PRACTICE_OPERATIONS, REPORTS, DASHBOARD, PRACTICE_DASHBOARD ->
                    ProductModuleCategory.FOUNDATION;
            case GST, GST_COMPLIANCE, ITR, ITR_COMPLIANCE, TDS, TDS_COMPLIANCE, TAX_NOTICES, TAX_NOTICE_MANAGEMENT ->
                    ProductModuleCategory.BUSINESS;
            case CLIENT_PORTAL, MARKETPLACE, GMAIL, SELF_ITR ->
                    ProductModuleCategory.OPTIONAL;
        };
    }

    public boolean isMandatory() {
        ProductModuleCategory cat = getDefaultCategory();
        return cat == ProductModuleCategory.CORE || cat == ProductModuleCategory.FOUNDATION;
    }

    public boolean isConfigurable() {
        return !isMandatory();
    }

    public boolean isSubscriptionControlled() {
        ProductModuleCategory cat = getDefaultCategory();
        return cat == ProductModuleCategory.BUSINESS || cat == ProductModuleCategory.OPTIONAL;
    }

    public boolean isUsageControlled() {
        return switch (this) {
            case GST, GST_COMPLIANCE, ITR, ITR_COMPLIANCE, TDS, TDS_COMPLIANCE, TAX_NOTICES, TAX_NOTICE_MANAGEMENT, SELF_ITR -> true;
            default -> false;
        };
    }

    public String getDisplayName() {
        return switch (this) {
            case ORGANIZATION -> "Organization & Practice Management";
            case LOCATIONS -> "Location & Branch Management";
            case USERS -> "User Governance & Team Management";
            case ROLES -> "Role-Based Access Control";
            case SECURITY -> "Security & Authentication";
            case SUBSCRIPTION -> "Subscription & Entitlement Engine";
            case MODULE_CONFIG -> "Module & Capability Configuration";
            case NOTIFICATIONS -> "Notifications & Alerts";
            case AUDIT -> "Enterprise Audit Logging";
            case CLIENTS -> "Client Management & Master CRM";
            case TASKS -> "Task & Workflow Management";
            case REMINDERS -> "Reminders & Automation";
            case DOCUMENTS -> "Document Vault & Storage";
            case DOCUMENT_REQUESTS -> "Document Request Workflows";
            case BILLING, BILLING_PRACTICE_OPERATIONS -> "Billing, Invoicing & Payments";
            case REPORTS -> "Practice Analytics & Reporting";
            case DASHBOARD, PRACTICE_DASHBOARD -> "Practice Intelligence Dashboard";
            case GST, GST_COMPLIANCE -> "GST Compliance & Returns";
            case ITR, ITR_COMPLIANCE -> "Income Tax (ITR) Compliance";
            case TDS, TDS_COMPLIANCE -> "TDS & TCS Compliance";
            case TAX_NOTICES, TAX_NOTICE_MANAGEMENT -> "Tax Notice Lifecycle Management";
            case CLIENT_PORTAL -> "Client Self-Service Portal";
            case MARKETPLACE -> "Practice Marketplace Profile";
            case GMAIL -> "Gmail & Email Integration";
            case SELF_ITR -> "Self ITR Individual Filing";
        };
    }
}

