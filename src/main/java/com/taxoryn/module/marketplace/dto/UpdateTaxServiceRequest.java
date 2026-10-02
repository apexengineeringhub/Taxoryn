package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing Tax Service Master entry (Service code is immutable)")
public class UpdateTaxServiceRequest {

    @Schema(description = "Category UUID", example = "c0000001-0000-0000-0000-000000000001")
    private UUID categoryId;

    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Customer-facing service title", example = "Income Tax Return Preparation & Filing")
    private String name;

    @Schema(description = "Customer-friendly description")
    private String description;

    @Schema(description = "Sort order index", example = "1")
    private Integer sortOrder;

    @Schema(description = "Active status flag", example = "true")
    private Boolean isActive;

    @DecimalMin(value = "0.00", message = "Suggested price cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Suggested price must have at most two decimal places")
    private BigDecimal suggestedPrice;

    @Size(min = 3, max = 3)
    private String currency;

    @Pattern(regexp = "^(ONE_TIME|PER_RETURN|PER_MONTH|PER_QUARTER|PER_SESSION|PER_NOTICE)$")
    @Size(max = 30)
    private String billingType;
}
