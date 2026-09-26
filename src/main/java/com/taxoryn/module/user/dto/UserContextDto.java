package com.taxoryn.module.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxoryn.module.moduleconfig.dto.EffectiveConfigurationResponse;
import com.taxoryn.module.organization.dto.LocationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated current user context payload including organization, roles, location scope, and effective modules")
public class UserContextDto {

    private UserDto user;
    private UUID organizationId;
    private String organizationName;
    private String practiceType;
    private Set<String> roles;
    private LocationScopeDto locationScope;
    private EffectiveConfigurationResponse effectiveConfiguration;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Location access scope representation")
    public static class LocationScopeDto {
        private String scopeType; // "ALL_LOCATIONS" or "ASSIGNED_LOCATIONS"

        @JsonProperty("isAllLocations")
        private boolean isAllLocations;

        private Set<UUID> assignedLocationIds;
        private List<LocationDto> accessibleLocations;

        @JsonProperty("isAllLocations")
        public boolean isAllLocations() {
            return isAllLocations;
        }

        @JsonProperty("isAllLocations")
        public void setAllLocations(boolean isAllLocations) {
            this.isAllLocations = isAllLocations;
        }
    }
}
