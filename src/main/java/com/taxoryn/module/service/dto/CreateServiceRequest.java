package com.taxoryn.module.service.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Custom Service Request Payload")
public class CreateServiceRequest {

    @NotBlank(message = "Service code is required")
    @Size(min = 2, max = 100, message = "Service code must be between 2 and 100 characters")
    private String serviceCode;

    @NotBlank(message = "Service name is required")
    @Size(min = 2, max = 255, message = "Service name must be between 2 and 255 characters")
    private String serviceName;

    private String description;

    @NotNull(message = "Service category is required")
    private ServiceCategory category;

    @DecimalMin(value = "0.00", message = "Default price cannot be negative")
    private BigDecimal defaultPrice;

    @Builder.Default
    private String billingUnit = "PER_APPLICATION";

    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("18.00");

    private String moduleCode;

    @Builder.Default
    private boolean configurable = true;
}
