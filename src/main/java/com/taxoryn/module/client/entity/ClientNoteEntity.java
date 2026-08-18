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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    public enum NoteType {
        CALL,
        EMAIL,
        MEETING,
        FOLLOW_UP,
        GENERAL
    }
}
