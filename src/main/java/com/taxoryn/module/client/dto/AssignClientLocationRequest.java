package com.taxoryn.module.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to assign a location to a client")
public class AssignClientLocationRequest {

    @NotNull(message = "Location ID cannot be null")
    @Schema(description = "UUID of the practice location", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID locationId;

    @JsonProperty("isPrimaryLocation")
    @Builder.Default
    private boolean primaryLocation = false;

    @JsonProperty("isPrimaryLocation")
    public boolean isPrimaryLocation() {
        return primaryLocation;
    }

    @JsonProperty("isPrimaryLocation")
    public void setPrimaryLocation(boolean primaryLocation) {
        this.primaryLocation = primaryLocation;
    }
}
