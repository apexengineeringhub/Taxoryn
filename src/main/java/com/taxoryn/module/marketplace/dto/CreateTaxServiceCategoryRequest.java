package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new Tax Service Category Master entry")
public class CreateTaxServiceCategoryRequest {

    @NotBlank(message = "Category code is required")
    @Size(min = 2, max = 100, message = "Code must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must contain only uppercase letters, numbers, and underscores")
    @Schema(description = "Machine-readable unique category code", example = "INCOME_TAX")
    private String code;

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Human-readable display name", example = "Income Tax")
    private String name;

    @Schema(description = "Category description", example = "Direct tax returns, notices, assessments and refunds")
    private String description;

    @Schema(description = "Lucide icon identifier", example = "FileText")
    private String icon;

    @Builder.Default
    @Schema(description = "Sort order index", example = "1")
    private Integer sortOrder = 0;

    @Builder.Default
    @Schema(description = "Active status flag", example = "true")
    private Boolean isActive = true;
}
