package com.taxoryn.module.notification.whatsapp.provider;

import java.util.Map;

public interface WhatsAppProvider {

    String getProviderName();

    WhatsAppSendResult sendTemplate(
            String phoneNumber,
            String templateName,
            Map<String, String> variables
    );

    WhatsAppSendResult sendTextMessage(
            String phoneNumber,
            String messageText
    );

    WhatsAppSendResult sendDocument(
            String phoneNumber,
            String documentUrl,
            String filename,
            String caption
    );
}
