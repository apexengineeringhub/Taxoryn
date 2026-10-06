package com.taxoryn.module.gov.model;

/**
 * Supported government integration provider types.
 * Pure provider abstraction with no hard-coded domain logic.
 */
public enum GovProviderType {
    /**
     * Goods and Services Tax Network / GSP Gateway.
     */
    GST,

    /**
     * Income Tax Department / E-Filing Gateway.
     */
    INCOME_TAX,

    /**
     * TDS Reconciliation Analysis and Correction Enabling System / TRACES.
     */
    TRACES,

    /**
     * Tax Deducted at Source (TDS/TCS) Government Gateway.
     */
    TDS
}
