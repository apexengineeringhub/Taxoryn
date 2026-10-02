package com.taxoryn.module.lead.dto;

import com.taxoryn.module.client.dto.CreateClientRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConvertPracticeLeadRequest {
    @Valid @NotNull private CreateClientRequest client;
}
