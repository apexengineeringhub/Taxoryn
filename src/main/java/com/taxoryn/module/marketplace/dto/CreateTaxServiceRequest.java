package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new Tax Service Master entry")
public class CreateTaxServiceRequest {

    @NotNull(message = "Category ID is required")
    @Schema(description = "UUID of the parent tax service category", example = "c0000001-0000-0000-0000-000000000001")
    private UUID categoryId;

    @NotBlank(message = "Service code is required")
    @Size(min = 2, max = 100, message = "Code must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must contain only uppercase letters, numbers, and underscores")
    @Schema(description = "Unique stable machine-readable service code", example = "INCOME_TAX_RETURN")
    private String code;

    @NotBlank(message = "Service name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Customer-facing service title", example = "Income Tax Return Filing")
    private String name;

    @Schema(description = "Customer-friendly description of what this service delivers", example = "Prepare and file your annual income tax return.")
    private String description;

    @Builder.Default
    @Schema(description = "Sort order index", example = "1")
    private Integer sortOrder = 0;

    @Builder.Default
    @Schema(description = "Active status flag", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Initial search aliases for customer discovery", example = "[\"ITR\", \"IT Return\", \"Income Tax Filing\"]")
    private List<String> aliases;
}
