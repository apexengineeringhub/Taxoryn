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
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.module.docrequest.dto.CreateClientAcknowledgementRequest;
import com.taxoryn.module.docrequest.dto.DeclineDocumentRequest;
import com.taxoryn.module.docrequest.dto.SendDocumentToClientRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.DocumentCategory;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.ExchangeType;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestDirection;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentScanStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
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
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final com.taxoryn.module.task.repository.TaskRepository taskRepository;
    private final com.taxoryn.module.employee.repository.EmployeeRepository employeeRepository;

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
                .taskId(request.getTaskId())
                .complianceId(request.getComplianceId())
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
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<DocumentRequestEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (accessibleClientIds != null) {
                if (accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getDirection() != null) {
                predicates.add(cb.equal(root.get("direction"), filter.getDirection()));
            }
            if (filter.getExchangeType() != null) {
                predicates.add(cb.equal(root.get("exchangeType"), filter.getExchangeType()));
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
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

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

            // Unblock linked Task if any
            if (request.getTaskId() != null) {
                taskRepository.findByIdAndOrganizationId(request.getTaskId(), organizationId).ifPresent(task -> {
                    if (task.getStatus() == com.taxoryn.module.task.entity.TaskEntity.TaskStatus.BLOCKED) {
                        task.setStatus(com.taxoryn.module.task.entity.TaskEntity.TaskStatus.IN_PROGRESS);
                        task.setBlockedReason(null);
                        taskRepository.save(task);

                        auditService.logEvent(
                                "TASK_UNBLOCKED",
                                "TASK",
                                task.getId().toString(),
                                organizationId,
                                "Task unblocked: All required documents accepted for request " + request.getRequestNumber()
                        );

                        if (task.getAssignedTo() != null) {
                            notificationService.notify(
                                    organizationId,
                                    task.getAssignedTo(),
                                    null,
                                    NotificationType.TASK_UNBLOCKED,
                                    "Task Unblocked: " + task.getTitle(),
                                    "All required client documents for \"" + request.getPurpose() + "\" have been accepted. You can now proceed with this task.",
                                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                                    "/tasks/" + task.getId(),
                                    "{\"taskId\":\"" + task.getId() + "\",\"requestId\":\"" + request.getId() + "\"}"
                            );
                        }
                    }
                });
            }

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

        // Block linked Task if any
        if (request.getTaskId() != null) {
            taskRepository.findByIdAndOrganizationId(request.getTaskId(), organizationId).ifPresent(task -> {
                if (task.getStatus() != com.taxoryn.module.task.entity.TaskEntity.TaskStatus.COMPLETED
                        && task.getStatus() != com.taxoryn.module.task.entity.TaskEntity.TaskStatus.CANCELLED) {
                    task.setStatus(com.taxoryn.module.task.entity.TaskEntity.TaskStatus.BLOCKED);
                    task.setBlockedReason("Document rejected: " + item.getTitle() + " - " + rejectRequest.getRejectionReason().trim());
                    taskRepository.save(task);

                    auditService.logEvent(
                            "TASK_BLOCKED",
                            "TASK",
                            task.getId().toString(),
                            organizationId,
                            "Task blocked: document rejected for " + request.getRequestNumber()
                    );
                }
            });
        }

        // 1. Notify client via In-App Notification
        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId).orElse(null);
        String practiceName = organizationRepository.findById(organizationId)
                .map(OrganizationEntity::getName)
                .orElse("Taxoryn Practice");

        if (client != null) {
            notificationService.notify(
                    organizationId,
                    null,
                    client.getId(),
                    NotificationType.DOCUMENT_REJECTED,
                    "Action Required: Document Needs Correction",
                    "Your document \"" + item.getTitle() + "\" for " + request.getPurpose() +
                            " was rejected: " + item.getRejectionReason() + ". Please re-upload a corrected file.",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/portal?tab=documents",
                    "{\"requestId\":\"" + request.getId() + "\",\"itemId\":\"" + item.getId() + "\"}"
            );
        }

        // 2. Notify client via Branded Email
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

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId).orElse(null);
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
    // Bidirectional Document Exchange & Delivery
    // =========================================================================

    @Override
    @Transactional
    public DocumentRequestDto createClientAcknowledgementRequest(CreateClientAcknowledgementRequest request) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        UUID organizationId = client.getOrganizationId();
        String requestNumber = generateRequestNumber();

        DocumentRequestEntity entity = DocumentRequestEntity.builder()
                .clientId(clientId)
                .requestNumber(requestNumber)
                .purpose(request.getPurpose().trim())
                .exchangeType(ExchangeType.ACKNOWLEDGEMENT_REQUEST)
                .direction(RequestDirection.CLIENT_TO_PRACTITIONER)
                .category(request.getCategory())
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .taxPeriod(request.getTaxPeriod())
                .message(request.getMessage() != null ? request.getMessage().trim() : null)
                .status(RequestStatus.REQUESTED)
                .complianceId(request.getComplianceId())
                .taskId(request.getTaskId())
                .sentAt(Instant.now())
                .build();
        entity.setOrganizationId(organizationId);

        DocumentRequestEntity saved = docRequestRepository.save(entity);

        // Notify practitioner
        try {
            UUID practitionerUserId = null;
            if (client.getAssignedEmployeeId() != null) {
                practitionerUserId = employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), organizationId)
                        .map(com.taxoryn.module.employee.entity.EmployeeEntity::getUserId)
                        .orElse(null);
            }
            if (practitionerUserId == null) {
                practitionerUserId = userRepository.findAllByOrganizationId(organizationId).stream()
                        .filter(u -> u.getStatus() == com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                        .findFirst()
                        .map(com.taxoryn.module.user.entity.UserEntity::getId)
                        .orElse(null);
            }

            if (practitionerUserId != null) {
                String categoryName = request.getCategory() != null ? request.getCategory().name() : "Document";
                notificationService.notify(
                        organizationId,
                        practitionerUserId,
                        null,
                        NotificationType.DOCUMENT_REQUIRED,
                        "Client Document Request: " + saved.getPurpose(),
                        client.getDisplayName() + " requested " + categoryName + " (" + saved.getPurpose() + ").",
                        Set.of(NotificationChannel.IN_APP),
                        "/documents",
                        "{\"requestId\":\"" + saved.getId() + "\",\"requestNumber\":\"" + saved.getRequestNumber() + "\"}"
                );
            }
        } catch (Exception e) {
            log.warn("Failed to notify practitioner for client acknowledgement request: {}", e.getMessage());
        }

        auditService.logEvent(
                "ACKNOWLEDGEMENT_REQUESTED",
                "DOCUMENT_REQUEST",
                saved.getId().toString(),
                organizationId,
                "Client " + client.getDisplayName() + " requested " + (request.getCategory() != null ? request.getCategory().name() : "document") + ": " + saved.getPurpose()
        );

        return toDto(saved);
    }

    @Override
    @Transactional
    public DocumentRequestDto sendDocumentToClient(SendDocumentToClientRequest request, MultipartFile file) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        DocumentRequestEntity targetRequest = null;
        UUID targetClientId;

        if (request.getRequestId() != null) {
            targetRequest = docRequestRepository.findByIdAndOrganizationId(request.getRequestId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", request.getRequestId()));

            if (targetRequest.getStatus() == RequestStatus.COMPLETED || targetRequest.getStatus() == RequestStatus.CANCELLED || targetRequest.getStatus() == RequestStatus.DECLINED) {
                throw new BadRequestException("Cannot fulfill a request that is already " + targetRequest.getStatus());
            }
            targetClientId = targetRequest.getClientId();
        } else {
            if (request.getClientId() == null) {
                throw new BadRequestException("Client ID is required when sending a document without an existing request");
            }
            targetClientId = request.getClientId();
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(targetClientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", targetClientId));

        // Resolve document
        DocumentEntity docEntity;
        if (file != null && !file.isEmpty()) {
            DocumentType docType = request.getDocumentType() != null ? request.getDocumentType() : DocumentType.OTHER;
            UploadDocumentRequest uploadReq = UploadDocumentRequest.builder()
                    .clientId(targetClientId)
                    .documentType(docType)
                    .financialYear(request.getFinancialYear())
                    .assessmentYear(request.getAssessmentYear())
                    .notes(request.getMessage() != null ? request.getMessage().trim() : "Delivered document to client")
                    .build();

            DocumentDto uploadedDoc = documentService.uploadDocument(file, uploadReq);
            docEntity = documentRepository.findById(uploadedDoc.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Document", "id", uploadedDoc.getId()));
        } else if (request.getExistingDocumentId() != null) {
            docEntity = documentRepository.findByIdAndOrganizationId(request.getExistingDocumentId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", "id", request.getExistingDocumentId()));

            // Ensure document belongs strictly to this client (prevent cross-client leakage)
            if (!Objects.equals(docEntity.getClientId(), targetClientId)) {
                throw new BadRequestException("Selected document does not belong to this client");
            }
            if (docEntity.getStatus() != DocumentStatus.ACTIVE) {
                throw new BadRequestException("Selected document is not active");
            }
            if (docEntity.getScanStatus() != DocumentScanStatus.CLEAN) {
                throw new BadRequestException("Cannot deliver a document that has not passed malware scanning or is infected (status: " + docEntity.getScanStatus() + ")");
            }
        } else {
            throw new BadRequestException("Either a file to upload or an existing clean document ID must be provided");
        }

        Instant now = Instant.now();
        if (targetRequest != null) {
            // Fulfill existing request
            targetRequest.setDeliveredDocumentId(docEntity.getId());
            targetRequest.setDeliveredAt(now);
            targetRequest.setStatus(RequestStatus.SENT);
            targetRequest.setCompletedAt(now);
            if (request.getCategory() != null) targetRequest.setCategory(request.getCategory());
            if (request.getFinancialYear() != null) targetRequest.setFinancialYear(request.getFinancialYear());
            if (request.getAssessmentYear() != null) targetRequest.setAssessmentYear(request.getAssessmentYear());
            if (request.getTaxPeriod() != null) targetRequest.setTaxPeriod(request.getTaxPeriod());
            if (request.getMessage() != null) targetRequest.setMessage(request.getMessage().trim());
        } else {
            // Proactive delivery
            String requestNumber = generateRequestNumber();
            String purpose = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : docEntity.getFileName();
            DocumentCategory category = request.getCategory() != null ? request.getCategory() : DocumentCategory.ACKNOWLEDGEMENT;

            targetRequest = DocumentRequestEntity.builder()
                    .clientId(targetClientId)
                    .requestNumber(requestNumber)
                    .purpose(purpose)
                    .exchangeType(ExchangeType.DOCUMENT_DELIVERY)
                    .direction(RequestDirection.PRACTITIONER_TO_CLIENT)
                    .category(category)
                    .financialYear(request.getFinancialYear())
                    .assessmentYear(request.getAssessmentYear())
                    .taxPeriod(request.getTaxPeriod())
                    .message(request.getMessage() != null ? request.getMessage().trim() : null)
                    .status(RequestStatus.SENT)
                    .requestedByUserId(currentUserId)
                    .deliveredDocumentId(docEntity.getId())
                    .deliveredAt(now)
                    .completedAt(now)
                    .complianceId(request.getComplianceId())
                    .taskId(request.getTaskId())
                    .gstFilingId(request.getGstFilingId())
                    .itrReturnId(request.getItrReturnId())
                    .tdsReturnId(request.getTdsReturnId())
                    .sentAt(now)
                    .build();
            targetRequest.setOrganizationId(organizationId);
        }

        DocumentRequestEntity saved = docRequestRepository.save(targetRequest);

        String practiceName = organizationRepository.findById(organizationId)
                .map(OrganizationEntity::getName)
                .orElse("Taxoryn Practice");

        // 1. In-App Notification to Client
        try {
            notificationService.notify(
                    organizationId,
                    null,
                    client.getId(),
                    NotificationType.DOCUMENT_REQUIRED,
                    "Document Ready: " + saved.getPurpose(),
                    "Your tax practitioner at " + practiceName + " has delivered document: " + docEntity.getFileName(),
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/portal?tab=documents",
                    "{\"requestId\":\"" + saved.getId() + "\",\"documentId\":\"" + docEntity.getId() + "\"}"
            );
        } catch (Exception e) {
            log.warn("Failed to notify client on document delivery: {}", e.getMessage());
        }

        // 2. Audit logs
        if (saved.getCategory() == DocumentCategory.ACKNOWLEDGEMENT || saved.getExchangeType() == ExchangeType.ACKNOWLEDGEMENT_REQUEST) {
            auditService.logEvent(
                    "ACKNOWLEDGEMENT_SENT",
                    "DOCUMENT_REQUEST",
                    saved.getId().toString(),
                    organizationId,
                    "Delivered acknowledgement '" + docEntity.getFileName() + "' for request " + saved.getRequestNumber() + " to client " + client.getDisplayName()
            );
        }
        auditService.logEvent(
                "DOCUMENT_SENT_TO_CLIENT",
                "DOCUMENT_REQUEST",
                saved.getId().toString(),
                organizationId,
                "Delivered document '" + docEntity.getFileName() + "' (" + saved.getPurpose() + ") to client " + client.getDisplayName()
        );

        return toDto(saved);
    }

    @Override
    @Transactional
    public DocumentRequestDto declineClientRequest(UUID requestId, DeclineDocumentRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentRequestEntity entity = docRequestRepository.findByIdAndOrganizationId(requestId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", requestId));

        if (entity.getDirection() != RequestDirection.CLIENT_TO_PRACTITIONER) {
            throw new BadRequestException("Only client-initiated requests can be declined");
        }
        if (entity.getStatus() == RequestStatus.COMPLETED || entity.getStatus() == RequestStatus.CANCELLED || entity.getStatus() == RequestStatus.DECLINED) {
            throw new BadRequestException("Request cannot be declined in its current state (" + entity.getStatus() + ")");
        }

        entity.setStatus(RequestStatus.DECLINED);
        entity.setDeclinedAt(Instant.now());
        entity.setDeclineReason(request.getDeclineReason().trim());
        DocumentRequestEntity saved = docRequestRepository.save(entity);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null);

        // Notify client
        if (client != null) {
            try {
                notificationService.notify(
                        organizationId,
                        null,
                        client.getId(),
                        NotificationType.DOCUMENT_REJECTED,
                        "Document Request Declined: " + saved.getPurpose(),
                        "Your request for \"" + saved.getPurpose() + "\" was declined: " + saved.getDeclineReason(),
                        Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                        "/portal?tab=documents",
                        "{\"requestId\":\"" + saved.getId() + "\"}"
                );
            } catch (Exception e) {
                log.warn("Failed to notify client of declined request: {}", e.getMessage());
            }
        }

        auditService.logEvent(
                "REQUEST_DECLINED",
                "DOCUMENT_REQUEST",
                saved.getId().toString(),
                organizationId,
                "Declined client request " + saved.getRequestNumber() + " (" + saved.getPurpose() + "). Reason: " + saved.getDeclineReason()
        );

        return toDto(saved);
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
    public List<DocumentRequestDto> getClientPortalDeliveredDocuments() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        return docRequestRepository.findDeliveredDocumentsForClient(clientId)
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

    @Override
    @Transactional
    public void recordClientDocumentViewed(UUID requestId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        DocumentRequestEntity request = docRequestRepository.findByIdAndClientId(requestId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", requestId));

        if (request.getStatus() == RequestStatus.SENT) {
            request.setStatus(RequestStatus.VIEWED);
            docRequestRepository.save(request);
        }

        auditService.logEvent(
                "DOCUMENT_VIEWED",
                "DOCUMENT_REQUEST",
                request.getId().toString(),
                request.getOrganizationId(),
                "Client viewed delivered document for request " + request.getRequestNumber()
        );
    }

    @Override
    @Transactional
    public void recordClientDocumentDownloaded(UUID requestId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        DocumentRequestEntity request = docRequestRepository.findByIdAndClientId(requestId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentRequest", "id", requestId));

        if (request.getStatus() == RequestStatus.SENT || request.getStatus() == RequestStatus.VIEWED) {
            request.setStatus(RequestStatus.DOWNLOADED);
            docRequestRepository.save(request);
        }

        auditService.logEvent(
                "DOCUMENT_DOWNLOADED",
                "DOCUMENT_REQUEST",
                request.getId().toString(),
                request.getOrganizationId(),
                "Client downloaded delivered document for request " + request.getRequestNumber()
        );
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

        // 5. Notify assigned practitioner
        try {
            ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId).orElse(null);
            String clientDisplayName = client != null ? client.getDisplayName() : "Client";
            UUID practitionerUserId = request.getRequestedByUserId();
            if (practitionerUserId == null && client != null && client.getAssignedEmployeeId() != null) {
                practitionerUserId = employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), organizationId)
                        .map(com.taxoryn.module.employee.entity.EmployeeEntity::getUserId)
                        .orElse(null);
            }

            if (practitionerUserId != null) {
                notificationService.notify(
                        organizationId,
                        practitionerUserId,
                        null,
                        NotificationType.DOCUMENT_UPLOADED,
                        "Document Uploaded: " + item.getTitle(),
                        clientDisplayName + " uploaded document for " + request.getPurpose() + " (" + item.getTitle() + ").",
                        Set.of(NotificationChannel.IN_APP),
                        "/documents",
                        "{\"requestId\":\"" + request.getId() + "\",\"itemId\":\"" + item.getId() + "\",\"documentId\":\"" + uploadedDoc.getId() + "\"}"
                );
            }
        } catch (Exception e) {
            log.warn("Failed to notify practitioner on document upload: {}", e.getMessage());
        }

        return toDto(request);
    }

    private String generateRequestNumber() {
        int year = Year.now().getValue();
        for (int i = 0; i < 10; i++) {
            String candidate = String.format("REQ-%d-%s", year, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            if (!docRequestRepository.existsByRequestNumber(candidate)) {
                return candidate;
            }
        }
        return "REQ-" + year + "-" + System.currentTimeMillis();
    }

    private DocumentRequestDto toDto(DocumentRequestEntity entity) {
        ClientEntity client = clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId()).orElse(null);
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

        String deliveredDocName = null;
        Long deliveredDocSize = null;
        String deliveredDocContentType = null;

        if (entity.getDeliveredDocumentId() != null) {
            DocumentEntity delDoc = docCache.computeIfAbsent(
                    entity.getDeliveredDocumentId(),
                    id -> documentRepository.findById(id).orElse(null)
            );
            if (delDoc != null) {
                deliveredDocName = delDoc.getFileName();
                deliveredDocSize = delDoc.getFileSize();
                deliveredDocContentType = delDoc.getContentType();
            }
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
                .taskId(entity.getTaskId())
                .complianceId(entity.getComplianceId())
                .exchangeType(entity.getExchangeType())
                .direction(entity.getDirection())
                .category(entity.getCategory())
                .taxPeriod(entity.getTaxPeriod())
                .deliveredDocumentId(entity.getDeliveredDocumentId())
                .deliveredDocumentName(deliveredDocName)
                .deliveredDocumentSize(deliveredDocSize)
                .deliveredDocumentContentType(deliveredDocContentType)
                .deliveredAt(entity.getDeliveredAt())
                .declinedAt(entity.getDeclinedAt())
                .declineReason(entity.getDeclineReason())
                .gstFilingId(entity.getGstFilingId())
                .itrReturnId(entity.getItrReturnId())
                .tdsReturnId(entity.getTdsReturnId())
                .noticeId(entity.getNoticeId())
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