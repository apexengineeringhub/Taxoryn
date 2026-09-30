package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkTemplateStatusRequest {

    @NotNull(message = "Target work template status is required")
    private WorkTemplateStatus status;
}
