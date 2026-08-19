package com.taxoryn.module.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "customSmsNotificationSender")
public class LogSmsNotificationSender implements SmsNotificationSender {

    @Override
    public boolean sendSms(String phoneNumber, String message) {
        log.info("[SMS_DISPATCH] To Phone: {} | Message: {}", phoneNumber, message);
        return true;
    }

    @Override
    public String getProviderName() {
        return "LOG_SMS";
    }
}
