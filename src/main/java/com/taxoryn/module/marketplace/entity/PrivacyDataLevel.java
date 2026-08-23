package com.taxoryn.module.marketplace.entity;

/**
 * 4-Tier Conceptual Data Classification for Taxoryn Marketplace.
 */
public enum PrivacyDataLevel {
    /**
     * Level 1: Public marketplace directory data.
     * (Practice profiles, verified credentials, ratings, offered services).
     */
    LEVEL_1_PUBLIC("Public Marketplace Data"),

    /**
     * Level 2: Early inquiry and practice matching disclosure.
     * (Tax service, financial year, broad customer classification, sanitized summary, approximate location).
     */
    LEVEL_2_EARLY_ENQUIRY("Early Enquiry Disclosure"),

    /**
     * Level 3: Private customer financial and identity details.
     * (Exact salary, PAN, Aadhaar, bank details, exact capital gains, detailed tax computation).
     */
    LEVEL_3_PRIVATE_CUSTOMER("Private Customer Data"),

    /**
     * Level 4: Sensitive statutory tax documents.
     * (ITR XMLs, Form 16, AIS/TIS, 26AS, bank statements, tax notices).
     */
    LEVEL_4_SENSITIVE_DOCUMENTS("Sensitive Tax Documents");

    private final String displayName;

    PrivacyDataLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
