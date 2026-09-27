package com.taxoryn.module.moduleconfig.dto;

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
public class OrganizationFeatureDto {

    private UUID organizationId;
    private String moduleCode;
    private String featureCode;
    private String featureName;
    private String featureDescription;
    private boolean enabled;
    private boolean explicitlyConfigured;
    private boolean entitled;
    private boolean effectiveAccess;
    private Instant updatedAt;
}
