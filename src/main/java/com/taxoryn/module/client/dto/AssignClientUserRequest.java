package com.taxoryn.module.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxoryn.module.client.entity.ClientAssignmentRole;
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
@Schema(description = "Request payload to assign a team member/user to a client portfolio")
public class AssignClientUserRequest {

    @NotNull(message = "User ID cannot be null")
    @Schema(description = "UUID of the user", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @Builder.Default
    @Schema(description = "Role in client engagement", example = "PRIMARY")
    private ClientAssignmentRole assignmentRole = ClientAssignmentRole.PRIMARY;

    @JsonProperty("isPrimaryResponsible")
    @Builder.Default
    private boolean primaryResponsible = false;

    @JsonProperty("isPrimaryResponsible")
    public boolean isPrimaryResponsible() {
        return primaryResponsible;
    }

    @JsonProperty("isPrimaryResponsible")
    public void setPrimaryResponsible(boolean primaryResponsible) {
        this.primaryResponsible = primaryResponsible;
    }
}
