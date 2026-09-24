package com.taxoryn.module.client.dto;

import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service Catalog item with capability and entitlement context")
public class ServiceCatalogItemDto {

    @Schema(description = "Service engagement type enum")
    private ClientServiceType serviceType;

    @Schema(description = "Service unique code")
    private String code;

    @Schema(description = "Human-friendly service display name")
    private String displayName;

    @Schema(description = "Detailed service description")
    private String description;

    @Schema(description = "Service category (TAX, COMPLIANCE, ADVISORY, OPERATIONS)")
    private String category;

    @Schema(description = "Associated product module code")
    private ProductModuleCode associatedModule;

    @Schema(description = "Associated product capability")
    private ProductCapability associatedCapability;

    @Schema(description = "Frontend navigation route path")
    private String routePath;

    @Schema(description = "Whether the service is active in catalog")
    private boolean active;

    @Schema(description = "Whether the module is enabled in tenant configuration")
    private boolean moduleEnabled;

    @Schema(description = "Whether the subscription is entitled to this service")
    private boolean subscriptionEntitled;

    @Schema(description = "Access status: AVAILABLE, UPGRADE_REQUIRED, MODULE_DISABLED, SUBSCRIPTION_REQUIRED")
    private String accessStatus;
}
