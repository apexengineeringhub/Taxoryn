package com.taxoryn.module.lead.dto;

import com.taxoryn.module.client.dto.CreateClientRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to convert a qualified lead into a client")
public class ConvertPracticeLeadRequest {
    @Valid
    @Schema(description = "Client aggregate details. If omitted or partial, fields will default from lead metadata.")
    private CreateClientRequest client;
}
