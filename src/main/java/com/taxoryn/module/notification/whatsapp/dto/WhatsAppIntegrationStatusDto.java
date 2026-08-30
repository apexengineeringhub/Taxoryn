package com.taxoryn.module.notification.whatsapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "WhatsApp Provider and Integration Status")
public class WhatsAppIntegrationStatusDto {
    boolean enabled;
    String provider;
    String baseUrl;
    boolean phoneNumberIdConfigured;
    boolean accessTokenConfigured;
    long totalMessagesSent;
    long totalMessagesFailed;
    long totalMessagesPending;
}
