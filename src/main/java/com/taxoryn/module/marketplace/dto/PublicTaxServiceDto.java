package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer-facing public tax service catalog entry")
public class PublicTaxServiceDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String categoryName;
    private Integer sortOrder;
}
