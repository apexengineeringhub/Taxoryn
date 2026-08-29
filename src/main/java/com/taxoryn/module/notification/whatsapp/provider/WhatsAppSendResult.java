package com.taxoryn.module.notification.whatsapp.provider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WhatsAppSendResult {
    boolean success;
    String providerName;
    String providerMessageId;
    String status; // e.g. SENT, FAILED
    String errorMessage;

    public static WhatsAppSendResult success(String providerName, String providerMessageId) {
        return WhatsAppSendResult.builder()
                .success(true)
                .providerName(providerName)
                .providerMessageId(providerMessageId)
                .status("SENT")
                .build();
    }

    public static WhatsAppSendResult failure(String providerName, String errorMessage) {
        return WhatsAppSendResult.builder()
                .success(false)
                .providerName(providerName)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
