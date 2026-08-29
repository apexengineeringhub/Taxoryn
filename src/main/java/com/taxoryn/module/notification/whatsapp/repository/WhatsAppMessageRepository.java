package com.taxoryn.module.notification.whatsapp.repository;

import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageEntity;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessageEntity, UUID> {

    List<WhatsAppMessageEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<WhatsAppMessageEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<WhatsAppMessageEntity> findByRecipientPhoneOrderByCreatedAtDesc(String recipientPhone);

    long countByStatus(WhatsAppMessageStatus status);

    long countByOrganizationIdAndStatus(UUID organizationId, WhatsAppMessageStatus status);

    @Query("SELECT m FROM WhatsAppMessageEntity m WHERE m.organizationId = :orgId OR (m.organizationId IS NULL AND :isSuperAdmin = true) ORDER BY m.createdAt DESC")
    Page<WhatsAppMessageEntity> findMessagesForContext(
            @Param("orgId") UUID orgId,
            @Param("isSuperAdmin") boolean isSuperAdmin,
            Pageable pageable
    );
}
