package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Controlled Tax Service Master DTO")
public class TaxServiceDto {

    private UUID id;
    private UUID categoryId;
    private String categoryCode;
    private String categoryName;
    private String code;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private List<TaxServiceAliasDto> aliases;
    private Instant createdAt;
    private Instant updatedAt;
}
