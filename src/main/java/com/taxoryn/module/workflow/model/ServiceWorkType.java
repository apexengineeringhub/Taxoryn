package com.taxoryn.module.workflow.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Controlled operational work type categorization for workflow steps.
 */
@Getter
@RequiredArgsConstructor
public enum ServiceWorkType {

    DATA_COLLECTION("Data Collection", "Gather transaction records, summaries, and raw files"),
    DOCUMENT_COLLECTION("Document Collection", "Request and collect supporting vouchers and statutory forms"),
    PREPARATION("Preparation", "Data compilation, register reconciliations, and return draft preparation"),
    COMPUTATION("Computation", "Tax calculation, adjustments, exemptions, and deductions computation"),
    REVIEW("Review & Quality Check", "Peer or senior partner verification, audit risk check, and technical review"),
    CLIENT_CONFIRMATION("Client Confirmation", "Obtain client approval on draft computation, challan, or submission"),
    FILING_PREPARATION("Filing Preparation", "Challan generation, JSON schema validation, and portal payload generation"),
    FILING("Statutory Filing", "Electronic submission to ITD, GSTN, TRACES, or MCA portals"),
    ACKNOWLEDGEMENT("Acknowledgement & Dispatch", "Download filed receipts, archive in client vault, and deliver to taxpayer"),
    HEARING_FOLLOW_UP("Hearing & Follow-up", "Virtual hearing representation, adjournment petition, and order tracking"),
    COMPLETION("Engagement Completion", "Milestone signoff, compliance ledger closure, and period wrapup"),
    OTHER("Other Operational Task", "Custom practice operational step");

    private final String displayName;
    private final String description;
}
