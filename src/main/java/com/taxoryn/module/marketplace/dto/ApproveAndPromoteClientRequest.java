package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Final Practice approval action to promote Onboarding record into Client Master and provision Client Portal credentials")
public class ApproveAndPromoteClientRequest {

    private UUID assignedEmployeeId;

    @Builder.Default
    @Schema(description = "Auto-create initial onboarding compliance task", example = "true")
    private Boolean createOnboardingTask = true;

    @Builder.Default
    @Schema(description = "Auto-provision user account with Client Portal login credentials", example = "true")
    private Boolean provisionClientPortalUser = true;

    @Schema(description = "Initial password for portal user (if null, auto-generated secure token is used)")
    private String initialPortalPassword;

    private String reviewerNotes;
}
