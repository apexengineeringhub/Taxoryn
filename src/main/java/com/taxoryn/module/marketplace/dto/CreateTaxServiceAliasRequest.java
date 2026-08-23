package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a new search alias for a Tax Service")
public class CreateTaxServiceAliasRequest {

    @NotBlank(message = "Alias is required")
    @Size(max = 255, message = "Alias must not exceed 255 characters")
    @Schema(description = "Search synonym or abbreviation", example = "ITR-1")
    private String alias;

    @Builder.Default
    @Schema(description = "Active status", example = "true")
    private Boolean isActive = true;
}
