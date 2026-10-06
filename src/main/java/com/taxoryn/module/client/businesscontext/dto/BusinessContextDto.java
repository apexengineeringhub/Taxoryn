package com.taxoryn.module.client.businesscontext.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unified, lightweight, cross-domain Business Context read model")
public class BusinessContextDto {

    @Schema(description = "Resolved organization / tenant ID")
    private UUID organizationId;

    @Schema(description = "Level 1: Client Context (Who is the client?)")
    private ClientSummaryContextDto client;

    @Schema(description = "Level 2: Service Context (What service offering is engaged?)")
    private ServiceSummaryContextDto service;

    @Schema(description = "Level 2: Engagement Context (What engagement contract governs this?)")
    private EngagementSummaryContextDto engagement;

    @Schema(description = "Level 2: Work & Task Context (What work is currently being executed?)")
    private WorkSummaryContextDto work;

    @Schema(description = "Level 3: Attention & Intelligence Context (What signals need review?)")
    private AttentionSummaryContextDto attention;

    @Schema(description = "Temporal Context (Due dates, periods, deadlines)")
    private TemporalSummaryContextDto temporalContext;

    @Schema(description = "Actor Context (Current user & resolution context)")
    private ActorSummaryContextDto actorContext;
}
