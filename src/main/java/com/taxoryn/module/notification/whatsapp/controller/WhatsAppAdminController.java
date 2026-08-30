package com.taxoryn.module.notification.whatsapp.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppIntegrationStatusDto;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppMessageDto;
import com.taxoryn.module.notification.whatsapp.service.WhatsAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/notifications/whatsapp", "/api/notifications/whatsapp"})
@RequiredArgsConstructor
@Tag(name = "WhatsApp Integration", description = "Endpoints for WhatsApp integration health and delivery logs")
public class WhatsAppAdminController {

    private final WhatsAppNotificationService whatsAppNotificationService;

    @GetMapping("/status")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPPORT_ADMIN')")
    @Operation(summary = "Get WhatsApp Integration Status", description = "Returns active provider, configuration status, and aggregate dispatch metrics")
    public ResponseEntity<ApiResponse<WhatsAppIntegrationStatusDto>> getStatus() {
        WhatsAppIntegrationStatusDto status = whatsAppNotificationService.getIntegrationStatus();
        return ResponseEntity.ok(ApiResponse.success("WhatsApp integration status retrieved", status));
    }

    @GetMapping("/messages")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPPORT_ADMIN')")
    @Operation(summary = "List WhatsApp message delivery logs", description = "Retrieves paginated WhatsApp message logs scoped to current tenant")
    public ResponseEntity<ApiResponse<PagedResponse<WhatsAppMessageDto>>> getMessages(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<WhatsAppMessageDto> paged = whatsAppNotificationService.getMessages(pageable);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp message logs retrieved", paged));
    }

    @PostMapping("/messages/{id}/resend")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Resend a WhatsApp message", description = "Re-dispatches a previously failed or pending message")
    public ResponseEntity<ApiResponse<WhatsAppMessageDto>> resendMessage(
            @PathVariable("id") java.util.UUID id
    ) {
        WhatsAppMessageDto resent = whatsAppNotificationService.resendMessage(id);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp message resent successfully", resent));
    }
}
