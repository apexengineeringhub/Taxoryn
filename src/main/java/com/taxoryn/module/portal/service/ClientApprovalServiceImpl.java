package com.taxoryn.module.portal.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.portal.dto.ClientApprovalDto;
import com.taxoryn.module.portal.dto.CreateClientApprovalRequest;
import com.taxoryn.module.portal.dto.DecideClientApprovalRequest;
import com.taxoryn.module.portal.entity.ClientApprovalDecision;
import com.taxoryn.module.portal.entity.ClientApprovalRequestEntity;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.MessageContextType;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientApprovalServiceImpl implements ClientApprovalService {

    private final ClientApprovalRequestRepository clientApprovalRequestRepository;
    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Autowired(required = false)
    private DocumentRequestRepository multiItemDocRequestRepository;

    @Autowired(required = false)
    private DocumentRequestItemRepository multiItemDocRequestItemRepository;

    @Autowired(required = false)
    private ComplianceWorkflowRepository complianceWorkflowRepository;

    @Autowired(required = false)
    private TaxNoticeRepository taxNoticeRepository;

    @Override
    @Transactional
    public ClientApprovalDto createApprovalRequest(UUID clientId, CreateClientApprovalRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validatePracticeClientAccess(clientId);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        // Validate document reference if provided
        String documentName = null;
        if (request.getDocumentId() != null) {
            DocumentEntity doc = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Document", "id", request.getDocumentId()));
            if (!organizationId.equals(doc.getOrganizationId()) || (doc.getClientId() != null && !clientId.equals(doc.getClientId()))) {
                throw new AccessDeniedException("Referenced document does not belong to this client and organization.");
            }
            documentName = doc.getFileName();
        }

        // Validate context item ownership
        if (request.getContextType() != null && request.getContextId() != null) {
            validateContextOwnership(organizationId, clientId, request.getContextType(), request.getContextId());
        }

        // Check for duplicate pending requests for the same context
        if (request.getContextId() != null) {
            var existing = clientApprovalRequestRepository
                    .findFirstByOrganizationIdAndClientIdAndApprovalTypeAndContextTypeAndContextIdAndStatus(
                            organizationId, clientId, request.getApprovalType(),
                            request.getContextType(), request.getContextId(), ClientApprovalStatus.PENDING);
            if (existing.isPresent()) {
                throw new DuplicateResourceException(
                        "An active pending approval request already exists for this context item.");
            }
        }

        UUID requestedBy = SecurityUtils.getCurrentUserId();
        String requestedByName = userRepository.findById(requestedBy)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .filter(s -> !s.isBlank())
                .orElse(SecurityUtils.getCurrentUserEmail());

        String contentVersion = request.getContentVersion() != null && !request.getContentVersion().isBlank()
                ? request.getContentVersion().trim()
                : "v1.0";

        ClientApprovalRequestEntity entity = ClientApprovalRequestEntity.builder()
                .clientId(clientId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .approvalType(request.getApprovalType())
                .status(ClientApprovalStatus.PENDING)
                .contextType(request.getContextType() != null ? request.getContextType() : MessageContextType.GENERAL)
                .contextId(request.getContextId())
                .contextTitle(request.getContextTitle())
                .documentId(request.getDocumentId())
                .contentVersion(contentVersion)
                .contentHash(request.getContentHash())
                .requestedByUserId(requestedBy)
                .requestedByName(requestedByName)
                .requestedAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .build();

        ClientApprovalRequestEntity saved = clientApprovalRequestRepository.save(entity);

        auditService.logEvent(
                "CLIENT_APPROVAL_REQUEST_CREATED",
                "CLIENT_APPROVAL",
                saved.getId().toString(),
                organizationId,
                "Created approval request '" + saved.getTitle() + "' for client " + client.getDisplayName()
        );

        return toDto(saved, documentName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientApprovalDto> getApprovalRequestsForClient(
            UUID clientId,
            ClientApprovalStatus status,
            MessageContextType contextType,
            UUID contextId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validatePracticeClientAccess(clientId);

        List<ClientApprovalRequestEntity> entities;
        if (contextType != null && contextId != null) {
            entities = clientApprovalRequestRepository
                    .findAllByOrganizationIdAndClientIdAndContextTypeAndContextIdOrderByCreatedAtDesc(
                            organizationId, clientId, contextType, contextId);
        } else if (status != null) {
            entities = clientApprovalRequestRepository
                    .findAllByOrganizationIdAndClientIdAndStatusOrderByCreatedAtDesc(
                            organizationId, clientId, status);
        } else {
            entities = clientApprovalRequestRepository
                    .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        }

        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientApprovalDto> getClientOwnApprovalRequests(ClientApprovalStatus status) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID clientId = SecurityUtils.requireCurrentClientId();

        List<ClientApprovalRequestEntity> entities;
        if (status != null) {
            entities = clientApprovalRequestRepository
                    .findAllByOrganizationIdAndClientIdAndStatusOrderByCreatedAtDesc(organizationId, clientId, status);
        } else {
            entities = clientApprovalRequestRepository
                    .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        }

        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientApprovalDto getApprovalRequestById(UUID approvalId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientApprovalRequestEntity entity = clientApprovalRequestRepository
                .findByIdAndOrganizationId(approvalId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientApprovalRequest", "id", approvalId));

        if (SecurityUtils.isClientPortalUser()) {
            UUID currentClientId = SecurityUtils.requireCurrentClientId();
            if (!entity.getClientId().equals(currentClientId)) {
                throw new AccessDeniedException("Access denied: You can only view approvals for your own client account.");
            }
        } else {
            validatePracticeClientAccess(entity.getClientId());
        }

        return toDto(entity);
    }

    @Override
    @Transactional
    public ClientApprovalDto decideApprovalRequest(UUID approvalId, DecideClientApprovalRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID clientId = SecurityUtils.requireCurrentClientId();

        ClientApprovalRequestEntity entity = clientApprovalRequestRepository
                .findByIdAndOrganizationIdAndClientId(approvalId, organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientApprovalRequest", "id", approvalId));

        if (entity.getStatus() != ClientApprovalStatus.PENDING) {
            throw new BadRequestException(
                    "This approval request has already been decided with status: " + entity.getStatus());
        }

        if (entity.isExpired()) {
            entity.setStatus(ClientApprovalStatus.EXPIRED);
            clientApprovalRequestRepository.save(entity);
            throw new BadRequestException("This approval request has expired and can no longer be decided.");
        }

        // Stale content version check
        if (request.getExpectedContentVersion() != null && !request.getExpectedContentVersion().isBlank()
                && entity.getContentVersion() != null) {
            if (!entity.getContentVersion().equalsIgnoreCase(request.getExpectedContentVersion().trim())) {
                throw new BadRequestException(
                        "CONTENT_VERSION_MISMATCH: The terms or document have been revised. Current version: " + entity.getContentVersion());
            }
        }

        UUID clientUserId = SecurityUtils.getCurrentUserId();
        String clientUserEmail = SecurityUtils.getCurrentUserEmail();
        String clientUserName = userRepository.findById(clientUserId)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .filter(s -> !s.isBlank())
                .orElse(clientUserEmail);

        if (request.getDecision() == ClientApprovalDecision.APPROVE) {
            entity.approve(clientUserId, clientUserName, clientUserEmail, request.getNotes());
            auditService.logEvent(
                    "CLIENT_APPROVAL_APPROVED",
                    "CLIENT_APPROVAL",
                    entity.getId().toString(),
                    organizationId,
                    "Client user " + clientUserName + " approved request: " + entity.getTitle()
            );
        } else if (request.getDecision() == ClientApprovalDecision.REJECT) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Rejection reason is required when declining an approval request.");
            }
            entity.reject(clientUserId, clientUserName, clientUserEmail, request.getRejectionReason().trim());
            auditService.logEvent(
                    "CLIENT_APPROVAL_REJECTED",
                    "CLIENT_APPROVAL",
                    entity.getId().toString(),
                    organizationId,
                    "Client user " + clientUserName + " rejected request: " + entity.getTitle() + ". Reason: " + request.getRejectionReason()
            );
        } else {
            throw new BadRequestException("Unsupported approval decision: " + request.getDecision());
        }

        ClientApprovalRequestEntity saved = clientApprovalRequestRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ClientApprovalDto revokeApprovalRequest(UUID clientId, UUID approvalId, String reason) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validatePracticeClientAccess(clientId);

        ClientApprovalRequestEntity entity = clientApprovalRequestRepository
                .findByIdAndOrganizationIdAndClientId(approvalId, organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientApprovalRequest", "id", approvalId));

        if (entity.getStatus() != ClientApprovalStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING approval requests can be revoked/cancelled. Current status: " + entity.getStatus());
        }

        UUID practiceUserId = SecurityUtils.getCurrentUserId();
        String practiceUserName = userRepository.findById(practiceUserId)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .filter(s -> !s.isBlank())
                .orElse(SecurityUtils.getCurrentUserEmail());

        entity.revoke(practiceUserId, practiceUserName, reason != null ? reason.trim() : "Cancelled by practice");
        ClientApprovalRequestEntity saved = clientApprovalRequestRepository.save(entity);

        auditService.logEvent(
                "CLIENT_APPROVAL_REVOKED",
                "CLIENT_APPROVAL",
                saved.getId().toString(),
                organizationId,
                "Practice user " + practiceUserName + " revoked approval request: " + saved.getTitle()
        );

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingApprovalsCount(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return clientApprovalRequestRepository.countByOrganizationIdAndClientIdAndStatus(
                organizationId, clientId, ClientApprovalStatus.PENDING);
    }

    private void validateContextOwnership(UUID organizationId, UUID clientId, MessageContextType contextType, UUID contextId) {
        if (contextType == null || contextId == null) return;
        switch (contextType) {
            case DOCUMENT_REQUEST -> {
                if (multiItemDocRequestRepository != null) {
                    var req = multiItemDocRequestRepository.findByIdAndOrganizationId(contextId, organizationId);
                    if (req.isEmpty() || !clientId.equals(req.get().getClientId())) {
                        throw new AccessDeniedException("Referenced document request does not belong to this client.");
                    }
                }
            }
            case DOCUMENT_ITEM -> {
                if (multiItemDocRequestItemRepository != null) {
                    var item = multiItemDocRequestItemRepository.findById(contextId);
                    if (item.isEmpty() || !organizationId.equals(item.get().getOrganizationId()) || !clientId.equals(item.get().getClientId())) {
                        throw new AccessDeniedException("Referenced document item does not belong to this client.");
                    }
                }
            }
            case COMPLIANCE_WORKFLOW -> {
                if (complianceWorkflowRepository != null) {
                    var wf = complianceWorkflowRepository.findByIdAndOrganizationId(contextId, organizationId);
                    if (wf.isEmpty() || !clientId.equals(wf.get().getClientId())) {
                        throw new AccessDeniedException("Referenced compliance workflow does not belong to this client.");
                    }
                }
            }
            case NOTICE -> {
                if (taxNoticeRepository != null) {
                    var not = taxNoticeRepository.findByIdAndOrganizationId(contextId, organizationId);
                    if (not.isEmpty() || !clientId.equals(not.get().getClientId())) {
                        throw new AccessDeniedException("Referenced notice does not belong to this client.");
                    }
                }
            }
            default -> {
                // GENERAL or ENGAGEMENT - no entity-level foreign key restriction
            }
        }
    }

    private void validatePracticeClientAccess(UUID clientId) {
        if (clientId == null) return;
        if (securityScopeEvaluator != null) {
            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            if (scope != null && !scope.isFirmAdmin()) {
                Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (accessibleClientIds == null || !accessibleClientIds.contains(clientId)) {
                    throw new AccessDeniedException("Access denied: You do not have permission to access approvals for this client.");
                }
            }
        }
    }

    private ClientApprovalDto toDto(ClientApprovalRequestEntity entity) {
        String docName = null;
        if (entity.getDocumentId() != null) {
            docName = documentRepository.findById(entity.getDocumentId())
                    .map(DocumentEntity::getFileName)
                    .orElse("Attached Document");
        }
        return toDto(entity, docName);
    }

    private ClientApprovalDto toDto(ClientApprovalRequestEntity entity, String documentName) {
        String downloadUrl = entity.getDocumentId() != null
                ? "/api/v1/portal/documents/" + entity.getDocumentId() + "/download"
                : null;

        return ClientApprovalDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .approvalType(entity.getApprovalType())
                .status(entity.getStatus())
                .contextType(entity.getContextType())
                .contextId(entity.getContextId())
                .contextTitle(entity.getContextTitle())
                .documentId(entity.getDocumentId())
                .documentName(documentName)
                .documentDownloadUrl(downloadUrl)
                .contentVersion(entity.getContentVersion())
                .contentHash(entity.getContentHash())
                .requestedByUserId(entity.getRequestedByUserId())
                .requestedByName(entity.getRequestedByName())
                .requestedAt(entity.getRequestedAt())
                .expiresAt(entity.getExpiresAt())
                .isExpired(entity.isExpired())
                .respondedByUserId(entity.getRespondedByUserId())
                .respondedByName(entity.getRespondedByName())
                .respondedByEmail(entity.getRespondedByEmail())
                .respondedAt(entity.getRespondedAt())
                .decisionNotes(entity.getDecisionNotes())
                .rejectionReason(entity.getRejectionReason())
                .revokedByUserId(entity.getRevokedByUserId())
                .revokedByName(entity.getRevokedByName())
                .revokedAt(entity.getRevokedAt())
                .revocationReason(entity.getRevocationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
