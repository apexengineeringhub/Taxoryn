package com.taxoryn.module.marketing.service;

import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketing.dto.CreateEarlyAccessRequest;
import com.taxoryn.module.marketing.dto.EarlyAccessResponse;
import com.taxoryn.module.marketing.entity.EarlyAccessRequestEntity;
import com.taxoryn.module.marketing.entity.EarlyAccessStatus;
import com.taxoryn.module.marketing.repository.EarlyAccessRequestRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarlyAccessServiceImpl implements EarlyAccessService {

    private final EarlyAccessRequestRepository earlyAccessRepository;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public EarlyAccessResponse submitEarlyAccessRequest(CreateEarlyAccessRequest request, String ipAddress, String userAgent) {
        if (request == null) {
            throw new IllegalArgumentException("Early access request body cannot be null");
        }

        // Anti-spam honeypot trap: if a bot filled the hidden honeypot field, silently return
        // success without storing to database or dispatching emails.
        if (StringUtils.hasText(request.getHoneypot())) {
            log.warn("Anti-spam honeypot triggered by IP {} with userAgent '{}' for simulated email '{}'",
                    ipAddress, userAgent, request.getEmail());
            return EarlyAccessResponse.builder()
                    .id(UUID.randomUUID())
                    .status("NEW")
                    .email(EarlyAccessResponse.maskEmail(request.getEmail()))
                    .message("Thank you for your interest in Taxoryn. We've received your practice access request.")
                    .createdAt(Instant.now())
                    .build();
        }

        // Field Normalization
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedName = request.getName().trim();
        String normalizedPracticeName = request.getPracticeName().trim();
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null;
        String city = StringUtils.hasText(request.getCity()) ? request.getCity().trim() : null;
        String practiceProfile = StringUtils.hasText(request.getPracticeProfile()) ? request.getPracticeProfile().trim() : null;
        String primaryArea = StringUtils.hasText(request.getPrimaryArea()) ? request.getPrimaryArea().trim() : null;
        String source = StringUtils.hasText(request.getSource()) ? request.getSource().trim() : "MARKETING_WEBSITE";

        // Check for existing active submission by the same normalized email
        Optional<EarlyAccessRequestEntity> existingOpt = earlyAccessRepository
                .findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EarlyAccessStatus.NEW);

        EarlyAccessRequestEntity savedEntity;
        boolean isDuplicate;

        if (existingOpt.isPresent()) {
            isDuplicate = true;
            EarlyAccessRequestEntity existing = existingOpt.get();
            existing.setName(normalizedName);
            existing.setPracticeName(normalizedPracticeName);
            existing.setPhone(phone);
            existing.setCity(city);
            existing.setPracticeProfile(practiceProfile);
            existing.setPrimaryArea(primaryArea);
            existing.setSource(source);
            existing.setIpAddress(ipAddress);
            existing.setUserAgent(userAgent);
            existing.setUpdatedAt(Instant.now());
            savedEntity = earlyAccessRepository.save(existing);
            log.info("Updated existing early access request: id={} for email={}", savedEntity.getId(), EarlyAccessResponse.maskEmail(normalizedEmail));
        } else {
            isDuplicate = false;
            EarlyAccessRequestEntity newEntity = EarlyAccessRequestEntity.builder()
                    .name(normalizedName)
                    .email(normalizedEmail)
                    .practiceName(normalizedPracticeName)
                    .phone(phone)
                    .city(city)
                    .practiceProfile(practiceProfile)
                    .primaryArea(primaryArea)
                    .source(source)
                    .status(EarlyAccessStatus.NEW)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            savedEntity = earlyAccessRepository.save(newEntity);
            log.info("Created new early access request: id={} for email={}", savedEntity.getId(), EarlyAccessResponse.maskEmail(normalizedEmail));
        }

        // Audit log without recording sensitive information
        auditService.logEvent(
                isDuplicate ? "EARLY_ACCESS_REQUEST_UPDATED" : "EARLY_ACCESS_REQUEST_CREATED",
                "EARLY_ACCESS",
                savedEntity.getId().toString(),
                null,
                "Practice access request received for practice: " + normalizedPracticeName + " (" + EarlyAccessResponse.maskEmail(normalizedEmail) + ")"
        );

        // Attempt asynchronous email notifications (Resend/SMTP) in a non-blocking background task
        // If email service fails or is unreachable, the lead is safely preserved.
        dispatchNotificationsAsync(normalizedName, normalizedEmail, normalizedPracticeName, phone, city, practiceProfile, primaryArea, savedEntity.getCreatedAt());

        return EarlyAccessResponse.builder()
                .id(savedEntity.getId())
                .status(savedEntity.getStatus().name())
                .email(EarlyAccessResponse.maskEmail(savedEntity.getEmail()))
                .message(isDuplicate
                        ? "Your practice access request has been updated. Our team will contact you shortly."
                        : "Thank you for your interest in Taxoryn. We've received your practice access request. We'll review your request and contact you shortly.")
                .createdAt(savedEntity.getCreatedAt() != null ? savedEntity.getCreatedAt() : Instant.now())
                .build();
    }

    private void dispatchNotificationsAsync(String name, String email, String practiceName, String phone,
                                            String city, String practiceProfile, String primaryArea, Instant createdAt) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Internal notification to support@taxoryn.com
                emailNotificationService.sendEarlyAccessInternalNotification(
                        name, email, practiceName, phone, city, practiceProfile, primaryArea, createdAt
                );
            } catch (Exception ex) {
                log.error("Failed to send internal early access notification for {}: {}",
                        EarlyAccessResponse.maskEmail(email), ex.getMessage());
            }

            try {
                // 2. Confirmation email to requester
                emailNotificationService.sendEarlyAccessConfirmation(email, name, practiceName);
            } catch (Exception ex) {
                log.error("Failed to send early access confirmation email to {}: {}",
                        EarlyAccessResponse.maskEmail(email), ex.getMessage());
            }
        });
    }
}
