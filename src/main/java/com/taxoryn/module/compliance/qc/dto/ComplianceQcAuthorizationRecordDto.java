package com.taxoryn.module.compliance.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Immutable compliance filing authorization event record")
public class ComplianceQcAuthorizationRecordDto {

    @Schema(description = "Record ID")
    private UUID id;

    @Schema(description = "Workflow ID")
    private UUID workflowId;

    @Schema(description = "Action type: GRANTED or REVOKED", example = "GRANTED")
    private String action;

    @Schema(description = "Authorization method", example = "CLIENT_CONSENT")
    private String authorizationMethod;

    @Schema(description = "Evidence reference identifier", example = "EMAIL-REF-2026-08")
    private String authorizationReference;

    @Schema(description = "Authorization notes or conditions")
    private String authorizationNotes;

    @Schema(description = "Name or entity that authorized filing")
    private String authorizedBy;

    @Schema(description = "Reason for revocation (if action is REVOKED)")
    private String revocationReason;

    @Schema(description = "ID of user who recorded the action")
    private UUID actorUserId;

    @Schema(description = "Email of user who recorded the action")
    private String actorEmail;

    @Schema(description = "Timestamp when the event occurred")
    private Instant createdAt;
}
