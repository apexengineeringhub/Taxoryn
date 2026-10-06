package com.taxoryn.module.client.businesscontext.provider;

import com.taxoryn.module.client.businesscontext.dto.AttentionSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.dto.ClientIntelligenceSummaryDto;
import com.taxoryn.module.client.service.ClientIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntelligenceSummaryContextProvider implements BusinessContextProvider<AttentionSummaryContextDto> {

    public static final String PROVIDER_KEY = "INTELLIGENCE_CONTEXT";

    private final ClientIntelligenceService clientIntelligenceService;

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public AttentionSummaryContextDto resolve(BusinessContextRequest request, UUID organizationId, UUID resolvedClientId) {
        if (resolvedClientId == null) {
            return null;
        }

        try {
            ClientIntelligenceSummaryDto intelligence = clientIntelligenceService.evaluateClient(organizationId, resolvedClientId);
            if (intelligence == null) {
                return AttentionSummaryContextDto.builder()
                        .totalAttentionSignalsCount(0)
                        .highPrioritySignalsCount(0)
                        .signals(new ArrayList<>())
                        .recommendedActions(new ArrayList<>())
                        .build();
            }

            return AttentionSummaryContextDto.builder()
                    .totalAttentionSignalsCount(intelligence.getTotalSignalsCount())
                    .highPrioritySignalsCount(intelligence.getHighPrioritySignalsCount())
                    .signals(intelligence.getNeedsAttentionSignals() != null ? intelligence.getNeedsAttentionSignals() : new ArrayList<>())
                    .recommendedActions(intelligence.getRecommendations() != null ? intelligence.getRecommendations() : new ArrayList<>())
                    .build();
        } catch (Exception e) {
            log.debug("No intelligence summary available for client: {}", resolvedClientId, e);
            return AttentionSummaryContextDto.builder()
                    .totalAttentionSignalsCount(0)
                    .highPrioritySignalsCount(0)
                    .signals(new ArrayList<>())
                    .recommendedActions(new ArrayList<>())
                    .build();
        }
    }
}
