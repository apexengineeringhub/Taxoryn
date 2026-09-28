package com.taxoryn.module.gmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send a reply to a Gmail conversation")
public class GmailReplyRequest {

    @Schema(description = "Optional ID of connected Gmail account / mailbox to send from. Defaults to conversation account.")
    private UUID replyAccountId;

    @NotBlank(message = "Recipient email is required")
    @Schema(description = "Recipient email address")
    private String to;

    @Schema(description = "Email subject (typically starts with Re:)")
    private String subject;

    @NotBlank(message = "Email body is required")
    @Schema(description = "Plain text or HTML email body")
    private String body;

    @Schema(description = "Optional In-Reply-To Message ID for threading header")
    private String inReplyToMessageId;
}
