package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.ClientNoteDto;
import com.taxoryn.module.client.dto.ClientOverviewDto;
import com.taxoryn.module.client.dto.ClientOverviewDto.ClientBillingSummary;
import com.taxoryn.module.client.dto.ClientOverviewDto.ClientComplianceSummary;
import com.taxoryn.module.client.dto.ClientOverviewDto.ClientDocumentSummary;
import com.taxoryn.module.client.dto.ClientOverviewDto.ClientNoticeSummary;
import com.taxoryn.module.client.dto.ClientOverviewDto.ClientTaskSummary;
import com.taxoryn.module.client.dto.ClientOverviewDto.StatutoryDetails;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.taxoryn.core.security.PasswordSecurityUtils;
import com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity;
import com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.client.dto.UpdateClientPortalStatusRequest;
import jakarta.persistence.criteria.Root;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import jakarta.persistence.criteria.Subquery;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientNoteRepository clientNoteRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationActivationTokenRepository organizationActivationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final TaxNoticeRepository noticeRepository;
    private final GstProfileRepository gstProfileRepository;
    private final GstReturnFilingRepository gstFilingRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final TdsProfileRepository tdsProfileRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.taxoryn.module.client.repository.ClientServiceRepository clientServiceRepository;
    private final ClientMapper clientMapper;
    private final TaskMapper taskMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;

    @Value("${taxoryn.auth.activation-url:${taxoryn.frontend.activation-url:${taxoryn.auth.activation-base-url:${taxoryn.mail.activation-url:${TAXORYN_ACTIVATION_URL:${taxoryn.frontend-url:${app.frontend-url:${TAXORYN_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}}/activate}}}}}")
    private String activationBaseUrl = "http://localhost:5173/activate";

    @Value("${taxoryn.frontend.login-url:${taxoryn.auth.login-url:${taxoryn.frontend-url:${app.frontend-url:${TAXORYN_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}}/login}}")
    private String portalLoginUrl = "http://localhost:5173/login";

    @Value("${taxoryn.auth.activation.expiration-hours:24}")
    private long activationExpirationHours = 24L;

    @Override
    @Transactional
    public ClientDto createClient(CreateClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        // Check MAX_CLIENTS Subscription Limit
        subscriptionService.checkClientLimit(organizationId);

        if (StringUtils.hasText(request.getPan())
                && clientRepository.existsByOrganizationIdAndPan(organizationId, request.getPan().toUpperCase().trim())) {
            throw new DuplicateResourceException("Client", "pan", request.getPan());
        }

        if (StringUtils.hasText(request.getGstin())
                && clientRepository.existsByOrganizationIdAndGstin(organizationId, request.getGstin().toUpperCase().trim())) {
            throw new DuplicateResourceException("Client", "gstin", request.getGstin());
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        ClientEntity client = ClientEntity.builder()
                .clientType(request.getClientType())
                .displayName(request.getDisplayName().trim())
                .legalName(StringUtils.hasText(request.getLegalName()) ? request.getLegalName().trim() : null)
                .tradeName(StringUtils.hasText(request.getTradeName()) ? request.getTradeName().trim() : null)
                .pan(StringUtils.hasText(request.getPan()) ? request.getPan().toUpperCase().trim() : null)
                .gstin(StringUtils.hasText(request.getGstin()) ? request.getGstin().toUpperCase().trim() : null)
                .tan(StringUtils.hasText(request.getTan()) ? request.getTan().toUpperCase().trim() : null)
                .cin(StringUtils.hasText(request.getCin()) ? request.getCin().toUpperCase().trim() : null)
                .dateOfIncorporation(request.getDateOfIncorporation())
                .email(StringUtils.hasText(request.getEmail()) ? request.getEmail().toLowerCase().trim() : null)
                .phone(request.getPhone())
                .altPhone(request.getAltPhone())
                .contactPersonName(request.getContactPersonName())
                .contactPersonDesignation(request.getContactPersonDesignation())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(StringUtils.hasText(request.getCountry()) ? request.getCountry() : "India")
                .pincode(request.getPincode())
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(organizationId);

        ClientEntity saved = clientRepository.save(client);
        log.info("Created client: id={}, displayName={} for tenant={}", saved.getId(), saved.getDisplayName(), organizationId);

        if (StringUtils.hasText(saved.getEmail())) {
            UserEntity portalUser = provisionUserForClient(organizationId, saved);
            sendClientPortalInvitation(saved, portalUser, organizationId);
        }

        ClientDto result = enrichDto(saved);
        auditService.logEvent("CLIENT_CREATED", "CLIENT", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional
    public ClientDto updateClient(UUID clientId, UpdateClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientDto oldSnapshot = enrichDto(client);

        if (StringUtils.hasText(request.getPan())) {
            String newPan = request.getPan().toUpperCase().trim();
            if (!newPan.equalsIgnoreCase(client.getPan())
                    && clientRepository.existsByOrganizationIdAndPan(organizationId, newPan)) {
                throw new DuplicateResourceException("Client", "pan", newPan);
            }
            client.setPan(newPan);
        } else {
            client.setPan(null);
        }

        if (StringUtils.hasText(request.getGstin())) {
            String newGstin = request.getGstin().toUpperCase().trim();
            if (!newGstin.equalsIgnoreCase(client.getGstin())
                    && clientRepository.existsByOrganizationIdAndGstin(organizationId, newGstin)) {
                throw new DuplicateResourceException("Client", "gstin", newGstin);
            }
            client.setGstin(newGstin);
        } else {
            client.setGstin(null);
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        client.setClientType(request.getClientType());
        client.setDisplayName(request.getDisplayName().trim());
        client.setLegalName(StringUtils.hasText(request.getLegalName()) ? request.getLegalName().trim() : null);
        client.setTradeName(StringUtils.hasText(request.getTradeName()) ? request.getTradeName().trim() : null);
        client.setTan(StringUtils.hasText(request.getTan()) ? request.getTan().toUpperCase().trim() : null);
        client.setCin(StringUtils.hasText(request.getCin()) ? request.getCin().toUpperCase().trim() : null);
        client.setDateOfIncorporation(request.getDateOfIncorporation());
        client.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().toLowerCase().trim() : null);
        client.setPhone(request.getPhone());
        client.setAltPhone(request.getAltPhone());
        client.setContactPersonName(request.getContactPersonName());
        client.setContactPersonDesignation(request.getContactPersonDesignation());
        client.setAddressLine1(request.getAddressLine1());
        client.setAddressLine2(request.getAddressLine2());
        client.setCity(request.getCity());
        client.setState(request.getState());
        if (StringUtils.hasText(request.getCountry())) {
            client.setCountry(request.getCountry());
        }
        client.setPincode(request.getPincode());
        client.setAssignedEmployeeId(request.getAssignedEmployeeId());
        client.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }

        ClientEntity saved = clientRepository.save(client);
        log.info("Updated client: id={} for tenant={}", saved.getId(), organizationId);
        ClientDto updatedDto = enrichDto(saved);
        auditService.logEvent("CLIENT_UPDATED", "CLIENT", saved.getId().toString(), oldSnapshot, updatedDto);
        return updatedDto;
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getClientById(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new com.taxoryn.core.exception.UnauthorizedException("Authenticated organization context is required to query client");
        }
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        return enrichDto(client);
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

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClientDto> getClients(ClientFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new com.taxoryn.core.exception.UnauthorizedException("Authenticated organization context is required to query clients");
        }
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<ClientEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Enforce RBAC/ABAC Scoping for Non-Admins (Principle of Least Privilege)
            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
                Set<UUID> assigneeIds = scope.getAccessibleAssigneeIds();

                Predicate scopePredicate;
                if (accessibleClientIds != null && !accessibleClientIds.isEmpty() && assigneeIds != null && !assigneeIds.isEmpty()) {
                    scopePredicate = cb.or(
                            root.get("id").in(accessibleClientIds),
                            root.get("assignedEmployeeId").in(assigneeIds)
                    );
                } else if (accessibleClientIds != null && !accessibleClientIds.isEmpty()) {
                    scopePredicate = root.get("id").in(accessibleClientIds);
                } else if (assigneeIds != null && !assigneeIds.isEmpty()) {
                    scopePredicate = root.get("assignedEmployeeId").in(assigneeIds);
                } else {
                    scopePredicate = cb.disjunction(); // No accessible clients
                }
                predicates.add(scopePredicate);
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String searchPattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                Predicate searchMatch = cb.or(
                        cb.like(cb.lower(root.get("displayName")), searchPattern),
                        cb.like(cb.lower(root.get("legalName")), searchPattern),
                        cb.like(cb.lower(root.get("tradeName")), searchPattern),
                        cb.like(cb.lower(root.get("pan")), searchPattern),
                        cb.like(cb.lower(root.get("gstin")), searchPattern),
                        cb.like(cb.lower(root.get("tan")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("phone")), searchPattern)
                );
                predicates.add(searchMatch);
            }

            if (filterRequest.getClientType() != null) {
                predicates.add(cb.equal(root.get("clientType"), filterRequest.getClientType()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (filterRequest.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filterRequest.getAssignedEmployeeId()));
            }

            if (StringUtils.hasText(filterRequest.getCity())) {
                predicates.add(cb.equal(cb.lower(root.get("city")), filterRequest.getCity().trim().toLowerCase()));
            }

            if (StringUtils.hasText(filterRequest.getState())) {
                predicates.add(cb.equal(cb.lower(root.get("state")), filterRequest.getState().trim().toLowerCase()));
            }

            if (StringUtils.hasText(filterRequest.getPan())) {
                predicates.add(cb.equal(cb.lower(root.get("pan")), filterRequest.getPan().trim().toLowerCase()));
            }

            if (StringUtils.hasText(filterRequest.getGstin())) {
                predicates.add(cb.equal(cb.lower(root.get("gstin")), filterRequest.getGstin().trim().toLowerCase()));
            }

            if (StringUtils.hasText(filterRequest.getPortalStatus()) && !"ALL".equalsIgnoreCase(filterRequest.getPortalStatus().trim())) {
                String portalStatusFilter = filterRequest.getPortalStatus().trim().toUpperCase();
                if ("NOT_ENABLED".equals(portalStatusFilter) || "NOT_PROVISIONED".equals(portalStatusFilter)) {
                    Subquery<UUID> sub = query.subquery(UUID.class);
                    Root<UserEntity> userRoot = sub.from(UserEntity.class);
                    sub.select(userRoot.get("clientId")).where(
                            cb.equal(userRoot.get("organizationId"), organizationId),
                            cb.isNotNull(userRoot.get("clientId"))
                    );
                    predicates.add(cb.not(root.get("id").in(sub)));
                } else {
                    try {
                        UserStatus targetStatus = UserStatus.valueOf(portalStatusFilter);
                        Subquery<UUID> sub = query.subquery(UUID.class);
                        Root<UserEntity> userRoot = sub.from(UserEntity.class);
                        sub.select(userRoot.get("clientId")).where(
                                cb.equal(userRoot.get("organizationId"), organizationId),
                                cb.equal(userRoot.get("status"), targetStatus),
                                cb.isNotNull(userRoot.get("clientId"))
                        );
                        predicates.add(root.get("id").in(sub));
                    } catch (IllegalArgumentException ignored) {
                        // ignore unrecognized filter value
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ClientEntity> page = clientRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichDto);
    }

    @Override
    @Transactional
    public ClientDto updateClientStatus(UUID clientId, UpdateClientStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientStatus oldStatus = client.getStatus();
        ClientStatus newStatus = request.getStatus();
        client.setStatus(newStatus);
        ClientEntity saved = clientRepository.save(client);
        log.info("Updated client status: id={}, newStatus={} for tenant={}", clientId, newStatus, organizationId);

        // If client is archived, disable portal access and revoke sessions
        if (newStatus == ClientStatus.ARCHIVED) {
            deactivatePortalAccessForClient(organizationId, client, "CLIENT_ARCHIVED");
        }

        ClientDto result = enrichDto(saved);
        auditService.logEvent("CLIENT_STATUS_UPDATED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, newStatus.name());
        return result;
    }

    @Override
    @Transactional
    public ClientDto updateClientPortalStatus(UUID clientId, UpdateClientPortalStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        UserStatus targetStatus = request.getPortalStatus();
        List<UserEntity> portalUsers = userRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);

        if (portalUsers.isEmpty()) {
            if (!StringUtils.hasText(client.getEmail())) {
                throw new com.taxoryn.core.exception.BadRequestException("Cannot configure portal access: client does not have an email address");
            }
            if (targetStatus == UserStatus.INVITED || targetStatus == UserStatus.ACTIVE) {
                UserEntity provisioned = provisionUserForClient(organizationId, client);
                if (targetStatus == UserStatus.INVITED) {
                    sendClientPortalInvitation(client, provisioned, organizationId);
                } else {
                    provisioned.setStatus(UserStatus.ACTIVE);
                    userRepository.save(provisioned);
                }
                portalUsers = List.of(provisioned);
            } else {
                throw new ResourceNotFoundException("Client portal user", "clientId", clientId);
            }
        }

        for (UserEntity user : portalUsers) {
            UserStatus oldStatus = user.getStatus();
            if (oldStatus == targetStatus) {
                continue;
            }

            user.setStatus(targetStatus);
            userRepository.save(user);

            String orgName = organizationRepository.findById(organizationId)
                    .map(OrganizationEntity::getName)
                    .orElse("Your Tax Practice");
            String recipientName = StringUtils.hasText(user.getFullName()) ? user.getFullName() : client.getDisplayName();

            if (targetStatus == UserStatus.SUSPENDED) {
                refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "CLIENT_PORTAL_SUSPENDED");
                log.info("Revoked refresh sessions for suspended client portal user: {}", user.getId());

                try {
                    emailNotificationService.sendClientPortalSuspendedEmail(
                            user.getEmail(),
                            recipientName,
                            client.getDisplayName(),
                            orgName
                    );
                } catch (Exception ex) {
                    log.error("Failed to dispatch client portal suspension email to {}: {}", user.getEmail(), ex.getMessage());
                }

                auditService.logEvent(organizationId, currentUserId, "CLIENT_PORTAL_SUSPENDED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, targetStatus.name());
            } else if (targetStatus == UserStatus.ACTIVE) {
                try {
                    emailNotificationService.sendClientPortalRestoredEmail(
                            user.getEmail(),
                            recipientName,
                            client.getDisplayName(),
                            orgName,
                            portalLoginUrl
                    );
                } catch (Exception ex) {
                    log.error("Failed to dispatch client portal restoration email to {}: {}", user.getEmail(), ex.getMessage());
                }

                auditService.logEvent(organizationId, currentUserId, "CLIENT_PORTAL_RESTORED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, targetStatus.name());
            } else if (targetStatus == UserStatus.INACTIVE) {
                refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "CLIENT_PORTAL_DEACTIVATED");
                log.info("Revoked refresh sessions for deactivated client portal user: {}", user.getId());

                if (oldStatus == UserStatus.ACTIVE || oldStatus == UserStatus.SUSPENDED) {
                    try {
                        emailNotificationService.sendClientPortalDeactivatedEmail(
                                user.getEmail(),
                                recipientName,
                                client.getDisplayName(),
                                orgName
                        );
                    } catch (Exception ex) {
                        log.error("Failed to dispatch client portal deactivation email to {}: {}", user.getEmail(), ex.getMessage());
                    }
                }

                auditService.logEvent(organizationId, currentUserId, "CLIENT_PORTAL_DEACTIVATED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, targetStatus.name());
            }
        }

        return enrichDto(client);
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

    @Override
    @Transactional
    public ClientDto assignEmployee(UUID clientId, AssignClientEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        UUID oldEmployeeId = client.getAssignedEmployeeId();
        client.setAssignedEmployeeId(request.getEmployeeId());
        ClientEntity saved = clientRepository.save(client);
        log.info("Assigned employee {} to client {} for tenant {}", request.getEmployeeId(), clientId, organizationId);
        ClientDto result = enrichDto(saved);
        auditService.logEvent("CLIENT_EMPLOYEE_ASSIGNED", "CLIENT", clientId.toString(), oldEmployeeId != null ? oldEmployeeId.toString() : null, request.getEmployeeId().toString());
        return result;
    }

    @Override
    @Transactional
    public void deleteClient(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientStatus oldStatus = client.getStatus();
        client.setStatus(ClientStatus.ARCHIVED);
        clientRepository.save(client);
        log.info("Archived client: id={} for tenant={}", clientId, organizationId);

        // Deactivate portal user access on archive
        deactivatePortalAccessForClient(organizationId, client, "CLIENT_ARCHIVED");

        auditService.logEvent("CLIENT_DELETED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, ClientStatus.ARCHIVED.name());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientOverviewDto getClientOverview(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ClientDto clientDto = enrichDto(client);

        // 1. Statutory Details
        StatutoryDetails statutory = StatutoryDetails.builder()
                .pan(client.getPan())
                .gstin(client.getGstin())
                .tan(client.getTan())
                .cin(client.getCin())
                .dateOfIncorporation(client.getDateOfIncorporation())
                .isPanValid(StringUtils.hasText(client.getPan()))
                .isGstActive(StringUtils.hasText(client.getGstin()) && client.getStatus() == ClientStatus.ACTIVE)
                .build();

        // 2. Active Client Services
        List<ClientOverviewDto.ClientServiceItem> services = new ArrayList<>();
        List<ClientServiceEntity> configuredServices = clientServiceRepository != null ?
                clientServiceRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId) : List.of();

        Map<ClientServiceType, ClientServiceEntity> configuredMap = configuredServices.stream()
                .collect(Collectors.toMap(ClientServiceEntity::getServiceType, s -> s, (a, b) -> a));

        // GST Service
        ClientServiceEntity gstService = configuredMap.get(ClientServiceType.GST_COMPLIANCE);
        services.add(ClientOverviewDto.ClientServiceItem.builder()
                .serviceCode("GST")
                .serviceName("GST Compliance & Return Filing")
                .status(gstService != null ? gstService.getStatus().name() : (StringUtils.hasText(client.getGstin()) ? "ACTIVE" : "CONFIGURED"))
                .identifier(client.getGstin() != null ? client.getGstin() : "GSTIN Pending")
                .summary(gstService != null && StringUtils.hasText(gstService.getNotes()) ? gstService.getNotes() : "GSTR-1, GSTR-3B monthly & quarterly return management")
                .routePath("/gst")
                .build());

        // ITR Service
        ClientServiceEntity itrService = configuredMap.get(ClientServiceType.ITR_COMPLIANCE);
        services.add(ClientOverviewDto.ClientServiceItem.builder()
                .serviceCode("ITR")
                .serviceName("Income Tax Returns & Computations")
                .status(itrService != null ? itrService.getStatus().name() : (StringUtils.hasText(client.getPan()) ? "ACTIVE" : "CONFIGURED"))
                .identifier(client.getPan() != null ? client.getPan() : "PAN Pending")
                .summary(itrService != null && StringUtils.hasText(itrService.getNotes()) ? itrService.getNotes() : "Income tax computations, advance tax, and e-filings")
                .routePath("/itr")
                .build());

        // TDS Service
        ClientServiceEntity tdsService = configuredMap.get(ClientServiceType.TDS_COMPLIANCE);
        services.add(ClientOverviewDto.ClientServiceItem.builder()
                .serviceCode("TDS")
                .serviceName("TDS & TCS Quarterly Returns")
                .status(tdsService != null ? tdsService.getStatus().name() : (StringUtils.hasText(client.getTan()) ? "ACTIVE" : "CONFIGURED"))
                .identifier(client.getTan() != null ? client.getTan() : "TAN Pending")
                .summary(tdsService != null && StringUtils.hasText(tdsService.getNotes()) ? tdsService.getNotes() : "Forms 24Q, 26Q, 27Q, 27EQ and ITNS 281 challans")
                .routePath("/tds")
                .build());

        // Tax Notices Service
        ClientServiceEntity noticeService = configuredMap.get(ClientServiceType.TAX_NOTICE_MANAGEMENT);
        services.add(ClientOverviewDto.ClientServiceItem.builder()
                .serviceCode("TAX_NOTICES")
                .serviceName("Tax Notices & Litigation Management")
                .status(noticeService != null ? noticeService.getStatus().name() : "ACTIVE")
                .identifier(null)
                .summary(noticeService != null && StringUtils.hasText(noticeService.getNotes()) ? noticeService.getNotes() : "Statutory notice tracking, hearing schedules, and response drafting")
                .routePath("/notices")
                .build());

        // Documents Service
        ClientServiceEntity docService = configuredMap.get(ClientServiceType.DOCUMENT_MANAGEMENT);
        services.add(ClientOverviewDto.ClientServiceItem.builder()
                .serviceCode("DOCUMENTS")
                .serviceName("Client Document Vault & Requests")
                .status(docService != null ? docService.getStatus().name() : "ACTIVE")
                .identifier(null)
                .summary(docService != null && StringUtils.hasText(docService.getNotes()) ? docService.getNotes() : "Secure permanent client archives and dynamic requests")
                .routePath("/documents")
                .build());

        // Billing Service
        ClientServiceEntity billService = configuredMap.get(ClientServiceType.BILLING_INVOICING);
        services.add(ClientOverviewDto.ClientServiceItem.builder()
                .serviceCode("BILLING")
                .serviceName("Invoicing & Fee Collections")
                .status(billService != null ? billService.getStatus().name() : "ACTIVE")
                .identifier(null)
                .summary(billService != null && StringUtils.hasText(billService.getNotes()) ? billService.getNotes() : "Practice fee billing, payment receipts, and balance tracking")
                .routePath("/billing")
                .build());

        // Add any additional configured custom services (Accounting, Audit, Other, etc.)
        for (ClientServiceEntity extra : configuredServices) {
            if (extra.getServiceType() != ClientServiceType.GST_COMPLIANCE &&
                extra.getServiceType() != ClientServiceType.ITR_COMPLIANCE &&
                extra.getServiceType() != ClientServiceType.TDS_COMPLIANCE &&
                extra.getServiceType() != ClientServiceType.TAX_NOTICE_MANAGEMENT &&
                extra.getServiceType() != ClientServiceType.DOCUMENT_MANAGEMENT &&
                extra.getServiceType() != ClientServiceType.BILLING_INVOICING) {
                services.add(ClientOverviewDto.ClientServiceItem.builder()
                        .serviceCode(extra.getServiceType().name())
                        .serviceName(extra.getServiceType().getDisplayName())
                        .status(extra.getStatus().name())
                        .identifier(null)
                        .summary(extra.getNotes() != null ? extra.getNotes() : extra.getServiceType().getDescription())
                        .routePath(extra.getServiceType().getRoutePath())
                        .build());
            }
        }

        // 3. Task Summary (scoped to staff deliverables if staff)
        List<TaskEntity> taskList;
        if (scope.isStaff()) {
            Set<UUID> selfIds = scope.getAccessibleAssigneeIds() != null ? scope.getAccessibleAssigneeIds() : Set.of();
            taskList = taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                    .filter(t -> t.getAssignedTo() != null && selfIds.contains(t.getAssignedTo()))
                    .toList();
        } else {
            Page<TaskEntity> tasksPage = taskRepository.findAllByOrganizationIdAndClientId(
                    organizationId,
                    clientId,
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            taskList = tasksPage.getContent();
        }

        long totalTasks = taskList.size();
        long completedTasks = taskList.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long pendingTasks = taskList.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED).count();
        long inProgressTasks = taskList.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long underReviewTasks = taskList.stream().filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW).count();
        long overdueTasks = taskList.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED).count();
        List<TaskDto> recentTasks = taskMapper.toDtoList(taskList.stream().limit(10).toList());

        ClientTaskSummary taskSummary = ClientTaskSummary.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgressTasks)
                .underReviewTasks(underReviewTasks)
                .overdueTasks(overdueTasks)
                .completedTasks(completedTasks)
                .recentTasks(recentTasks)
                .build();

        // 4. Compliance Breakdown (GST, ITR, TDS)
        // 4A. GST
        Optional<com.taxoryn.module.gst.entity.GstProfileEntity> gstProfile = gstProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        List<com.taxoryn.module.gst.entity.GstReturnFilingEntity> gstFilings = gstFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(organizationId, clientId);
        long gstTotal = gstFilings.size();
        long gstPending = gstFilings.stream().filter(f -> f.getFilingStatus() == com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.PENDING || f.getFilingStatus() == com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.PREPARED || f.getFilingStatus() == com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.UNDER_REVIEW).count();
        long gstFiled = gstFilings.stream().filter(f -> f.getFilingStatus() == com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.FILED).count();
        long gstOverdue = gstFilings.stream().filter(f -> f.getFilingStatus() == com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.OVERDUE || (f.getDueDate() != null && f.getDueDate().isBefore(LocalDate.now()) && f.getFilingStatus() != com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.FILED && f.getFilingStatus() != com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.CANCELLED)).count();
        com.taxoryn.module.gst.entity.GstReturnFilingEntity nextGst = gstFilings.stream()
                .filter(f -> f.getFilingStatus() != com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.FILED && f.getFilingStatus() != com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.CANCELLED)
                .min(java.util.Comparator.comparing(f -> f.getDueDate() != null ? f.getDueDate() : LocalDate.MAX))
                .orElse(null);

        ClientOverviewDto.GstComplianceDetails gstDetails = ClientOverviewDto.GstComplianceDetails.builder()
                .registered(gstProfile.isPresent() || StringUtils.hasText(client.getGstin()))
                .gstin(client.getGstin())
                .filingFrequency(gstProfile.map(p -> p.getFilingFrequency() != null ? p.getFilingFrequency().name() : "MONTHLY").orElse("MONTHLY"))
                .totalFilings(gstTotal)
                .pendingFilings(gstPending)
                .filedFilings(gstFiled)
                .overdueFilings(gstOverdue)
                .nextDueDate(nextGst != null ? nextGst.getDueDate() : null)
                .nextReturnType(nextGst != null && nextGst.getReturnType() != null ? nextGst.getReturnType().name() : null)
                .nextReturnPeriod(nextGst != null ? nextGst.getReturnPeriod() : null)
                .build();

        // 4B. ITR
        Optional<com.taxoryn.module.itr.entity.ItrProfileEntity> itrProfile = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        List<com.taxoryn.module.itr.entity.ItrReturnEntity> itrReturns = itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(organizationId, clientId);
        long itrTotal = itrReturns.size();
        long itrPending = itrReturns.stream().filter(r -> r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.FILED && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.COMPLETED && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.CANCELLED).count();
        long itrFiled = itrReturns.stream().filter(r -> r.getStatus() == com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.FILED || r.getStatus() == com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.COMPLETED).count();
        long itrOverdue = itrReturns.stream().filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now()) && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.FILED && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.COMPLETED && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.CANCELLED).count();
        com.taxoryn.module.itr.entity.ItrReturnEntity nextItr = itrReturns.stream()
                .filter(r -> r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.FILED && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.COMPLETED && r.getStatus() != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.CANCELLED)
                .min(java.util.Comparator.comparing(r -> r.getDueDate() != null ? r.getDueDate() : LocalDate.MAX))
                .orElse(null);

        ClientOverviewDto.ItrComplianceDetails itrDetails = ClientOverviewDto.ItrComplianceDetails.builder()
                .registered(itrProfile.isPresent() || StringUtils.hasText(client.getPan()))
                .pan(client.getPan())
                .taxpayerType(itrProfile.map(p -> p.getTaxpayerType() != null ? p.getTaxpayerType().name() : null).orElse(null))
                .defaultItrType(itrProfile.map(p -> p.getDefaultItrType() != null ? p.getDefaultItrType().name() : null).orElse(null))
                .totalReturns(itrTotal)
                .pendingReturns(itrPending)
                .filedReturns(itrFiled)
                .overdueReturns(itrOverdue)
                .nextDueDate(nextItr != null ? nextItr.getDueDate() : null)
                .currentAssessmentYear(nextItr != null ? nextItr.getAssessmentYear() : (itrReturns.isEmpty() ? null : itrReturns.get(0).getAssessmentYear()))
                .currentStatus(nextItr != null && nextItr.getStatus() != null ? nextItr.getStatus().name() : (itrReturns.isEmpty() ? "NOT_INITIATED" : itrReturns.get(0).getStatus().name()))
                .build();

        // 4C. TDS
        Optional<com.taxoryn.module.tds.entity.TdsProfileEntity> tdsProfile = tdsProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        List<com.taxoryn.module.tds.entity.TdsReturnEntity> tdsReturns = tdsReturnRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);
        long tdsTotal = tdsReturns.size();
        long tdsPending = tdsReturns.stream().filter(r -> r.getFilingStatus() != com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.FILED && r.getFilingStatus() != com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.CANCELLED).count();
        long tdsFiled = tdsReturns.stream().filter(r -> r.getFilingStatus() == com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.FILED).count();
        long tdsOverdue = tdsReturns.stream().filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now()) && r.getFilingStatus() != com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.FILED && r.getFilingStatus() != com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.CANCELLED).count();
        com.taxoryn.module.tds.entity.TdsReturnEntity nextTds = tdsReturns.stream()
                .filter(r -> r.getFilingStatus() != com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.FILED && r.getFilingStatus() != com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus.CANCELLED)
                .min(java.util.Comparator.comparing(r -> r.getDueDate() != null ? r.getDueDate() : LocalDate.MAX))
                .orElse(null);

        ClientOverviewDto.TdsComplianceDetails tdsDetails = ClientOverviewDto.TdsComplianceDetails.builder()
                .registered(tdsProfile.isPresent() || StringUtils.hasText(client.getTan()))
                .tan(client.getTan())
                .deductorType(tdsProfile.map(p -> p.getDeductorType() != null ? p.getDeductorType().name() : null).orElse(null))
                .totalReturns(tdsTotal)
                .pendingReturns(tdsPending)
                .filedReturns(tdsFiled)
                .overdueReturns(tdsOverdue)
                .nextDueDate(nextTds != null ? nextTds.getDueDate() : null)
                .currentQuarter(nextTds != null && nextTds.getQuarter() != null ? nextTds.getQuarter().name() : null)
                .currentFinancialYear(nextTds != null ? nextTds.getFinancialYear() : null)
                .build();

        ClientComplianceSummary complianceSummary = ClientComplianceSummary.builder()
                .gstStatus(StringUtils.hasText(client.getGstin()) ? (gstOverdue > 0 ? gstOverdue + " Overdue GST Return(s)" : (gstPending > 0 ? "Filing in Progress (" + gstPending + " Pending)" : "All Returns Filed on Schedule")) : "Not Registered for GST")
                .itrStatus(StringUtils.hasText(client.getPan()) ? (itrOverdue > 0 ? "AY " + (nextItr != null ? nextItr.getAssessmentYear() : "") + " Overdue" : (itrPending > 0 ? "AY Computation in Progress" : "ITR Compliant")) : "PAN Required for ITR")
                .tdsStatus(StringUtils.hasText(client.getTan()) ? (tdsOverdue > 0 ? tdsOverdue + " Overdue TDS Return(s)" : (tdsPending > 0 ? "TDS Quarter in Progress" : "TDS Returns Up-to-date")) : "No TAN Registered")
                .accountingStatus("Active Financial Year 2024-25")
                .gstDetails(gstDetails)
                .itrDetails(itrDetails)
                .tdsDetails(tdsDetails)
                .build();

        // 5. Documents Vault Summary
        List<com.taxoryn.module.document.entity.DocumentEntity> docs = documentRepository.findAllByOrganizationIdAndClientIdAndStatus(organizationId, clientId, com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus.ACTIVE);
        long totalDocuments = docs.size();
        List<String> documentCategories = docs.stream()
                .map(d -> d.getDocumentType() != null ? d.getDocumentType().name() : "OTHER")
                .distinct()
                .toList();
        List<ClientOverviewDto.ClientDocumentItem> recentDocuments = docs.stream()
                .sorted(java.util.Comparator.comparing(com.taxoryn.module.document.entity.DocumentEntity::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(10)
                .map(d -> ClientOverviewDto.ClientDocumentItem.builder()
                        .id(d.getId())
                        .fileName(d.getFileName())
                        .documentCategory(d.getDocumentType() != null ? d.getDocumentType().name() : "OTHER")
                        .fileSize(d.getFileSize())
                        .fileType(d.getContentType())
                        .uploadedAt(d.getCreatedAt())
                        .fileUrl("/api/v1/documents/" + d.getId() + "/download")
                        .build())
                .toList();

        ClientDocumentSummary documentSummary = ClientDocumentSummary.builder()
                .totalDocuments(totalDocuments)
                .documentCategories(documentCategories.isEmpty() ? List.of("GST_INVOICES", "ITR_COMPUTATIONS", "BANK_STATEMENTS", "KYC_DOCUMENTS") : documentCategories)
                .recentDocuments(recentDocuments)
                .build();

        // 6. Document Requests Summary
        List<com.taxoryn.module.docrequest.entity.DocumentRequestEntity> docRequests = documentRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        long totalDocRequests = docRequests.size();
        long pendingDocRequests = docRequests.stream().filter(r -> r.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.REQUESTED || r.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT || r.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED).count();
        long receivedDocRequests = docRequests.stream().filter(r -> r.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.COMPLETED || r.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.VIEWED || r.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.IN_REVIEW).count();
        long overdueDocRequests = docRequests.stream().filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now()) && r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.COMPLETED && r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.CANCELLED).count();
        List<ClientOverviewDto.ClientDocRequestItem> recentDocRequests = docRequests.stream().limit(10).map(r -> ClientOverviewDto.ClientDocRequestItem.builder()
                .id(r.getId())
                .requestNumber(r.getRequestNumber())
                .title(r.getPurpose() != null ? r.getPurpose() : "Document Request #" + r.getRequestNumber())
                .status(r.getStatus() != null ? r.getStatus().name() : "SENT")
                .priority("MEDIUM")
                .dueDate(r.getDueDate())
                .totalItems(r.getItems() != null ? r.getItems().size() : 0)
                .receivedItems(r.getItems() != null ? (int) r.getItems().stream().filter(i -> i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.UPLOADED || i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.ACCEPTED).count() : 0)
                .createdAt(r.getCreatedAt())
                .build()).toList();

        ClientOverviewDto.ClientDocRequestSummary docRequestsSummary = ClientOverviewDto.ClientDocRequestSummary.builder()
                .totalRequests(totalDocRequests)
                .pendingRequests(pendingDocRequests)
                .receivedRequests(receivedDocRequests)
                .overdueRequests(overdueDocRequests)
                .recentRequests(recentDocRequests)
                .build();

        // 7. Billing Summary - ZERO TRUST: Null/Redacted for non-billing staff
        ClientBillingSummary billingSummary = null;
        if (securityScopeEvaluator.hasBillingAccess(scope)) {
            List<com.taxoryn.module.billing.entity.InvoiceEntity> invoices = invoiceRepository.findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(organizationId, clientId);
            double totalInvoiced = invoices.stream()
                    .filter(i -> i.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.CANCELLED)
                    .mapToDouble(i -> i.getTotal() != null ? i.getTotal().doubleValue() : 0.0)
                    .sum();
            double totalPaid = invoices.stream()
                    .filter(i -> i.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.CANCELLED)
                    .mapToDouble(i -> i.getPaidAmount() != null ? i.getPaidAmount().doubleValue() : 0.0)
                    .sum();
            double balanceDue = invoices.stream()
                    .filter(i -> i.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.CANCELLED)
                    .mapToDouble(i -> i.getBalanceDue() != null ? i.getBalanceDue().doubleValue() : 0.0)
                    .sum();
            long overdueInvoices = invoices.stream()
                    .filter(i -> i.getDueDate() != null && i.getDueDate().isBefore(LocalDate.now()) && (i.getStatus() == com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.ISSUED || i.getStatus() == com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.PARTIALLY_PAID || i.getStatus() == com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.OVERDUE))
                    .count();
            List<ClientOverviewDto.ClientInvoiceItem> recentInvoices = invoices.stream().limit(10).map(i -> ClientOverviewDto.ClientInvoiceItem.builder()
                    .id(i.getId())
                    .invoiceNumber(i.getInvoiceNumber())
                    .invoiceDate(i.getInvoiceDate())
                    .dueDate(i.getDueDate())
                    .total(i.getTotal())
                    .paidAmount(i.getPaidAmount())
                    .balanceDue(i.getBalanceDue())
                    .status(i.getStatus() != null ? i.getStatus().name() : "DRAFT")
                    .build()).toList();

            billingSummary = ClientBillingSummary.builder()
                    .totalInvoiced(totalInvoiced)
                    .totalPaid(totalPaid)
                    .outstandingBalance(balanceDue)
                    .currency("INR")
                    .totalInvoicesCount(invoices.size())
                    .overdueInvoicesCount(overdueInvoices)
                    .recentInvoices(recentInvoices)
                    .build();
        }

        // 8. Tax Notices Summary
        List<TaxNoticeEntity> clientNotices = noticeRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);
        long totalNotices = clientNotices.size();
        Set<NoticeStatus> closed = Set.of(NoticeStatus.RESOLVED, NoticeStatus.DEMAND_DROPPED, NoticeStatus.APPEAL_FILED, NoticeStatus.CLOSED);
        long activeNotices = clientNotices.stream().filter(n -> !closed.contains(n.getStatus())).count();
        long overdueNotices = clientNotices.stream().filter(n -> !closed.contains(n.getStatus()) && n.getResponseDueDate() != null && n.getResponseDueDate().isBefore(LocalDate.now())).count();
        long hearingsScheduled = clientNotices.stream().filter(n -> n.getHearingDate() != null && !n.getHearingDate().isBefore(LocalDate.now())).count();
        java.math.BigDecimal totalDemand = clientNotices.stream()
                .filter(n -> !closed.contains(n.getStatus()) && n.getDemandAmount() != null)
                .map(TaxNoticeEntity::getDemandAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        List<ClientOverviewDto.ClientNoticeItem> recentNotices = clientNotices.stream().limit(10).map(n -> ClientOverviewDto.ClientNoticeItem.builder()
                .id(n.getId())
                .noticeNumber(n.getNoticeNumber())
                .issuingAuthority(n.getDepartment() != null ? n.getDepartment().name() : null)
                .section(n.getSection())
                .taxPeriod(n.getTaxPeriod())
                .status(n.getStatus() != null ? n.getStatus().name() : null)
                .demandAmount(n.getDemandAmount())
                .responseDueDate(n.getResponseDueDate())
                .hearingDate(n.getHearingDate())
                .build()).toList();

        ClientNoticeSummary noticeSummary = ClientNoticeSummary.builder()
                .totalNotices(totalNotices)
                .activeNotices(activeNotices)
                .overdueNotices(overdueNotices)
                .hearingsScheduled(hearingsScheduled)
                .totalDemandAmount(totalDemand)
                .recentNotices(recentNotices)
                .build();

        // 9. Recent Communication Notes
        List<ClientNoteEntity> noteEntities = clientNoteRepository.findTop10ByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        List<ClientNoteDto> recentNotes = clientMapper.toNoteDtoList(noteEntities);

        // 10. Unified Chronological Activity Timeline
        List<ClientOverviewDto.ClientActivityItem> activityTimeline = new ArrayList<>();

        // Add Notes to Activity Timeline
        noteEntities.forEach(n -> activityTimeline.add(ClientOverviewDto.ClientActivityItem.builder()
                .id("NOTE-" + n.getId())
                .eventType("CLIENT_NOTE")
                .title(n.getTitle())
                .description(n.getContent())
                .performedBy(n.getAuthorName() != null ? n.getAuthorName() : "Practitioner")
                .timestamp(n.getCreatedAt())
                .category("COMMUNICATION")
                .build()));

        // Add Recent Audit Logs
        try {
            Page<com.taxoryn.module.audit.entity.AuditLogEntity> audits = auditLogRepository.findAllByOrganizationIdAndEntityId(
                    organizationId,
                    clientId.toString(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            audits.getContent().forEach(a -> activityTimeline.add(ClientOverviewDto.ClientActivityItem.builder()
                    .id("AUDIT-" + a.getId())
                    .eventType(a.getAction() != null ? a.getAction() : "AUDIT_EVENT")
                    .title(a.getAction() != null ? a.getAction().replace('_', ' ') : "Audit Action")
                    .description(a.getNewValue() != null ? "Value: " + a.getNewValue() : (a.getOldValue() != null ? "Previous: " + a.getOldValue() : "Audit activity on client record"))
                    .performedBy("System")
                    .timestamp(a.getCreatedAt())
                    .category("AUDIT")
                    .build()));
        } catch (Exception ignored) {
            // Safe fallback if audit log table structure varies
        }

        // Add Recent Document Requests to Activity Timeline
        docRequests.stream().limit(5).forEach(dr -> activityTimeline.add(ClientOverviewDto.ClientActivityItem.builder()
                .id("DOCREQ-" + dr.getId())
                .eventType("DOCUMENT_REQUEST")
                .title("Document Request: " + dr.getRequestNumber())
                .description(dr.getPurpose() + " (Status: " + dr.getStatus() + ")")
                .performedBy("Practice Team")
                .timestamp(dr.getCreatedAt())
                .category("DOCUMENTS")
                .build()));

        // Sort Activity Timeline Descending by timestamp
        activityTimeline.sort(java.util.Comparator.comparing(
                ClientOverviewDto.ClientActivityItem::getTimestamp,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        ));

        return ClientOverviewDto.builder()
                .client(clientDto)
                .statutory(statutory)
                .services(services)
                .taskSummary(taskSummary)
                .complianceSummary(complianceSummary)
                .documentsSummary(documentSummary)
                .docRequestsSummary(docRequestsSummary)
                .billingSummary(billingSummary)
                .noticeSummary(noticeSummary)
                .recentNotes(recentNotes)
                .activityTimeline(activityTimeline.stream().limit(25).toList())
                .build();
    }

    @Override
    @Transactional
    public ClientNoteDto addClientNote(UUID clientId, CreateClientNoteRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        ClientNoteEntity note = ClientNoteEntity.builder()
                .clientId(clientId)
                .authorId(currentUserId)
                .noteType(request.getNoteType())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .build();
        note.setOrganizationId(organizationId);

        ClientNoteEntity saved = clientNoteRepository.save(note);
        log.info("Added communication note: id={} for client={}", saved.getId(), clientId);
        return clientMapper.toNoteDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientNoteDto> getClientNotes(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        List<ClientNoteEntity> notes = clientNoteRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        return clientMapper.toNoteDtoList(notes);
    }

    @Override
    @Transactional
    public com.taxoryn.module.client.dto.BulkImportResultDto bulkCreateClients(List<CreateClientRequest> requests) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        com.taxoryn.module.client.dto.BulkImportResultDto result = com.taxoryn.module.client.dto.BulkImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) {
            return result;
        }

        // 1. Preload existing PANs and GSTINs for this organization to eliminate N+1 DB roundtrips
        List<ClientEntity> existingClients = clientRepository.findAllByOrganizationId(organizationId);
        Set<String> existingPans = existingClients.stream()
                .map(ClientEntity::getPan)
                .filter(StringUtils::hasText)
                .map(p -> p.toUpperCase().trim())
                .collect(Collectors.toSet());
        Set<String> existingGstins = existingClients.stream()
                .map(ClientEntity::getGstin)
                .filter(StringUtils::hasText)
                .map(g -> g.toUpperCase().trim())
                .collect(Collectors.toSet());

        // In-file duplicate tracking sets
        Set<String> seenPansInFile = new HashSet<>();
        Set<String> seenGstinsInFile = new HashSet<>();

        // Regex patterns
        final Pattern panPattern = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
        final Pattern gstinPattern = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
        final Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        final Pattern phonePattern = Pattern.compile("^[6-9][0-9]{9}$");
        final Pattern pincodePattern = Pattern.compile("^[1-9][0-9]{5}$");

        int rowNum = 1;
        for (CreateClientRequest req : requests) {
            rowNum++;

            // Normalization
            String displayName = req.getDisplayName() != null ? req.getDisplayName().trim() : null;
            String legalName = StringUtils.hasText(req.getLegalName()) ? req.getLegalName().trim() : null;
            String tradeName = StringUtils.hasText(req.getTradeName()) ? req.getTradeName().trim() : null;
            String pan = StringUtils.hasText(req.getPan()) ? req.getPan().toUpperCase().trim() : null;
            String gstin = StringUtils.hasText(req.getGstin()) ? req.getGstin().toUpperCase().trim() : null;
            String email = StringUtils.hasText(req.getEmail()) ? req.getEmail().toLowerCase().trim() : null;
            String rawPhone = req.getPhone();
            String phone = normalizeIndianMobile(rawPhone);
            String rawPincode = req.getPincode();
            String pincode = normalizePincode(rawPincode);
            String city = StringUtils.hasText(req.getCity()) ? req.getCity().trim() : null;
            String state = StringUtils.hasText(req.getState()) ? req.getState().trim() : null;
            String addressLine1 = StringUtils.hasText(req.getAddressLine1()) ? req.getAddressLine1().trim() : null;
            String addressLine2 = StringUtils.hasText(req.getAddressLine2()) ? req.getAddressLine2().trim() : null;
            String contactPerson = StringUtils.hasText(req.getContactPersonName()) ? req.getContactPersonName().trim() : null;
            String notes = StringUtils.hasText(req.getNotes()) ? req.getNotes().trim() : null;

            // Determine client type
            ClientEntity.ClientType clientType = req.getClientType();
            if (clientType == null) {
                clientType = StringUtils.hasText(gstin) ? ClientEntity.ClientType.PRIVATE_LIMITED : ClientEntity.ClientType.INDIVIDUAL;
            }

            // 2. Validate Client Name (Required)
            if (!StringUtils.hasText(displayName)) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName("Unknown")
                        .pan(pan != null ? pan : "MISSING")
                        .field("Client Name")
                        .invalidValue("")
                        .reason("Display name / Business name is required")
                        .suggestedCorrection("Provide a valid client display name or entity name")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            // 3. Validate PAN (Required for Indian tax practice client onboarding)
            if (!StringUtils.hasText(pan)) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan("MISSING")
                        .field("PAN")
                        .invalidValue("")
                        .reason("PAN number is required for client onboarding")
                        .suggestedCorrection("Enter 10-character alphanumeric PAN (e.g., ABCDE1234F)")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            if (!panPattern.matcher(pan).matches()) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("PAN")
                        .invalidValue(pan)
                        .reason("Invalid PAN format (expected 5 letters, 4 digits, 1 letter)")
                        .suggestedCorrection("Verify PAN format: 5 uppercase letters, 4 digits, 1 uppercase letter (e.g. ABCDE1234F)")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            // 4. In-File Duplicate PAN Check
            if (seenPansInFile.contains(pan)) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("PAN")
                        .invalidValue(pan)
                        .reason("Duplicate PAN detected within the uploaded spreadsheet file")
                        .suggestedCorrection("Remove or combine duplicate row from the spreadsheet")
                        .duplicate(true)
                        .build());
                result.setTotalSkipped(result.getTotalSkipped() + 1);
                continue;
            }

            // 5. Existing Practice DB Duplicate PAN Check
            if (existingPans.contains(pan)) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("PAN")
                        .invalidValue(pan)
                        .reason("Duplicate client with PAN " + pan + " already exists in practice")
                        .suggestedCorrection("Client already registered. Review existing profile in Clients Directory")
                        .duplicate(true)
                        .build());
                result.setTotalSkipped(result.getTotalSkipped() + 1);
                continue;
            }

            // 6. Validate GSTIN (Optional, but if provided must be valid and match PAN)
            if (gstin != null) {
                if (!gstinPattern.matcher(gstin).matches()) {
                    result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                            .rowNumber(rowNum)
                            .clientName(displayName)
                            .pan(pan)
                            .field("GSTIN")
                            .invalidValue(gstin)
                            .reason("Invalid GSTIN format: " + gstin)
                            .suggestedCorrection("Ensure 15-character GSTIN format (e.g., 27ABCDE1234F1Z5)")
                            .duplicate(false)
                            .build());
                    result.setTotalFailed(result.getTotalFailed() + 1);
                    continue;
                }

                // PAN-GSTIN Consistency: Characters 3-12 of GSTIN must equal the client's PAN
                String gstinPan = gstin.substring(2, 12);
                if (!gstinPan.equals(pan)) {
                    result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                            .rowNumber(rowNum)
                            .clientName(displayName)
                            .pan(pan)
                            .field("GSTIN / PAN")
                            .invalidValue(gstin)
                            .reason("GSTIN embedded PAN (" + gstinPan + ") does not match client PAN (" + pan + ")")
                            .suggestedCorrection("Ensure GSTIN belongs to the client with PAN " + pan)
                            .duplicate(false)
                            .build());
                    result.setTotalFailed(result.getTotalFailed() + 1);
                    continue;
                }

                // In-File Duplicate GSTIN Check
                if (seenGstinsInFile.contains(gstin)) {
                    result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                            .rowNumber(rowNum)
                            .clientName(displayName)
                            .pan(pan)
                            .field("GSTIN")
                            .invalidValue(gstin)
                            .reason("Duplicate GSTIN detected within the uploaded spreadsheet file")
                            .suggestedCorrection("Ensure unique GSTIN per row in the spreadsheet")
                            .duplicate(true)
                            .build());
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                // Existing Practice DB Duplicate GSTIN Check
                if (existingGstins.contains(gstin)) {
                    result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                            .rowNumber(rowNum)
                            .clientName(displayName)
                            .pan(pan)
                            .field("GSTIN")
                            .invalidValue(gstin)
                            .reason("Duplicate client with GSTIN " + gstin + " already exists in practice")
                            .suggestedCorrection("GSTIN already registered. Review existing profile in Clients Directory")
                            .duplicate(true)
                            .build());
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }
            }

            // 7. Validate Email (if provided)
            if (email != null && !emailPattern.matcher(email).matches()) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("Email")
                        .invalidValue(email)
                        .reason("Invalid email address format: " + email)
                        .suggestedCorrection("Provide a valid email address (e.g. contact@example.com)")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            // 8. Validate Mobile Phone (if provided)
            if (phone != null && !phonePattern.matcher(phone).matches()) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("Mobile Phone")
                        .invalidValue(rawPhone)
                        .reason("Invalid Indian mobile number: " + rawPhone)
                        .suggestedCorrection("Provide a 10-digit Indian mobile number starting with 6, 7, 8, or 9")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            // 9. Validate Pincode (if provided)
            if (pincode != null && !pincodePattern.matcher(pincode).matches()) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("Pincode")
                        .invalidValue(rawPincode)
                        .reason("Invalid Indian postal PIN code: " + rawPincode)
                        .suggestedCorrection("Provide a 6-digit Indian PIN code (e.g. 400001)")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            // 10. Persist Valid Client
            try {
                subscriptionService.checkClientLimit(organizationId);

                ClientEntity client = ClientEntity.builder()
                        .clientType(clientType)
                        .displayName(displayName)
                        .legalName(legalName)
                        .tradeName(tradeName != null ? tradeName : displayName)
                        .pan(pan)
                        .gstin(gstin)
                        .tan(StringUtils.hasText(req.getTan()) ? req.getTan().toUpperCase().trim() : null)
                        .cin(StringUtils.hasText(req.getCin()) ? req.getCin().toUpperCase().trim() : null)
                        .dateOfIncorporation(req.getDateOfIncorporation())
                        .email(email)
                        .phone(phone)
                        .altPhone(StringUtils.hasText(req.getAltPhone()) ? req.getAltPhone().trim() : null)
                        .contactPersonName(contactPerson)
                        .contactPersonDesignation(StringUtils.hasText(req.getContactPersonDesignation()) ? req.getContactPersonDesignation().trim() : null)
                        .addressLine1(addressLine1)
                        .addressLine2(addressLine2)
                        .city(city)
                        .state(state)
                        .country("India")
                        .pincode(pincode)
                        .notes(notes)
                        .status(ClientStatus.ACTIVE)
                        .build();
                client.setOrganizationId(organizationId);

                ClientEntity saved = clientRepository.save(client);
                result.getImportedClients().add(enrichDto(saved));
                result.setTotalSuccess(result.getTotalSuccess() + 1);

                // Track in seen sets and existing sets to avoid intra-batch collisions
                seenPansInFile.add(pan);
                existingPans.add(pan);
                if (gstin != null) {
                    seenGstinsInFile.add(gstin);
                    existingGstins.add(gstin);
                }
            } catch (Exception ex) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(displayName)
                        .pan(pan)
                        .field("Persistence")
                        .invalidValue("")
                        .reason(ex.getMessage())
                        .suggestedCorrection("Check subscription limit or database connectivity")
                        .duplicate(false)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        // Audit Logging
        Map<String, Object> auditSummary = new HashMap<>();
        auditSummary.put("totalProcessed", result.getTotalProcessed());
        auditSummary.put("totalSuccess", result.getTotalSuccess());
        auditSummary.put("totalSkipped", result.getTotalSkipped());
        auditSummary.put("totalFailed", result.getTotalFailed());
        auditService.logEvent("CLIENT_IMPORT_COMPLETED", "CLIENT", organizationId.toString(), null, auditSummary);

        log.info("Completed bulk client import for orgId={}: {} success, {} failed, {} skipped",
                organizationId, result.getTotalSuccess(), result.getTotalFailed(), result.getTotalSkipped());

        return result;
    }

    @Override
    @Transactional
    public void resendPortalInvitation(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        if (!StringUtils.hasText(client.getEmail())) {
            throw new com.taxoryn.core.exception.BadRequestException("Cannot send portal invitation: client has no registered email address");
        }

        List<UserEntity> existingUsers = userRepository.findAllByOrganizationIdAndClientId(organizationId, client.getId());
        UserEntity user;

        if (existingUsers.isEmpty()) {
            // NOT_PROVISIONED case: provision fresh portal user in INVITED state
            user = provisionUserForClient(organizationId, client);
        } else {
            user = existingUsers.get(0);
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new com.taxoryn.core.exception.BadRequestException("Client portal access is already active for this client. If they cannot log in, please guide them to use password recovery.");
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new com.taxoryn.core.exception.BadRequestException("Client portal access is currently suspended for this client. Please restore portal access before resending an invitation.");
            }
            if (user.getStatus() == UserStatus.INACTIVE) {
                throw new com.taxoryn.core.exception.BadRequestException("Client portal access is currently inactive for this client. Please enable portal access before resending an invitation.");
            }
            // If user is INVITED (or PENDING), re-issue invitation
        }

        sendClientPortalInvitation(client, user, organizationId);
    }

    private UserEntity provisionUserForClient(UUID organizationId, ClientEntity client) {
        String email = client.getEmail().trim().toLowerCase();

        // 1. Check if a portal user already exists for this exact organization + client (CASE A)
        List<UserEntity> existingClientUsers = userRepository.findAllByOrganizationIdAndClientId(organizationId, client.getId());
        if (!existingClientUsers.isEmpty()) {
            return existingClientUsers.get(0);
        }

        // 2. Check if an existing user with this email exists within THIS same organization
        Optional<UserEntity> orgUserOpt = userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, email);
        if (orgUserOpt.isPresent()) {
            UserEntity orgUser = orgUserOpt.get();

            // Safety Check (CASE C): If user belongs to another client, reject
            if (orgUser.getClientId() != null && !orgUser.getClientId().equals(client.getId())) {
                log.warn("Cannot attach user {} to client {} because user is already linked to client {}",
                        orgUser.getId(), client.getId(), orgUser.getClientId());
                throw new DuplicateResourceException("This email address is already associated with another client portal account in this practice. Please use the existing account or another email address.");
            }

            // Safety Check (CASE D & E): If user is an internal practice staff/admin or platform user, reject conversion
            boolean isPracticeStaffOrPlatform = orgUser.getRoles() != null && orgUser.getRoles().stream()
                    .anyMatch(r -> !"CLIENT_USER".equals(r.getCode()) && !"CLIENT_ADMIN".equals(r.getCode()) && !"CLIENT_VIEWER".equals(r.getCode()) && !"ROLE_CLIENT_USER".equals(r.getCode()));
            if (isPracticeStaffOrPlatform && orgUser.getClientId() == null) {
                log.warn("Cannot convert practice staff/platform user {} to client portal user for client {}", orgUser.getId(), client.getId());
                throw new DuplicateResourceException("This email address is already registered as an internal practice user. A separate client portal email address is required.");
            }

            // CASE B: Orphan client portal user in the same organization
            if (orgUser.getClientId() == null) {
                orgUser.setClientId(client.getId());
                if (orgUser.getRoles() == null || orgUser.getRoles().isEmpty()) {
                    RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")
                            .or(() -> roleRepository.findByCodeAndOrganizationId("CLIENT_USER", organizationId))
                            .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                                    .code("CLIENT_USER")
                                    .name("Client User")
                                    .isSystemRole(true)
                                    .build()));
                    orgUser.setRoles(new HashSet<>(Set.of(clientRole)));
                }
                UserEntity saved = userRepository.save(orgUser);
                log.info("Reconciled orphan client portal user {} for client {} in tenant {}", saved.getId(), client.getId(), organizationId);
                auditService.logEvent(
                        organizationId,
                        saved.getId(),
                        "CLIENT_PORTAL_USER_RECONCILED",
                        "CLIENT",
                        client.getId().toString(),
                        null,
                        "Reconciled orphan client portal user " + saved.getEmail() + " for client " + client.getDisplayName()
                );
                return saved;
            }
            return orgUser;
        }

        // 3. Safety Check (CASE F): Check if email exists in another organization
        Optional<UserEntity> globalUserOpt = userRepository.findByEmailIgnoreCase(email);
        if (globalUserOpt.isPresent() && !globalUserOpt.get().getOrganizationId().equals(organizationId)) {
            log.warn("Email {} belongs to another organization {}", email, globalUserOpt.get().getOrganizationId());
            throw new DuplicateResourceException("The email address is already registered and cannot be used for this client portal account.");
        }

        // 4. Create fresh CLIENT_USER within this organization
        RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")
                .or(() -> roleRepository.findByCodeAndOrganizationId("CLIENT_USER", organizationId))
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .isSystemRole(true)
                        .build()));

        String contactName = StringUtils.hasText(client.getContactPersonName())
                ? client.getContactPersonName().trim()
                : client.getDisplayName().trim();
        String firstName = contactName;
        String lastName = null;
        if (contactName.contains(" ")) {
            int idx = contactName.lastIndexOf(" ");
            firstName = contactName.substring(0, idx).trim();
            lastName = contactName.substring(idx + 1).trim();
        }

        UserEntity user = UserEntity.builder()
                .organizationId(organizationId)
                .clientId(client.getId())
                .email(email)
                .passwordHash("") // Empty password hash until first-time activation password setup
                .firstName(firstName)
                .lastName(lastName)
                .phone(client.getPhone())
                .status(UserStatus.INVITED)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build();

        try {
            return userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            return userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, email)
                    .filter(u -> u.getClientId() == null || u.getClientId().equals(client.getId()))
                    .map(u -> {
                        if (u.getClientId() == null) {
                            u.setClientId(client.getId());
                            return userRepository.save(u);
                        }
                        return u;
                    })
                    .orElseThrow(() -> new DuplicateResourceException("User already exists with email: '" + email + "'"));
        }
    }

    private void sendClientPortalInvitation(ClientEntity client, UserEntity user, UUID organizationId) {
        if (!StringUtils.hasText(client.getEmail())) {
            return;
        }
        try {
            // Invalidate any existing pending activation tokens for this user
            organizationActivationTokenRepository.invalidateAllPendingTokensForUser(user.getId(), Instant.now());

            // Generate Secure Activation Token (SHA-256 hashed at rest)
            String rawToken = PasswordSecurityUtils.generateSecureToken();
            String tokenHash = PasswordSecurityUtils.hashSha256(rawToken);
            Instant expiresAt = Instant.now().plus(activationExpirationHours, ChronoUnit.HOURS);

            OrganizationActivationTokenEntity activationToken = OrganizationActivationTokenEntity.builder()
                    .userId(user.getId())
                    .organizationId(organizationId)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();
            organizationActivationTokenRepository.save(activationToken);

            String orgName = organizationRepository.findById(organizationId)
                    .map(OrganizationEntity::getName)
                    .orElse("Your Tax Practice");

            String activationUrl = activationBaseUrl + "?token=" + rawToken;

            String recipientName = StringUtils.hasText(client.getContactPersonName())
                    ? client.getContactPersonName()
                    : client.getDisplayName();

            emailNotificationService.sendClientPortalInvitationEmail(
                    client.getEmail(),
                    recipientName,
                    client.getDisplayName(),
                    orgName,
                    activationUrl,
                    activationExpirationHours
            );

            auditService.logEvent(
                    organizationId,
                    user.getId(),
                    "CLIENT_PORTAL_INVITED",
                    "CLIENT",
                    client.getId().toString(),
                    null,
                    "Dispatched client portal invitation email to " + client.getEmail()
            );
        } catch (Exception ex) {
            log.error("Failed to send client portal invitation email to {}: {}", client.getEmail(), ex.getMessage(), ex);
            auditService.logEvent(
                    organizationId,
                    user != null ? user.getId() : null,
                    "CLIENT_PORTAL_INVITATION_FAILED",
                    "CLIENT",
                    client.getId().toString(),
                    null,
                    "Failed to dispatch client portal invitation email: " + ex.getMessage()
            );
        }
    }

    private String normalizeIndianMobile(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return digitsOnly.substring(2);
        } else if (digitsOnly.length() == 11 && digitsOnly.startsWith("0")) {
            return digitsOnly.substring(1);
        } else if (digitsOnly.length() == 10) {
            return digitsOnly;
        }
        return digitsOnly;
    }

    private String normalizePincode(String pincode) {
        if (!StringUtils.hasText(pincode)) return null;
        String digitsOnly = pincode.replaceAll("[^0-9]", "");
        return digitsOnly.length() == 6 ? digitsOnly : digitsOnly;
    }

    private ClientDto enrichDto(ClientEntity client) {
        ClientDto dto = clientMapper.toDto(client);
        if (client.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), client.getOrganizationId())
                    .ifPresent(emp -> dto.setAssignedEmployeeName(emp.getFullName()));
        }

        // Enrich with Client Portal user status
        List<UserEntity> portalUsers = userRepository.findAllByOrganizationIdAndClientId(client.getOrganizationId(), client.getId());
        if (!portalUsers.isEmpty()) {
            UserEntity primaryUser = portalUsers.get(0);
            dto.setPortalUserId(primaryUser.getId());
            dto.setPortalUserEmail(primaryUser.getEmail());
            dto.setPortalStatus(primaryUser.getStatus() != null ? primaryUser.getStatus().name() : "INVITED");
        } else if (StringUtils.hasText(client.getEmail())) {
            dto.setPortalStatus("NOT_PROVISIONED");
        } else {
            dto.setPortalStatus("NO_EMAIL");
        }

        return dto;
    }
}
