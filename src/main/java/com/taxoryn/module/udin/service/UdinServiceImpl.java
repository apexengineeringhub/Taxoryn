package com.taxoryn.module.udin.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.udin.dto.CancelUdinRequest;
import com.taxoryn.module.udin.dto.CreateUdinRequest;
import com.taxoryn.module.udin.dto.UdinDto;
import com.taxoryn.module.udin.dto.UdinFilterRequest;
import com.taxoryn.module.udin.dto.UdinSummaryDto;
import com.taxoryn.module.udin.dto.UpdateUdinRequest;
import com.taxoryn.module.udin.dto.UpdateUdinVerificationRequest;
import com.taxoryn.module.udin.entity.UdinEntity;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import com.taxoryn.module.udin.repository.UdinRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UdinServiceImpl implements UdinService {

    private final UdinRepository udinRepository;
    private final ClientRepository clientRepository;
    private final EngagementRepository engagementRepository;
    private final ServiceRepository serviceRepository;
    private final InvoiceRepository invoiceRepository;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public UdinDto createUdin(CreateUdinRequest request) {
        UUID organizationId = resolveOrganizationId();

        String normalizedUdin = validateAndNormalizeUdin(request.getUdin());

        if (udinRepository.existsByOrganizationIdAndUdin(organizationId, normalizedUdin)) {
            throw new BusinessValidationException("UDIN '" + normalizedUdin + "' is already registered in this organization.");
        }

        // Validate client if provided
        ClientEntity client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
        }

        // Validate engagement if provided
        EngagementEntity engagement = null;
        if (request.getEngagementId() != null) {
            engagement = engagementRepository.findByIdAndOrganizationId(request.getEngagementId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", request.getEngagementId()));
        }

        // Validate service if provided
        ServiceEntity service = null;
        if (request.getServiceId() != null) {
            service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));
        }

        // Validate invoice if provided
        InvoiceEntity invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndOrganizationId(request.getInvoiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));
        }

        // Validate document if provided
        DocumentEntity document = null;
        if (request.getDocumentId() != null) {
            document = documentRepository.findByIdAndOrganizationId(request.getDocumentId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", "id", request.getDocumentId()));
        }

        UdinEntity entity = UdinEntity.builder()
                .udin(normalizedUdin)
                .clientId(client != null ? client.getId() : null)
                .engagementId(engagement != null ? engagement.getId() : null)
                .serviceId(service != null ? service.getId() : null)
                .documentId(document != null ? document.getId() : null)
                .invoiceId(invoice != null ? invoice.getId() : null)
                .documentType(request.getDocumentType())
                .documentTitle(request.getDocumentTitle().trim())
                .documentDescription(StringUtils.hasText(request.getDocumentDescription()) ? request.getDocumentDescription().trim() : null)
                .signatoryName(request.getSignatoryName().trim())
                .signatoryMembershipNo(StringUtils.hasText(request.getSignatoryMembershipNo()) ? request.getSignatoryMembershipNo().trim() : null)
                .generationDate(request.getGenerationDate())
                .status(UdinStatus.ACTIVE)
                .verificationStatus(UdinVerificationStatus.NOT_VERIFIED)
                .verificationSource("MANUAL")
                .financialFiguresJson(request.getFinancialFiguresJson())
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        UdinEntity saved = udinRepository.save(entity);

        auditService.logEvent(
                "UDIN_REGISTERED",
                "UDIN",
                saved.getId().toString(),
                null,
                "Registered UDIN " + saved.getUdin() + " for " + saved.getDocumentTitle()
        );

        log.info("Created UDIN entry id={}, udin={}, org={}", saved.getId(), saved.getUdin(), organizationId);
        return mapToDto(saved, client, engagement, service, document, invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public UdinDto getUdinById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        UdinEntity entity = udinRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("UDIN Record", "id", id));

        return enrichSingleDto(entity, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UdinDto> getUdins(UdinFilterRequest filter, Pageable pageable) {
        UUID organizationId = resolveOrganizationId();

        Specification<UdinEntity> spec = createSpecification(organizationId, filter);
        Page<UdinEntity> page = udinRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return PagedResponse.<UdinDto>builder()
                    .content(List.of())
                    .pageNumber(page.getNumber())
                    .pageSize(page.getSize())
                    .totalElements(0)
                    .totalPages(0)
                    .isFirst(true)
                    .isLast(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }

        // Batch fetch related entities
        Set<UUID> clientIds = page.getContent().stream().map(UdinEntity::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> engagementIds = page.getContent().stream().map(UdinEntity::getEngagementId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> serviceIds = page.getContent().stream().map(UdinEntity::getServiceId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> documentIds = page.getContent().stream().map(UdinEntity::getDocumentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> invoiceIds = page.getContent().stream().map(UdinEntity::getInvoiceId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, ClientEntity> clientMap = clientIds.isEmpty() ? Map.of() :
                clientRepository.findAllById(clientIds).stream().collect(Collectors.toMap(ClientEntity::getId, c -> c));

        Map<UUID, EngagementEntity> engagementMap = engagementIds.isEmpty() ? Map.of() :
                engagementRepository.findAllById(engagementIds).stream().collect(Collectors.toMap(EngagementEntity::getId, e -> e));

        Map<UUID, ServiceEntity> serviceMap = serviceIds.isEmpty() ? Map.of() :
                serviceRepository.findAllById(serviceIds).stream().collect(Collectors.toMap(ServiceEntity::getId, s -> s));

        Map<UUID, DocumentEntity> documentMap = documentIds.isEmpty() ? Map.of() :
                documentRepository.findAllById(documentIds).stream().collect(Collectors.toMap(DocumentEntity::getId, d -> d));

        Map<UUID, InvoiceEntity> invoiceMap = invoiceIds.isEmpty() ? Map.of() :
                invoiceRepository.findAllById(invoiceIds).stream().collect(Collectors.toMap(InvoiceEntity::getId, i -> i));

        List<UdinDto> dtos = page.getContent().stream()
                .map(e -> mapToDto(
                        e,
                        clientMap.get(e.getClientId()),
                        engagementMap.get(e.getEngagementId()),
                        serviceMap.get(e.getServiceId()),
                        documentMap.get(e.getDocumentId()),
                        invoiceMap.get(e.getInvoiceId())
                ))
                .collect(Collectors.toList());

        return PagedResponse.<UdinDto>builder()
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
    public UdinSummaryDto getUdinSummary() {
        UUID organizationId = resolveOrganizationId();

        long total = udinRepository.countByOrganizationId(organizationId);
        long active = udinRepository.countByOrganizationIdAndStatus(organizationId, UdinStatus.ACTIVE);
        long cancelled = udinRepository.countByOrganizationIdAndStatus(organizationId, UdinStatus.CANCELLED);
        long verified = udinRepository.countByOrganizationIdAndVerificationStatus(organizationId, UdinVerificationStatus.VERIFIED);
        long unverified = udinRepository.countByOrganizationIdAndVerificationStatus(organizationId, UdinVerificationStatus.NOT_VERIFIED);
        long failed = udinRepository.countByOrganizationIdAndVerificationStatus(organizationId, UdinVerificationStatus.FAILED);

        return UdinSummaryDto.builder()
                .totalCount(total)
                .activeCount(active)
                .verifiedCount(verified)
                .unverifiedCount(unverified)
                .failedVerificationCount(failed)
                .cancelledCount(cancelled)
                .build();
    }

    @Override
    @Transactional
    public UdinDto updateUdin(UUID id, UpdateUdinRequest request) {
        UUID organizationId = resolveOrganizationId();
        UdinEntity entity = udinRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("UDIN Record", "id", id));

        if (request.getClientId() != null) {
            clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
            entity.setClientId(request.getClientId());
        } else {
            entity.setClientId(null);
        }

        if (request.getEngagementId() != null) {
            engagementRepository.findByIdAndOrganizationId(request.getEngagementId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", request.getEngagementId()));
            entity.setEngagementId(request.getEngagementId());
        } else {
            entity.setEngagementId(null);
        }

        if (request.getServiceId() != null) {
            serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));
            entity.setServiceId(request.getServiceId());
        } else {
            entity.setServiceId(null);
        }

        if (request.getDocumentId() != null) {
            documentRepository.findByIdAndOrganizationId(request.getDocumentId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", "id", request.getDocumentId()));
            entity.setDocumentId(request.getDocumentId());
        } else {
            entity.setDocumentId(null);
        }

        if (request.getInvoiceId() != null) {
            invoiceRepository.findByIdAndOrganizationId(request.getInvoiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));
            entity.setInvoiceId(request.getInvoiceId());
        } else {
            entity.setInvoiceId(null);
        }

        if (request.getDocumentType() != null) {
            entity.setDocumentType(request.getDocumentType());
        }
        if (StringUtils.hasText(request.getDocumentTitle())) {
            entity.setDocumentTitle(request.getDocumentTitle().trim());
        }
        if (request.getDocumentDescription() != null) {
            entity.setDocumentDescription(request.getDocumentDescription().trim());
        }
        if (StringUtils.hasText(request.getSignatoryName())) {
            entity.setSignatoryName(request.getSignatoryName().trim());
        }
        if (request.getSignatoryMembershipNo() != null) {
            entity.setSignatoryMembershipNo(request.getSignatoryMembershipNo().trim());
        }
        if (request.getGenerationDate() != null) {
            entity.setGenerationDate(request.getGenerationDate());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getFinancialFiguresJson() != null) {
            entity.setFinancialFiguresJson(request.getFinancialFiguresJson());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        UdinEntity updated = udinRepository.save(entity);

        auditService.logEvent(
                "UDIN_UPDATED",
                "UDIN",
                updated.getId().toString(),
                null,
                "Updated details for UDIN " + updated.getUdin()
        );

        log.info("Updated UDIN entry id={}, udin={}, org={}", updated.getId(), updated.getUdin(), organizationId);
        return enrichSingleDto(updated, organizationId);
    }

    @Override
    @Transactional
    public UdinDto updateVerification(UUID id, UpdateUdinVerificationRequest request) {
        UUID organizationId = resolveOrganizationId();
        UdinEntity entity = udinRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("UDIN Record", "id", id));

        UdinVerificationStatus oldStatus = entity.getVerificationStatus();
        entity.setVerificationStatus(request.getVerificationStatus());
        if (StringUtils.hasText(request.getVerificationSource())) {
            entity.setVerificationSource(request.getVerificationSource().trim());
        }
        if (request.getVerificationRemarks() != null) {
            entity.setVerificationRemarks(request.getVerificationRemarks().trim());
        }
        entity.setVerifiedAt(Instant.now());

        UdinEntity saved = udinRepository.save(entity);

        auditService.logEvent(
                "UDIN_VERIFICATION_STATUS_CHANGED",
                "UDIN",
                saved.getId().toString(),
                oldStatus.name(),
                "Changed verification status to " + request.getVerificationStatus() + " for UDIN " + saved.getUdin()
        );

        log.info("Updated UDIN verification id={}, udin={}, status={}, org={}", saved.getId(), saved.getUdin(), request.getVerificationStatus(), organizationId);
        return enrichSingleDto(saved, organizationId);
    }

    @Override
    @Transactional
    public UdinDto cancelUdin(UUID id, CancelUdinRequest request) {
        UUID organizationId = resolveOrganizationId();
        UdinEntity entity = udinRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("UDIN Record", "id", id));

        entity.setStatus(UdinStatus.CANCELLED);
        String reasonText = "[Cancellation Reason]: " + request.getReason().trim();
        String currentNotes = entity.getNotes();
        entity.setNotes(StringUtils.hasText(currentNotes) ? currentNotes + "\n" + reasonText : reasonText);

        UdinEntity saved = udinRepository.save(entity);

        auditService.logEvent(
                "UDIN_CANCELLED",
                "UDIN",
                saved.getId().toString(),
                null,
                "Cancelled UDIN " + saved.getUdin() + ": " + request.getReason().trim()
        );

        log.info("Cancelled UDIN id={}, udin={}, org={}", saved.getId(), saved.getUdin(), organizationId);
        return enrichSingleDto(saved, organizationId);
    }

    @Override
    @Transactional
    public void deleteUdin(UUID id) {
        UUID organizationId = resolveOrganizationId();
        UdinEntity entity = udinRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("UDIN Record", "id", id));

        udinRepository.delete(entity);

        auditService.logEvent(
                "UDIN_DELETED",
                "UDIN",
                id.toString(),
                entity.getUdin(),
                "Deleted UDIN record"
        );

        log.info("Deleted UDIN record id={}, udin={}, org={}", id, entity.getUdin(), organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UdinDto> getUdinsByClientId(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        List<UdinEntity> entities = udinRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        return entities.stream().map(e -> enrichSingleDto(e, organizationId)).collect(Collectors.toList());
    }

    private Specification<UdinEntity> createSpecification(UUID organizationId, UdinFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filter != null) {
                if (StringUtils.hasText(filter.getSearch())) {
                    String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
                    Predicate udinMatch = cb.like(cb.lower(root.get("udin")), term);
                    Predicate titleMatch = cb.like(cb.lower(root.get("documentTitle")), term);
                    Predicate signatoryMatch = cb.like(cb.lower(root.get("signatoryName")), term);
                    Predicate descMatch = cb.like(cb.lower(root.get("documentDescription")), term);
                    predicates.add(cb.or(udinMatch, titleMatch, signatoryMatch, descMatch));
                }

                if (filter.getClientId() != null) {
                    predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
                }

                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getVerificationStatus() != null) {
                    predicates.add(cb.equal(root.get("verificationStatus"), filter.getVerificationStatus()));
                }

                if (filter.getDocumentType() != null) {
                    predicates.add(cb.equal(root.get("documentType"), filter.getDocumentType()));
                }

                if (StringUtils.hasText(filter.getSignatoryMembershipNo())) {
                    predicates.add(cb.equal(root.get("signatoryMembershipNo"), filter.getSignatoryMembershipNo().trim()));
                }

                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("generationDate"), filter.getStartDate()));
                }

                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("generationDate"), filter.getEndDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UdinDto enrichSingleDto(UdinEntity entity, UUID organizationId) {
        ClientEntity client = entity.getClientId() != null ? clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null) : null;
        EngagementEntity engagement = entity.getEngagementId() != null ? engagementRepository.findByIdAndOrganizationId(entity.getEngagementId(), organizationId).orElse(null) : null;
        ServiceEntity service = entity.getServiceId() != null ? serviceRepository.findById(entity.getServiceId()).orElse(null) : null;
        DocumentEntity document = entity.getDocumentId() != null ? documentRepository.findByIdAndOrganizationId(entity.getDocumentId(), organizationId).orElse(null) : null;
        InvoiceEntity invoice = entity.getInvoiceId() != null ? invoiceRepository.findByIdAndOrganizationId(entity.getInvoiceId(), organizationId).orElse(null) : null;

        return mapToDto(entity, client, engagement, service, document, invoice);
    }

    private UdinDto mapToDto(
            UdinEntity entity,
            ClientEntity client,
            EngagementEntity engagement,
            ServiceEntity service,
            DocumentEntity document,
            InvoiceEntity invoice
    ) {
        return UdinDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .udin(entity.getUdin())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientPan(client != null ? client.getPan() : null)
                .engagementId(entity.getEngagementId())
                .engagementName(engagement != null ? engagement.getName() : null)
                .serviceId(entity.getServiceId())
                .serviceName(service != null ? service.getServiceName() : null)
                .documentId(entity.getDocumentId())
                .documentFileName(document != null ? document.getFileName() : null)
                .invoiceId(entity.getInvoiceId())
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .documentType(entity.getDocumentType())
                .documentTitle(entity.getDocumentTitle())
                .documentDescription(entity.getDocumentDescription())
                .signatoryName(entity.getSignatoryName())
                .signatoryMembershipNo(entity.getSignatoryMembershipNo())
                .generationDate(entity.getGenerationDate())
                .status(entity.getStatus())
                .verificationStatus(entity.getVerificationStatus())
                .verificationSource(entity.getVerificationSource())
                .verifiedBy(entity.getVerifiedBy())
                .verifiedAt(entity.getVerifiedAt())
                .verificationRemarks(entity.getVerificationRemarks())
                .financialFiguresJson(entity.getFinancialFiguresJson())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String validateAndNormalizeUdin(String udin) {
        if (!StringUtils.hasText(udin)) {
            throw new BusinessValidationException("UDIN cannot be blank.");
        }
        String trimmed = udin.trim().toUpperCase();
        if (trimmed.length() != 18) {
            throw new BusinessValidationException("UDIN must be exactly 18 characters long. Given: " + trimmed.length());
        }
        if (!trimmed.matches("^[A-Z0-9]{18}$")) {
            throw new BusinessValidationException("UDIN must contain only alphanumeric characters.");
        }
        return trimmed;
    }

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedException("No active organization context found");
        }
        return tenantId;
    }
}
