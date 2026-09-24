package com.taxoryn.module.moduleconfig.dto;

import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
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
public class OrganizationModuleDto {

    private UUID organizationId;
    private ProductModuleCode moduleCode;
    private String moduleName;
    private String moduleDescription;
    private ProductModuleCategory category;
    private boolean enabled;
    private boolean explicitlyConfigured;
    private boolean entitled;
    private String subscriptionStatus;
    private boolean effectiveAccess;
    private String accessStatus;
    private String reason;
    private Instant updatedAt;
}
