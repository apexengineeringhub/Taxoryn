package com.taxoryn.module.portal.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Action decision taken by client on an approval request")
public enum ClientApprovalDecision {
    @Schema(description = "Approve and consent to request")
    APPROVE,

    @Schema(description = "Reject or decline request")
    REJECT
}
