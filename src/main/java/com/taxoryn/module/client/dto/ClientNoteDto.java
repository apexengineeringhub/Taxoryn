package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Communication / Interaction Note Details")
public class ClientNoteDto {

    @Schema(description = "Note ID")
    private UUID id;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Author user/practitioner ID")
    private UUID authorId;

    @Schema(description = "Author practitioner full name", example = "Vikram Sharma")
    private String authorName;

    @Schema(description = "Interaction type", example = "MEETING")
    private NoteType noteType;

    @Schema(description = "Note title / subject", example = "GST Audit Scope Alignment Discussion")
    private String title;

    @Schema(description = "Detailed note content", example = "Met with CFO to finalize timeline for GST annual return filing...")
    private String content;

    @Schema(description = "Created timestamp")
    private Instant createdAt;
}
