package com.taxoryn.module.marketplace.entity;

/**
 * Controlled classification of customers for tax marketplace requirements.
 */
public enum CustomerTaxpayerType {
    SALARIED("Salaried Individual"),
    SELF_EMPLOYED("Self-Employed Professional"),
    BUSINESS_OWNER("Business Owner / MSME"),
    FREELANCER("Freelancer / Consultant"),
    INVESTOR("Investor / Trader"),
    OTHER("Other Taxpayer");

    private final String displayName;

    CustomerTaxpayerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
