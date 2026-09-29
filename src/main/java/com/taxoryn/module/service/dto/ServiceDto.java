package com.taxoryn.module.service.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service Catalog Item Model")
public class ServiceDto {

    private UUID id;
    private UUID organizationId;
    private String serviceCode;
    private String serviceName;
    private String description;
    private ServiceCategory category;
    private ServiceStatus status;
    private boolean configurable;
    private String moduleCode;
    private boolean isGlobal;
    private boolean isAvailable; // Whether practice's subscription / module config allows this service
    private String availabilityReason;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
