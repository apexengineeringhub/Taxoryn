package com.taxoryn.module.portal.dto;

import com.taxoryn.module.portal.entity.ClientApprovalType;
import com.taxoryn.module.portal.entity.MessageContextType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Payload for creating a client approval request")
public class CreateClientApprovalRequest {

    @NotBlank(message = "Approval title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Approval type is required")
    private ClientApprovalType approvalType;

    @Builder.Default
    private MessageContextType contextType = MessageContextType.GENERAL;

    private UUID contextId;
    private String contextTitle;

    private UUID documentId;

    @Size(max = 50, message = "Content version must not exceed 50 characters")
    private String contentVersion;

    @Size(max = 128, message = "Content hash must not exceed 128 characters")
    private String contentHash;

    private Instant expiresAt;
}
