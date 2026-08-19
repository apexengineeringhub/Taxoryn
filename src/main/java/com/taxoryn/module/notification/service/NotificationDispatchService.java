package com.taxoryn.module.notification.service;

import com.taxoryn.module.notification.channel.EmailNotificationSender;
import com.taxoryn.module.notification.channel.SmsNotificationSender;
import com.taxoryn.module.notification.channel.WhatsAppNotificationSender;
import com.taxoryn.module.notification.entity.NotificationEntity.DeliveryStatus;
import com.taxoryn.module.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Executes the actual out-of-band delivery for a persisted {@code NotificationEntity} on the
 * dedicated {@code notificationExecutor} thread pool, keeping the request thread free.
 * <p>
 * This class depends only on the {@link EmailNotificationSender}, {@link SmsNotificationSender},
 * and {@link WhatsAppNotificationSender} abstractions - never a concrete vendor SDK - so swapping
 * providers (e.g. SES/SendGrid for email, Twilio/MSG91 for SMS, Meta Cloud API for WhatsApp) only
 * requires registering a new bean for the relevant interface; nothing here changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationSender emailNotificationSender;
    private final SmsNotificationSender smsNotificationSender;
    private final WhatsAppNotificationSender whatsAppNotificationSender;

    @Async("notificationExecutor")
    @Transactional
    public void dispatchEmail(UUID notificationId, String recipientEmail, String recipientName, String subject, String message) {
        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("Skipping EMAIL dispatch for notification {}: recipient has no email on file", notificationId);
            notificationRepository.updateEmailStatus(notificationId, DeliveryStatus.FAILED);
            return;
        }
        try {
            boolean sent = emailNotificationSender.sendEmail(recipientEmail, recipientName, subject, message, Map.of());
            notificationRepository.updateEmailStatus(notificationId, sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
        } catch (Exception ex) {
            log.error("EMAIL dispatch failed for notification {}: {}", notificationId, ex.getMessage(), ex);
            notificationRepository.updateEmailStatus(notificationId, DeliveryStatus.FAILED);
        }
    }

    @Async("notificationExecutor")
    @Transactional
    public void dispatchSms(UUID notificationId, String recipientPhone, String message) {
        if (!StringUtils.hasText(recipientPhone)) {
            log.warn("Skipping SMS dispatch for notification {}: recipient has no phone on file", notificationId);
            notificationRepository.updateSmsStatus(notificationId, DeliveryStatus.FAILED);
            return;
        }
        try {
            boolean sent = smsNotificationSender.sendSms(recipientPhone, message);
            notificationRepository.updateSmsStatus(notificationId, sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
        } catch (Exception ex) {
            log.error("SMS dispatch failed for notification {}: {}", notificationId, ex.getMessage(), ex);
            notificationRepository.updateSmsStatus(notificationId, DeliveryStatus.FAILED);
        }
    }

    @Async("notificationExecutor")
    @Transactional
    public void dispatchWhatsApp(UUID notificationId, String recipientPhone, String message) {
        if (!StringUtils.hasText(recipientPhone)) {
            log.warn("Skipping WHATSAPP dispatch for notification {}: recipient has no phone on file", notificationId);
            notificationRepository.updateWhatsAppStatus(notificationId, DeliveryStatus.FAILED);
            return;
        }
        try {
            boolean sent = whatsAppNotificationSender.sendWhatsApp(recipientPhone, message, null, Map.of());
            notificationRepository.updateWhatsAppStatus(notificationId, sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
        } catch (Exception ex) {
            log.error("WHATSAPP dispatch failed for notification {}: {}", notificationId, ex.getMessage(), ex);
            notificationRepository.updateWhatsAppStatus(notificationId, DeliveryStatus.FAILED);
        }
    }
}
