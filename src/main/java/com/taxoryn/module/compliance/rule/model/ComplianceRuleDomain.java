package com.taxoryn.module.compliance.rule.model;

/**
 * Domain / Tax area classification for a compliance rule in Taxoryn.
 */
public enum ComplianceRuleDomain {
    GST("Goods & Services Tax (GST)"),
    TDS("Tax Deducted / Collected at Source (TDS/TCS)"),
    INCOME_TAX("Income Tax Computations & Filings"),
    MCA_ROC("Ministry of Corporate Affairs / ROC"),
    STATUTORY_AUDIT("Statutory & Tax Audit"),
    PAYROLL_LABOUR("Payroll, PF, ESIC & Labour Law"),
    OTHER("Other Regulatory / Practice Compliance");

    private final String displayName;

    ComplianceRuleDomain(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
