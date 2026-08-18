package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add Client Communication / Interaction Note Payload")
public class CreateClientNoteRequest {

    @NotNull(message = "Note type is required")
    @Schema(description = "Type of interaction", example = "MEETING")
    private NoteType noteType;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 255, message = "Title must be between 2 and 255 characters")
    @Schema(description = "Brief title or subject", example = "Quarterly Tax Strategy Review")
    private String title;

    @NotBlank(message = "Content is required")
    @Schema(description = "Detailed notes or minutes of meeting", example = "Reviewed Q3 advance tax calculation and reconciled TDS credit in 26AS...")
    private String content;
}
