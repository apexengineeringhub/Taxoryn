package com.taxoryn.module.compliance.qc.model;

/**
 * Lifecycle status of a specific review finding.
 */
public enum ComplianceQcFindingStatus {
    OPEN,        // Finding reported by reviewer, awaiting preparer correction
    CORRECTED,   // Preparer has addressed finding and provided response
    RESOLVED,    // Reviewer has verified the correction and resolved finding
    DISMISSED    // Reviewer determined finding is no longer applicable / false positive
}
