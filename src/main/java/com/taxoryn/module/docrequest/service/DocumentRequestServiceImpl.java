package com.taxoryn.module.docrequest.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestFilterRequest;
import com.taxoryn.module.docrequest.dto.DocumentRequestItemDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestSummaryDto;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentService;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRequestServiceImpl implements DocumentRequestService {

    private final DocumentRequestRepository docRequestRepository;
    private final DocumentRequestItemRepository docRequestItemRepository;
    private final ClientRepository clientRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public DocumentRequestDto createAndSendRequest(CreateDocumentRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        String requestNumber = generateRequestNumber();

        DocumentRequestEntity entity = DocumentRequestEntity.builder()
                .clientId(client.getId())
                .requestNumber(requestNumber)
                .purpose(request.getPurpose().trim())
                .dueDate(request.getDueDate())
                .message(request.getMessage() != null ? request.getMessage().trim() : null)
                .status(RequestStatus.SENT)
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .requestedByUserId(currentUserId)
                .sentAt(Instant.now())
                .build();
        entity.setOrganizationId(organizationId);

        List<DocumentRequestItemEntity> items = new ArrayList<>();
        List<String> itemTitles = new ArrayList<>();

        for (CreateDocumentRequestItem itemDto : request.getItems()) {
            DocumentRequestItemEntity item = DocumentRequestItemEntity.builder()
                    .request(entity)
                    .clientId(client.getId())
                    .documentType(itemDto.getDocumentType())
                    .title(itemDto.getTitle().trim())
                    .description(itemDto.getDescription() != null ? itemDto.getDescription().trim() : null)
                    .required(itemDto.isRequired())
                    .status(ItemStatus.PENDING)
                    .build();
            item.setOrganizationId(organizationId);
            items.add(item);
            itemTitles.add(item.getTitle());
        }

        entity.setItems(items);
        DocumentRequestEntity saved = docRequestRepository.save(entity);

        // Fetch organization name for branding
        String practiceName = organizationRepository.findById(organizationId)
                .map(OrganizationEntity::getName)
                .orElse("Taxoryn Practice");

        // 1. In-App Notification
        try {
            notificationService.notify(
                    organizationId,
                    null,
                    client.getId(),
                    NotificationType.DOCUMENT_REQUIRED,
                    "Documents Requested: " + saved.getPurpose(),
                    "Your tax consultant at " + practiceName + " has requested " + items.size() + " documents for " + saved.getPurpose() +
                            (saved.getDueDate() != null ? " (Due by: " + saved.getDueDate() + ")" : ""),
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/portal?tab=documents",
                    "{\"requestId\":\"" + saved.getId() + "\",\"requestNumber\":\"" + saved.getRequestNumber() + "\"}"
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch in-app notification for doc request: {}", e.getMessage());
        }

        // 2. Branded HTML Email
        if (StringUtils.hasText(client.getEmail())) {
            try {
                emailNotificationService.sendDocumentRequestEmail(
                        client.getEmail(),
                        client.getDisplayName(),
                        saved.getPurpose(),
                        practiceName,
                        saved.getDueDate(),
                        saved.getMessage(),
                        itemTitles
                );
            } catch (Exception e) {
                log.warn("Failed to send document request email to {}: {}", client.getEmail(), e.getMessage());
            }
        }

        // 3. Audit log
        auditService.logEvent(
                "DOCUMENT_REQUEST_CREATED",
                "DOCUMENT_REQUEST",
                saved.getId().toString(),
                organizationId,
                "Created document request " + saved.getRequestNumber() + " (" + saved.getPurpose() + ") with " + items.size() + " items for client " + client.getDisplayName()
        );
        auditService.logEvent(
                "DOCUMENT_REQUEST_SENT",
                "DOCUMENT_REQUEST",
                saved.getId().toString(),
                organizationId,
                "Sent document request " + saved.getRequestNumber() + " to client " + client.getDisplayName()
        );

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentRequestDto getRequestById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentRequestEntity entity = docRequestRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", id));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentRequestDto> getRequests(DocumentRequestFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<DocumentRequestEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (StringUtils.hasText(filter.getSearch())) {
                String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("purpose")), term),
                        cb.like(cb.lower(root.get("requestNumber")), term)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<DocumentRequestEntity> entityPage = docRequestRepository.findAll(spec, pageable);
        return PagedResponse.of(entityPage, this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRequestDto> getClientRequests(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return docRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentRequestSummaryDto getSummaryStats() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<DocumentRequestEntity> all = docRequestRepository.findAll((root, query, cb) -> cb.equal(root.get("organizationId"), organizationId));

        long total = all.size();
        long pending = 0;
        long partial = 0;
        long completed = 0;
        long overdue = 0;

        LocalDate today = LocalDate.now();

        for (DocumentRequestEntity r : all) {
            boolean isOverdue = r.getDueDate() != null && today.isAfter(r.getDueDate()) &&
                    r.getStatus() != RequestStatus.COMPLETED && r.getStatus() != RequestStatus.CANCELLED;

            if (isOverdue) {
                overdue++;
            }

            if (r.getStatus() == RequestStatus.SENT || r.getStatus() == RequestStatus.DRAFT) {
                pending++;
            } else if (r.getStatus() == RequestStatus.PARTIALLY_COMPLETED) {
                partial++;
            } else if (r.getStatus() == RequestStatus.COMPLETED) {
                completed++;
            }
        }

        return DocumentRequestSummaryDto.builder()
                .totalRequests(total)
                .pendingRequests(pending)
                .partiallyCompletedRequests(partial)
                .completedRequests(completed)
                .overdueRequests(overdue)
                .build();
    }

    @Override
    @Transactional
    public DocumentRequestDto acceptItem(UUID itemId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        DocumentRequestItemEntity item = docRequestItemRepository.findByIdAndOrganizationId(itemId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequestItem", "id", itemId));

        if (item.getStatus() == ItemStatus.PENDING) {
            throw new BadRequestException("Cannot accept a document that has not been uploaded yet");
        }

        item.setStatus(ItemStatus.ACCEPTED);
        item.setReviewedByUserId(currentUserId);
        item.setReviewedAt(Instant.now());
        item.setRejectionReason(null);
        docRequestItemRepository.save(item);

        DocumentRequestEntity request = item.getRequest();
        List<DocumentRequestItemEntity> allItems = docRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(request.getId());

        boolean allRequiredAccepted = allItems.stream()
                .filter(DocumentRequestItemEntity::isRequired)
                .allMatch(i -> i.getStatus() == ItemStatus.ACCEPTED);

        if (allRequiredAccepted) {
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletedAt(Instant.now());
            docRequestRepository.save(request);

            auditService.logEvent(
                    "DOCUMENT_REQUEST_COMPLETED",
                    "DOCUMENT_REQUEST",
                    request.getId().toString(),
                    organizationId,
                    "All required documents accepted for request " + request.getRequestNumber()
            );
        } else {
            request.setStatus(RequestStatus.PARTIALLY_COMPLETED);
            docRequestRepository.save(request);
        }

        auditService.logEvent(
                "DOCUMENT_ACCEPTED",
                "DOCUMENT_REQUEST_ITEM",
                item.getId().toString(),
                organizationId,
                "Accepted document '" + item.getTitle() + "' for request " + request.getRequestNumber()
        );

        return toDto(request);
    }

    @Override
    @Transactional
    public DocumentRequestDto rejectItem(UUID itemId, RejectDocumentItemRequest rejectRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (!StringUtils.hasText(rejectRequest.getRejectionReason())) {
            throw new BadRequestException("Rejection reason is required");
        }

        DocumentRequestItemEntity item = docRequestItemRepository.findByIdAndOrganizationId(itemId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequestItem", "id", itemId));

        item.setStatus(ItemStatus.REJECTED);
        item.setRejectionReason(rejectRequest.getRejectionReason().trim());
        item.setReviewedByUserId(currentUserId);
        item.setReviewedAt(Instant.now());
        docRequestItemRepository.save(item);

        DocumentRequestEntity request = item.getRequest();
        if (request.getStatus() == RequestStatus.COMPLETED) {
            request.setStatus(RequestStatus.PARTIALLY_COMPLETED);
            request.setCompletedAt(null);
            docRequestRepository.save(request);
        }

        // Notify client about rejection
        ClientEntity client = clientRepository.findById(request.getClientId()).orElse(null);
        String practiceName = organizationRepository.findById(organizationId)
                .map(OrganizationEntity::getName)
                .orElse("Taxoryn Practice");

        if (client != null && StringUtils.hasText(client.getEmail())) {
            try {
                emailNotificationService.sendDocumentRejectedEmail(
                        client.getEmail(),
                        client.getDisplayName(),
                        request.getPurpose(),
                        item.getTitle(),
                        item.getRejectionReason(),
                        practiceName
                );
            } catch (Exception e) {
                log.warn("Failed to send rejection email to {}: {}", client.getEmail(), e.getMessage());
            }
        }

        auditService.logEvent(
                "DOCUMENT_REJECTED",
                "DOCUMENT_REQUEST_ITEM",
                item.getId().toString(),
                organizationId,
                "Rejected document '" + item.getTitle() + "' for request " + request.getRequestNumber() + ". Reason: " + item.getRejectionReason()
        );

        return toDto(request);
    }

    @Override
    @Transactional
    public void sendReminder(UUID requestId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentRequestEntity request = docRequestRepository.findByIdAndOrganizationId(requestId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", requestId));

        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Cannot send reminder for a completed or cancelled request");
        }

        ClientEntity client = clientRepository.findById(request.getClientId()).orElse(null);
        if (client == null) return;

        List<DocumentRequestItemEntity> items = docRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(request.getId());
        List<String> pendingTitles = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.PENDING || i.getStatus() == ItemStatus.REJECTED)
                .map(DocumentRequestItemEntity::getTitle)
                .toList();

        String practiceName = organizationRepository.findById(organizationId)
                .map(OrganizationEntity::getName)
                .orElse("Taxoryn Practice");

        if (StringUtils.hasText(client.getEmail())) {
            try {
                emailNotificationService.sendDocumentReminderEmail(
                        client.getEmail(),
                        client.getDisplayName(),
                        request.getPurpose(),
                        practiceName,
                        request.getDueDate(),
                        pendingTitles
                );
            } catch (Exception e) {
                log.warn("Failed to send reminder email to {}: {}", client.getEmail(), e.getMessage());
            }
        }

        auditService.logEvent(
                "DOCUMENT_REQUEST_REMINDER_SENT",
                "DOCUMENT_REQUEST",
                request.getId().toString(),
                organizationId,
                "Sent reminder for document request " + request.getRequestNumber() + " to client " + client.getDisplayName()
        );
    }

    @Override
    @Transactional
    public DocumentRequestDto cancelRequest(UUID requestId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentRequestEntity request = docRequestRepository.findByIdAndOrganizationId(requestId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", requestId));

        request.setStatus(RequestStatus.CANCELLED);
        DocumentRequestEntity saved = docRequestRepository.save(request);

        auditService.logEvent(
                "DOCUMENT_REQUEST_CANCELLED",
                "DOCUMENT_REQUEST",
                saved.getId().toString(),
                organizationId,
                "Cancelled document request " + saved.getRequestNumber()
        );

        return toDto(saved);
    }

    @Override
    @Transactional
    public DocumentRequestDto uploadItemDocument(UUID itemId, MultipartFile file) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentRequestItemEntity item = docRequestItemRepository.findByIdAndOrganizationId(itemId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequestItem", "id", itemId));

        return processItemUpload(item, file, organizationId);
    }

    // =========================================================================
    // Client Portal Methods
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRequestDto> getClientPortalRequests() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        return docRequestRepository.findAllByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentRequestDto getClientPortalRequestById(UUID requestId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        DocumentRequestEntity request = docRequestRepository.findByIdAndClientId(requestId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", requestId));
        return toDto(request);
    }

    @Override
    @Transactional
    public DocumentRequestDto uploadClientPortalItemDocument(UUID itemId, MultipartFile file) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        DocumentRequestItemEntity item = docRequestItemRepository.findByIdAndClientId(itemId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequestItem", "id", itemId));

        return processItemUpload(item, file, item.getOrganizationId());
    }

    private DocumentRequestDto processItemUpload(DocumentRequestItemEntity item, MultipartFile file, UUID organizationId) {
        DocumentRequestEntity request = item.getRequest();

        // 1. Upload via DocumentService
        UploadDocumentRequest uploadReq = UploadDocumentRequest.builder()
                .clientId(item.getClientId())
                .documentType(item.getDocumentType())
                .notes("Uploaded for document request: " + request.getRequestNumber() + " (" + item.getTitle() + ")")
                .build();

        DocumentDto uploadedDoc = documentService.uploadDocument(file, uploadReq);

        // 2. Update item state
        item.setUploadedDocumentId(uploadedDoc.getId());
        item.setUploadedAt(Instant.now());
        item.setStatus(ItemStatus.UPLOADED);
        item.setRejectionReason(null);
        docRequestItemRepository.save(item);

        // 3. Update parent request status
        if (request.getStatus() == RequestStatus.SENT || request.getStatus() == RequestStatus.DRAFT) {
            request.setStatus(RequestStatus.PARTIALLY_COMPLETED);
            docRequestRepository.save(request);
        }

        // 4. Audit log
        auditService.logEvent(
                "DOCUMENT_UPLOADED",
                "DOCUMENT_REQUEST_ITEM",
                item.getId().toString(),
                organizationId,
                "Uploaded document '" + uploadedDoc.getFileName() + "' for request item '" + item.getTitle() + "' in " + request.getRequestNumber()
        );

        return toDto(request);
    }

    private String generateRequestNumber() {
        int year = Year.now().getValue();
        long count = docRequestRepository.count();
        return String.format("REQ-%d-%06d", year, count + 1001);
    }

    private DocumentRequestDto toDto(DocumentRequestEntity entity) {
        ClientEntity client = clientRepository.findById(entity.getClientId()).orElse(null);
        String clientName = client != null ? client.getDisplayName() : "Unknown Client";
        String clientPan = client != null ? client.getPan() : null;

        String requestedByName = null;
        if (entity.getRequestedByUserId() != null) {
            requestedByName = userRepository.findById(entity.getRequestedByUserId())
                    .map(u -> u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : ""))
                    .orElse(null);
        }

        List<DocumentRequestItemEntity> items = entity.getItems() != null
                ? entity.getItems()
                : docRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(entity.getId());

        int total = items.size();
        int uploaded = 0;
        int accepted = 0;
        int pending = 0;
        int rejected = 0;

        List<DocumentRequestItemDto> itemDtos = new ArrayList<>();
        Map<UUID, DocumentEntity> docCache = new HashMap<>();

        for (DocumentRequestItemEntity item : items) {
            if (item.getStatus() == ItemStatus.ACCEPTED) accepted++;
            else if (item.getStatus() == ItemStatus.UPLOADED || item.getStatus() == ItemStatus.UNDER_REVIEW) uploaded++;
            else if (item.getStatus() == ItemStatus.REJECTED) rejected++;
            else pending++;

            String docName = null;
            Long docSize = null;
            String docContentType = null;

            if (item.getUploadedDocumentId() != null) {
                DocumentEntity doc = docCache.computeIfAbsent(
                        item.getUploadedDocumentId(),
                        id -> documentRepository.findById(id).orElse(null)
                );
                if (doc != null) {
                    docName = doc.getFileName();
                    docSize = doc.getFileSize();
                    docContentType = doc.getContentType();
                }
            }

            itemDtos.add(DocumentRequestItemDto.builder()
                    .id(item.getId())
                    .requestId(entity.getId())
                    .clientId(item.getClientId())
                    .documentType(item.getDocumentType())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .required(item.isRequired())
                    .status(item.getStatus())
                    .uploadedDocumentId(item.getUploadedDocumentId())
                    .uploadedDocumentName(docName)
                    .uploadedDocumentSize(docSize)
                    .uploadedDocumentContentType(docContentType)
                    .uploadedAt(item.getUploadedAt())
                    .reviewedByUserId(item.getReviewedByUserId())
                    .reviewedAt(item.getReviewedAt())
                    .rejectionReason(item.getRejectionReason())
                    .build());
        }

        boolean isOverdue = entity.getDueDate() != null &&
                LocalDate.now().isAfter(entity.getDueDate()) &&
                entity.getStatus() != RequestStatus.COMPLETED &&
                entity.getStatus() != RequestStatus.CANCELLED;

        return DocumentRequestDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .clientPan(clientPan)
                .requestNumber(entity.getRequestNumber())
                .purpose(entity.getPurpose())
                .dueDate(entity.getDueDate())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .requestedByUserId(entity.getRequestedByUserId())
                .requestedByName(requestedByName)
                .sentAt(entity.getSentAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .totalItems(total)
                .uploadedItems(uploaded)
                .acceptedItems(accepted)
                .pendingItems(pending)
                .rejectedItems(rejected)
                .isOverdue(isOverdue)
                .items(itemDtos)
                .build();
    }
}