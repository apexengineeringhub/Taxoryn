package com.taxoryn.module.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "customWhatsAppNotificationSender")
public class LogWhatsAppNotificationSender implements WhatsAppNotificationSender {

    @Override
    public boolean sendWhatsApp(String phoneNumber, String message, String templateName, Map<String, Object> templateParams) {
        log.info("[WHATSAPP_DISPATCH] To Phone: {} | Template: {} | Message: {} | Params: {}",
                phoneNumber, templateName != null ? templateName : "NONE", message, templateParams);
        return true;
    }

    @Override
    public String getProviderName() {
        return "LOG_WHATSAPP";
    }
}
