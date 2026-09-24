package com.taxoryn.module.compliance.model;

import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import lombok.Getter;

/**
 * Controlled enumeration of Indian statutory and practice compliance obligation types.
 */
@Getter
public enum ComplianceObligationType {

    GST_RETURN(
            "GST Return & Tax Payment",
            ProductModuleCode.GST,
            ProductCapability.COMPLIANCE_CALENDAR,
            "GSTR-1, GSTR-3B, CMP-08 periodic return filing and liability settlement"
    ),

    ITR_FILING(
            "Income Tax Return (ITR)",
            ProductModuleCode.ITR,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Annual Income Tax Return filing under Income Tax Act, 1961"
    ),

    TDS_RETURN(
            "TDS / TCS Periodic Return",
            ProductModuleCode.TDS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Quarterly TDS/TCS statement (Form 24Q, 26Q, 27Q, 27EQ) and monthly Challan 281 deposits"
    ),

    TAX_NOTICE_RESPONSE(
            "Tax Notice Written Response",
            ProductModuleCode.TAX_NOTICES,
            ProductCapability.TAX_NOTICE_MANAGEMENT,
            "Formal statutory response or submission against assessment, scrutiny, or demand notice"
    ),

    TAX_NOTICE_HEARING(
            "Tax Notice Personal Hearing",
            ProductModuleCode.TAX_NOTICES,
            ProductCapability.TAX_NOTICE_MANAGEMENT,
            "Scheduled personal hearing, appellate appearance, or virtual conference"
    ),

    TAX_AUDIT(
            "Tax Audit Report (Sec 44AB)",
            ProductModuleCode.ITR,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Tax Audit reporting Form 3CA/3CB and statement of particulars Form 3CD"
    ),

    STATUTORY_AUDIT(
            "Statutory Company Audit",
            ProductModuleCode.CLIENTS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Companies Act 2013 statutory audit report and financial statements finalization"
    ),

    ROC_COMPLIANCE(
            "ROC / MCA Annual Compliance",
            ProductModuleCode.CLIENTS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "MCA annual filings (AOC-4, MGT-7, DIR-3 KYC) for registered companies and LLPs"
    ),

    PAYROLL_COMPLIANCE(
            "Payroll, PF & ESI Monthly Compliance",
            ProductModuleCode.CLIENTS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Monthly PF (ECR), ESI contribution deposit, and Professional Tax settlement"
    ),

    ADVANCE_TAX(
            "Advance Income Tax Installment",
            ProductModuleCode.ITR,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Quarterly advance tax payment (15%, 45%, 75%, 100%) under section 208/211"
    ),

    SELF_ASSESSMENT_TAX(
            "Self-Assessment Tax Deposit",
            ProductModuleCode.ITR,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Payment of tax liability under section 140A prior to return filing"
    ),

    OTHER(
            "General / Custom Compliance Obligation",
            ProductModuleCode.CLIENTS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Practice-defined or ad-hoc client statutory obligation"
    );

    private final String displayName;
    private final ProductModuleCode requiredModule;
    private final ProductCapability requiredCapability;
    private final String description;

    ComplianceObligationType(
            String displayName,
            ProductModuleCode requiredModule,
            ProductCapability requiredCapability,
            String description
    ) {
        this.displayName = displayName;
        this.requiredModule = requiredModule;
        this.requiredCapability = requiredCapability;
        this.description = description;
    }
}
