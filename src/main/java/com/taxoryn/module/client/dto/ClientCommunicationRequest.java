package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.entity.ClientNoteEntity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class ClientCommunicationRequest {
    @NotNull
    private NoteType communicationType = NoteType.NOTE;

    @Size(max = 255)
    private String subject;

    @NotBlank
    private String content;

    @NotNull
    private Instant occurredAt;

    private Visibility visibility = Visibility.INTERNAL;

    private boolean followUpRequired;

    private Instant followUpDate;
}
