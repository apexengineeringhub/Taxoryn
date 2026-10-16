package com.taxoryn.module.portal.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category of approval or consent requested from client")
public enum ClientApprovalType {
    @Schema(description = "Approval of computed tax liability and deductions before return filing")
    TAX_COMPUTATION_APPROVAL,

    @Schema(description = "Explicit client consent authorizing practitioner to file return")
    FILING_CONSENT,

    @Schema(description = "Client sign-off on delivered financial, audit, or tax document")
    DOCUMENT_SIGN_OFF,

    @Schema(description = "Client acknowledgment of engagement terms, scope, or fee schedule")
    ENGAGEMENT_TERMS,

    @Schema(description = "Client confirmation of turnover, bank accounts, or financial disclosures")
    INFORMATION_CONFIRMATION,

    @Schema(description = "General consultation or advisory recommendation sign-off")
    GENERAL_APPROVAL
}
