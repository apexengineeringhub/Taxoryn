package com.taxoryn.module.client.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.ClientTimelineFilterRequest;
import com.taxoryn.module.client.dto.ClientTimelineItemDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for projecting normalized client activity, events, and audit trail into a unified timeline.
 */
public interface ClientTimelineService {

    PagedResponse<ClientTimelineItemDto> getClientTimeline(UUID clientId, ClientTimelineFilterRequest filter);

    List<ClientTimelineItemDto> getRecentTimeline(UUID organizationId, UUID clientId, int limit);
}
