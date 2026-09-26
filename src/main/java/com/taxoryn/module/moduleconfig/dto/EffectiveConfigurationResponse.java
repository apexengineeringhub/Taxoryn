package com.taxoryn.module.moduleconfig.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveConfigurationResponse {

    private UUID organizationId;
    private String subscriptionPlan;
    private String subscriptionStatus;
    private boolean multiLocationEnabled;
    private int maxLocations;
    private int activeLocationCount;
    private Map<String, Boolean> modules;
    private Map<String, Map<String, Boolean>> features;
    private List<String> navigationItems;
}
