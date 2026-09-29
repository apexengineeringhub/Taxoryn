package com.taxoryn.module.gmail.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.dto.GmailConversationDto;
import com.taxoryn.module.gmail.dto.GmailConversationFilterRequest;
import com.taxoryn.module.gmail.dto.GmailConversationUpdateDto;
import com.taxoryn.module.gmail.dto.GmailMessageViewDto;
import com.taxoryn.module.gmail.dto.GmailReplyRequest;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailConversationServiceImpl implements GmailConversationService {

    private final GmailConversationRepository conversationRepository;
    private final GmailAccountRepository accountRepository;
    private final GmailOAuthService gmailOAuthService;
    private final GmailApiClient apiClient;
    private final TokenEncryptionService encryptionService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PracticeSecurityScopeEvaluator scopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GmailConversationDto> getConversations(GmailConversationFilterRequest filterRequest, PracticeSecurityScope scope) {
        Specification<GmailConversationEntity> spec = buildSpecification(filterRequest, scope);

        String sortBy = (filterRequest != null && StringUtils.hasText(filterRequest.getSortBy())) ? filterRequest.getSortBy() : "lastMessageAt";
        Sort.Direction direction = (filterRequest != null && "ASC".equalsIgnoreCase(filterRequest.getSortDirection())) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int page = filterRequest != null ? Math.max(0, filterRequest.getPage()) : 0;
        int size = filterRequest != null ? Math.max(1, filterRequest.getSize()) : 20;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<GmailConversationEntity> pagedResult = conversationRepository.findAll(spec, pageRequest);

        return PagedResponse.of(pagedResult, this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public GmailConversationDto getConversation(UUID conversationId, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        return mapToDto(conv);
    }

    @Override
    @Transactional
    public List<GmailMessageViewDto> getConversationMessages(UUID conversationId, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        // Mark as read if currently unread
        if (Boolean.TRUE.equals(conv.getIsUnread())) {
            conv.setIsUnread(false);
            conversationRepository.save(conv);
        }

        if (conv.getGmailAccountId() == null || !StringUtils.hasText(conv.getThreadId())) {
            return buildFallbackMessageList(conv);
        }

        try {
            GmailAccountEntity account = gmailOAuthService.getValidAuthenticatedAccount(conv.getOrganizationId(), conv.getGmailAccountId());
            String accessToken = encryptionService.decrypt(account.getEncryptedAccessToken());

            GmailThreadModels.ThreadDetail threadDetail = apiClient.getThreadFull(accessToken, conv.getThreadId());
            if (threadDetail == null || threadDetail.getMessages() == null || threadDetail.getMessages().isEmpty()) {
                return buildFallbackMessageList(conv);
            }

            List<GmailMessageViewDto> messageViews = new ArrayList<>();
            for (GmailThreadModels.MessageMetadata msg : threadDetail.getMessages()) {
                messageViews.add(mapToMessageView(msg, conv.getThreadId()));
            }

            return messageViews;
        } catch (Exception ex) {
            log.warn("Failed to fetch live Gmail thread messages for conversationId={}: {}. Falling back to metadata.", conversationId, ex.getMessage());
            return buildFallbackMessageList(conv);
        }
    }

    @Override
    @Transactional
    public GmailConversationDto sendReply(UUID conversationId, GmailReplyRequest request, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        UUID accountIdToUse = request.getReplyAccountId() != null ? request.getReplyAccountId() : conv.getGmailAccountId();
        if (accountIdToUse == null) {
            throw new BadRequestException("No connected Gmail mailbox associated with this conversation.");
        }

        GmailAccountEntity account = gmailOAuthService.getValidAuthenticatedAccount(conv.getOrganizationId(), accountIdToUse);
        String accessToken = encryptionService.decrypt(account.getEncryptedAccessToken());

        String fromEmail = account.getEmailAddress();
        String toEmail = StringUtils.hasText(request.getTo()) ? request.getTo().trim() : conv.getSenderEmail();
        String subject = StringUtils.hasText(request.getSubject())
                ? request.getSubject().trim()
                : ("Re: " + (conv.getSubject() != null ? conv.getSubject() : "Enquiry"));

        String rawMime = buildMimeMessage(fromEmail, toEmail, subject, request.getBody(), request.getInReplyToMessageId());
        String base64UrlEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(rawMime.getBytes(StandardCharsets.UTF_8));

        apiClient.sendMessage(accessToken, base64UrlEncoded, conv.getThreadId());

        // Update conversation operational metadata
        GmailConversationStatus oldStatus = conv.getStatus();
        conv.setStatus(GmailConversationStatus.REPLIED);
        conv.setLastMessageAt(Instant.now());
        if (conv.getFirstResponseAt() == null) {
            conv.setFirstResponseAt(Instant.now());
        }
        if (conv.getResolvedAt() == null) {
            conv.setResolvedAt(Instant.now());
        }
        conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 1) + 1);
        conv.setIsUnread(false);

        GmailConversationEntity saved = conversationRepository.save(conv);

        auditService.logEvent(
                scope.getOrganizationId(),
                scope.getUserId(),
                "GMAIL_REPLY_SENT",
                "GMAIL_CONVERSATION",
                conversationId.toString(),
                oldStatus != null ? oldStatus.name() : null,
                "Reply sent from " + fromEmail + " to " + toEmail
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GmailConversationDto updateConversation(UUID conversationId, GmailConversationUpdateDto request, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        if (request.getStatus() != null) {
            GmailConversationStatus oldStatus = conv.getStatus();
            conv.setStatus(request.getStatus());
            if (request.getStatus() == GmailConversationStatus.RESOLVED || request.getStatus() == GmailConversationStatus.CLOSED || request.getStatus() == GmailConversationStatus.REPLIED) {
                if (conv.getResolvedAt() == null) {
                    conv.setResolvedAt(Instant.now());
                }
            }
            auditService.logEvent(
                    scope.getOrganizationId(),
                    scope.getUserId(),
                    "GMAIL_CONVERSATION_STATUS_CHANGED",
                    "GMAIL_CONVERSATION",
                    conversationId.toString(),
                    oldStatus != null ? oldStatus.name() : null,
                    request.getStatus().name()
            );
        }

        if (request.getPriority() != null) {
            conv.setPriority(request.getPriority());
        }

        if (request.getAssignedUserId() != null) {
            if (Objects.equals(request.getAssignedUserId(), UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                conv.setAssignedUserId(null);
            } else {
                validateAssigneeBelongsToOrg(request.getAssignedUserId(), scope.getOrganizationId());
                conv.setAssignedUserId(request.getAssignedUserId());
            }
        }

        if (request.getLocationId() != null) {
            conv.setLocationId(request.getLocationId());
        }

        if (request.getIsUnread() != null) {
            conv.setIsUnread(request.getIsUnread());
        }

        if (request.getIsStarred() != null) {
            conv.setIsStarred(request.getIsStarred());
        }

        GmailConversationEntity saved = conversationRepository.save(conv);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GmailConversationDto assignConversation(UUID conversationId, UUID assignedUserId, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        if (assignedUserId != null) {
            validateAssigneeBelongsToOrg(assignedUserId, scope.getOrganizationId());
        }

        UUID oldAssignee = conv.getAssignedUserId();
        conv.setAssignedUserId(assignedUserId);
        GmailConversationEntity saved = conversationRepository.save(conv);

        auditService.logEvent(
                scope.getOrganizationId(),
                scope.getUserId(),
                "GMAIL_CONVERSATION_ASSIGNED",
                "GMAIL_CONVERSATION",
                conversationId.toString(),
                oldAssignee != null ? oldAssignee.toString() : "UNASSIGNED",
                assignedUserId != null ? assignedUserId.toString() : "UNASSIGNED"
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GmailConversationDto linkClient(UUID conversationId, UUID clientId, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));

        conv.setClientId(client.getId());
        if (client.getLocationId() != null && conv.getLocationId() == null) {
            conv.setLocationId(client.getLocationId());
        }

        GmailConversationEntity saved = conversationRepository.save(conv);

        auditService.logEvent(
                scope.getOrganizationId(),
                scope.getUserId(),
                "GMAIL_CONVERSATION_CLIENT_LINKED",
                "GMAIL_CONVERSATION",
                conversationId.toString(),
                null,
                clientId.toString()
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GmailConversationDto unlinkClient(UUID conversationId, PracticeSecurityScope scope) {
        GmailConversationEntity conv = conversationRepository.findByIdAndOrganizationId(conversationId, scope.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Gmail conversation not found with ID: " + conversationId));

        validateScopeAccess(conv, scope);

        UUID oldClient = conv.getClientId();
        conv.setClientId(null);
        GmailConversationEntity saved = conversationRepository.save(conv);

        auditService.logEvent(
                scope.getOrganizationId(),
                scope.getUserId(),
                "GMAIL_CONVERSATION_CLIENT_UNLINKED",
                "GMAIL_CONVERSATION",
                conversationId.toString(),
                oldClient != null ? oldClient.toString() : null,
                null
        );

        return mapToDto(saved);
    }

    private Specification<GmailConversationEntity> buildSpecification(GmailConversationFilterRequest filter, PracticeSecurityScope scope) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Organization ID Isolation
            predicates.add(cb.equal(root.get("organizationId"), scope.getOrganizationId()));

            // 2. Practice Security Scoping
            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleClients = scopeEvaluator.getAccessibleClientIds(scope);
                Set<UUID> accessibleAssignees = scope.getAccessibleAssigneeIds();
                Set<UUID> accessibleLocations = scope.getAccessibleLocationIds();

                List<Predicate> scopePredicates = new ArrayList<>();

                if (accessibleAssignees != null && !accessibleAssignees.isEmpty()) {
                    scopePredicates.add(root.get("assignedUserId").in(accessibleAssignees));
                }
                if (accessibleClients != null && !accessibleClients.isEmpty()) {
                    scopePredicates.add(root.get("clientId").in(accessibleClients));
                }
                if (accessibleLocations != null && !accessibleLocations.isEmpty()) {
                    scopePredicates.add(root.get("locationId").in(accessibleLocations));
                }

                // If no specific assignments yet, allow viewing unassigned conversations within organization
                scopePredicates.add(cb.isNull(root.get("assignedUserId")));

                predicates.add(cb.or(scopePredicates.toArray(new Predicate[0])));
            }

            // 3. User Filter Criteria
            if (filter != null) {
                if (StringUtils.hasText(filter.getSearch())) {
                    String pattern = "%" + filter.getSearch().toLowerCase().trim() + "%";
                    Predicate searchPred = cb.or(
                            cb.like(cb.lower(root.get("subject")), pattern),
                            cb.like(cb.lower(root.get("snippet")), pattern),
                            cb.like(cb.lower(root.get("senderName")), pattern),
                            cb.like(cb.lower(root.get("senderEmail")), pattern)
                    );
                    predicates.add(searchPred);
                }

                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (StringUtils.hasText(filter.getCategory())) {
                    String cat = filter.getCategory().trim().toUpperCase();
                    if ("SUPPORT_REQUEST".equals(cat) || "SUPPORT".equals(cat)) {
                        predicates.add(cb.or(
                                cb.like(cb.lower(root.get("recipientEmails")), "%support@%"),
                                cb.like(cb.lower(root.get("senderEmail")), "%support@%"),
                                cb.like(cb.lower(root.get("subject")), "%support%"),
                                cb.like(cb.lower(root.get("subject")), "%help%"),
                                cb.like(cb.lower(root.get("snippet")), "%support%"),
                                cb.like(cb.lower(root.get("snippet")), "%help%")
                        ));
                    } else if ("PRACTITIONER_ENQUIRY".equals(cat) || "PRACTITIONER".equals(cat)) {
                        predicates.add(cb.or(
                                cb.like(cb.lower(root.get("recipientEmails")), "%info@%"),
                                cb.like(cb.lower(root.get("recipientEmails")), "%sales@%"),
                                cb.like(cb.lower(root.get("senderEmail")), "%info@%"),
                                cb.like(cb.lower(root.get("subject")), "%practitioner%"),
                                cb.like(cb.lower(root.get("subject")), "%doctor%"),
                                cb.like(cb.lower(root.get("subject")), "%onboard%"),
                                cb.like(cb.lower(root.get("subject")), "%demo%"),
                                cb.like(cb.lower(root.get("subject")), "%pricing%"),
                                cb.like(cb.lower(root.get("snippet")), "%practitioner%"),
                                cb.like(cb.lower(root.get("snippet")), "%onboard%")
                        ));
                    }
                }

                if (filter.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
                }

                if (filter.getClientId() != null) {
                    predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
                }

                if (filter.getLocationId() != null) {
                    predicates.add(cb.equal(root.get("locationId"), filter.getLocationId()));
                }

                if (filter.getAssignedUserId() != null) {
                    predicates.add(cb.equal(root.get("assignedUserId"), filter.getAssignedUserId()));
                }

                if (Boolean.TRUE.equals(filter.getUnassignedOnly())) {
                    predicates.add(cb.isNull(root.get("assignedUserId")));
                }

                if (Boolean.TRUE.equals(filter.getUnlinkedClientOnly())) {
                    predicates.add(cb.isNull(root.get("clientId")));
                }

                if (Boolean.TRUE.equals(filter.getUnreadOnly())) {
                    predicates.add(cb.isTrue(root.get("isUnread")));
                }

                if (filter.getGmailAccountId() != null) {
                    predicates.add(cb.equal(root.get("gmailAccountId"), filter.getGmailAccountId()));
                }

                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("lastMessageAt"), filter.getFromDate()));
                }

                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("lastMessageAt"), filter.getToDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateScopeAccess(GmailConversationEntity conv, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            return;
        }

        if (conv.getAssignedUserId() != null && scope.getAccessibleAssigneeIds() != null
                && scope.getAccessibleAssigneeIds().contains(conv.getAssignedUserId())) {
            return;
        }

        if (conv.getClientId() != null) {
            Set<UUID> accessibleClients = scopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClients != null && accessibleClients.contains(conv.getClientId())) {
                return;
            }
        }

        if (conv.getLocationId() != null && scope.getAccessibleLocationIds() != null
                && scope.getAccessibleLocationIds().contains(conv.getLocationId())) {
            return;
        }

        if (conv.getAssignedUserId() == null) {
            return; // Unassigned conversations visible for assignment
        }

        throw new ForbiddenException("Access denied: You do not have permission to view or modify this conversation.");
    }

    private void validateAssigneeBelongsToOrg(UUID userId, UUID organizationId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (!organizationId.equals(user.getOrganizationId())) {
            throw new ForbiddenException("Assigned user does not belong to the active practice organization");
        }
    }

    private GmailMessageViewDto mapToMessageView(GmailThreadModels.MessageMetadata msg, String threadId) {
        String from = "";
        String to = "";
        String cc = "";
        String subject = "";
        Instant date = null;

        StringBuilder plain = new StringBuilder();
        StringBuilder html = new StringBuilder();

        if (msg.getPayload() != null) {
            if (msg.getPayload().getHeaders() != null) {
                for (GmailThreadModels.HeaderEntry header : msg.getPayload().getHeaders()) {
                    if ("From".equalsIgnoreCase(header.getName())) from = header.getValue();
                    else if ("To".equalsIgnoreCase(header.getName())) to = header.getValue();
                    else if ("Cc".equalsIgnoreCase(header.getName())) cc = header.getValue();
                    else if ("Subject".equalsIgnoreCase(header.getName())) subject = header.getValue();
                }
            }
            extractBody(msg.getPayload(), plain, html);
        }

        if (msg.getInternalDate() != null && msg.getInternalDate() > 0) {
            date = Instant.ofEpochMilli(msg.getInternalDate());
        }

        List<String> labels = msg.getLabelIds() != null ? msg.getLabelIds() : Collections.emptyList();
        boolean isDraft = labels.contains("DRAFT");
        boolean isStarred = labels.contains("STARRED");

        return GmailMessageViewDto.builder()
                .id(msg.getId())
                .threadId(threadId)
                .from(from)
                .to(to)
                .cc(cc)
                .subject(subject)
                .snippet(msg.getSnippet())
                .bodyPlain(plain.toString().trim())
                .bodyHtml(html.toString().trim())
                .date(date)
                .labelIds(labels)
                .isDraft(isDraft)
                .isStarred(isStarred)
                .build();
    }

    private void extractBody(GmailThreadModels.MessagePayload payload, StringBuilder plain, StringBuilder html) {
        if (payload == null) return;
        if (payload.getBody() != null && StringUtils.hasText(payload.getBody().getData())) {
            String decoded = decodeBase64Url(payload.getBody().getData());
            if ("text/html".equalsIgnoreCase(payload.getMimeType())) {
                html.append(decoded);
            } else {
                plain.append(decoded);
            }
        }
        if (payload.getParts() != null) {
            for (GmailThreadModels.MessagePart part : payload.getParts()) {
                extractPartBody(part, plain, html);
            }
        }
    }

    private void extractPartBody(GmailThreadModels.MessagePart part, StringBuilder plain, StringBuilder html) {
        if (part == null) return;
        if (part.getBody() != null && StringUtils.hasText(part.getBody().getData())) {
            String decoded = decodeBase64Url(part.getBody().getData());
            if ("text/html".equalsIgnoreCase(part.getMimeType())) {
                html.append(decoded);
            } else {
                plain.append(decoded);
            }
        }
        if (part.getParts() != null) {
            for (GmailThreadModels.MessagePart subPart : part.getParts()) {
                extractPartBody(subPart, plain, html);
            }
        }
    }

    private String decodeBase64Url(String base64UrlData) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64UrlData);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64UrlData);
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private List<GmailMessageViewDto> buildFallbackMessageList(GmailConversationEntity conv) {
        GmailMessageViewDto fallback = GmailMessageViewDto.builder()
                .id(conv.getId().toString())
                .threadId(conv.getThreadId())
                .from(conv.getSenderName() != null ? (conv.getSenderName() + " <" + conv.getSenderEmail() + ">") : conv.getSenderEmail())
                .to(conv.getRecipientEmails())
                .subject(conv.getSubject())
                .snippet(conv.getSnippet())
                .bodyPlain(conv.getSnippet())
                .date(conv.getLastMessageAt() != null ? conv.getLastMessageAt() : conv.getCreatedAt())
                .labelIds(Collections.emptyList())
                .isDraft(false)
                .isStarred(Boolean.TRUE.equals(conv.getIsStarred()))
                .build();
        return List.of(fallback);
    }

    private String buildMimeMessage(String fromEmail, String toEmail, String subject, String body, String inReplyTo) {
        StringBuilder mime = new StringBuilder();
        mime.append("From: ").append(fromEmail).append("\r\n");
        mime.append("To: ").append(toEmail).append("\r\n");
        if (StringUtils.hasText(subject)) {
            mime.append("Subject: ").append(subject).append("\r\n");
        }
        if (StringUtils.hasText(inReplyTo)) {
            mime.append("In-Reply-To: ").append(inReplyTo).append("\r\n");
            mime.append("References: ").append(inReplyTo).append("\r\n");
        }
        mime.append("MIME-Version: 1.0\r\n");
        mime.append("Content-Type: text/plain; charset=UTF-8\r\n");
        mime.append("Content-Transfer-Encoding: base64\r\n\r\n");
        mime.append(Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)));
        return mime.toString();
    }

    public static String deriveCategory(String senderEmail, String recipientEmails, String subject, String snippet) {
        String allText = ((subject != null ? subject : "") + " "
                + (snippet != null ? snippet : "") + " "
                + (senderEmail != null ? senderEmail : "") + " "
                + (recipientEmails != null ? recipientEmails : "")).toLowerCase();

        if (allText.contains("support@") || allText.contains("help") || allText.contains("issue")
                || allText.contains("bug") || allText.contains("error") || allText.contains("ticket")
                || allText.contains("technical") || allText.contains("support")) {
            return "SUPPORT_REQUEST";
        }
        if (allText.contains("info@") || allText.contains("sales@") || allText.contains("practitioner")
                || allText.contains("doctor") || allText.contains("join") || allText.contains("onboard")
                || allText.contains("demo") || allText.contains("pricing") || allText.contains("subscription")
                || allText.contains("signup") || allText.contains("partner") || allText.contains("enquiry")
                || allText.contains("inquiry")) {
            return "PRACTITIONER_ENQUIRY";
        }
        return "GENERAL_ENQUIRY";
    }

    private GmailConversationDto mapToDto(GmailConversationEntity entity) {
        String clientName = null;
        String clientPan = null;
        String clientGstin = null;
        if (entity.getClientId() != null) {
            Optional<ClientEntity> clientOpt = clientRepository.findById(entity.getClientId());
            if (clientOpt.isPresent()) {
                ClientEntity client = clientOpt.get();
                clientName = client.getDisplayName();
                clientPan = client.getPan();
                clientGstin = client.getGstin();
            }
        }

        String locationName = null;
        if (entity.getLocationId() != null) {
            locationName = locationRepository.findById(entity.getLocationId())
                    .map(LocationEntity::getName)
                    .orElse(null);
        }

        String assigneeName = null;
        if (entity.getAssignedUserId() != null) {
            assigneeName = userRepository.findById(entity.getAssignedUserId())
                    .map(UserEntity::getFullName)
                    .orElse(null);
        }

        String mailboxEmail = null;
        if (entity.getGmailAccountId() != null) {
            mailboxEmail = accountRepository.findById(entity.getGmailAccountId())
                    .map(GmailAccountEntity::getEmailAddress)
                    .orElse(null);
        }

        String category = deriveCategory(entity.getSenderEmail(), entity.getRecipientEmails(), entity.getSubject(), entity.getSnippet());

        return GmailConversationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .gmailAccountId(entity.getGmailAccountId())
                .threadId(entity.getThreadId())
                .clientId(entity.getClientId())
                .clientDisplayName(clientName)
                .clientPan(clientPan)
                .clientGstin(clientGstin)
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .assignedUserId(entity.getAssignedUserId())
                .assignedUserName(assigneeName)
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .category(category)
                .mailboxEmail(mailboxEmail)
                .subject(entity.getSubject())
                .snippet(entity.getSnippet())
                .senderEmail(entity.getSenderEmail())
                .senderName(entity.getSenderName())
                .recipientEmails(entity.getRecipientEmails())
                .messageCount(entity.getMessageCount())
                .lastMessageAt(entity.getLastMessageAt())
                .firstResponseAt(entity.getFirstResponseAt())
                .resolvedAt(entity.getResolvedAt())
                .isUnread(entity.getIsUnread())
                .isStarred(entity.getIsStarred())
                .gmailLabels(entity.getGmailLabels())
                .webLink(entity.getWebLink())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
