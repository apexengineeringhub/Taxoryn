package com.taxoryn.module.lead.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "practice_lead_activities")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PracticeLeadActivityEntity extends TenantAuditableEntity {
    @Column(name = "lead_id", nullable = false) private UUID leadId;
    @Enumerated(EnumType.STRING) @Column(name = "activity_type", nullable = false, length = 24) private ActivityType activityType;
    @Column(length = 255) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "author_name", length = 255) private String authorName;
    public enum ActivityType { NOTE, EMAIL, PHONE_CALL, MEETING, WHATSAPP, SYSTEM_EVENT }
}
