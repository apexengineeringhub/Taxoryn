package com.taxoryn.module.notification.channel;

import java.util.Map;

public interface EmailNotificationSender {

    boolean sendEmail(String recipientEmail, String recipientName, String subject, String content, Map<String, Object> templateData);

    String getProviderName();
}
