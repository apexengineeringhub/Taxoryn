package com.taxoryn.module.service.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
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
@Schema(description = "Update Service Request Payload")
public class UpdateServiceRequest {

    @Size(min = 2, max = 255, message = "Service name must be between 2 and 255 characters")
    private String serviceName;

    private String description;
    private ServiceCategory category;
    private ServiceStatus status;
    private String moduleCode;
    private Boolean configurable;
}
