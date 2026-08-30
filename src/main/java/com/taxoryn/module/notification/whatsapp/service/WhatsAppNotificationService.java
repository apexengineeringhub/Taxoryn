package com.taxoryn.module.notification.whatsapp.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppIntegrationStatusDto;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppMessageDto;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageEntity;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WhatsAppNotificationService {

    WhatsAppMessageEntity sendWelcomeMessage(UserRegisteredEvent event);

    WhatsAppMessageEntity sendInvoiceIssuedMessage(com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent event);

    WhatsAppMessageEntity sendPaymentReceivedMessage(com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent event);

    WhatsAppMessageEntity sendInvoiceReminderMessage(com.taxoryn.module.notification.whatsapp.event.InvoiceReminderEvent event);

    WhatsAppMessageEntity sendTextMessage(UUID organizationId, UUID userId, String rawPhone, String messageText);

    void processWebhookPayload(String payload, String signature);

    WhatsAppMessageDto resendMessage(UUID messageId);

    WhatsAppIntegrationStatusDto getIntegrationStatus();

    PagedResponse<WhatsAppMessageDto> getMessages(Pageable pageable);
}
