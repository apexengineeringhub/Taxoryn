package com.taxoryn.module.engagement.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.dto.EngagementDto;
import com.taxoryn.module.engagement.dto.EngagementFilterRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;

import java.util.List;
import java.util.UUID;

public interface EngagementService {

    EngagementDto createEngagement(CreateEngagementRequest request);

    EngagementDto getEngagementById(UUID id);

    PagedResponse<EngagementDto> getEngagements(EngagementFilterRequest filterRequest);

    List<EngagementDto> getEngagementsByClientId(UUID clientId);

    EngagementDto updateEngagement(UUID id, UpdateEngagementRequest request);

    EngagementDto updateEngagementStatus(UUID id, UpdateEngagementStatusRequest request);

    void deleteEngagement(UUID id);
}
