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
@Schema(description = "Tax Service Category DTO")
public class TaxServiceCategoryDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
