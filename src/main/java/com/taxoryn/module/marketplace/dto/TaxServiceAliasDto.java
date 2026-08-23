package com.taxoryn.module.marketplace.dto;

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
@Schema(description = "Tax Service Alias DTO")
public class TaxServiceAliasDto {

    private UUID id;
    private UUID taxServiceId;
    private String alias;
    private String normalizedAlias;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
