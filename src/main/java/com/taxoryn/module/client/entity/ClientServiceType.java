package com.taxoryn.module.client.entity;

import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standard client service engagement types provided by CA & tax practices.
 */
@Getter
@RequiredArgsConstructor
public enum ClientServiceType {
    GST_COMPLIANCE(
            "GST Compliance & Returns",
            "GSTR-1, GSTR-3B monthly and quarterly compliance, reconciliations, and filing",
            "TAX",
            ProductModuleCode.GST,
            ProductCapability.GST_COMPLIANCE,
            "/gst"
    ),
    ITR_COMPLIANCE(
            "Income Tax Computations & Filing",
            "ITR-1 to ITR-7 computation, advance tax computation, tax audits, and e-filing",
            "TAX",
            ProductModuleCode.ITR,
            ProductCapability.ITR_COMPLIANCE,
            "/itr"
    ),
    TDS_COMPLIANCE(
            "TDS / TCS Quarterly Filing",
            "Forms 24Q, 26Q, 27Q, 27EQ quarterly returns, ITNS 281 challans, and 16A generation",
            "TAX",
            ProductModuleCode.TDS,
            ProductCapability.TDS_COMPLIANCE,
            "/tds"
    ),
    TAX_NOTICE_MANAGEMENT(
            "Tax Notice & Assessment Representation",
            "Statutory notice tracking, hearing schedules, and response drafting across ITD, GSTN, and CPC",
            "ADVISORY",
            ProductModuleCode.TAX_NOTICES,
            ProductCapability.TAX_NOTICE_MANAGEMENT,
            "/notices"
    ),
    COMPLIANCE_CALENDAR(
            "Statutory Compliance Tracking",
            "Automated monitoring of statutory due dates, reminders, and deadline alerts",
            "COMPLIANCE",
            ProductModuleCode.TASKS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "/compliance-calendar"
    ),
    DOCUMENT_MANAGEMENT(
            "Client Vault & Document Exchange",
            "Permanent digital client archive and automated document requests",
            "OPERATIONS",
            ProductModuleCode.DOCUMENTS,
            ProductCapability.DOCUMENT_MANAGEMENT,
            "/documents"
    ),
    BILLING_INVOICING(
            "Retainer Billing & Fee Collections",
            "Professional fee invoicing, automated payment receipts, and balance tracking",
            "OPERATIONS",
            ProductModuleCode.BILLING,
            ProductCapability.BILLING_INVOICING,
            "/billing"
    ),
    ACCOUNTING_BOOKKEEPING(
            "Accounting & Bookkeeping Services",
            "General ledger maintenance, bank reconciliations, and periodic financial statements",
            "ADVISORY",
            null,
            null,
            null
    ),
    AUDIT_ASSURANCE(
            "Statutory & Internal Audit Assurance",
            "Statutory audit, tax audit u/s 44AB, GST audit, and internal controls review",
            "ADVISORY",
            null,
            null,
            null
    ),
    OTHER(
            "Custom Advisory & General Services",
            "Specialized consultancy, ROC compliance, certifications, and business advisory",
            "ADVISORY",
            null,
            null,
            null
    );

    private final String displayName;
    private final String description;
    private final String category;
    private final ProductModuleCode associatedModule;
    private final ProductCapability associatedCapability;
    private final String routePath;
}
