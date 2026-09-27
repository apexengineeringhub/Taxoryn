package com.taxoryn.module.task.dto;

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
@Schema(description = "Request payload to assign a Work Item to a user")
public class AssignWorkItemRequest {

    @NotNull(message = "Assigned user ID is required")
    @Schema(description = "Target assigned user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID assignedUserId;

    @Schema(description = "Optional assignment notes")
    private String notes;
}
