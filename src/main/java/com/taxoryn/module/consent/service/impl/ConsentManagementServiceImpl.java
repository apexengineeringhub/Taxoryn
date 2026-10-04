package com.taxoryn.module.consent.service.impl;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.consent.dto.ApproveConsentRequest;
import com.taxoryn.module.consent.dto.ConsentDto;
import com.taxoryn.module.consent.dto.CreateConsentRequest;
import com.taxoryn.module.consent.dto.RevokeConsentRequest;
import com.taxoryn.module.consent.entity.TaxpayerConsentEntity;
import com.taxoryn.module.consent.model.ConsentStatus;
import com.taxoryn.module.consent.repository.TaxpayerConsentRepository;
import com.taxoryn.module.consent.service.ConsentManagementService;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentManagementServiceImpl implements ConsentManagementService {

    private final TaxpayerConsentRepository consentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ConsentDto createConsent(CreateConsentRequest request) {
        if (request == null || request.getClientId() == null || request.getDelegateUserId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Client ID and Delegate User ID are mandatory");
        }
        if (request.getDelegationType() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Delegation type is required");
        }
        if (request.getScopes() == null || request.getScopes().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "At least one operational consent scope is required");
        }
        if (request.getValidUntil() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Consent validity expiration timestamp is required");
        }

        UUID tenantId = requireActiveTenantId();
        Instant now = Instant.now();
        Instant validFrom = request.getValidFrom() != null ? request.getValidFrom() : now;

        if (request.getValidUntil().isBefore(validFrom) || request.getValidUntil().equals(validFrom)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "validUntil must be strictly after validFrom");
        }

        // 1. Verify Client belongs to Tenant
        clientRepository.findByIdAndOrganizationId(request.getClientId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Client not found or does not belong to organization"));

        // 2. Verify Delegate User belongs to Tenant
        userRepository.findByIdAndOrganizationId(request.getDelegateUserId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Delegate user not found or does not belong to organization"));

        // 3. Check for identical active overlapping delegation
        List<TaxpayerConsentEntity> existingActive = consentRepository.findAllByOrganizationIdAndClientIdAndDelegateUserIdAndStatus(
                tenantId, request.getClientId(), request.getDelegateUserId(), ConsentStatus.ACTIVE);

        for (TaxpayerConsentEntity active : existingActive) {
            if (active.isUsableAt(now) && active.getScopes().equals(request.getScopes())) {
                log.info("[CONSENT] Reusing identical active delegation id={} for client id={}, delegate id={}",
                        active.getId(), request.getClientId(), request.getDelegateUserId());
                return mapToDto(active);
            }
        }

        ConsentStatus initialStatus = request.isAutoApprove() ? ConsentStatus.ACTIVE : ConsentStatus.PENDING;
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId()
                : UUID.randomUUID().toString();
        String consentRef = "CONSENT_REF_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TaxpayerConsentEntity entity = TaxpayerConsentEntity.builder()
                .clientId(request.getClientId())
                .delegateUserId(request.getDelegateUserId())
                .delegationType(request.getDelegationType())
                .status(initialStatus)
                .consentMethod(request.getConsentMethod())
                .scopes(new HashSet<>(request.getScopes()))
                .validFrom(validFrom)
                .validUntil(request.getValidUntil())
                .consentReference(consentRef)
                .correlationId(correlationId)
                .build();
        entity.setOrganizationId(tenantId);

        TaxpayerConsentEntity saved = consentRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                initialStatus == ConsentStatus.ACTIVE ? "CONSENT_APPROVED" : "CONSENT_CREATED",
                "TAXPAYER_CONSENT",
                saved.getId().toString(),
                null,
                Map.of(
                        "consentId", saved.getId().toString(),
                        "clientId", saved.getClientId().toString(),
                        "delegateUserId", saved.getDelegateUserId().toString(),
                        "status", saved.getStatus().name(),
                        "scopes", saved.getScopes().stream().map(Enum::name).collect(Collectors.joining(","))
                )
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ConsentDto getConsentById(UUID consentId) {
        if (consentId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Consent ID is required");
        }
        UUID tenantId = requireActiveTenantId();
        TaxpayerConsentEntity entity = consentRepository.findByIdAndOrganizationId(consentId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Taxpayer consent not found"));

        checkAndApplyLazyExpiration(entity);
        return mapToDto(entity);
    }

    @Override
    @Transactional
    public List<ConsentDto> getConsentsForClient(UUID clientId) {
        if (clientId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Client ID is required");
        }
        UUID tenantId = requireActiveTenantId();
        clientRepository.findByIdAndOrganizationId(clientId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Client not found"));

        List<TaxpayerConsentEntity> list = consentRepository.findAllByOrganizationIdAndClientId(tenantId, clientId);
        list.forEach(this::checkAndApplyLazyExpiration);
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ConsentDto> getDelegationsForUser(UUID delegateUserId) {
        if (delegateUserId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Delegate User ID is required");
        }
        UUID tenantId = requireActiveTenantId();
        userRepository.findByIdAndOrganizationId(delegateUserId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        List<TaxpayerConsentEntity> list = consentRepository.findAllByOrganizationIdAndDelegateUserId(tenantId, delegateUserId);
        list.forEach(this::checkAndApplyLazyExpiration);
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConsentDto approveConsent(UUID consentId, ApproveConsentRequest request) {
        if (consentId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Consent ID is required");
        }
        UUID tenantId = requireActiveTenantId();
        TaxpayerConsentEntity entity = consentRepository.findByIdAndOrganizationId(consentId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Taxpayer consent not found"));

        if (entity.getStatus() == ConsentStatus.ACTIVE) {
            return mapToDto(entity);
        }
        if (entity.getStatus().isTerminal()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cannot approve consent in terminal state: " + entity.getStatus());
        }

        UUID consentingUser = request != null && request.getConsentingUserId() != null
                ? request.getConsentingUserId()
                : entity.getDelegateUserId();

        entity.approve(consentingUser);
        TaxpayerConsentEntity saved = consentRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "CONSENT_APPROVED",
                "TAXPAYER_CONSENT",
                saved.getId().toString(),
                null,
                Map.of(
                        "consentId", saved.getId().toString(),
                        "clientId", saved.getClientId().toString(),
                        "consentingUserId", consentingUser.toString()
                )
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ConsentDto rejectConsent(UUID consentId, String reason) {
        if (consentId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Consent ID is required");
        }
        UUID tenantId = requireActiveTenantId();
        TaxpayerConsentEntity entity = consentRepository.findByIdAndOrganizationId(consentId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Taxpayer consent not found"));

        if (entity.getStatus().isTerminal()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cannot reject consent in terminal state: " + entity.getStatus());
        }

        entity.reject(reason != null ? reason : "Consent rejected by taxpayer");
        TaxpayerConsentEntity saved = consentRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "CONSENT_REJECTED",
                "TAXPAYER_CONSENT",
                saved.getId().toString(),
                null,
                Map.of(
                        "consentId", saved.getId().toString(),
                        "clientId", saved.getClientId().toString(),
                        "reason", entity.getRejectionReason()
                )
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ConsentDto revokeConsent(UUID consentId, RevokeConsentRequest request) {
        if (consentId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Consent ID is required");
        }
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Revocation reason is required");
        }

        UUID tenantId = requireActiveTenantId();
        TaxpayerConsentEntity entity = consentRepository.findByIdAndOrganizationId(consentId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Taxpayer consent not found"));

        if (entity.getStatus() == ConsentStatus.REVOKED) {
            return mapToDto(entity);
        }

        entity.revoke(entity.getDelegateUserId(), request.getReason().trim());
        TaxpayerConsentEntity saved = consentRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "CONSENT_REVOKED",
                "TAXPAYER_CONSENT",
                saved.getId().toString(),
                null,
                Map.of(
                        "consentId", saved.getId().toString(),
                        "clientId", saved.getClientId().toString(),
                        "reason", request.getReason().trim()
                )
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public List<ConsentDto> getActiveConsentsForClient(UUID clientId) {
        if (clientId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Client ID is required");
        }
        UUID tenantId = requireActiveTenantId();
        Instant now = Instant.now();

        List<TaxpayerConsentEntity> list = consentRepository.findAllByOrganizationIdAndClientIdAndStatus(
                tenantId, clientId, ConsentStatus.ACTIVE);

        return list.stream()
                .filter(c -> {
                    checkAndApplyLazyExpiration(c);
                    return c.isUsableAt(now);
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void checkAndApplyLazyExpiration(TaxpayerConsentEntity entity) {
        if (entity.getStatus() == ConsentStatus.ACTIVE && entity.isExpired()) {
            entity.setStatus(ConsentStatus.EXPIRED);
            consentRepository.save(entity);
        }
    }

    private ConsentDto mapToDto(TaxpayerConsentEntity entity) {
        return ConsentDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .consentingUserId(entity.getConsentingUserId())
                .delegateUserId(entity.getDelegateUserId())
                .delegationType(entity.getDelegationType())
                .status(entity.getStatus())
                .consentMethod(entity.getConsentMethod())
                .scopes(new HashSet<>(entity.getScopes()))
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .revokedAt(entity.getRevokedAt())
                .revokedBy(entity.getRevokedBy())
                .revocationReason(entity.getRevocationReason())
                .rejectionReason(entity.getRejectionReason())
                .consentReference(entity.getConsentReference())
                .correlationId(entity.getCorrelationId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required");
        }
        return tenantId;
    }
}
