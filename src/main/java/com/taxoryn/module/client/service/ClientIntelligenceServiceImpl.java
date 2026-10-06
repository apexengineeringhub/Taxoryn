package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.dto.ClientActionRecommendationDto;
import com.taxoryn.module.client.dto.ClientContactDto;
import com.taxoryn.module.client.dto.ClientIntelligenceSignalDto;
import com.taxoryn.module.client.dto.ClientIntelligenceSummaryDto;
import com.taxoryn.module.client.dto.ClientProfileCompletenessDto;
import com.taxoryn.module.client.entity.ClientActionType;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.SignalCategory;
import com.taxoryn.module.client.entity.SignalPriority;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientIntelligenceServiceImpl implements ClientIntelligenceService {

    private final ClientRepository clientRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final ClientContactRepository clientContactRepository;
    private final ClientBranchRepository clientBranchRepository;
    private final ClientProfileCompletenessEvaluator completenessEvaluator;
    private final com.taxoryn.module.user.repository.UserRepository userRepository;

    @Override
    public ClientIntelligenceSummaryDto evaluateClient(UUID organizationId, UUID clientId) {
        if (organizationId == null || clientId == null) {
            throw new IllegalArgumentException("organizationId and clientId must not be null");
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        return computeIntelligenceSummary(client);
    }

    @Override
    public ClientIntelligenceSummaryDto evaluateClient(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        return evaluateClient(organizationId, clientId);
    }

    @Override
    public List<ClientActionRecommendationDto> getRecommendations(UUID clientId) {
        ClientIntelligenceSummaryDto summary = evaluateClient(clientId);
        return summary.getRecommendations();
    }

    @Override
    public int countAttentionSignals(UUID organizationId, UUID clientId) {
        if (organizationId == null || clientId == null) {
            return 0;
        }
        return clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .map(client -> computeSignals(client).size())
                .orElse(0);
    }

    @Override
    public int countHighPrioritySignals(UUID organizationId, UUID clientId) {
        if (organizationId == null || clientId == null) {
            return 0;
        }
        return clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .map(client -> (int) computeSignals(client).stream()
                        .filter(s -> s.getPriority() == SignalPriority.CRITICAL || s.getPriority() == SignalPriority.HIGH)
                        .count())
                .orElse(0);
    }

    private ClientIntelligenceSummaryDto computeIntelligenceSummary(ClientEntity client) {
        Instant evaluatedAt = Instant.now();
        List<ClientIntelligenceSignalDto> signals = computeSignals(client);

        int criticalCount = (int) signals.stream().filter(s -> s.getPriority() == SignalPriority.CRITICAL).count();
        int highCount = (int) signals.stream().filter(s -> s.getPriority() == SignalPriority.HIGH).count();

        // Sort signals by priority
        signals.sort(Comparator.comparingInt(s -> priorityOrder(s.getPriority())));

        // Generate recommendations from actionable signals
        List<ClientActionRecommendationDto> recommendations = signals.stream()
                .filter(ClientIntelligenceSignalDto::isActionable)
                .map(this::toRecommendation)
                .collect(Collectors.toList());

        return ClientIntelligenceSummaryDto.builder()
                .clientId(client.getId())
                .totalSignalsCount(signals.size())
                .criticalSignalsCount(criticalCount)
                .highPrioritySignalsCount(highCount)
                .needsAttentionSignals(signals)
                .recommendations(recommendations)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    private List<ClientIntelligenceSignalDto> computeSignals(ClientEntity client) {
        List<ClientIntelligenceSignalDto> signals = new ArrayList<>();
        UUID organizationId = client.getOrganizationId();
        UUID clientId = client.getId();
        Instant now = Instant.now();

        // 1. PROFILE_INCOMPLETE
        ClientProfileCompletenessDto completeness = completenessEvaluator.evaluate(client);
        if (completeness.getCompletionPercentage() < 80) {
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_PROFILE_INCOMPLETE_" + clientId)
                    .signalCode("PROFILE_INCOMPLETE")
                    .category(SignalCategory.PROFILE)
                    .priority(SignalPriority.MEDIUM)
                    .title("Client Profile Incomplete")
                    .reason("Profile completeness is at " + completeness.getCompletionPercentage() + "%. "
                            + (completeness.getMissingSections() != null ? completeness.getMissingSections().size() : 0) + " required sections are unpopulated.")
                    .recommendedAction("Complete missing tax registrations and business classification details in client profile.")
                    .actionType(ClientActionType.COMPLETE_PROFILE)
                    .suggestedRoute("/clients/" + clientId + "/profile")
                    .actionable(true)
                    .sourceDataRef("completenessPercentage=" + completeness.getCompletionPercentage())
                    .detectedAt(now)
                    .build());
        }

        // 2. NO_PRIMARY_CONTACT
        boolean hasPrimaryContact = false;
        long totalContacts = 0;
        if (clientContactRepository != null) {
            hasPrimaryContact = clientContactRepository.findByOrganizationIdAndClientIdAndPrimaryContactTrue(organizationId, clientId).isPresent();
            totalContacts = clientContactRepository.countByOrganizationIdAndClientId(organizationId, clientId);
        }
        if (!hasPrimaryContact && (client.getStatus() == ClientStatus.ACTIVE || client.getStatus() == ClientStatus.ONBOARDING)) {
            SignalPriority priority = client.getStatus() == ClientStatus.ACTIVE ? SignalPriority.HIGH : SignalPriority.MEDIUM;
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_NO_PRIMARY_CONTACT_" + clientId)
                    .signalCode("NO_PRIMARY_CONTACT")
                    .category(SignalCategory.CONTACT)
                    .priority(priority)
                    .title("No Primary Contact Designated")
                    .reason(totalContacts == 0
                            ? "Client has no registered contact persons."
                            : "Client has " + totalContacts + " contact(s), but none is marked as primary contact.")
                    .recommendedAction("Designate a primary contact person for official communication and notices.")
                    .actionType(ClientActionType.ADD_PRIMARY_CONTACT)
                    .suggestedRoute("/clients/" + clientId + "/contacts")
                    .actionable(true)
                    .sourceDataRef("primaryContact=null")
                    .detectedAt(now)
                    .build());
        }

        // 3. NO_PRIMARY_BRANCH
        boolean hasPrimaryBranch = false;
        long totalBranches = 0;
        if (clientBranchRepository != null) {
            hasPrimaryBranch = clientBranchRepository.findByOrganizationIdAndClientIdAndPrimaryBranchTrue(organizationId, clientId).isPresent();
            totalBranches = clientBranchRepository.countByOrganizationIdAndClientId(organizationId, clientId);
        }
        if (!hasPrimaryBranch) {
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_NO_PRIMARY_BRANCH_" + clientId)
                    .signalCode("NO_PRIMARY_BRANCH")
                    .category(SignalCategory.LOCATION)
                    .priority(SignalPriority.MEDIUM)
                    .title("No Primary Business Location / Registered Office")
                    .reason(totalBranches == 0
                            ? "Client has no registered branch or office locations."
                            : "Client has " + totalBranches + " branch(es), but none is marked as primary / registered office.")
                    .recommendedAction("Register client principal place of business or mark a branch as primary.")
                    .actionType(ClientActionType.ADD_PRIMARY_BRANCH)
                    .suggestedRoute("/clients/" + clientId + "/branches")
                    .actionable(true)
                    .sourceDataRef("primaryBranch=null")
                    .detectedAt(now)
                    .build());
        }

        // 4. NO_ACTIVE_SERVICE & 5. SUSPENDED_SERVICE
        if (clientServiceRepository != null) {
            List<ClientServiceEntity> services = clientServiceRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
            long activeServicesCount = services.stream().filter(s -> s.getStatus() == ClientServiceStatus.ACTIVE).count();
            long suspendedServicesCount = services.stream().filter(s -> s.getStatus() == ClientServiceStatus.SUSPENDED).count();

            if (client.getStatus() == ClientStatus.ACTIVE && activeServicesCount == 0) {
                signals.add(ClientIntelligenceSignalDto.builder()
                        .id("SIG_NO_ACTIVE_SERVICE_" + clientId)
                        .signalCode("NO_ACTIVE_SERVICE")
                        .category(SignalCategory.SERVICE)
                        .priority(SignalPriority.HIGH)
                        .title("Active Client Without Active Services")
                        .reason("Client is marked as ACTIVE but has 0 active service relationships configured.")
                        .recommendedAction("Assign active service offerings (e.g. GST, ITR, TDS, Accounting) to this client.")
                        .actionType(ClientActionType.CONFIGURE_SERVICE)
                        .suggestedRoute("/clients/" + clientId + "/services")
                        .actionable(true)
                        .sourceDataRef("activeServicesCount=0")
                        .detectedAt(now)
                        .build());
            }

            if (suspendedServicesCount > 0) {
                signals.add(ClientIntelligenceSignalDto.builder()
                        .id("SIG_SUSPENDED_SERVICE_" + clientId)
                        .signalCode("SUSPENDED_SERVICE")
                        .category(SignalCategory.SERVICE)
                        .priority(SignalPriority.HIGH)
                        .title("Suspended Service Relationships")
                        .reason("Client has " + suspendedServicesCount + " suspended service relationship(s) requiring review.")
                        .recommendedAction("Review suspended service terms and resume or terminate the service.")
                        .actionType(ClientActionType.RESUME_SERVICE)
                        .suggestedRoute("/clients/" + clientId + "/services")
                        .actionable(true)
                        .sourceDataRef("suspendedServicesCount=" + suspendedServicesCount)
                        .detectedAt(now)
                        .build());
            }
        }

        // 6. CLIENT_INACTIVE
        if (client.getStatus() == ClientStatus.INACTIVE) {
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_CLIENT_INACTIVE_" + clientId)
                    .signalCode("CLIENT_INACTIVE")
                    .category(SignalCategory.LIFECYCLE)
                    .priority(SignalPriority.MEDIUM)
                    .title("Client is Inactive")
                    .reason("Client status is INACTIVE. Compliance filing and automated reminders are held.")
                    .recommendedAction("Review client status and activate when engagement resumes.")
                    .actionType(ClientActionType.ACTIVATE_CLIENT)
                    .suggestedRoute("/clients/" + clientId + "/lifecycle")
                    .actionable(true)
                    .sourceDataRef("status=INACTIVE")
                    .detectedAt(now)
                    .build());
        }

        // 7. CLIENT_SUSPENDED
        if (client.getStatus() == ClientStatus.SUSPENDED) {
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_CLIENT_SUSPENDED_" + clientId)
                    .signalCode("CLIENT_SUSPENDED")
                    .category(SignalCategory.LIFECYCLE)
                    .priority(SignalPriority.HIGH)
                    .title("Client Lifecycle Suspended")
                    .reason("Client account is suspended: " + (StringUtils.hasText(client.getStatusChangeReason()) ? client.getStatusChangeReason() : "Administrative hold"))
                    .recommendedAction("Resolve hold conditions and restore client status.")
                    .actionType(ClientActionType.REVIEW_CLIENT_STATUS)
                    .suggestedRoute("/clients/" + clientId + "/lifecycle")
                    .actionable(true)
                    .sourceDataRef("status=SUSPENDED")
                    .detectedAt(now)
                    .build());
        }

        // 8. CLIENT_ARCHIVED
        if (client.getStatus() == ClientStatus.ARCHIVED) {
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_CLIENT_ARCHIVED_" + clientId)
                    .signalCode("CLIENT_ARCHIVED")
                    .category(SignalCategory.LIFECYCLE)
                    .priority(SignalPriority.INFO)
                    .title("Client Record Archived")
                    .reason("Client is archived for historical record retention.")
                    .recommendedAction("No action required unless unarchiving for reactivated engagement.")
                    .actionType(ClientActionType.REVIEW_CLIENT_STATUS)
                    .suggestedRoute("/clients/" + clientId + "/lifecycle")
                    .actionable(false)
                    .sourceDataRef("status=ARCHIVED")
                    .detectedAt(now)
                    .build());
        }

        // 9. PENDING_PORTAL_INVITATION
        List<UserEntity> portalUsers = userRepository != null ? userRepository.findAllByOrganizationIdAndClientId(organizationId, clientId) : List.of();
        boolean hasActivePortalUser = portalUsers.stream().anyMatch(u -> u.getStatus() == com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE);
        if (!hasActivePortalUser) {
            String clientEmail = client.getEmail();
            boolean hasContactEmail = clientContactRepository != null && clientContactRepository.findByOrganizationIdAndClientIdAndPrimaryContactTrue(organizationId, clientId)
                    .filter(c -> StringUtils.hasText(c.getEmail())).isPresent();
            if (StringUtils.hasText(clientEmail) || hasContactEmail) {
                String portalState = portalUsers.isEmpty() ? "NOT_PROVISIONED" : portalUsers.get(0).getStatus().name();
                signals.add(ClientIntelligenceSignalDto.builder()
                        .id("SIG_PENDING_PORTAL_INVITATION_" + clientId)
                        .signalCode("PENDING_PORTAL_INVITATION")
                        .category(SignalCategory.ONBOARDING)
                        .priority(SignalPriority.LOW)
                        .title("Client Portal Access Pending")
                        .reason("Client has a valid email address but portal status is " + portalState + ".")
                        .recommendedAction("Invite client to the client portal for digital document exchange and payment access.")
                        .actionType(ClientActionType.INVITE_PORTAL_USER)
                        .suggestedRoute("/clients/" + clientId + "/portal")
                        .actionable(true)
                        .sourceDataRef("portalStatus=" + portalState)
                        .detectedAt(now)
                        .build());
            }
        }

        // 10. NO_BUSINESS_LOCATIONS
        if (totalBranches == 0 && !StringUtils.hasText(client.getCity()) && !StringUtils.hasText(client.getState())) {
            signals.add(ClientIntelligenceSignalDto.builder()
                    .id("SIG_NO_BUSINESS_LOCATIONS_" + clientId)
                    .signalCode("NO_BUSINESS_LOCATIONS")
                    .category(SignalCategory.LOCATION)
                    .priority(SignalPriority.MEDIUM)
                    .title("Missing Geographic Business Location")
                    .reason("No operating city or state configured on client master and no branches exist.")
                    .recommendedAction("Provide registered city and state or create branch business locations.")
                    .actionType(ClientActionType.ADD_PRIMARY_BRANCH)
                    .suggestedRoute("/clients/" + clientId + "/branches")
                    .actionable(true)
                    .sourceDataRef("city=null,branches=0")
                    .detectedAt(now)
                    .build());
        }

        return signals;
    }

    private ClientActionRecommendationDto toRecommendation(ClientIntelligenceSignalDto signal) {
        return ClientActionRecommendationDto.builder()
                .id("REC_" + signal.getSignalCode())
                .actionType(signal.getActionType())
                .title(signal.getTitle())
                .reason(signal.getReason())
                .priority(signal.getPriority())
                .clientId(UUID.fromString(signal.getId().substring(signal.getId().lastIndexOf('_') + 1)))
                .relatedEntityId(null)
                .source(signal.getSignalCode())
                .actionable(signal.isActionable())
                .suggestedRoute(signal.getSuggestedRoute())
                .build();
    }

    private int priorityOrder(SignalPriority priority) {
        if (priority == null) return 99;
        switch (priority) {
            case CRITICAL: return 1;
            case HIGH: return 2;
            case MEDIUM: return 3;
            case LOW: return 4;
            case INFO: return 5;
            default: return 99;
        }
    }
}
