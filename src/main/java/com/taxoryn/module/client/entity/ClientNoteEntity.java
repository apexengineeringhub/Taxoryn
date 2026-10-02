package com.taxoryn.module.client.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "client_notes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientNoteEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 50)
    @Builder.Default
    private NoteType noteType = NoteType.GENERAL;

    @Column(name = "title")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 24)
    @Builder.Default
    private Visibility visibility = Visibility.INTERNAL;

    @Column(name = "follow_up_required", nullable = false)
    @Builder.Default
    private boolean followUpRequired = false;

    @Column(name = "follow_up_date")
    private Instant followUpDate;

    public enum NoteType {
        NOTE,
        PHONE_CALL,
        CALL,
        EMAIL,
        MEETING,
        WHATSAPP,
        CLIENT_PORTAL,
        DOCUMENT_REQUEST,
        TASK,
        COMPLIANCE,
        NOTICE,
        BILLING,
        SYSTEM_EVENT,
        FOLLOW_UP,
        GENERAL
    }

    public enum Visibility {
        INTERNAL,
        CLIENT_VISIBLE
    }
}
