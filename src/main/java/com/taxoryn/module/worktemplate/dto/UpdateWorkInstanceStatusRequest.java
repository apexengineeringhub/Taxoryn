package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkInstanceStatusRequest {

    @NotNull(message = "Status is required")
    private WorkInstanceStatus status;

    private String notes;
}
