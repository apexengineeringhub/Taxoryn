package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.entity.ClientNoteEntity.Visibility;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ClientCommunicationDto {
    UUID id;
    UUID clientId;
    NoteType communicationType;
    String subject;
    String content;
    Instant occurredAt;
    Visibility visibility;
    boolean followUpRequired;
    Instant followUpDate;
    UUID createdBy;
    String createdByName;
    Instant createdAt;
    Instant updatedAt;
}
