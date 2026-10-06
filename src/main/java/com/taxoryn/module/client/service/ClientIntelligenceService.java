package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientActionRecommendationDto;
import com.taxoryn.module.client.dto.ClientIntelligenceSummaryDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for deterministic rule-based client intelligence, attention signals, and next-best-action recommendations.
 */
public interface ClientIntelligenceService {

    ClientIntelligenceSummaryDto evaluateClient(UUID organizationId, UUID clientId);

    ClientIntelligenceSummaryDto evaluateClient(UUID clientId);

    List<ClientActionRecommendationDto> getRecommendations(UUID clientId);

    int countAttentionSignals(UUID organizationId, UUID clientId);

    int countHighPrioritySignals(UUID organizationId, UUID clientId);
}
