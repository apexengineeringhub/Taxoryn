package com.taxoryn.module.notification.whatsapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Schema(description = "WhatsApp Message Delivery Record")
public class WhatsAppMessageDto {
    UUID id;
    UUID organizationId;
    UUID userId;
    String recipientPhone;
    String templateType;
    String templateName;
    String messageContent;
    String provider;
    String providerMessageId;
    String status;
    String errorMessage;
    Instant sentAt;
    Instant createdAt;
}
