package com.taxoryn.module.compliance.applicability.model;

/**
 * Supported comparison operators for compliance rule applicability criteria.
 */
public enum ApplicabilityCriterionOperator {
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN,
    IS_TRUE,
    IS_FALSE,
    IS_PRESENT,
    IS_NOT_PRESENT
}
