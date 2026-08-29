package com.taxoryn.module.notification.whatsapp.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component("logWhatsAppProvider")
public class LogWhatsAppProvider implements WhatsAppProvider {

    @Override
    public String getProviderName() {
        return "LOG";
    }

    @Override
    public WhatsAppSendResult sendTemplate(String phoneNumber, String templateName, Map<String, String> variables) {
        String msgId = "LOG-WA-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[WHATSAPP_LOG_PROVIDER] Dispatched template '{}' to phone '{}' | msgId: {} | variables: {}",
                templateName, phoneNumber, msgId, variables);
        return WhatsAppSendResult.success(getProviderName(), msgId);
    }

    @Override
    public WhatsAppSendResult sendTextMessage(String phoneNumber, String messageText) {
        String msgId = "LOG-WA-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[WHATSAPP_LOG_PROVIDER] Dispatched text message to phone '{}' | msgId: {} | text: {}",
                phoneNumber, msgId, messageText);
        return WhatsAppSendResult.success(getProviderName(), msgId);
    }
}
