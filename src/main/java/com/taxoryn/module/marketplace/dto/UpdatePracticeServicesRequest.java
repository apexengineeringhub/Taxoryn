package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(description = "Request to update/select practice services from the controlled master")
public class UpdatePracticeServicesRequest {

    @NotEmpty(message = "At least one tax service must be selected")
    @Schema(description = "List of active TaxService UUIDs offered by the practice")
    private List<UUID> taxServiceIds;
}
