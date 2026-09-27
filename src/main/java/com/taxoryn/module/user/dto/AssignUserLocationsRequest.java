package com.taxoryn.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to assign locations to a user")
public class AssignUserLocationsRequest {

    @NotNull(message = "Location IDs list cannot be null")
    @Schema(description = "List of location UUIDs belonging to the organization", example = "[\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\"]")
    private List<UUID> locationIds;
}
