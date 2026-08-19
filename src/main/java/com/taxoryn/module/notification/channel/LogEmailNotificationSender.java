package com.taxoryn.module.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "customEmailNotificationSender")
public class LogEmailNotificationSender implements EmailNotificationSender {

    @Override
    public boolean sendEmail(String recipientEmail, String recipientName, String subject, String content, Map<String, Object> templateData) {
        log.info("[EMAIL_DISPATCH] To: '{}' <{}> | Subject: '{}' | Content: {}",
                recipientName != null ? recipientName : "Recipient",
                recipientEmail,
                subject,
                content);
        return true;
    }

    @Override
    public String getProviderName() {
        return "LOG_EMAIL";
    }
}
