package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to create a new government connection for an organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGovConnectionRequest {

    @NotNull(message = "Provider type is required")
    private GovProviderType providerType;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String description;

    @Builder.Default
    private String environment = "PRODUCTION";

    private String metadata;
}
