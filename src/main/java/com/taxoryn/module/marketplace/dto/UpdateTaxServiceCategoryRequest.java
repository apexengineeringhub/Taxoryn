package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing Tax Service Category Master entry")
public class UpdateTaxServiceCategoryRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Human-readable display name", example = "Income Tax")
    private String name;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Lucide icon identifier", example = "FileText")
    private String icon;

    @Schema(description = "Sort order index", example = "1")
    private Integer sortOrder;

    @Schema(description = "Active status flag", example = "true")
    private Boolean isActive;
}
