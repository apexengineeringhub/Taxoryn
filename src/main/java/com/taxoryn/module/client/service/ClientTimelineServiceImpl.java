package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.ClientTimelineFilterRequest;
import com.taxoryn.module.client.dto.ClientTimelineItemDto;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.TimelineEventCategory;
import com.taxoryn.module.client.entity.TimelineEventSeverity;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRelationshipRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.portal.entity.ClientApprovalRequestEntity;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.ClientPortalMessageEntity;
import com.taxoryn.module.portal.entity.PortalMessageSenderType;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientTimelineServiceImpl implements ClientTimelineService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClientRepository clientRepository;
    private final AuditLogRepository auditLogRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final ClientContactRepository clientContactRepository;
    private final ClientBranchRepository clientBranchRepository;
    private final ClientRelationshipRepository clientRelationshipRepository;
    private final ClientNoteRepository clientNoteRepository;
    private final UserRepository userRepository;

    // Collaboration Repositories
    private final ClientPortalMessageRepository clientPortalMessageRepository;
    private final ClientApprovalRequestRepository clientApprovalRequestRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestItemRepository documentRequestItemRepository;
    private final ReminderRepository reminderRepository;
    private final ClientServiceWorkflowRepository clientServiceWorkflowRepository;
    private final ComplianceWorkflowRepository complianceWorkflowRepository;

    @Override
    public PagedResponse<ClientTimelineItemDto> getClientTimeline(UUID clientId, ClientTimelineFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }

        // Validate client exists and belongs to organization
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        final ClientTimelineFilterRequest actualFilter = filter != null ? filter : ClientTimelineFilterRequest.builder().build();

        List<ClientTimelineItemDto> allItems = collectAllTimelineItems(organizationId, clientId, actualFilter);

        return paginate(allItems, actualFilter.getPage(), actualFilter.getSize());
    }

    @Override
    public PagedResponse<ClientTimelineItemDto> getPortalClientTimeline(UUID clientId, ClientTimelineFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }

        UUID callerClientId = SecurityUtils.getCurrentClientId().orElse(null);
        if (callerClientId == null || !callerClientId.equals(clientId)) {
            throw new AccessDeniedException("Client portal users may only access their own timeline.");
        }

        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        ClientTimelineFilterRequest actualFilter = filter != null ? filter : ClientTimelineFilterRequest.builder().build();
        actualFilter.setClientVisibleOnly(true);

        List<ClientTimelineItemDto> allItems = collectAllTimelineItems(organizationId, clientId, actualFilter);

        // Sanitize for client portal — strip internal metadata, IP addresses, internal user IDs
        List<ClientTimelineItemDto> sanitized = allItems.stream()
                .map(this::sanitizeForPortal)
                .collect(Collectors.toList());

        return paginate(sanitized, actualFilter.getPage(), actualFilter.getSize());
    }

    @Override
    public List<ClientTimelineItemDto> getRecentTimeline(UUID organizationId, UUID clientId, int limit) {
        if (organizationId == null || clientId == null) {
            return Collections.emptyList();
        }

        ClientTimelineFilterRequest filter = ClientTimelineFilterRequest.builder()
                .page(0)
                .size(Math.max(1, Math.min(limit, 50)))
                .build();

        List<ClientTimelineItemDto> allItems = collectAllTimelineItems(organizationId, clientId, filter);
        int boundedLimit = Math.min(allItems.size(), filter.getSize());
        return allItems.subList(0, boundedLimit);
    }

    // =========================================================================
    // Core Multi-Source Aggregation
    // =========================================================================

    private List<ClientTimelineItemDto> collectAllTimelineItems(UUID organizationId, UUID clientId, ClientTimelineFilterRequest filter) {
        List<ClientTimelineItemDto> items = new ArrayList<>();

        // 1. Contextual Client Communication (Portal Messages)
        collectPortalMessages(organizationId, clientId, items);

        // 2. Client Approvals & Consent
        collectClientApprovals(organizationId, clientId, items);

        // 3. Document Requests & Item Collections
        collectDocumentRequests(organizationId, clientId, items);

        // 4. Follow-up Reminders & Nudges
        collectRemindersAndNudges(organizationId, clientId, items);

        // 5. Compliance Workflow Milestones
        collectComplianceWorkflows(organizationId, clientId, items);

        // 6. Practice Audit Logs & Notes (Only if clientVisibleOnly is not requested)
        if (!Boolean.TRUE.equals(filter.getClientVisibleOnly())) {
            collectAuditLogsAndNotes(organizationId, clientId, filter, items);
        }

        // Batch resolve actor names for any missing user names
        resolveActorNames(organizationId, items);

        // Filter and Sort deterministically
        return items.stream()
                .filter(item -> matchesFilter(item, filter))
                .sorted(getDeterministicComparator())
                .collect(Collectors.toList());
    }

    private void collectPortalMessages(UUID orgId, UUID clientId, List<ClientTimelineItemDto> items) {
        List<ClientPortalMessageEntity> messages = clientPortalMessageRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtAsc(orgId, clientId);

        for (ClientPortalMessageEntity msg : messages) {
            boolean isClient = msg.getSenderType() == PortalMessageSenderType.CLIENT;
            String eventType = isClient ? "PORTAL_MESSAGE_RECEIVED" : "PORTAL_MESSAGE_SENT";
            String title = isClient
                    ? "Message from Client" + (StringUtils.hasText(msg.getContextTitle()) ? " (" + msg.getContextTitle() + ")" : "")
                    : "Message sent by Practice" + (StringUtils.hasText(msg.getContextTitle()) ? " (" + msg.getContextTitle() + ")" : "");

            Map<String, Object> meta = new HashMap<>();
            meta.put("senderType", msg.getSenderType().name());
            meta.put("readByClient", msg.isReadByClient());
            meta.put("readByPractice", msg.isReadByPractice());
            if (msg.getContextType() != null) meta.put("contextType", msg.getContextType().name());
            if (msg.getContextTitle() != null) meta.put("contextTitle", msg.getContextTitle());

            items.add(ClientTimelineItemDto.builder()
                    .id(msg.getId())
                    .clientId(clientId)
                    .organizationId(orgId)
                    .eventType(eventType)
                    .eventCategory(TimelineEventCategory.COMMUNICATION)
                    .title(title)
                    .description(msg.getMessageBody())
                    .occurredAt(msg.getCreatedAt())
                    .actorId(msg.getSenderUserId())
                    .actorName(msg.getSenderName())
                    .sourceModule("PORTAL_MESSAGING")
                    .severity(TimelineEventSeverity.INFO)
                    .relatedEntityId(msg.getId().toString())
                    .relatedEntityType("PORTAL_MESSAGE")
                    .actionUrl("/portal/action-center")
                    .clientVisible(true)
                    .contextType(msg.getContextType() != null ? msg.getContextType().name() : "COMMUNICATION")
                    .contextId(msg.getContextId() != null ? msg.getContextId().toString() : null)
                    .metadata(meta)
                    .build());
        }
    }

    private void collectClientApprovals(UUID orgId, UUID clientId, List<ClientTimelineItemDto> items) {
        List<ClientApprovalRequestEntity> approvals = clientApprovalRequestRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);

        for (ClientApprovalRequestEntity appr : approvals) {
            String contextType = appr.getContextType() != null ? appr.getContextType().name() : "APPROVAL";
            String contextId = appr.getContextId() != null ? appr.getContextId().toString() : null;

            // Event 1: Request Created
            items.add(ClientTimelineItemDto.builder()
                    .id(appr.getId())
                    .clientId(clientId)
                    .organizationId(orgId)
                    .eventType("APPROVAL_REQUESTED")
                    .eventCategory(TimelineEventCategory.APPROVAL)
                    .title("Approval Requested: " + appr.getTitle())
                    .description(StringUtils.hasText(appr.getDescription()) ? appr.getDescription() : "Approval requested for client sign-off.")
                    .occurredAt(appr.getRequestedAt() != null ? appr.getRequestedAt() : appr.getCreatedAt())
                    .actorId(appr.getRequestedByUserId())
                    .actorName("Practitioner")
                    .sourceModule("CLIENT_APPROVAL")
                    .severity(TimelineEventSeverity.INFO)
                    .relatedEntityId(appr.getId().toString())
                    .relatedEntityType("CLIENT_APPROVAL")
                    .actionUrl("/portal/action-center")
                    .clientVisible(true)
                    .contextType(contextType)
                    .contextId(contextId)
                    .build());

            // Event 2: Decided (Approved or Rejected)
            if (appr.getStatus() == ClientApprovalStatus.APPROVED && appr.getRespondedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(appr.getId(), "DECIDED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("APPROVAL_ACCEPTED")
                        .eventCategory(TimelineEventCategory.APPROVAL)
                        .title("Approval Granted: " + appr.getTitle())
                        .description(StringUtils.hasText(appr.getDecisionNotes()) ? appr.getDecisionNotes() : "Client signed off on requested items.")
                        .occurredAt(appr.getRespondedAt())
                        .actorName(StringUtils.hasText(appr.getRespondedByName()) ? appr.getRespondedByName() : "Client Signatory")
                        .sourceModule("CLIENT_APPROVAL")
                        .severity(TimelineEventSeverity.SUCCESS)
                        .relatedEntityId(appr.getId().toString())
                        .relatedEntityType("CLIENT_APPROVAL")
                        .actionUrl("/portal/action-center")
                        .clientVisible(true)
                        .contextType(contextType)
                        .contextId(contextId)
                        .build());
            } else if (appr.getStatus() == ClientApprovalStatus.REJECTED && appr.getRespondedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(appr.getId(), "REJECTED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("APPROVAL_REJECTED")
                        .eventCategory(TimelineEventCategory.APPROVAL)
                        .title("Approval Declined: " + appr.getTitle())
                        .description(StringUtils.hasText(appr.getRejectionReason()) ? appr.getRejectionReason() : (StringUtils.hasText(appr.getDecisionNotes()) ? appr.getDecisionNotes() : "Client declined this approval request."))
                        .occurredAt(appr.getRespondedAt())
                        .actorName(StringUtils.hasText(appr.getRespondedByName()) ? appr.getRespondedByName() : "Client Signatory")
                        .sourceModule("CLIENT_APPROVAL")
                        .severity(TimelineEventSeverity.WARNING)
                        .relatedEntityId(appr.getId().toString())
                        .relatedEntityType("CLIENT_APPROVAL")
                        .actionUrl("/portal/action-center")
                        .clientVisible(true)
                        .contextType(contextType)
                        .contextId(contextId)
                        .build());
            }

            // Event 3: Revoked
            if (appr.getStatus() == ClientApprovalStatus.REVOKED && appr.getRevokedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(appr.getId(), "REVOKED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("APPROVAL_REVOKED")
                        .eventCategory(TimelineEventCategory.APPROVAL)
                        .title("Approval Revoked: " + appr.getTitle())
                        .description(StringUtils.hasText(appr.getRevocationReason()) ? appr.getRevocationReason() : "Approval was revoked by practitioner.")
                        .occurredAt(appr.getRevokedAt())
                        .actorId(appr.getRevokedByUserId())
                        .actorName("Practitioner")
                        .sourceModule("CLIENT_APPROVAL")
                        .severity(TimelineEventSeverity.WARNING)
                        .relatedEntityId(appr.getId().toString())
                        .relatedEntityType("CLIENT_APPROVAL")
                        .actionUrl("/portal/action-center")
                        .clientVisible(true)
                        .contextType(contextType)
                        .contextId(contextId)
                        .build());
            }
        }
    }

    private void collectDocumentRequests(UUID orgId, UUID clientId, List<ClientTimelineItemDto> items) {
        List<DocumentRequestEntity> docRequests = documentRequestRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);

        for (DocumentRequestEntity req : docRequests) {
            String docTitle = StringUtils.hasText(req.getPurpose()) ? req.getPurpose() : "Document Request";

            // Event 1: Request Created
            items.add(ClientTimelineItemDto.builder()
                    .id(req.getId())
                    .clientId(clientId)
                    .organizationId(orgId)
                    .eventType("DOCUMENT_REQUEST_CREATED")
                    .eventCategory(TimelineEventCategory.DOCUMENT)
                    .title("Document Request: " + docTitle)
                    .description(StringUtils.hasText(req.getMessage()) ? req.getMessage() : "Documents requested from client.")
                    .occurredAt(req.getCreatedAt())
                    .actorName("Practitioner")
                    .sourceModule("DOCREQUEST")
                    .severity(TimelineEventSeverity.INFO)
                    .relatedEntityId(req.getId().toString())
                    .relatedEntityType("DOCUMENT_REQUEST")
                    .actionUrl("/documents")
                    .clientVisible(true)
                    .contextType("DOCUMENT_REQUEST")
                    .contextId(req.getId().toString())
                    .build());

            // Event 2: Item Uploads / Approvals
            List<DocumentRequestItemEntity> reqItems = documentRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(req.getId());
            for (DocumentRequestItemEntity item : reqItems) {
                if (item.getUploadedAt() != null) {
                    items.add(ClientTimelineItemDto.builder()
                            .id(deriveDeterministicUuid(item.getId(), "UPLOADED"))
                            .clientId(clientId)
                            .organizationId(orgId)
                            .eventType("DOCUMENT_UPLOADED")
                            .eventCategory(TimelineEventCategory.DOCUMENT)
                            .title("Document Submitted: " + item.getTitle())
                            .description("Client submitted document file for " + docTitle)
                            .occurredAt(item.getUploadedAt())
                            .actorName("Client")
                            .sourceModule("DOCREQUEST")
                            .severity(TimelineEventSeverity.SUCCESS)
                            .relatedEntityId(item.getId().toString())
                            .relatedEntityType("DOCUMENT_REQUEST_ITEM")
                            .actionUrl("/documents")
                            .clientVisible(true)
                            .contextType("DOCUMENT_REQUEST")
                            .contextId(req.getId().toString())
                            .build());
                }

                if (item.getStatus() == ItemStatus.ACCEPTED && item.getReviewedAt() != null) {
                    items.add(ClientTimelineItemDto.builder()
                            .id(deriveDeterministicUuid(item.getId(), "ACCEPTED"))
                            .clientId(clientId)
                            .organizationId(orgId)
                            .eventType("DOCUMENT_APPROVED")
                            .eventCategory(TimelineEventCategory.DOCUMENT)
                            .title("Document Approved: " + item.getTitle())
                            .description("Document verified and approved.")
                            .occurredAt(item.getReviewedAt())
                            .actorName("Practitioner")
                            .sourceModule("DOCREQUEST")
                            .severity(TimelineEventSeverity.SUCCESS)
                            .relatedEntityId(item.getId().toString())
                            .relatedEntityType("DOCUMENT_REQUEST_ITEM")
                            .actionUrl("/documents")
                            .clientVisible(true)
                            .contextType("DOCUMENT_REQUEST")
                            .contextId(req.getId().toString())
                            .build());
                } else if (item.getStatus() == ItemStatus.REJECTED && item.getReviewedAt() != null) {
                    items.add(ClientTimelineItemDto.builder()
                            .id(deriveDeterministicUuid(item.getId(), "REJECTED"))
                            .clientId(clientId)
                            .organizationId(orgId)
                            .eventType("DOCUMENT_REJECTED")
                            .eventCategory(TimelineEventCategory.DOCUMENT)
                            .title("Document Rejected: " + item.getTitle())
                            .description(StringUtils.hasText(item.getRejectionReason()) ? item.getRejectionReason() : "Document rejected, resubmission requested.")
                            .occurredAt(item.getReviewedAt())
                            .actorName("Practitioner")
                            .sourceModule("DOCREQUEST")
                            .severity(TimelineEventSeverity.WARNING)
                            .relatedEntityId(item.getId().toString())
                            .relatedEntityType("DOCUMENT_REQUEST_ITEM")
                            .actionUrl("/documents")
                            .clientVisible(true)
                            .contextType("DOCUMENT_REQUEST")
                            .contextId(req.getId().toString())
                            .build());
                }
            }

            // Event 3: Request Completed
            if (req.getCompletedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(req.getId(), "COMPLETED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("DOCUMENT_REQUEST_COMPLETED")
                        .eventCategory(TimelineEventCategory.DOCUMENT)
                        .title("Document Collection Completed: " + docTitle)
                        .description("All requested documents have been gathered and verified.")
                        .occurredAt(req.getCompletedAt())
                        .actorName("System")
                        .sourceModule("DOCREQUEST")
                        .severity(TimelineEventSeverity.SUCCESS)
                        .relatedEntityId(req.getId().toString())
                        .relatedEntityType("DOCUMENT_REQUEST")
                        .actionUrl("/documents")
                        .clientVisible(true)
                        .contextType("DOCUMENT_REQUEST")
                        .contextId(req.getId().toString())
                        .build());
            }
        }
    }

    private void collectRemindersAndNudges(UUID orgId, UUID clientId, List<ClientTimelineItemDto> items) {
        List<ReminderEntity> reminders = reminderRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);

        for (ReminderEntity rem : reminders) {
            String contextType = rem.getReferenceType() != null ? rem.getReferenceType() : "REMINDER";
            String contextId = rem.getReferenceId();

            // Event 1: Follow-up Scheduled (Internal only)
            items.add(ClientTimelineItemDto.builder()
                    .id(rem.getId())
                    .clientId(clientId)
                    .organizationId(orgId)
                    .eventType("FOLLOW_UP_SCHEDULED")
                    .eventCategory(TimelineEventCategory.WORK)
                    .title("Follow-up Scheduled: " + rem.getTitle())
                    .description(rem.getDescription())
                    .occurredAt(rem.getCreatedAt())
                    .actorName("System")
                    .sourceModule("REMINDER")
                    .severity(TimelineEventSeverity.INFO)
                    .relatedEntityId(rem.getId().toString())
                    .relatedEntityType("REMINDER")
                    .actionUrl("/reminders")
                    .clientVisible(false) // internal practitioner follow-up
                    .contextType(contextType)
                    .contextId(contextId)
                    .build());

            // Event 2: Manual Nudge Sent (Client visible!)
            if (rem.getLastAttemptAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(rem.getId(), "NUDGE"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("FOLLOW_UP_NUDGE_SENT")
                        .eventCategory(TimelineEventCategory.COMMUNICATION)
                        .title("Follow-up Nudge: " + rem.getTitle())
                        .description(StringUtils.hasText(rem.getNotes()) ? rem.getNotes() : "Practitioner dispatched reminder nudge to client.")
                        .occurredAt(rem.getLastAttemptAt())
                        .actorName("Practitioner")
                        .sourceModule("REMINDER")
                        .severity(TimelineEventSeverity.INFO)
                        .relatedEntityId(rem.getId().toString())
                        .relatedEntityType("REMINDER")
                        .actionUrl("/reminders")
                        .clientVisible(true) // Clients receive the nudge
                        .contextType(contextType)
                        .contextId(contextId)
                        .build());
            }

            // Event 3: Resolved (Internal only)
            if (rem.getStatus() == ReminderStatus.COMPLETED && rem.getCompletedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(rem.getId(), "RESOLVED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("FOLLOW_UP_RESOLVED")
                        .eventCategory(TimelineEventCategory.WORK)
                        .title("Follow-up Resolved: " + rem.getTitle())
                        .description(StringUtils.hasText(rem.getNotes()) ? rem.getNotes() : "Action item completed.")
                        .occurredAt(rem.getCompletedAt())
                        .actorName("Practitioner")
                        .sourceModule("REMINDER")
                        .severity(TimelineEventSeverity.SUCCESS)
                        .relatedEntityId(rem.getId().toString())
                        .relatedEntityType("REMINDER")
                        .actionUrl("/reminders")
                        .clientVisible(false)
                        .contextType(contextType)
                        .contextId(contextId)
                        .build());
            }

            // Event 4: Cancelled (Internal only)
            if (rem.getStatus() == ReminderStatus.CANCELLED && rem.getCancelledAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(rem.getId(), "CANCELLED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("FOLLOW_UP_CANCELLED")
                        .eventCategory(TimelineEventCategory.WORK)
                        .title("Follow-up Cancelled: " + rem.getTitle())
                        .description(StringUtils.hasText(rem.getNotes()) ? rem.getNotes() : "Follow-up cancelled.")
                        .occurredAt(rem.getCancelledAt())
                        .actorName("System")
                        .sourceModule("REMINDER")
                        .severity(TimelineEventSeverity.INFO)
                        .relatedEntityId(rem.getId().toString())
                        .relatedEntityType("REMINDER")
                        .actionUrl("/reminders")
                        .clientVisible(false)
                        .contextType(contextType)
                        .contextId(contextId)
                        .build());
            }
        }
    }

    private void collectComplianceWorkflows(UUID orgId, UUID clientId, List<ClientTimelineItemDto> items) {
        // Client Service Workflows
        List<ClientServiceWorkflowEntity> cswList = clientServiceWorkflowRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);

        for (ClientServiceWorkflowEntity csw : cswList) {
            items.add(ClientTimelineItemDto.builder()
                    .id(csw.getId())
                    .clientId(clientId)
                    .organizationId(orgId)
                    .eventType("COMPLIANCE_WORKFLOW_STARTED")
                    .eventCategory(TimelineEventCategory.COMPLIANCE)
                    .title("Workflow Started: " + csw.getTitle())
                    .description("Compliance service workflow initiated.")
                    .occurredAt(csw.getCreatedAt())
                    .actorName("Practice")
                    .sourceModule("COMPLIANCE")
                    .severity(TimelineEventSeverity.INFO)
                    .relatedEntityId(csw.getId().toString())
                    .relatedEntityType("CLIENT_SERVICE_WORKFLOW")
                    .actionUrl("/compliance-work")
                    .clientVisible(true)
                    .contextType("COMPLIANCE_WORKFLOW")
                    .contextId(csw.getId().toString())
                    .build());

            if (csw.getCompletedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(csw.getId(), "COMPLETED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("COMPLIANCE_WORKFLOW_COMPLETED")
                        .eventCategory(TimelineEventCategory.COMPLIANCE)
                        .title("Workflow Completed: " + csw.getTitle())
                        .description("Service workflow successfully finished.")
                        .occurredAt(csw.getCompletedAt())
                        .actorName("Practice")
                        .sourceModule("COMPLIANCE")
                        .severity(TimelineEventSeverity.SUCCESS)
                        .relatedEntityId(csw.getId().toString())
                        .relatedEntityType("CLIENT_SERVICE_WORKFLOW")
                        .actionUrl("/compliance-work")
                        .clientVisible(true)
                        .contextType("COMPLIANCE_WORKFLOW")
                        .contextId(csw.getId().toString())
                        .build());
            }
        }

        // Compliance Workflows (Obligation-specific)
        List<ComplianceWorkflowEntity> cwList = complianceWorkflowRepository
                .findByOrganizationIdAndClientId(orgId, clientId);

        for (ComplianceWorkflowEntity cw : cwList) {
            if (cw.getWorkflowStatus() == ComplianceWorkflowStatus.COMPLETED && cw.getUpdatedAt() != null) {
                items.add(ClientTimelineItemDto.builder()
                        .id(deriveDeterministicUuid(cw.getId(), "COMPLIANCE_FULFILLED"))
                        .clientId(clientId)
                        .organizationId(orgId)
                        .eventType("COMPLIANCE_OBLIGATION_FULFILLED")
                        .eventCategory(TimelineEventCategory.COMPLIANCE)
                        .title("Compliance Obligation Fulfilled")
                        .description("Statutory compliance obligation successfully finalized.")
                        .occurredAt(cw.getUpdatedAt())
                        .actorName("Practice")
                        .sourceModule("COMPLIANCE")
                        .severity(TimelineEventSeverity.SUCCESS)
                        .relatedEntityId(cw.getId().toString())
                        .relatedEntityType("COMPLIANCE_WORKFLOW")
                        .actionUrl("/compliance-work")
                        .clientVisible(true)
                        .contextType("COMPLIANCE_WORKFLOW")
                        .contextId(cw.getId().toString())
                        .build());
            }
        }
    }

    private void collectAuditLogsAndNotes(UUID orgId, UUID clientId, ClientTimelineFilterRequest filter, List<ClientTimelineItemDto> items) {
        Set<String> allEntityIds = collectAllRelatedEntityIds(orgId, clientId);
        if (allEntityIds.isEmpty()) return;

        Specification<AuditLogEntity> spec = createSpecification(orgId, allEntityIds, filter);
        // Limit audit logs to most recent 200 to prevent unbounded memory usage
        PageRequest pageable = PageRequest.of(0, 200, Sort.by(Sort.Order.desc("createdAt")));
        Page<AuditLogEntity> logPage = auditLogRepository.findAll(spec, pageable);

        Map<UUID, String> userNames = resolveUserNames(orgId, logPage.getContent());

        for (AuditLogEntity log : logPage.getContent()) {
            items.add(toDto(log, clientId, userNames));
        }

        // Also add practice communication notes
        List<ClientNoteEntity> notes = clientNoteRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);
        for (ClientNoteEntity note : notes) {
            boolean isClientVisible = note.getVisibility() == ClientNoteEntity.Visibility.CLIENT_VISIBLE;
            items.add(ClientTimelineItemDto.builder()
                    .id(note.getId())
                    .clientId(clientId)
                    .organizationId(orgId)
                    .eventType("COMMUNICATION_NOTE")
                    .eventCategory(TimelineEventCategory.COMMUNICATION)
                    .title("Note: " + (StringUtils.hasText(note.getTitle()) ? note.getTitle() : "Communication Note"))
                    .description(note.getContent())
                    .occurredAt(note.getOccurredAt() != null ? note.getOccurredAt() : note.getCreatedAt())
                    .actorId(note.getAuthorId())
                    .actorName(note.getAuthorName())
                    .sourceModule("CLIENT_COMMUNICATION")
                    .severity(TimelineEventSeverity.INFO)
                    .relatedEntityId(note.getId().toString())
                    .relatedEntityType("CLIENT_NOTE")
                    .clientVisible(isClientVisible)
                    .contextType("COMMUNICATION")
                    .contextId(note.getId().toString())
                    .build());
        }
    }

    private Set<String> collectAllRelatedEntityIds(UUID organizationId, UUID clientId) {
        Set<String> ids = new HashSet<>();
        ids.add(clientId.toString());

        if (clientServiceRepository != null) {
            clientServiceRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId)
                    .forEach(s -> ids.add(s.getId().toString()));
        }
        if (clientContactRepository != null) {
            clientContactRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryContactDescCreatedAtAsc(organizationId, clientId)
                    .forEach(c -> ids.add(c.getId().toString()));
        }
        if (clientBranchRepository != null) {
            clientBranchRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryBranchDescCreatedAtAsc(organizationId, clientId)
                    .forEach(b -> ids.add(b.getId().toString()));
        }
        if (clientRelationshipRepository != null) {
            clientRelationshipRepository.findAllForClient(organizationId, clientId)
                    .forEach(r -> ids.add(r.getId().toString()));
        }

        return ids;
    }

    private Specification<AuditLogEntity> createSpecification(UUID organizationId, Set<String> allEntityIds, ClientTimelineFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (allEntityIds != null && !allEntityIds.isEmpty()) {
                predicates.add(root.get("entityId").in(allEntityIds));
            } else {
                predicates.add(cb.equal(root.get("entityId"), ""));
            }

            if (StringUtils.hasText(filter.getEventType())) {
                predicates.add(cb.equal(root.get("action"), filter.getEventType().trim()));
            }

            if (filter.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFrom()));
            }

            if (filter.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =========================================================================
    // Filtering, Sorting, and Pagination
    // =========================================================================

    private boolean matchesFilter(ClientTimelineItemDto item, ClientTimelineFilterRequest filter) {
        if (filter == null) return true;

        if (Boolean.TRUE.equals(filter.getClientVisibleOnly()) && !item.isClientVisible()) {
            return false;
        }

        if (filter.getCategory() != null && item.getEventCategory() != filter.getCategory()) {
            return false;
        }

        if (StringUtils.hasText(filter.getEventType()) && (item.getEventType() == null || !item.getEventType().equalsIgnoreCase(filter.getEventType().trim()))) {
            return false;
        }

        if (filter.getFrom() != null && item.getOccurredAt() != null && item.getOccurredAt().isBefore(filter.getFrom())) {
            return false;
        }

        if (filter.getTo() != null && item.getOccurredAt() != null && item.getOccurredAt().isAfter(filter.getTo())) {
            return false;
        }

        if (StringUtils.hasText(filter.getContextType()) && (item.getContextType() == null || !item.getContextType().equalsIgnoreCase(filter.getContextType().trim()))) {
            return false;
        }

        if (StringUtils.hasText(filter.getContextId()) && (item.getContextId() == null || !item.getContextId().equalsIgnoreCase(filter.getContextId().trim()))) {
            return false;
        }

        return true;
    }

    private Comparator<ClientTimelineItemDto> getDeterministicComparator() {
        return Comparator.comparing(ClientTimelineItemDto::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getId() != null ? item.getId().toString() : "", Comparator.reverseOrder())
                .thenComparing(item -> item.getEventType() != null ? item.getEventType() : "", Comparator.naturalOrder());
    }

    private PagedResponse<ClientTimelineItemDto> paginate(List<ClientTimelineItemDto> items, Integer page, Integer size) {
        int pageNum = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        int fromIndex = Math.min(pageNum * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<ClientTimelineItemDto> pageContent = items.subList(fromIndex, toIndex);

        return PagedResponse.<ClientTimelineItemDto>builder()
                .content(pageContent)
                .pageNumber(pageNum)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isFirst(pageNum == 0)
                .isLast(pageNum >= totalPages - 1 || totalPages == 0)
                .hasNext(pageNum < totalPages - 1)
                .hasPrevious(pageNum > 0 && totalPages > 0)
                .build();
    }

    private ClientTimelineItemDto sanitizeForPortal(ClientTimelineItemDto item) {
        Map<String, Object> sanitizedMeta = new HashMap<>();
        if (item.getMetadata() != null) {
            // Only preserve safe metadata for clients
            for (Map.Entry<String, Object> entry : item.getMetadata().entrySet()) {
                String key = entry.getKey();
                if (!"ipAddress".equalsIgnoreCase(key) &&
                        !"requestId".equalsIgnoreCase(key) &&
                        !"oldValue".equalsIgnoreCase(key) &&
                        !"newValue".equalsIgnoreCase(key)) {
                    sanitizedMeta.put(key, entry.getValue());
                }
            }
        }

        return ClientTimelineItemDto.builder()
                .id(item.getId())
                .clientId(item.getClientId())
                .organizationId(item.getOrganizationId())
                .eventType(item.getEventType())
                .eventCategory(item.getEventCategory())
                .title(item.getTitle())
                .description(item.getDescription())
                .occurredAt(item.getOccurredAt())
                .actorId(null) // do not leak internal user UUIDs to client portal
                .actorName(item.getActorName())
                .sourceModule(item.getSourceModule())
                .severity(item.getSeverity())
                .relatedEntityId(item.getRelatedEntityId())
                .relatedEntityType(item.getRelatedEntityType())
                .actionUrl(item.getActionUrl())
                .clientVisible(true)
                .contextType(item.getContextType())
                .contextId(item.getContextId())
                .metadata(sanitizedMeta)
                .build();
    }

    private void resolveActorNames(UUID organizationId, List<ClientTimelineItemDto> items) {
        Set<UUID> userIds = items.stream()
                .filter(i -> !StringUtils.hasText(i.getActorName()) || "Practitioner".equalsIgnoreCase(i.getActorName()))
                .map(ClientTimelineItemDto::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) return;

        Map<UUID, String> userNames = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> {
            String name = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail();
            userNames.put(user.getId(), name);
        });

        for (ClientTimelineItemDto item : items) {
            if (item.getActorId() != null && userNames.containsKey(item.getActorId())) {
                item.setActorName(userNames.get(item.getActorId()));
            }
        }
    }

    private UUID deriveDeterministicUuid(UUID sourceId, String suffix) {
        return UUID.nameUUIDFromBytes((sourceId.toString() + "-" + suffix).getBytes(StandardCharsets.UTF_8));
    }

    private Map<UUID, String> resolveUserNames(UUID organizationId, List<AuditLogEntity> logs) {
        Set<UUID> userIds = logs.stream()
                .map(AuditLogEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, String> map = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> {
            String name = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail();
            map.put(user.getId(), name);
        });
        return map;
    }

    private ClientTimelineItemDto toDto(AuditLogEntity log, UUID clientId, Map<UUID, String> userNames) {
        TimelineEventCategory category = resolveCategory(log.getAction(), log.getEntityType());
        TimelineEventSeverity severity = resolveSeverity(log.getAction(), log.getNewValue());
        String actorName = log.getUserId() != null ? userNames.getOrDefault(log.getUserId(), "Practice User") : "System";

        Map<String, Object> meta = new HashMap<>();
        if (StringUtils.hasText(log.getIpAddress())) meta.put("ipAddress", log.getIpAddress());
        if (StringUtils.hasText(log.getRequestId())) meta.put("requestId", log.getRequestId());
        if (StringUtils.hasText(log.getOldValue())) meta.put("oldValue", log.getOldValue());
        if (StringUtils.hasText(log.getNewValue())) meta.put("newValue", log.getNewValue());

        return ClientTimelineItemDto.builder()
                .id(log.getId())
                .clientId(clientId)
                .organizationId(log.getOrganizationId())
                .eventType(log.getAction())
                .eventCategory(category)
                .title(formatTitle(log.getAction()))
                .description(formatDescription(log))
                .occurredAt(log.getCreatedAt())
                .actorId(log.getUserId())
                .actorName(actorName)
                .sourceModule(resolveSourceModule(category))
                .severity(severity)
                .relatedEntityId(log.getEntityId())
                .relatedEntityType(log.getEntityType())
                .clientVisible(false) // internal audit logs are not client visible by default
                .metadata(meta)
                .build();
    }

    public static TimelineEventCategory resolveCategory(String action, String entityType) {
        if (action == null && entityType == null) return TimelineEventCategory.SYSTEM;
        String act = action != null ? action.toUpperCase() : "";
        String ent = entityType != null ? entityType.toUpperCase() : "";

        if (act.contains("APPROVAL") || ent.contains("APPROVAL")) return TimelineEventCategory.APPROVAL;
        if (act.contains("PROFILE") || ent.contains("PROFILE")) return TimelineEventCategory.PROFILE;
        if (act.contains("SERVICE") || ent.contains("SERVICE")) return TimelineEventCategory.SERVICE;
        if (act.contains("CONTACT") || ent.contains("CONTACT")) return TimelineEventCategory.CONTACT;
        if (act.contains("BRANCH") || ent.contains("BRANCH") || act.contains("LOCATION") || ent.contains("LOCATION")) return TimelineEventCategory.BRANCH;
        if (act.contains("RELATIONSHIP") || ent.contains("RELATIONSHIP") || act.contains("GROUP")) return TimelineEventCategory.RELATIONSHIP;
        if (act.contains("ENGAGEMENT") || ent.contains("ENGAGEMENT")) return TimelineEventCategory.ENGAGEMENT;
        if (act.contains("NOTE") || act.contains("COMMUNICATION") || ent.contains("COMMUNICATION") || ent.contains("NOTE") || act.contains("MESSAGE")) return TimelineEventCategory.COMMUNICATION;
        if (act.contains("DOC") || ent.contains("DOC")) return TimelineEventCategory.DOCUMENT;
        if (act.contains("BILL") || act.contains("INVOICE") || ent.contains("INVOICE") || ent.contains("BILLING")) return TimelineEventCategory.BILLING;
        if (act.contains("PAYMENT") || ent.contains("PAYMENT")) return TimelineEventCategory.PAYMENT;
        if (act.contains("NOTICE") || act.contains("FILING") || act.contains("RETURN") || act.contains("COMPLIANCE") || act.contains("GST") || act.contains("ITR") || act.contains("TDS")) return TimelineEventCategory.COMPLIANCE;
        if (act.contains("CLIENT") || ent.contains("CLIENT")) return TimelineEventCategory.CLIENT;
        return TimelineEventCategory.SYSTEM;
    }

    public static TimelineEventSeverity resolveSeverity(String action, String newValue) {
        if (action == null) return TimelineEventSeverity.INFO;
        String act = action.toUpperCase();
        if (act.contains("ERROR") || act.contains("FAILED") || act.contains("SUSPEND") || act.contains("TERMINAT") || act.contains("CANCEL") || act.contains("DELETE") || act.contains("REMOVED") || act.contains("REJECT")) {
            return TimelineEventSeverity.WARNING;
        }
        if (act.contains("CREATE") || act.contains("ACTIVAT") || act.contains("APPROV") || act.contains("ACCEPTED") || act.contains("FIL") || act.contains("PAID") || act.contains("RESUM") || act.contains("ASSIGNED") || act.contains("PRIMARY") || act.contains("COMPLET")) {
            return TimelineEventSeverity.SUCCESS;
        }
        return TimelineEventSeverity.INFO;
    }

    public static String resolveSourceModule(TimelineEventCategory category) {
        if (category == null) return "CLIENT";
        switch (category) {
            case APPROVAL:
                return "CLIENT_APPROVAL";
            case SERVICE:
            case ENGAGEMENT:
                return "CLIENT_SERVICE";
            case CONTACT:
            case BRANCH:
            case RELATIONSHIP:
            case PROFILE:
            case CLIENT:
                return "CLIENT";
            case COMMUNICATION:
                return "CLIENT_COMMUNICATION";
            case DOCUMENT:
                return "DOCUMENT";
            case BILLING:
            case PAYMENT:
                return "BILLING";
            case COMPLIANCE:
            case GOVERNMENT:
                return "COMPLIANCE";
            default:
                return "SYSTEM";
        }
    }

    public static String formatTitle(String action) {
        if (!StringUtils.hasText(action)) return "Activity Event";
        String[] parts = action.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    public static String formatDescription(AuditLogEntity log) {
        if (StringUtils.hasText(log.getNewValue())) {
            return log.getNewValue();
        }
        if (StringUtils.hasText(log.getOldValue())) {
            return "Previous value: " + log.getOldValue();
        }
        return "Action " + formatTitle(log.getAction()) + " executed on " + (log.getEntityType() != null ? log.getEntityType().toLowerCase() : "client record");
    }
}
