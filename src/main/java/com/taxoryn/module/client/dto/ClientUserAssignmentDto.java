package com.taxoryn.module.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxoryn.module.client.entity.ClientAssignmentRole;
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
@Schema(description = "Client User Portfolio Assignment Details")
public class ClientUserAssignmentDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private ClientAssignmentRole assignmentRole;

    @JsonProperty("isPrimaryResponsible")
    private boolean primaryResponsible;

    private Instant assignedAt;

    @JsonProperty("isActive")
    private boolean active;

    @JsonProperty("isPrimaryResponsible")
    public boolean isPrimaryResponsible() {
        return primaryResponsible;
    }

    @JsonProperty("isPrimaryResponsible")
    public void setPrimaryResponsible(boolean primaryResponsible) {
        this.primaryResponsible = primaryResponsible;
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
