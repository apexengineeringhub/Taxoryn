package com.taxoryn.module.portal.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a client approval request")
public enum ClientApprovalStatus {
    @Schema(description = "Awaiting client review and decision")
    PENDING,

    @Schema(description = "Explicitly approved or consented by client")
    APPROVED,

    @Schema(description = "Rejected or revisions requested by client")
    REJECTED,

    @Schema(description = "Passed expiry deadline without client decision")
    EXPIRED,

    @Schema(description = "Cancelled or revoked by practice before decision")
    REVOKED
}
