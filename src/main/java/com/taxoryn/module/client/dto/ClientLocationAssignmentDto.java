package com.taxoryn.module.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "Client Location Assignment Details")
public class ClientLocationAssignmentDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private UUID locationId;
    private String locationName;
    private String locationCode;
    private String city;
    private String state;

    @JsonProperty("isPrimaryLocation")
    private boolean primaryLocation;

    private Instant assignedAt;

    @JsonProperty("isActive")
    private boolean active;

    @JsonProperty("isPrimaryLocation")
    public boolean isPrimaryLocation() {
        return primaryLocation;
    }

    @JsonProperty("isPrimaryLocation")
    public void setPrimaryLocation(boolean primaryLocation) {
        this.primaryLocation = primaryLocation;
    }

    @JsonProperty("isActive")
    public boolean isActive() {
        return active;
    }

    @JsonProperty("isActive")
    public void setActive(boolean active) {
        this.active = active;
    }
}
