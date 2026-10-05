package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.ClientTimelineFilterRequest;
import com.taxoryn.module.client.dto.ClientTimelineItemDto;
import com.taxoryn.module.client.entity.TimelineEventCategory;
import com.taxoryn.module.client.service.ClientTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/timeline")
@RequiredArgsConstructor
@Tag(name = "Client Timeline", description = "Client activity, events, and audit timeline projection endpoints")
public class ClientTimelineController {

    private final ClientTimelineService clientTimelineService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_READ') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_STAFF') or hasRole('PARTNER')")
    @Operation(summary = "Get paginated, filterable timeline of client events and audit activities")
    public ResponseEntity<ApiResponse<PagedResponse<ClientTimelineItemDto>>> getClientTimeline(
            @PathVariable UUID clientId,
            @RequestParam(required = false) TimelineEventCategory category,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ClientTimelineFilterRequest filter = ClientTimelineFilterRequest.builder()
                .category(category)
                .eventType(eventType)
                .from(from)
                .to(to)
                .page(page)
                .size(size)
                .build();

        PagedResponse<ClientTimelineItemDto> response = clientTimelineService.getClientTimeline(clientId, filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
