package com.taxoryn.module.compliance.applicability.model;

/**
 * Standardized client compliance facts resolved from Client Profile and Compliance Profile.
 */
public enum ComplianceFactAttribute {
    // Client Domain Facts
    CLIENT_TYPE("Client Legal Entity Type / Constitution"),

    // GST Facts
    GST_APPLICABLE("GST Compliance Applicable Flag"),
    GST_REGISTRATION_TYPE("GST Registration Type (REGULAR, COMPOSITION, etc.)"),
    GST_FILING_FREQUENCY("GST Return Filing Frequency (MONTHLY, QUARTERLY, etc.)"),
    GST_COMPOSITION_SCHEME("GST Composition Scheme Indicator"),
    GST_EINVOICE_APPLICABLE("GST E-Invoicing Applicable Flag"),
    GST_EWAYBILL_APPLICABLE("GST E-Way Bill Applicable Flag"),

    // TDS / TCS Facts
    TDS_APPLICABLE("TDS / TCS Compliance Applicable Flag"),
    TDS_FILING_FREQUENCY("TDS Statement Filing Frequency"),
    TDS_DEDUCTOR_CATEGORY("TDS Deductor Category (COMPANY, INDIVIDUAL, etc.)"),
    TDS_LOWER_DEDUCTION_CERTIFICATE("TDS Section 197 Lower Deduction Certificate Flag"),

    // Income Tax Facts
    ITR_APPLICABLE("Income Tax (ITR) Compliance Applicable Flag"),
    ITR_CATEGORY("Income Tax Assessee Category"),
    ITR_TAX_AUDIT_APPLICABLE("Tax Audit u/s 44AB Applicable Flag"),
    ITR_TRANSFER_PRICING_APPLICABLE("Transfer Pricing u/s 92E Applicable Flag"),
    ADVANCE_TAX_APPLICABLE("Advance Tax Payment Applicable Flag"),

    // MCA / ROC Facts
    MCA_FILING_APPLICABLE("MCA / ROC Statutory Filing Applicable Flag"),

    // Payroll & Labour Facts
    PF_ESI_APPLICABLE("EPF / ESIC Payroll Compliance Applicable Flag"),
    PROFESSIONAL_TAX_APPLICABLE("State Professional Tax Applicable Flag");

    private final String description;

    ComplianceFactAttribute(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
