package com.taxoryn.module.compliance.applicability.model;

/**
 * Deterministic result state of evaluating a compliance rule against a client's facts.
 */
public enum ApplicabilityResultState {
    /**
     * Rule is fully applicable to the client.
     */
    APPLICABLE,

    /**
     * Rule is conclusively not applicable to the client.
     */
    NOT_APPLICABLE,

    /**
     * Missing or unconfigured compliance facts prevent conclusive determination
     * (e.g. GST/TDS/ITR not configured, missing registration type or filing frequency).
     */
    INSUFFICIENT_DATA,

    /**
     * The rule configuration or criteria syntax is invalid or unsupported.
     */
    CONFIGURATION_ERROR
}
