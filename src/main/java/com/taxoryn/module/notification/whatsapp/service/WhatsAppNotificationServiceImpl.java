package com.taxoryn.module.notification.whatsapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.notification.whatsapp.config.WhatsAppProperties;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppIntegrationStatusDto;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppMessageDto;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageEntity;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageStatus;
import com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent;
import com.taxoryn.module.notification.whatsapp.event.InvoiceReminderEvent;
import com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegistrationType;
import com.taxoryn.module.notification.whatsapp.provider.WhatsAppProvider;
import com.taxoryn.module.notification.whatsapp.provider.WhatsAppSendResult;
import com.taxoryn.module.notification.whatsapp.repository.WhatsAppMessageRepository;
import com.taxoryn.module.notification.whatsapp.template.WhatsAppTemplateFormatter;
import com.taxoryn.module.notification.whatsapp.template.WhatsAppTemplateType;
import com.taxoryn.module.notification.whatsapp.util.PhoneNumberNormalizer;
import com.taxoryn.module.organization.entity.OrganizationSettingsEntity;
import com.taxoryn.module.organization.repository.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotificationServiceImpl implements WhatsAppNotificationService {

    private final WhatsAppProperties properties;
    private final WhatsAppMessageRepository messageRepository;
    private final WhatsAppTemplateFormatter templateFormatter;
    private final ApplicationContext applicationContext;
    private final AuditService auditService;
    private final OrganizationSettingsRepository organizationSettingsRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WhatsAppMessageEntity sendWelcomeMessage(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("Cannot send WhatsApp welcome message: event is null");
            return null;
        }

        String rawPhone = event.getPhone();
        String normalizedPhone = PhoneNumberNormalizer.normalize(rawPhone);

        if (!StringUtils.hasText(normalizedPhone)) {
            log.info("Skipping WhatsApp welcome notification for user id={}: no phone number provided", event.getUserId());
            return null;
        }

        WhatsAppTemplateType templateType = (event.getRegistrationType() == UserRegistrationType.PRACTITIONER)
                ? WhatsAppTemplateType.WELCOME_PRACTITIONER
                : WhatsAppTemplateType.WELCOME_INDIVIDUAL;

        String templateName = properties.getTemplates().getOrDefault(
                templateType.name(),
                templateType.getDefaultTemplateName()
        );

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("name", event.getFullName());
        variables.put("email", event.getEmail() != null ? event.getEmail() : "");
        variables.put("mobile", normalizedPhone);
        variables.put("practiceName", event.getOrganizationName() != null ? event.getOrganizationName() : "Your Tax Practice");
        variables.put("loginUrl", properties.getLoginUrl());

        String formattedBody = templateFormatter.format(templateType, variables);

        WhatsAppMessageEntity messageEntity = WhatsAppMessageEntity.builder()
                .organizationId(event.getOrganizationId())
                .userId(event.getUserId())
                .recipientPhone(normalizedPhone)
                .templateType(templateType.name())
                .templateName(templateName)
                .messageContent(formattedBody)
                .provider(properties.getProvider())
                .status(WhatsAppMessageStatus.PENDING)
                .build();

        messageEntity = messageRepository.save(messageEntity);

        if (!properties.isEnabled()) {
            log.info("[WHATSAPP_DISABLED] WhatsApp dispatch is disabled in configuration. Skipped welcome dispatch for user={} (phone={})",
                    event.getEmail(), maskPhone(normalizedPhone));
            messageEntity.setStatus(WhatsAppMessageStatus.PENDING);
            messageEntity.setErrorMessage("WhatsApp integration is disabled in configuration");
            return messageRepository.save(messageEntity);
        }

        return dispatchMessage(messageEntity, normalizedPhone, templateName, variables, event.getUserId(), "USER");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WhatsAppMessageEntity sendInvoiceIssuedMessage(InvoiceIssuedEvent event) {
        if (event == null || !StringUtils.hasText(event.getClientPhone())) {
            log.info("Skipping WhatsApp invoice notification: event is null or client phone is missing");
            return null;
        }

        if (!isOrgWhatsAppEnabled(event.getOrganizationId())) {
            log.info("Skipping WhatsApp invoice notification: disabled in organization settings for orgId={}", event.getOrganizationId());
            return null;
        }

        String normalizedPhone = PhoneNumberNormalizer.normalize(event.getClientPhone());
        if (!StringUtils.hasText(normalizedPhone)) {
            log.warn("Skipping WhatsApp invoice notification: invalid phone '{}'", event.getClientPhone());
            return null;
        }

        WhatsAppTemplateType templateType = WhatsAppTemplateType.INVOICE_ISSUED;
        String templateName = properties.getTemplates().getOrDefault(templateType.name(), templateType.getDefaultTemplateName());

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("clientName", event.getClientName() != null ? event.getClientName() : "Valued Client");
        variables.put("invoiceNumber", event.getInvoiceNumber());
        variables.put("organizationName", event.getOrganizationName() != null ? event.getOrganizationName() : "Tax Practice");
        variables.put("currency", event.getCurrency() != null ? event.getCurrency() : "INR");
        variables.put("totalAmount", event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "0.00");
        variables.put("dueDate", event.getDueDate() != null ? event.getDueDate().toString() : "Due upon receipt");
        variables.put("invoiceUrl", properties.getLoginUrl());

        String formattedBody = templateFormatter.format(templateType, variables);

        WhatsAppMessageEntity entity = WhatsAppMessageEntity.builder()
                .organizationId(event.getOrganizationId())
                .recipientPhone(normalizedPhone)
                .templateType(templateType.name())
                .templateName(templateName)
                .messageContent(formattedBody)
                .mediaUrl(event.getPdfUrl())
                .provider(properties.getProvider())
                .status(WhatsAppMessageStatus.PENDING)
                .build();

        entity = messageRepository.save(entity);

        if (!properties.isEnabled()) {
            entity.setErrorMessage("WhatsApp integration is disabled in configuration");
            return messageRepository.save(entity);
        }

        return dispatchMessage(entity, normalizedPhone, templateName, variables, event.getInvoiceId(), "INVOICE");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WhatsAppMessageEntity sendPaymentReceivedMessage(PaymentReceivedEvent event) {
        if (event == null || !StringUtils.hasText(event.getClientPhone())) {
            log.info("Skipping WhatsApp payment notification: event is null or client phone missing");
            return null;
        }

        if (!isOrgWhatsAppEnabled(event.getOrganizationId())) {
            log.info("Skipping WhatsApp payment notification: disabled in organization settings for orgId={}", event.getOrganizationId());
            return null;
        }

        String normalizedPhone = PhoneNumberNormalizer.normalize(event.getClientPhone());
        if (!StringUtils.hasText(normalizedPhone)) {
            return null;
        }

        WhatsAppTemplateType templateType = WhatsAppTemplateType.PAYMENT_RECEIVED;
        String templateName = properties.getTemplates().getOrDefault(templateType.name(), templateType.getDefaultTemplateName());

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("clientName", event.getClientName() != null ? event.getClientName() : "Valued Client");
        variables.put("invoiceNumber", event.getInvoiceNumber());
        variables.put("organizationName", event.getOrganizationName() != null ? event.getOrganizationName() : "Tax Practice");
        variables.put("currency", event.getCurrency() != null ? event.getCurrency() : "INR");
        variables.put("amountPaid", event.getAmountPaid() != null ? event.getAmountPaid().toPlainString() : "0.00");
        variables.put("remainingBalance", event.getRemainingBalance() != null ? event.getRemainingBalance().toPlainString() : "0.00");
        variables.put("paymentReference", event.getPaymentReference() != null ? event.getPaymentReference() : "N/A");

        String formattedBody = templateFormatter.format(templateType, variables);

        WhatsAppMessageEntity entity = WhatsAppMessageEntity.builder()
                .organizationId(event.getOrganizationId())
                .recipientPhone(normalizedPhone)
                .templateType(templateType.name())
                .templateName(templateName)
                .messageContent(formattedBody)
                .provider(properties.getProvider())
                .status(WhatsAppMessageStatus.PENDING)
                .build();

        entity = messageRepository.save(entity);

        if (!properties.isEnabled()) {
            entity.setErrorMessage("WhatsApp integration is disabled in configuration");
            return messageRepository.save(entity);
        }

        return dispatchMessage(entity, normalizedPhone, templateName, variables, event.getPaymentId(), "PAYMENT");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WhatsAppMessageEntity sendInvoiceReminderMessage(InvoiceReminderEvent event) {
        if (event == null || !StringUtils.hasText(event.getClientPhone())) {
            return null;
        }

        if (!isOrgWhatsAppEnabled(event.getOrganizationId())) {
            return null;
        }

        String normalizedPhone = PhoneNumberNormalizer.normalize(event.getClientPhone());
        if (!StringUtils.hasText(normalizedPhone)) {
            return null;
        }

        WhatsAppTemplateType templateType = WhatsAppTemplateType.INVOICE_REMINDER;
        String templateName = properties.getTemplates().getOrDefault(templateType.name(), templateType.getDefaultTemplateName());

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("clientName", event.getClientName() != null ? event.getClientName() : "Valued Client");
        variables.put("invoiceNumber", event.getInvoiceNumber());
        variables.put("organizationName", event.getOrganizationName() != null ? event.getOrganizationName() : "Tax Practice");
        variables.put("currency", event.getCurrency() != null ? event.getCurrency() : "INR");
        variables.put("balanceAmount", event.getBalanceAmount() != null ? event.getBalanceAmount().toPlainString() : "0.00");
        variables.put("dueDate", event.getDueDate() != null ? event.getDueDate().toString() : "Overdue");
        variables.put("invoiceUrl", properties.getLoginUrl());

        String formattedBody = templateFormatter.format(templateType, variables);

        WhatsAppMessageEntity entity = WhatsAppMessageEntity.builder()
                .organizationId(event.getOrganizationId())
                .recipientPhone(normalizedPhone)
                .templateType(templateType.name())
                .templateName(templateName)
                .messageContent(formattedBody)
                .provider(properties.getProvider())
                .status(WhatsAppMessageStatus.PENDING)
                .build();

        entity = messageRepository.save(entity);

        if (!properties.isEnabled()) {
            entity.setErrorMessage("WhatsApp integration is disabled in configuration");
            return messageRepository.save(entity);
        }

        return dispatchMessage(entity, normalizedPhone, templateName, variables, event.getInvoiceId(), "INVOICE");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WhatsAppMessageEntity sendTextMessage(UUID organizationId, UUID userId, String rawPhone, String messageText) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(rawPhone);
        if (!StringUtils.hasText(normalizedPhone)) {
            log.warn("Cannot send WhatsApp text message: invalid recipient phone '{}'", rawPhone);
            return null;
        }

        WhatsAppMessageEntity messageEntity = WhatsAppMessageEntity.builder()
                .organizationId(organizationId)
                .userId(userId)
                .recipientPhone(normalizedPhone)
                .templateType("CUSTOM_TEXT")
                .templateName("custom_text")
                .messageContent(messageText)
                .provider(properties.getProvider())
                .status(WhatsAppMessageStatus.PENDING)
                .build();

        messageEntity = messageRepository.save(messageEntity);

        if (!properties.isEnabled()) {
            log.info("[WHATSAPP_DISABLED] Skipped custom WhatsApp message to {}", maskPhone(normalizedPhone));
            messageEntity.setErrorMessage("WhatsApp integration disabled in configuration");
            return messageRepository.save(messageEntity);
        }

        try {
            WhatsAppProvider provider = resolveProvider();
            messageEntity.setProvider(provider.getProviderName());

            WhatsAppSendResult result = provider.sendTextMessage(normalizedPhone, messageText);
            if (result.isSuccess()) {
                messageEntity.setStatus(WhatsAppMessageStatus.SENT);
                messageEntity.setProviderMessageId(result.getProviderMessageId());
                messageEntity.setSentAt(Instant.now());
                messageEntity.setErrorMessage(null);
            } else {
                messageEntity.setStatus(WhatsAppMessageStatus.FAILED);
                messageEntity.setErrorMessage(result.getErrorMessage());
            }
        } catch (Exception ex) {
            log.error("Failed sending WhatsApp text message: {}", ex.getMessage(), ex);
            messageEntity.setStatus(WhatsAppMessageStatus.FAILED);
            messageEntity.setErrorMessage(ex.getMessage());
        }

        return messageRepository.save(messageEntity);
    }

    @Override
    @Transactional
    public void processWebhookPayload(String payload, String signature) {
        if (!StringUtils.hasText(payload)) {
            return;
        }

        // SECURITY (Section 38 - Webhook Security): this endpoint is publicly reachable
        // (permitAll in SecurityConfig) since Meta cannot authenticate with our JWTs. Without
        // verifying the X-Hub-Signature-256 HMAC, ANY caller can post arbitrary payloads that
        // get parsed and used to mutate WhatsAppMessageEntity delivery status records.
        if (StringUtils.hasText(properties.getAppSecret())) {
            if (!isValidMetaSignature(payload, signature, properties.getAppSecret())) {
                log.warn("Rejected WhatsApp webhook payload: X-Hub-Signature-256 missing or invalid.");
                return;
            }
        } else {
            // No app secret configured - this is a production misconfiguration. We still
            // process (to avoid breaking existing deployments that haven't wired this up yet)
            // but this must be treated as a finding: the webhook is currently unauthenticated.
            log.warn("SECURITY: taxoryn.whatsapp.app-secret is not configured - processing WhatsApp " +
                    "webhook payload WITHOUT signature verification. Configure the Meta App Secret " +
                    "to prevent spoofed webhook deliveries.");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!root.has("entry") || !root.get("entry").isArray()) {
                return;
            }

            for (JsonNode entry : root.get("entry")) {
                if (!entry.has("changes") || !entry.get("changes").isArray()) continue;
                for (JsonNode change : entry.get("changes")) {
                    JsonNode value = change.path("value");
                    if (value.has("statuses") && value.get("statuses").isArray()) {
                        for (JsonNode statusNode : value.get("statuses")) {
                            String messageId = statusNode.path("id").asText(null);
                            String statusStr = statusNode.path("status").asText(null);
                            long timestampSec = statusNode.path("timestamp").asLong(0);
                            Instant eventTime = timestampSec > 0 ? Instant.ofEpochSecond(timestampSec) : Instant.now();

                            if (StringUtils.hasText(messageId) && StringUtils.hasText(statusStr)) {
                                updateMessageStatusFromWebhook(messageId, statusStr, eventTime, statusNode);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error parsing Meta WhatsApp webhook payload: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Verifies the Meta "X-Hub-Signature-256" header: {@code sha256=<hex-hmac>} computed over
     * the raw request body using the App Secret as the HMAC-SHA256 key. Uses a constant-time
     * comparison to avoid leaking timing information about the expected signature.
     */
    private boolean isValidMetaSignature(String payload, String signatureHeader, String appSecret) {
        if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            String expectedHex = signatureHeader.substring("sha256=".length()).trim();
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    appSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] expected = hexToBytes(expectedHex);
            return expected != null && java.security.MessageDigest.isEqual(computed, expected);
        } catch (Exception ex) {
            log.error("Error computing WhatsApp webhook HMAC signature: {}", ex.getMessage(), ex);
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return null;
        }
        try {
            int len = hex.length();
            byte[] out = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                        + Character.digit(hex.charAt(i + 1), 16));
            }
            return out;
        } catch (Exception ex) {
            return null;
        }
    }

    private void updateMessageStatusFromWebhook(String providerMessageId, String metaStatus, Instant eventTime, JsonNode statusNode) {
        Optional<WhatsAppMessageEntity> optional = messageRepository.findByProviderMessageId(providerMessageId);
        if (optional.isEmpty()) {
            log.debug("Webhook status update for unknown providerMessageId: {}", providerMessageId);
            return;
        }

        WhatsAppMessageEntity entity = optional.get();
        switch (metaStatus.toLowerCase()) {
            case "delivered" -> {
                entity.setStatus(WhatsAppMessageStatus.DELIVERED);
                entity.setDeliveredAt(eventTime);
            }
            case "read" -> {
                entity.setStatus(WhatsAppMessageStatus.READ);
                entity.setReadAt(eventTime);
                if (entity.getDeliveredAt() == null) {
                    entity.setDeliveredAt(eventTime);
                }
            }
            case "sent" -> {
                if (entity.getStatus() == WhatsAppMessageStatus.PENDING) {
                    entity.setStatus(WhatsAppMessageStatus.SENT);
                }
                if (entity.getSentAt() == null) {
                    entity.setSentAt(eventTime);
                }
            }
            case "failed" -> {
                entity.setStatus(WhatsAppMessageStatus.FAILED);
                JsonNode errors = statusNode.path("errors");
                if (errors.isArray() && errors.size() > 0) {
                    entity.setErrorMessage(errors.get(0).path("title").asText("Delivery failed") + ": " + errors.get(0).path("message").asText(""));
                }
            }
        }
        messageRepository.save(entity);
        log.info("Updated WhatsApp message {} status to {} via Meta webhook", entity.getId(), entity.getStatus());
    }

    @Override
    @Transactional
    public WhatsAppMessageDto resendMessage(UUID messageId) {
        WhatsAppMessageEntity entity = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp message not found: " + messageId));

        UUID currentOrgId = null;
        try {
            currentOrgId = SecurityUtils.getCurrentOrganizationId();
        } catch (Exception ignored) {}

        if (currentOrgId != null && entity.getOrganizationId() != null && !currentOrgId.equals(entity.getOrganizationId())) {
            throw new BusinessValidationException("Unauthorized to resend message belonging to another organization");
        }

        if (!properties.isEnabled()) {
            throw new BusinessValidationException("WhatsApp integration is currently disabled in configuration");
        }

        WhatsAppProvider provider = resolveProvider();
        entity.setProvider(provider.getProviderName());
        entity.setStatus(WhatsAppMessageStatus.PENDING);
        entity.setErrorMessage(null);

        WhatsAppSendResult result;
        if (StringUtils.hasText(entity.getTemplateName()) && !"custom_text".equalsIgnoreCase(entity.getTemplateName())) {
            result = provider.sendTemplate(entity.getRecipientPhone(), entity.getTemplateName(), Map.of());
        } else {
            result = provider.sendTextMessage(entity.getRecipientPhone(), entity.getMessageContent());
        }

        if (result.isSuccess()) {
            entity.setStatus(WhatsAppMessageStatus.SENT);
            entity.setProviderMessageId(result.getProviderMessageId());
            entity.setSentAt(Instant.now());
            entity.setErrorMessage(null);
        } else {
            entity.setStatus(WhatsAppMessageStatus.FAILED);
            entity.setErrorMessage(result.getErrorMessage());
        }

        WhatsAppMessageEntity saved = messageRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsAppIntegrationStatusDto getIntegrationStatus() {
        long totalSent = messageRepository.countByStatus(WhatsAppMessageStatus.SENT)
                + messageRepository.countByStatus(WhatsAppMessageStatus.DELIVERED)
                + messageRepository.countByStatus(WhatsAppMessageStatus.READ);
        long totalFailed = messageRepository.countByStatus(WhatsAppMessageStatus.FAILED);
        long totalPending = messageRepository.countByStatus(WhatsAppMessageStatus.PENDING);

        return WhatsAppIntegrationStatusDto.builder()
                .enabled(properties.isEnabled())
                .provider(properties.getProvider())
                .baseUrl(properties.getBaseUrl())
                .phoneNumberIdConfigured(StringUtils.hasText(properties.getPhoneNumberId()))
                .accessTokenConfigured(StringUtils.hasText(properties.getAccessToken()))
                .totalMessagesSent(totalSent)
                .totalMessagesFailed(totalFailed)
                .totalMessagesPending(totalPending)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WhatsAppMessageDto> getMessages(Pageable pageable) {
        UUID currentOrgId = null;
        try {
            currentOrgId = SecurityUtils.getCurrentOrganizationId();
        } catch (Exception ignored) {}

        boolean isSuperAdmin = false;
        try {
            isSuperAdmin = SecurityUtils.hasRole("TAXORYN_SUPERADMIN") || SecurityUtils.hasRole("SUPER_ADMIN");
        } catch (Exception ignored) {}

        Page<WhatsAppMessageEntity> page;
        if (currentOrgId != null && !isSuperAdmin) {
            page = messageRepository.findByOrganizationIdOrderByCreatedAtDesc(currentOrgId, pageable);
        } else {
            page = messageRepository.findAll(pageable);
        }

        return PagedResponse.of(page, this::toDto);
    }

    private WhatsAppMessageEntity dispatchMessage(
            WhatsAppMessageEntity messageEntity,
            String normalizedPhone,
            String templateName,
            Map<String, String> variables,
            UUID entityId,
            String entityType) {
        try {
            WhatsAppProvider provider = resolveProvider();
            messageEntity.setProvider(provider.getProviderName());

            WhatsAppSendResult result = provider.sendTemplate(normalizedPhone, templateName, variables);

            if (result.isSuccess()) {
                messageEntity.setStatus(WhatsAppMessageStatus.SENT);
                messageEntity.setProviderMessageId(result.getProviderMessageId());
                messageEntity.setSentAt(Instant.now());
                messageEntity.setErrorMessage(null);
                log.info("WhatsApp message successfully sent to {} via provider={}: msgId={}",
                        maskPhone(normalizedPhone), provider.getProviderName(), result.getProviderMessageId());

                auditService.logEvent("WHATSAPP_MESSAGE_SENT", entityType,
                        entityId != null ? entityId.toString() : "SYSTEM",
                        messageEntity.getOrganizationId(),
                        "Sent WhatsApp " + messageEntity.getTemplateType() + " notification to " + maskPhone(normalizedPhone));
            } else {
                messageEntity.setStatus(WhatsAppMessageStatus.FAILED);
                messageEntity.setErrorMessage(result.getErrorMessage());
                log.warn("WhatsApp message failed for {} via provider={}: {}",
                        maskPhone(normalizedPhone), provider.getProviderName(), result.getErrorMessage());

                auditService.logEvent("WHATSAPP_MESSAGE_FAILED", entityType,
                        entityId != null ? entityId.toString() : "SYSTEM",
                        messageEntity.getOrganizationId(),
                        "Failed WhatsApp " + messageEntity.getTemplateType() + " notification to " + maskPhone(normalizedPhone) + ": " + result.getErrorMessage());
            }
        } catch (Exception ex) {
            log.error("Unhandled exception sending WhatsApp message to {}: {}", maskPhone(normalizedPhone), ex.getMessage(), ex);
            messageEntity.setStatus(WhatsAppMessageStatus.FAILED);
            messageEntity.setErrorMessage(ex.getMessage());
        }

        return messageRepository.save(messageEntity);
    }

    private boolean isOrgWhatsAppEnabled(UUID organizationId) {
        if (organizationId == null) {
            return true;
        }
        return organizationSettingsRepository.findByOrganizationId(organizationId)
                .map(OrganizationSettingsEntity::isEnableWhatsappNotifications)
                .orElse(true); // Default to true if not configured yet
    }

    private WhatsAppMessageDto toDto(WhatsAppMessageEntity entity) {
        return WhatsAppMessageDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .userId(entity.getUserId())
                .recipientPhone(maskPhone(entity.getRecipientPhone()))
                .templateType(entity.getTemplateType())
                .templateName(entity.getTemplateName())
                .messageContent(entity.getMessageContent())
                .provider(entity.getProvider())
                .providerMessageId(entity.getProviderMessageId())
                .status(entity.getStatus().name())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .deliveredAt(entity.getDeliveredAt())
                .readAt(entity.getReadAt())
                .mediaUrl(entity.getMediaUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private WhatsAppProvider resolveProvider() {
        String configured = properties.getProvider();
        if (StringUtils.hasText(configured)) {
            if ("META".equalsIgnoreCase(configured) && applicationContext.containsBean("metaWhatsAppProvider")) {
                return applicationContext.getBean("metaWhatsAppProvider", WhatsAppProvider.class);
            }
            if ("MOCK".equalsIgnoreCase(configured) && applicationContext.containsBean("mockWhatsAppProvider")) {
                return applicationContext.getBean("mockWhatsAppProvider", WhatsAppProvider.class);
            }
        }
        return applicationContext.getBean("logWhatsAppProvider", WhatsAppProvider.class);
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
