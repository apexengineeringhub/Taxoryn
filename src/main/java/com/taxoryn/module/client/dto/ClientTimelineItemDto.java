package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.TimelineEventCategory;
import com.taxoryn.module.client.entity.TimelineEventSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Normalized client timeline entry projection")
public class ClientTimelineItemDto {

    @Schema(description = "Unique timeline item ID")
    private UUID id;

    @Schema(description = "Associated client ID")
    private UUID clientId;

    @Schema(description = "Owning organization / tenant ID")
    private UUID organizationId;

    @Schema(description = "Event type code (e.g. CLIENT_CREATED, SERVICE_ADDED, STATUS_CHANGED)")
    private String eventType;

    @Schema(description = "Categorization for timeline filtering")
    private TimelineEventCategory eventCategory;

    @Schema(description = "Human-readable event title")
    private String title;

    @Schema(description = "Detailed event description or narrative")
    private String description;

    @Schema(description = "Timestamp when the event occurred")
    private Instant occurredAt;

    @Schema(description = "User ID who triggered the event, or null if system-generated")
    private UUID actorId;

    @Schema(description = "Name of the actor or practitioner")
    private String actorName;

    @Schema(description = "Source subsystem or module")
    private String sourceModule;

    @Schema(description = "Visual severity / status indication")
    private TimelineEventSeverity severity;

    @Schema(description = "ID of related entity (e.g. serviceId, contactId, invoiceId)")
    private String relatedEntityId;

    @Schema(description = "Type name of related entity (e.g. CLIENT_SERVICE, CLIENT_CONTACT)")
    private String relatedEntityType;

    @Schema(description = "Additional context or payload details")
    private Map<String, Object> metadata;

    @Schema(description = "Direct deep-link action URL in Taxoryn UI")
    private String actionUrl;

    @Schema(description = "Whether this timeline event is safely visible to client portal users")
    @Builder.Default
    private boolean clientVisible = false;

    @Schema(description = "Coarse context category (e.g. DOCUMENT_REQUEST, APPROVAL, COMMUNICATION, COMPLIANCE)")
    private String contextType;

    @Schema(description = "Target context entity ID string")
    private String contextId;
}
