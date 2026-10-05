package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.client.dto.ClientActionRecommendationDto;
import com.taxoryn.module.client.dto.ClientIntelligenceSummaryDto;
import com.taxoryn.module.client.service.ClientIntelligenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{clientId}")
@RequiredArgsConstructor
@Tag(name = "Client Intelligence", description = "Deterministic client intelligence, attention signals, and next-best-action recommendations")
public class ClientIntelligenceController {

    private final ClientIntelligenceService clientIntelligenceService;

    @GetMapping("/intelligence")
    @PreAuthorize("hasAuthority('CLIENT_READ') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_STAFF') or hasRole('PARTNER')")
    @Operation(summary = "Get deterministic intelligence signals and attention summary for a client")
    public ResponseEntity<ApiResponse<ClientIntelligenceSummaryDto>> getClientIntelligence(@PathVariable UUID clientId) {
        ClientIntelligenceSummaryDto summary = clientIntelligenceService.evaluateClient(clientId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('CLIENT_READ') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_STAFF') or hasRole('PARTNER')")
    @Operation(summary = "Get prioritized next-best-action recommendations for a client")
    public ResponseEntity<ApiResponse<List<ClientActionRecommendationDto>>> getClientRecommendations(@PathVariable UUID clientId) {
        List<ClientActionRecommendationDto> recommendations = clientIntelligenceService.getRecommendations(clientId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }
}
