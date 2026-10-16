package com.taxoryn.module.portal.dto;

import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.ClientApprovalType;
import com.taxoryn.module.portal.entity.MessageContextType;
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
@Schema(description = "Client Approval & Consent Request Details")
public class ClientApprovalDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String title;
    private String description;
    private ClientApprovalType approvalType;
    private ClientApprovalStatus status;
    private MessageContextType contextType;
    private UUID contextId;
    private String contextTitle;
    private UUID documentId;
    private String documentName;
    private String documentDownloadUrl;
    private String contentVersion;
    private String contentHash;
    private UUID requestedByUserId;
    private String requestedByName;
    private Instant requestedAt;
    private Instant expiresAt;
    private boolean isExpired;
    private UUID respondedByUserId;
    private String respondedByName;
    private String respondedByEmail;
    private Instant respondedAt;
    private String decisionNotes;
    private String rejectionReason;
    private UUID revokedByUserId;
    private String revokedByName;
    private Instant revokedAt;
    private String revocationReason;
    private Instant createdAt;
    private Instant updatedAt;
}
