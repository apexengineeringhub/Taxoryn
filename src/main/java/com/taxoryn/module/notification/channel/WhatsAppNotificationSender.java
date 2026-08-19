package com.taxoryn.module.notification.channel;

import java.util.Map;

public interface WhatsAppNotificationSender {

    boolean sendWhatsApp(String phoneNumber, String message, String templateName, Map<String, Object> templateParams);

    String getProviderName();
}
