package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer-facing public tax service category with active child services")
public class PublicTaxServiceCategoryDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;

    @Builder.Default
    private List<PublicTaxServiceDto> services = new ArrayList<>();
}
