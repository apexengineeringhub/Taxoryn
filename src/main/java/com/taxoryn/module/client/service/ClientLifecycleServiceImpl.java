package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientLifecycleSummaryDto;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Authoritative implementation of {@link ClientLifecycleService}.
 * Enforces explicit transition policy, records status metadata, handles archive session revocation,
 * and emits structured audit trail events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientLifecycleServiceImpl implements ClientLifecycleService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Lazy
    private final ClientService clientService;

    private static final Map<ClientStatus, Set<ClientStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<ClientStatus, Set<ClientStatus>> map = new EnumMap<>(ClientStatus.class);

        // ONBOARDING: can progress to ACTIVE, or be placed on hold (INACTIVE, SUSPENDED), or ARCHIVED
        map.put(ClientStatus.ONBOARDING, Set.of(
                ClientStatus.ONBOARDING,
                ClientStatus.ACTIVE,
                ClientStatus.INACTIVE,
                ClientStatus.SUSPENDED,
                ClientStatus.ARCHIVED
        ));

        // ACTIVE: can become INACTIVE, SUSPENDED, ARCHIVED, or return to ONBOARDING for major re-onboarding
        map.put(ClientStatus.ACTIVE, Set.of(
                ClientStatus.ACTIVE,
                ClientStatus.INACTIVE,
                ClientStatus.SUSPENDED,
                ClientStatus.ARCHIVED,
                ClientStatus.ONBOARDING
        ));

        // INACTIVE: can be reactivated to ACTIVE, placed in ONBOARDING, SUSPENDED, or ARCHIVED
        map.put(ClientStatus.INACTIVE, Set.of(
                ClientStatus.INACTIVE,
                ClientStatus.ACTIVE,
                ClientStatus.SUSPENDED,
                ClientStatus.ARCHIVED,
                ClientStatus.ONBOARDING
        ));

        // SUSPENDED: can be restored to ACTIVE, transitioned to INACTIVE or ARCHIVED, or back to ONBOARDING
        map.put(ClientStatus.SUSPENDED, Set.of(
                ClientStatus.SUSPENDED,
                ClientStatus.ACTIVE,
                ClientStatus.INACTIVE,
                ClientStatus.ARCHIVED,
                ClientStatus.ONBOARDING
        ));

        // PROSPECT: can be formally initiated for ONBOARDING, directly made ACTIVE, or archived
        map.put(ClientStatus.PROSPECT, Set.of(
                ClientStatus.PROSPECT,
                ClientStatus.ONBOARDING,
                ClientStatus.ACTIVE,
                ClientStatus.INACTIVE,
                ClientStatus.ARCHIVED
        ));

        // ARCHIVED: terminal state, but allows controlled un-archiving / reactivation with audit
        map.put(ClientStatus.ARCHIVED, Set.of(
                ClientStatus.ARCHIVED,
                ClientStatus.ACTIVE,
                ClientStatus.INACTIVE
        ));

        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    @Override
    @Transactional
    public ClientDto transitionStatus(UUID clientId, UpdateClientStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required to update client status");
        }

        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Target client status is required");
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientStatus oldStatus = client.getStatus();
        ClientStatus targetStatus = request.getStatus();

        if (!isTransitionAllowed(oldStatus, targetStatus)) {
            throw new BadRequestException("Invalid client lifecycle transition from " + oldStatus + " to " + targetStatus);
        }

        Instant now = Instant.now();
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String reason = StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null;

        client.setStatus(targetStatus);
        client.setStatusChangedAt(now);
        client.setStatusChangedBy(currentUserId);
        client.setStatusChangeReason(reason);

        ClientEntity saved = clientRepository.save(client);
        log.info("Transitioned client lifecycle status: id={}, from={}, to={} for tenant={}",
                clientId, oldStatus, targetStatus, organizationId);

        // If client is transitioned to ARCHIVED, revoke and deactivate portal access
        if (targetStatus == ClientStatus.ARCHIVED && oldStatus != ClientStatus.ARCHIVED) {
            deactivatePortalAccessForClient(organizationId, saved, "CLIENT_ARCHIVED");
        }

        auditService.logEvent(
                organizationId,
                currentUserId,
                "CLIENT_STATUS_TRANSITIONED",
                "CLIENT",
                clientId.toString(),
                oldStatus != null ? oldStatus.name() : null,
                targetStatus.name() + (reason != null ? " [Reason: " + reason + "]" : "")
        );

        return clientService.getClientById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientLifecycleSummaryDto getLifecycleSummary(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required to query client lifecycle");
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientStatus currentStatus = client.getStatus() != null ? client.getStatus() : ClientStatus.ACTIVE;

        return ClientLifecycleSummaryDto.builder()
                .clientId(client.getId())
                .organizationId(client.getOrganizationId())
                .currentStatus(currentStatus)
                .statusChangedAt(client.getStatusChangedAt())
                .statusChangedBy(client.getStatusChangedBy())
                .statusChangeReason(client.getStatusChangeReason())
                .allowedTransitions(getAllowedTransitions(currentStatus))
                .active(currentStatus == ClientStatus.ACTIVE)
                .onboarding(currentStatus == ClientStatus.ONBOARDING)
                .archived(currentStatus == ClientStatus.ARCHIVED)
                .suspended(currentStatus == ClientStatus.SUSPENDED)
                .inactive(currentStatus == ClientStatus.INACTIVE)
                .build();
    }

    @Override
    public boolean isTransitionAllowed(ClientStatus currentStatus, ClientStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        Set<ClientStatus> validTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        return validTargets != null && validTargets.contains(targetStatus);
    }

    @Override
    public Set<ClientStatus> getAllowedTransitions(ClientStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

    private void deactivatePortalAccessForClient(UUID organizationId, ClientEntity client, String reason) {
        List<UserEntity> portalUsers = userRepository.findAllByOrganizationIdAndClientId(organizationId, client.getId());
        for (UserEntity user : portalUsers) {
            UserStatus oldStatus = user.getStatus();
            if (oldStatus != UserStatus.INACTIVE) {
                user.setStatus(UserStatus.INACTIVE);
                userRepository.save(user);
                refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), reason);

                if (oldStatus == UserStatus.ACTIVE) {
                    try {
                        String orgName = organizationRepository.findById(organizationId)
                                .map(OrganizationEntity::getName)
                                .orElse("Your Tax Practice");
                        String recipientName = StringUtils.hasText(user.getFullName()) ? user.getFullName() : client.getDisplayName();
                        emailNotificationService.sendClientPortalDeactivatedEmail(
                                user.getEmail(),
                                recipientName,
                                client.getDisplayName(),
                                orgName
                        );
                    } catch (Exception ex) {
                        log.error("Failed to send client portal deactivation email on client archive for {}: {}", user.getEmail(), ex.getMessage());
                    }
                }
                auditService.logEvent(organizationId, SecurityUtils.getCurrentUserId(), "CLIENT_PORTAL_DEACTIVATED", "CLIENT", client.getId().toString(), oldStatus != null ? oldStatus.name() : null, UserStatus.INACTIVE.name());
            }
        }
    }

    private void validateClientAccess(ClientEntity client) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (!scope.isFirmAdmin()) {
            Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            boolean isAssigned = (client.getAssignedEmployeeId() != null && scope.getAccessibleAssigneeIds() != null && scope.getAccessibleAssigneeIds().contains(client.getAssignedEmployeeId()));
            if (!isAssigned && (accessibleClientIds == null || !accessibleClientIds.contains(client.getId()))) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission to access clients outside your assigned department or portfolio.");
            }
        }
    }
}
