package com.taxoryn.module.client.businesscontext.dto;

import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Level 2: Engagement Context Model (What engagement contract governs this?)")
public class EngagementSummaryContextDto {

    @Schema(description = "Engagement ID")
    private UUID engagementId;

    @Schema(description = "Engagement reference code")
    private String engagementCode;

    @Schema(description = "Engagement title / name")
    private String engagementName;

    @Schema(description = "Engagement status")
    private EngagementStatus status;

    @Schema(description = "Engagement priority")
    private EngagementPriority priority;

    @Schema(description = "Associated catalog service offering ID")
    private UUID serviceId;

    @Schema(description = "Associated service offering name")
    private String serviceName;

    @Schema(description = "Associated client service relationship ID")
    private UUID clientServiceId;

    @Schema(description = "Assigned lead practitioner / employee user ID")
    private UUID assignedUserId;

    @Schema(description = "Assigned lead practitioner name")
    private String assignedUserName;

    @Schema(description = "Reviewer practitioner / employee user ID")
    private UUID reviewerUserId;

    @Schema(description = "Reviewer practitioner name")
    private String reviewerUserName;

    @Schema(description = "Engagement start date")
    private LocalDate startDate;

    @Schema(description = "Engagement expected end date")
    private LocalDate endDate;

    @Schema(description = "Whether the engagement is active (NOT_STARTED, IN_PROGRESS, IN_REVIEW)")
    private boolean active;
}
