package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientCommunicationDto;
import com.taxoryn.module.client.dto.ClientCommunicationRequest;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.entity.ClientNoteEntity.Visibility;
import com.taxoryn.module.client.repository.ClientCommunicationTimelineRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientCommunicationTimelineService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClientCommunicationTimelineRepository timelineRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<ClientCommunicationDto> listPracticeEntries(UUID clientId, NoteType type,
                                                                      Instant dateFrom, Instant dateTo,
                                                                      Boolean followUpRequired,
                                                                      int page, int size) {
        UUID organizationId = requirePracticeClientAccess(clientId);
        validateDateRange(dateFrom, dateTo);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), boundedSize(size),
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("createdAt")));
        Page<ClientNoteEntity> entries = timelineRepository.findTimelineEntries(
                organizationId, clientId, type, dateFrom, dateTo, followUpRequired, null, pageable);
        return PagedResponse.of(entries, this::toDto);
    }

    @Transactional(readOnly = true)
    public ClientCommunicationDto getPracticeEntry(UUID clientId, UUID communicationId) {
        UUID organizationId = requirePracticeClientAccess(clientId);
        return toDto(findEntry(clientId, communicationId, organizationId));
    }

    @Transactional
    public ClientCommunicationDto create(UUID clientId, ClientCommunicationRequest request) {
        UUID organizationId = requirePracticeClientAccess(clientId);
        UUID userId = requireCurrentUser();
        validateRequest(request);
        ClientNoteEntity entry = ClientNoteEntity.builder()
                .clientId(clientId)
                .authorId(userId)
                .authorName(resolveUserName(userId, organizationId))
                .noteType(request.getCommunicationType())
                .title(normalizeSubject(request.getSubject()))
                .content(request.getContent().trim())
                .occurredAt(request.getOccurredAt())
                .visibility(request.getVisibility() == null ? Visibility.INTERNAL : request.getVisibility())
                .followUpRequired(request.isFollowUpRequired())
                .followUpDate(request.isFollowUpRequired() ? request.getFollowUpDate() : null)
                .build();
        entry.setOrganizationId(organizationId);
        ClientNoteEntity saved = timelineRepository.save(entry);
        auditService.logEvent("CLIENT_COMMUNICATION_CREATED", "CLIENT_COMMUNICATION", saved.getId().toString(), null,
                auditSummary(saved));
        return toDto(saved);
    }

    @Transactional
    public ClientCommunicationDto update(UUID clientId, UUID communicationId, ClientCommunicationRequest request) {
        UUID organizationId = requirePracticeClientAccess(clientId);
        validateRequest(request);
        ClientNoteEntity entry = findEntry(clientId, communicationId, organizationId);
        ensureManualEntry(entry);

        String oldSummary = auditSummary(entry);
        entry.setNoteType(request.getCommunicationType());
        entry.setTitle(normalizeSubject(request.getSubject()));
        entry.setContent(request.getContent().trim());
        entry.setOccurredAt(request.getOccurredAt());
        if (request.getVisibility() != null) entry.setVisibility(request.getVisibility());
        entry.setFollowUpRequired(request.isFollowUpRequired());
        entry.setFollowUpDate(request.isFollowUpRequired() ? request.getFollowUpDate() : null);

        ClientNoteEntity saved = timelineRepository.save(entry);
        auditService.logEvent("CLIENT_COMMUNICATION_UPDATED", "CLIENT_COMMUNICATION", saved.getId().toString(),
                oldSummary, auditSummary(saved));
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID clientId, UUID communicationId) {
        UUID organizationId = requirePracticeClientAccess(clientId);
        ClientNoteEntity entry = findEntry(clientId, communicationId, organizationId);
        ensureManualEntry(entry);
        timelineRepository.delete(entry);
        auditService.logEvent("CLIENT_COMMUNICATION_DELETED", "CLIENT_COMMUNICATION", entry.getId().toString(),
                auditSummary(entry), null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClientCommunicationDto> listClientVisibleEntries(UUID clientId, int page, int size) {
        UUID callerClientId = SecurityUtils.getCurrentClientId().orElse(null);
        if (callerClientId == null || !callerClientId.equals(clientId)) {
            throw new AccessDeniedException("Client portal users may only view communications for their own client profile.");
        }
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) throw new UnauthorizedException("Authenticated organization context is required.");
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        PageRequest pageable = PageRequest.of(Math.max(page, 0), boundedSize(size),
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("createdAt")));
        Page<ClientNoteEntity> entries = timelineRepository.findTimelineEntries(
                organizationId, clientId, null, null, null, null, Visibility.CLIENT_VISIBLE, pageable);
        return PagedResponse.of(entries, this::toDto);
    }

    private UUID requirePracticeClientAccess(UUID clientId) {
        // The existing ClientService performs tenant lookup and PracticeSecurityScopeEvaluator checks.
        clientService.getClientById(clientId);
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) throw new UnauthorizedException("Authenticated organization context is required.");
        return organizationId;
    }

    private UUID requireCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new UnauthorizedException("Authenticated user context is required.");
        return userId;
    }

    private ClientNoteEntity findEntry(UUID clientId, UUID communicationId, UUID organizationId) {
        return timelineRepository.findByIdAndOrganizationIdAndClientId(communicationId, organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client communication", "id", communicationId));
    }

    private void ensureManualEntry(ClientNoteEntity entry) {
        if (entry.getNoteType() == NoteType.SYSTEM_EVENT) {
            throw new BadRequestException("System-generated timeline events cannot be changed or deleted.");
        }
    }

    private void validateRequest(ClientCommunicationRequest request) {
        if (request.getCommunicationType() == null) throw new BadRequestException("Communication type is required.");
        if (request.getCommunicationType() == NoteType.SYSTEM_EVENT) {
            throw new BadRequestException("System-generated timeline events cannot be created or edited manually.");
        }
        if (request.getOccurredAt() == null) throw new BadRequestException("Occurred at is required.");
        if (!StringUtils.hasText(request.getContent())) throw new BadRequestException("Communication content is required.");
        if (request.isFollowUpRequired() && request.getFollowUpDate() == null) {
            throw new BadRequestException("Follow-up date is required when follow-up is enabled.");
        }
        if (request.isFollowUpRequired() && request.getFollowUpDate().isBefore(request.getOccurredAt())) {
            throw new BadRequestException("Follow-up date cannot be earlier than the communication date.");
        }
    }

    private void validateDateRange(Instant dateFrom, Instant dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be earlier than or equal to dateTo.");
        }
    }

    private String resolveUserName(UUID userId, UUID organizationId) {
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .map(UserEntity::getFullName)
                .filter(StringUtils::hasText)
                .orElse("Practice user");
    }

    private ClientCommunicationDto toDto(ClientNoteEntity entry) {
        return ClientCommunicationDto.builder()
                .id(entry.getId())
                .clientId(entry.getClientId())
                .communicationType(entry.getNoteType())
                .subject(entry.getTitle())
                .content(entry.getContent())
                .occurredAt(entry.getOccurredAt())
                .visibility(entry.getVisibility() == null ? Visibility.INTERNAL : entry.getVisibility())
                .followUpRequired(entry.isFollowUpRequired())
                .followUpDate(entry.getFollowUpDate())
                .createdBy(entry.getAuthorId())
                .createdByName(StringUtils.hasText(entry.getAuthorName()) ? entry.getAuthorName() : "Practice user")
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private String normalizeSubject(String subject) {
        return StringUtils.hasText(subject) ? subject.trim() : null;
    }

    private String auditSummary(ClientNoteEntity entry) {
        return "type=" + entry.getNoteType() + ";visibility=" + entry.getVisibility()
                + ";followUpRequired=" + entry.isFollowUpRequired();
    }

    private int boundedSize(int requestedSize) {
        if (requestedSize < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }
}
