package com.taxoryn.module.notification.whatsapp.controller;

import com.taxoryn.module.notification.whatsapp.config.WhatsAppProperties;
import com.taxoryn.module.notification.whatsapp.service.WhatsAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/whatsapp/webhook")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook", description = "Inbound Meta WhatsApp Cloud API Webhook Listener")
public class WhatsAppWebhookController {

    private final WhatsAppProperties properties;
    private final WhatsAppNotificationService notificationService;

    @GetMapping
    @Operation(summary = "Verify Meta WhatsApp Webhook Challenge Token")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("Received Meta WhatsApp Webhook verification request: mode={}, tokenPresent={}", mode, token != null);

        if ("subscribe".equalsIgnoreCase(mode) && properties.getWebhookVerifyToken() != null && properties.getWebhookVerifyToken().equals(token)) {
            log.info("Meta WhatsApp Webhook subscription verified successfully.");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Meta WhatsApp Webhook verification failed. Token mismatch or invalid mode.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed: Token mismatch");
    }

    @PostMapping
    @Operation(summary = "Handle Inbound Meta WhatsApp Webhook Delivery Updates")
    public ResponseEntity<String> handleWebhookEvent(
            @RequestBody String payload,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {

        log.debug("Received Meta WhatsApp Webhook payload: {}", payload);
        notificationService.processWebhookPayload(payload, signature);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
