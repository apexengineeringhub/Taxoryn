package com.taxoryn.module.notification.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notification.dto.NotificationDto;
import com.taxoryn.module.notification.dto.NotificationFilterRequest;
import com.taxoryn.module.notification.dto.SendNotificationRequest;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;

import java.util.Set;
import java.util.UUID;

/**
 * Central abstraction for the notification engine.
 * <p>
 * Every notification, regardless of source (user action, background scheduler, or another
 * module reacting to a domain event), is recorded in-app first and then fanned out
 * asynchronously to any additionally requested channel (Email / SMS / WhatsApp) through the
 * pluggable {@code *NotificationSender} interfaces. Callers of this service never talk to a
 * concrete provider directly, which keeps the application decoupled from any specific
 * Email/SMS/WhatsApp vendor.
 */
public interface NotificationService {

    /**
     * Dispatches a notification on behalf of an authenticated caller (e.g. an admin manually
     * notifying a user/client, or a REST client of the notification API).
     * Resolves the organization from the current tenant context.
     */
    NotificationDto send(SendNotificationRequest request);

    /**
     * Core, tenant-explicit entry point used internally by other modules and scheduled jobs
     * (which typically run outside of an authenticated HTTP request, e.g. under
     * {@code TenantContext}) to raise a notification without needing a security context.
     *
     * @param organizationId tenant that owns this notification
     * @param userId         recipient firm user id (nullable if targeting a client)
     * @param clientId       recipient client id (nullable if targeting a firm user)
     * @param notificationType classification of the notification
     * @param title          short notification title
     * @param message        notification body
     * @param channels       delivery channels to attempt in addition to the always-recorded in-app entry
     * @param actionUrl      optional deep link / action URL
     * @param metadata       optional JSON/contextual metadata blob
     */
    NotificationDto notify(UUID organizationId,
                            UUID userId,
                            UUID clientId,
                            NotificationType notificationType,
                            String title,
                            String message,
                            Set<NotificationChannel> channels,
                            String actionUrl,
                            String metadata);

    /**
     * Retrieves the paginated in-app notification history for the current recipient
     * (firm user or client-portal user), newest first, with optional read/type filters.
     */
    PagedResponse<NotificationDto> getNotifications(NotificationFilterRequest filterRequest);

    /**
     * Returns the count of unread in-app notifications for the current recipient.
     */
    long getUnreadCount();

    /**
     * Marks a single notification belonging to the current recipient as read.
     */
    NotificationDto markAsRead(UUID notificationId);

    /**
     * Marks every unread notification belonging to the current recipient as read.
     *
     * @return number of notifications updated
     */
    int markAllAsRead();

    /**
     * Deletes/dismisses a notification belonging to the current recipient.
     */
    void deleteNotification(UUID notificationId);
}
