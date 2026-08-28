package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_enquiry_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceEnquiryMessageEntity extends AuditableEntity {

    @Column(name = "enquiry_id", nullable = false)
    private UUID enquiryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", insertable = false, updatable = false)
    private MarketplaceLeadEntity enquiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private MessageSenderType senderType;

    @Column(name = "sender_user_id")
    private UUID senderUserId;

    @Column(name = "sender_name", nullable = false, length = 150)
    private String senderName;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "is_read_by_customer", nullable = false)
    @Builder.Default
    private Boolean isReadByCustomer = false;

    @Column(name = "is_read_by_practice", nullable = false)
    @Builder.Default
    private Boolean isReadByPractice = false;

    @Column(name = "read_at")
    private Instant readAt;
}
