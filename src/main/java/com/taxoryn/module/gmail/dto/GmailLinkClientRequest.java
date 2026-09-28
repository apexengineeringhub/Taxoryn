package com.taxoryn.module.gmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to link a conversation to a Taxoryn client")
public class GmailLinkClientRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Target Client ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID clientId;
}
