package com.taxoryn.module.notification.repository;

import com.taxoryn.module.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID>, JpaSpecificationExecutor<NotificationEntity> {

    Optional<NotificationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationIdAndUserIdAndIsReadFalse(UUID organizationId, UUID userId);

    long countByOrganizationIdAndClientIdAndIsReadFalse(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
            UUID organizationId, String entityType, String entityId, NotificationEntity.NotificationType notificationType, Instant createdAt);

    boolean existsByOrganizationIdAndClientIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
            UUID organizationId, UUID clientId, NotificationEntity.NotificationType notificationType, Instant createdAt);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.organizationId = :organizationId AND n.userId = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.organizationId = :organizationId AND n.clientId = :clientId AND n.isRead = false")
    int markAllAsReadForClient(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.emailStatus = :status WHERE n.id = :id")
    void updateEmailStatus(@Param("id") UUID id, @Param("status") NotificationEntity.DeliveryStatus status);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.smsStatus = :status WHERE n.id = :id")
    void updateSmsStatus(@Param("id") UUID id, @Param("status") NotificationEntity.DeliveryStatus status);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.whatsappStatus = :status WHERE n.id = :id")
    void updateWhatsAppStatus(@Param("id") UUID id, @Param("status") NotificationEntity.DeliveryStatus status);
}
