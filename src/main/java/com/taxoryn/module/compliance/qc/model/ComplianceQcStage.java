package com.taxoryn.module.compliance.qc.model;

/**
 * Quality Control stages representing the controlled lifecycle progression:
 * Preparation -> Validation -> Review -> Correction -> Approval -> Authorization -> Submission
 */
public enum ComplianceQcStage {
    PREPARATION,      // Work in progress, gathering documents, drafting computation
    VALIDATION,       // Deterministic pre-review validation checks (rules, checklist, profiles)
    REVIEW,           // Independent maker-checker review (UNDER_REVIEW)
    CORRECTION,       // Changes requested by reviewer; resolving findings (CHANGES_REQUIRED)
    APPROVAL,         // Technical sign-off passed by reviewer (READY_FOR_FILING)
    AUTHORIZATION,    // Filing authorized by client / designated authority
    SUBMISSION        // Official government filing completed (FILED / COMPLETED)
}
