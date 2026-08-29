package com.taxoryn.module.notification.whatsapp.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.notification.whatsapp.config.WhatsAppProperties;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppIntegrationStatusDto;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppMessageDto;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageEntity;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageStatus;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegistrationType;
import com.taxoryn.module.notification.whatsapp.provider.WhatsAppProvider;
import com.taxoryn.module.notification.whatsapp.provider.WhatsAppSendResult;
import com.taxoryn.module.notification.whatsapp.repository.WhatsAppMessageRepository;
import com.taxoryn.module.notification.whatsapp.template.WhatsAppTemplateFormatter;
import com.taxoryn.module.notification.whatsapp.template.WhatsAppTemplateType;
import com.taxoryn.module.notification.whatsapp.util.PhoneNumberNormalizer;
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

        try {
            WhatsAppProvider provider = resolveProvider();
            messageEntity.setProvider(provider.getProviderName());

            WhatsAppSendResult result = provider.sendTemplate(normalizedPhone, templateName, variables);

            if (result.isSuccess()) {
                messageEntity.setStatus(WhatsAppMessageStatus.SENT);
                messageEntity.setProviderMessageId(result.getProviderMessageId());
                messageEntity.setSentAt(Instant.now());
                messageEntity.setErrorMessage(null);
                log.info("WhatsApp welcome message successfully sent to {} via provider={}: msgId={}",
                        maskPhone(normalizedPhone), provider.getProviderName(), result.getProviderMessageId());

                auditService.logEvent("WHATSAPP_MESSAGE_SENT", "USER",
                        event.getUserId() != null ? event.getUserId().toString() : "SYSTEM",
                        event.getOrganizationId(),
                        "Sent WhatsApp welcome notification to " + maskPhone(normalizedPhone));
            } else {
                messageEntity.setStatus(WhatsAppMessageStatus.FAILED);
                messageEntity.setErrorMessage(result.getErrorMessage());
                log.warn("WhatsApp welcome message failed for {} via provider={}: {}",
                        maskPhone(normalizedPhone), provider.getProviderName(), result.getErrorMessage());

                auditService.logEvent("WHATSAPP_MESSAGE_FAILED", "USER",
                        event.getUserId() != null ? event.getUserId().toString() : "SYSTEM",
                        event.getOrganizationId(),
                        "Failed WhatsApp welcome notification to " + maskPhone(normalizedPhone) + ": " + result.getErrorMessage());
            }
        } catch (Exception ex) {
            log.error("Unhandled exception sending WhatsApp welcome message to {}: {}", maskPhone(normalizedPhone), ex.getMessage(), ex);
            messageEntity.setStatus(WhatsAppMessageStatus.FAILED);
            messageEntity.setErrorMessage(ex.getMessage());
        }

        return messageRepository.save(messageEntity);
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
    @Transactional(readOnly = true)
    public WhatsAppIntegrationStatusDto getIntegrationStatus() {
        long totalSent = messageRepository.countByStatus(WhatsAppMessageStatus.SENT);
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
        if (currentOrgId != null) {
            page = messageRepository.findByOrganizationIdOrderByCreatedAtDesc(currentOrgId, pageable);
        } else {
            page = messageRepository.findAll(pageable);
        }

        return PagedResponse.of(page, this::toDto);
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
