package com.taxoryn.module.notification.whatsapp.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("mockWhatsAppProvider")
public class MockWhatsAppProvider implements WhatsAppProvider {

    @Getter
    @Setter
    private boolean shouldFail = false;

    @Getter
    @Setter
    private String failureReason = "Simulated WhatsApp provider outage";

    @Getter
    private final List<SentRecord> sentRecords = new ArrayList<>();

    public record SentRecord(String phoneNumber, String templateName, Map<String, String> variables, String textMessage, String messageId) {}

    public void clear() {
        sentRecords.clear();
        shouldFail = false;
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public WhatsAppSendResult sendTemplate(String phoneNumber, String templateName, Map<String, String> variables) {
        if (shouldFail) {
            return WhatsAppSendResult.failure(getProviderName(), failureReason);
        }
        String msgId = "MOCK-WA-" + UUID.randomUUID().toString().substring(0, 8);
        sentRecords.add(new SentRecord(phoneNumber, templateName, variables, null, msgId));
        return WhatsAppSendResult.success(getProviderName(), msgId);
    }

    @Override
    public WhatsAppSendResult sendTextMessage(String phoneNumber, String messageText) {
        if (shouldFail) {
            return WhatsAppSendResult.failure(getProviderName(), failureReason);
        }
        String msgId = "MOCK-WA-" + UUID.randomUUID().toString().substring(0, 8);
        sentRecords.add(new SentRecord(phoneNumber, null, Map.of(), messageText, msgId));
        return WhatsAppSendResult.success(getProviderName(), msgId);
    }
}
