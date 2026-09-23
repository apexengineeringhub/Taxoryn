package com.taxoryn.module.compliance.entity;

import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standard work types for Compliance Work Items.
 */
@Getter
@RequiredArgsConstructor
public enum ComplianceWorkType {

    GST_RETURN(
            "GST Return Filing",
            ProductModuleCode.GST,
            ProductCapability.GST_COMPLIANCE,
            "Periodic GST Return Preparation, ITC Validation & Filing (GSTR-1, GSTR-3B, GSTR-9)"
    ),
    ITR_RETURN(
            "Income Tax Return",
            ProductModuleCode.ITR,
            ProductCapability.ITR_COMPLIANCE,
            "Income Tax Return Computation, Schedule Verification & e-Filing (ITR-1 through ITR-7)"
    ),
    TDS_RETURN(
            "TDS / TCS Return",
            ProductModuleCode.TDS,
            ProductCapability.TDS_COMPLIANCE,
            "Quarterly TDS/TCS Statement Preparation & Form 24Q/26Q/27Q/27EQ"
    ),
    TAX_NOTICE(
            "Tax Notice Response / Hearing",
            ProductModuleCode.TAX_NOTICES,
            ProductCapability.TAX_NOTICE_MANAGEMENT,
            "Notice Assessment, Ground Preparation, Written Submissions & Hearing Representation"
    ),
    COMPLIANCE_TASK(
            "General Statutory Compliance",
            ProductModuleCode.TASKS,
            ProductCapability.COMPLIANCE_CALENDAR,
            "Periodic Statutory Obligation, Advance Tax Computation, or Statutory Audit item"
    ),
    DOCUMENT_COLLECTION(
            "Document & Data Collection",
            ProductModuleCode.DOCUMENTS,
            ProductCapability.DOCUMENT_MANAGEMENT,
            "Client Working Papers, Bank Statements, Books of Accounts & Information Requests"
    ),
    OTHER(
            "Other Professional Compliance Work",
            null,
            null,
            "Advisory memo, RoC form, or custom compliance engagement task"
    );

    private final String displayName;
    private final ProductModuleCode associatedModule;
    private final ProductCapability associatedCapability;
    private final String description;
}
