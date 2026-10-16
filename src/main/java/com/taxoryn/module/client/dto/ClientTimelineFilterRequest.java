package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.TimelineEventCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter criteria for client timeline queries")
public class ClientTimelineFilterRequest {

    @Schema(description = "Filter by specific event category")
    private TimelineEventCategory category;

    @Schema(description = "Filter by specific event action/type code")
    private String eventType;

    @Schema(description = "Filter events occurred on or after this timestamp")
    private Instant from;

    @Schema(description = "Filter events occurred on or before this timestamp")
    private Instant to;

    @Schema(description = "Filter by coarse context type (e.g. DOCUMENT_REQUEST, APPROVAL, COMMUNICATION, COMPLIANCE)")
    private String contextType;

    @Schema(description = "Filter by specific context entity ID")
    private String contextId;

    @Schema(description = "Filter only events that are safely visible to client portal users")
    private Boolean clientVisibleOnly;

    @Schema(description = "Page number (0-indexed, default 0)")
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "Page size (default 20, max 100)")
    @Builder.Default
    private Integer size = 20;
}
