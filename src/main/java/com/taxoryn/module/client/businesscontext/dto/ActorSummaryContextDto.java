package com.taxoryn.module.client.businesscontext.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Actor & Authorization Context Model (Who is requesting and what are their permissions?)")
public class ActorSummaryContextDto {

    @Schema(description = "Current authenticated user ID")
    private UUID currentUserId;

    @Schema(description = "Current authenticated user email")
    private String currentUserEmail;

    @Schema(description = "Current organization / tenant ID")
    private UUID organizationId;

    @Schema(description = "Roles held by the current user")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Schema(description = "Whether the caller has permission to view client details")
    private boolean canAccessClient;

    @Schema(description = "Whether the caller has permission to view engagement details")
    private boolean canAccessEngagement;

    @Schema(description = "Whether the caller has permission to view work / task details")
    private boolean canAccessWork;
}
