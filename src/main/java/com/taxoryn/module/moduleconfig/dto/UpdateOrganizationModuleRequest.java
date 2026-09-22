package com.taxoryn.module.moduleconfig.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationModuleRequest {

    @NotNull(message = "Enabled state is required")
    private Boolean enabled;
}
