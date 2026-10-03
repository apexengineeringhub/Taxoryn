package com.taxoryn.module.dsc.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.dsc.dto.CreateDscRequest;
import com.taxoryn.module.dsc.dto.DscDto;
import com.taxoryn.module.dsc.dto.DscFilterRequest;
import com.taxoryn.module.dsc.dto.DscSummaryDto;
import com.taxoryn.module.dsc.dto.UpdateDscRequest;
import com.taxoryn.module.dsc.entity.DscEntity;
import com.taxoryn.module.dsc.model.DscStatus;
import com.taxoryn.module.dsc.repository.DscRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DscServiceImpl implements DscService {

    private final DscRepository dscRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private static final int EXPIRY_WARNING_THRESHOLD_DAYS = 30;

    @Override
    @Transactional
    public DscDto createDsc(CreateDscRequest request) {
        UUID organizationId = resolveOrganizationId();

        validateDates(request.getIssuedDate(), request.getExpiryDate());

        ClientEntity client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
        }

        LocalDate today = LocalDate.now();
        DscStatus initialStatus = DscStatus.ACTIVE;
        if (request.getExpiryDate().isBefore(today)) {
            initialStatus = DscStatus.EXPIRED;
        } else if (!request.getExpiryDate().isAfter(today.plusDays(EXPIRY_WARNING_THRESHOLD_DAYS))) {
            initialStatus = DscStatus.EXPIRING;
        }

        DscEntity entity = DscEntity.builder()
                .clientId(client != null ? client.getId() : null)
                .holderName(request.getHolderName().trim())
                .certificateIdentifier(StringUtils.hasText(request.getCertificateIdentifier()) ? request.getCertificateIdentifier().trim() : null)
                .certificateType(request.getCertificateType() != null ? request.getCertificateType() : com.taxoryn.module.dsc.model.DscCertificateType.CLASS_3)
                .issuer(StringUtils.hasText(request.getIssuer()) ? request.getIssuer().trim() : null)
                .issuedDate(request.getIssuedDate())
                .expiryDate(request.getExpiryDate())
                .status(initialStatus)
                .applicableServices(StringUtils.hasText(request.getApplicableServices()) ? request.getApplicableServices().trim() : null)
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        DscEntity saved = dscRepository.save(entity);

        auditService.logEvent(
                "DSC_CREATED",
                "DSC",
                saved.getId().toString(),
                null,
                "Registered DSC for " + saved.getHolderName() + " (expires " + saved.getExpiryDate() + ")"
        );

        log.info("Created DSC entry id={}, holder={}, org={}", saved.getId(), saved.getHolderName(), organizationId);
        return mapToDto(saved, client, today);
    }

    @Override
    @Transactional
    public DscDto updateDsc(UUID id, UpdateDscRequest request) {
        UUID organizationId = resolveOrganizationId();
        DscEntity entity = dscRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital Signature Certificate", "id", id));

        LocalDate newIssuedDate = request.getIssuedDate() != null ? request.getIssuedDate() : entity.getIssuedDate();
        LocalDate newExpiryDate = request.getExpiryDate() != null ? request.getExpiryDate() : entity.getExpiryDate();
        validateDates(newIssuedDate, newExpiryDate);

        ClientEntity client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
            entity.setClientId(client.getId());
        }

        if (StringUtils.hasText(request.getHolderName())) {
            entity.setHolderName(request.getHolderName().trim());
        }
        if (request.getCertificateIdentifier() != null) {
            entity.setCertificateIdentifier(request.getCertificateIdentifier().trim());
        }
        if (request.getCertificateType() != null) {
            entity.setCertificateType(request.getCertificateType());
        }
        if (request.getIssuer() != null) {
            entity.setIssuer(request.getIssuer().trim());
        }
        entity.setIssuedDate(newIssuedDate);
        entity.setExpiryDate(newExpiryDate);

        if (request.getApplicableServices() != null) {
            entity.setApplicableServices(request.getApplicableServices().trim());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        LocalDate today = LocalDate.now();
        if (entity.getStatus() != DscStatus.REVOKED && entity.getStatus() != DscStatus.INACTIVE) {
            entity.setStatus(entity.getEffectiveStatus(today));
        }

        DscEntity updated = dscRepository.save(entity);

        auditService.logEvent(
                "DSC_UPDATED",
                "DSC",
                updated.getId().toString(),
                null,
                "Updated metadata for DSC " + updated.getHolderName()
        );

        if (client == null && updated.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(updated.getClientId(), organizationId).orElse(null);
        }

        log.info("Updated DSC entry id={}, holder={}, org={}", updated.getId(), updated.getHolderName(), organizationId);
        return mapToDto(updated, client, today);
    }

    @Override
    @Transactional(readOnly = true)
    public DscDto getDscById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        DscEntity entity = dscRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital Signature Certificate", "id", id));

        ClientEntity client = null;
        if (entity.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null);
        }

        return mapToDto(entity, client, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DscDto> getDscList(DscFilterRequest request) {
        UUID organizationId = resolveOrganizationId();
        LocalDate today = LocalDate.now();

        Sort sort = Sort.by(Sort.Direction.ASC, "expiryDate");
        if (StringUtils.hasText(request.getSortBy())) {
            Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, request.getSortBy());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<DscEntity> spec = createSpecification(organizationId, request, today);
        Page<DscEntity> page = dscRepository.findAll(spec, pageable);

        Set<UUID> clientIds = page.getContent().stream()
                .map(DscEntity::getClientId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, ClientEntity> clientMap = clientIds.isEmpty() ? Map.of() :
                clientRepository.findAllById(clientIds).stream()
                        .collect(Collectors.toMap(ClientEntity::getId, c -> c));

        List<DscDto> dtos = page.getContent().stream()
                .map(e -> mapToDto(e, clientMap.get(e.getClientId()), today))
                .collect(Collectors.toList());

        return PagedResponse.<DscDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DscSummaryDto getDscSummary() {
        UUID organizationId = resolveOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(EXPIRY_WARNING_THRESHOLD_DAYS);

        long total = dscRepository.countByOrganizationId(organizationId);
        long revoked = dscRepository.countByOrganizationIdAndStatus(organizationId, DscStatus.REVOKED);
        long inactive = dscRepository.countByOrganizationIdAndStatus(organizationId, DscStatus.INACTIVE);
        long expired = dscRepository.countExpiredByOrganizationId(organizationId, today);
        long expiringSoon = dscRepository.countExpiringSoonByOrganizationId(organizationId, today, cutoffDate);
        long active = dscRepository.countActiveByOrganizationId(organizationId, cutoffDate);

        return DscSummaryDto.builder()
                .total(total)
                .active(active)
                .expiringSoon(expiringSoon)
                .expired(expired)
                .revoked(revoked)
                .inactive(inactive)
                .build();
    }

    @Override
    @Transactional
    public DscDto activateDsc(UUID id) {
        UUID organizationId = resolveOrganizationId();
        DscEntity entity = dscRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital Signature Certificate", "id", id));

        LocalDate today = LocalDate.now();
        entity.setStatus(DscStatus.ACTIVE);
        entity.setStatus(entity.getEffectiveStatus(today));
        DscEntity saved = dscRepository.save(entity);

        auditService.logEvent("DSC_ACTIVATED", "DSC", saved.getId().toString(), null, "Activated DSC " + saved.getHolderName());

        ClientEntity client = entity.getClientId() != null ? clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null) : null;
        return mapToDto(saved, client, today);
    }

    @Override
    @Transactional
    public DscDto deactivateDsc(UUID id) {
        UUID organizationId = resolveOrganizationId();
        DscEntity entity = dscRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital Signature Certificate", "id", id));

        entity.setStatus(DscStatus.INACTIVE);
        DscEntity saved = dscRepository.save(entity);

        auditService.logEvent("DSC_DEACTIVATED", "DSC", saved.getId().toString(), null, "Deactivated DSC " + saved.getHolderName());

        ClientEntity client = entity.getClientId() != null ? clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null) : null;
        return mapToDto(saved, client, LocalDate.now());
    }

    @Override
    @Transactional
    public DscDto revokeDsc(UUID id, String reason) {
        UUID organizationId = resolveOrganizationId();
        DscEntity entity = dscRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital Signature Certificate", "id", id));

        entity.setStatus(DscStatus.REVOKED);
        if (StringUtils.hasText(reason)) {
            String updatedNotes = (entity.getNotes() != null ? entity.getNotes() + "\n" : "") + "[Revocation Reason]: " + reason.trim();
            entity.setNotes(updatedNotes);
        }
        DscEntity saved = dscRepository.save(entity);

        auditService.logEvent("DSC_REVOKED", "DSC", saved.getId().toString(), null, "Revoked DSC " + saved.getHolderName() + (reason != null ? " (" + reason + ")" : ""));

        ClientEntity client = entity.getClientId() != null ? clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null) : null;
        return mapToDto(saved, client, LocalDate.now());
    }

    @Override
    @Transactional
    public void deleteDsc(UUID id) {
        UUID organizationId = resolveOrganizationId();
        DscEntity entity = dscRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital Signature Certificate", "id", id));

        dscRepository.delete(entity);
        auditService.logEvent("DSC_DELETED", "DSC", id.toString(), entity.getHolderName(), "Deleted DSC record");
        log.info("Deleted DSC entry id={}, org={}", id, organizationId);
    }

    @Override
    @Transactional
    public int checkAndTriggerExpiryReminders() {
        UUID organizationId = resolveOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(EXPIRY_WARNING_THRESHOLD_DAYS);

        List<DscEntity> expiringList = dscRepository.findExpiringSoon(organizationId, today, cutoffDate);
        int dispatched = 0;

        for (DscEntity dsc : expiringList) {
            long daysLeft = dsc.getDaysUntilExpiry(today);
            String clientInfo = "";
            if (dsc.getClientId() != null) {
                clientRepository.findByIdAndOrganizationId(dsc.getClientId(), organizationId)
                        .ifPresent(c -> log.debug("DSC linked to client {}", c.getDisplayName()));
            }

            notificationService.notify(
                    organizationId,
                    null,
                    dsc.getClientId(),
                    NotificationEntity.NotificationType.SYSTEM_NOTIFICATION,
                    "DSC Expiry Alert: " + dsc.getHolderName(),
                    "Digital Signature Certificate for " + dsc.getHolderName() + " expires in " + daysLeft + " day(s) on " + dsc.getExpiryDate() + ".",
                    Set.of(NotificationEntity.NotificationChannel.IN_APP),
                    "/dsc-register",
                    null
            );
            dispatched++;
        }

        log.info("Dispatched {} DSC expiry reminder notifications for tenant {}", dispatched, organizationId);
        return dispatched;
    }

    private Specification<DscEntity> createSpecification(UUID organizationId, DscFilterRequest request, LocalDate today) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (StringUtils.hasText(request.getSearch())) {
                String term = "%" + request.getSearch().trim().toLowerCase() + "%";
                Predicate holderMatch = cb.like(cb.lower(root.get("holderName")), term);
                Predicate idMatch = cb.like(cb.lower(root.get("certificateIdentifier")), term);
                Predicate issuerMatch = cb.like(cb.lower(root.get("issuer")), term);
                predicates.add(cb.or(holderMatch, idMatch, issuerMatch));
            }

            if (request.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), request.getClientId()));
            }

            if (request.getCertificateType() != null) {
                predicates.add(cb.equal(root.get("certificateType"), request.getCertificateType()));
            }

            if (StringUtils.hasText(request.getService())) {
                String svcTerm = "%" + request.getService().trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("applicableServices")), svcTerm));
            }

            if (request.getStatus() != null) {
                switch (request.getStatus()) {
                    case REVOKED -> predicates.add(cb.equal(root.get("status"), DscStatus.REVOKED));
                    case INACTIVE -> predicates.add(cb.equal(root.get("status"), DscStatus.INACTIVE));
                    case EXPIRED -> {
                        predicates.add(cb.not(root.get("status").in(DscStatus.REVOKED, DscStatus.INACTIVE)));
                        predicates.add(cb.lessThan(root.get("expiryDate"), today));
                    }
                    case EXPIRING -> {
                        predicates.add(cb.not(root.get("status").in(DscStatus.REVOKED, DscStatus.INACTIVE)));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("expiryDate"), today));
                        predicates.add(cb.lessThanOrEqualTo(root.get("expiryDate"), today.plusDays(EXPIRY_WARNING_THRESHOLD_DAYS)));
                    }
                    case ACTIVE -> {
                        predicates.add(cb.not(root.get("status").in(DscStatus.REVOKED, DscStatus.INACTIVE)));
                        predicates.add(cb.greaterThan(root.get("expiryDate"), today.plusDays(EXPIRY_WARNING_THRESHOLD_DAYS)));
                    }
                }
            }

            if (request.getExpiringWithinDays() != null && request.getExpiringWithinDays() > 0) {
                predicates.add(cb.not(root.get("status").in(DscStatus.REVOKED, DscStatus.INACTIVE)));
                predicates.add(cb.greaterThanOrEqualTo(root.get("expiryDate"), today));
                predicates.add(cb.lessThanOrEqualTo(root.get("expiryDate"), today.plusDays(request.getExpiringWithinDays())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DscDto mapToDto(DscEntity entity, ClientEntity client, LocalDate today) {
        DscStatus effectiveStatus = entity.getEffectiveStatus(today);
        long daysUntilExpiry = entity.getDaysUntilExpiry(today);

        return DscDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientPan(client != null ? client.getPan() : null)
                .clientGstin(client != null ? client.getGstin() : null)
                .holderName(entity.getHolderName())
                .certificateIdentifier(entity.getCertificateIdentifier())
                .certificateType(entity.getCertificateType())
                .issuer(entity.getIssuer())
                .issuedDate(entity.getIssuedDate())
                .expiryDate(entity.getExpiryDate())
                .status(effectiveStatus)
                .daysUntilExpiry(daysUntilExpiry)
                .applicableServices(entity.getApplicableServices())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateDates(LocalDate issuedDate, LocalDate expiryDate) {
        if (issuedDate != null && expiryDate != null && issuedDate.isAfter(expiryDate)) {
            throw new BusinessValidationException("DSC issue date (" + issuedDate + ") cannot be after expiry date (" + expiryDate + ").");
        }
    }

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedException("No active organization context found");
        }
        return tenantId;
    }
}
