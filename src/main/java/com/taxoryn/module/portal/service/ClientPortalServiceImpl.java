package com.taxoryn.module.portal.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.portal.dto.ClientDocumentRequestDto;
import com.taxoryn.module.portal.dto.ClientGstStatusDto;
import com.taxoryn.module.portal.dto.ClientItrStatusDto;
import com.taxoryn.module.portal.dto.ClientNotificationDto;
import com.taxoryn.module.portal.dto.ClientPortalDashboardDto;
import com.taxoryn.module.portal.dto.ClientPortalProfileDto;
import com.taxoryn.module.portal.dto.ClientPortalUserDto;
import com.taxoryn.module.portal.dto.ClientTaskDto;
import com.taxoryn.module.portal.dto.CreateClientDocumentRequest;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.portal.dto.UpdateClientPortalProfileRequest;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus;
import com.taxoryn.module.portal.entity.ClientNotificationEntity;
import com.taxoryn.module.portal.entity.ClientNotificationEntity.NotificationType;
import com.taxoryn.module.portal.mapper.ClientPortalMapper;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.portal.dto.PortalMessageAttachmentDto;
import com.taxoryn.module.portal.entity.MessageContextType;

import com.taxoryn.core.security.PasswordSecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity;
import com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientPortalServiceImpl implements ClientPortalService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationActivationTokenRepository organizationActivationTokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private final ClientNotificationRepository notificationRepository;
    private final ClientDocumentRequestRepository docRequestRepository;
    private final com.taxoryn.module.billing.repository.InvoiceRepository invoiceRepository;
    private final com.taxoryn.module.billing.mapper.InvoiceMapper invoiceMapper;
    private final ClientPortalMapper mapper;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.taxoryn.module.docrequest.service.DocumentRequestService multiItemDocRequestService;
    private final com.taxoryn.module.docrequest.repository.DocumentRequestRepository multiItemDocRequestRepository;
    private final com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository multiItemDocRequestItemRepository;
    private final com.taxoryn.module.user.service.ProfileImageService profileImageService;
    private final com.taxoryn.module.portal.repository.ClientPortalMessageRepository clientPortalMessageRepository;
    private final com.taxoryn.module.portal.websocket.PortalChatEventPublisher portalChatEventPublisher;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ObjectMapper objectMapper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComplianceWorkflowRepository complianceWorkflowRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TaxNoticeRepository taxNoticeRepository;

    @Value("${taxoryn.auth.activation-url:${taxoryn.frontend.activation-url:${taxoryn.auth.activation-base-url:${taxoryn.mail.activation-url:${TAXORYN_ACTIVATION_URL:${taxoryn.frontend-url:${app.frontend-url:${TAXORYN_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}}/activate}}}}}")
    private String activationBaseUrl = "http://localhost:5173/activate";

    @Value("${taxoryn.auth.activation.expiration-hours:24}")
    private long activationExpirationHours = 24L;

    @Override
    @Transactional
    public ClientPortalUserDto registerClientPortalUser(RegisterClientPortalUserRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        if (!SecurityUtils.isClientPortalUser()) {
            validatePracticeClientAccess(client.getId());
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        String roleCode = "CLIENT_ADMIN".equalsIgnoreCase(request.getRole()) ? "CLIENT_ADMIN" : "CLIENT_USER";

        // 1. Check if user already exists within the same organization
        Optional<UserEntity> orgUserOpt = userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, normalizedEmail);
        if (orgUserOpt.isPresent()) {
            UserEntity orgUser = orgUserOpt.get();

            // Check if user is an internal staff/platform user (not client portal user)
            boolean isStaffOrAdmin = orgUser.getRoles() != null && orgUser.getRoles().stream()
                    .anyMatch(r -> !"CLIENT_USER".equalsIgnoreCase(r.getCode())
                            && !"ROLE_CLIENT_USER".equalsIgnoreCase(r.getCode())
                            && !"CLIENT_ADMIN".equalsIgnoreCase(r.getCode())
                            && !"ROLE_CLIENT_ADMIN".equalsIgnoreCase(r.getCode()));
            if (isStaffOrAdmin) {
                throw new DuplicateResourceException("This email address is already registered as an internal practice user. A separate client portal email address is required.");
            }

            // Check if user is linked to another client in the same organization (Case C)
            if (orgUser.getClientId() != null && !orgUser.getClientId().equals(client.getId())) {
                throw new DuplicateResourceException("This email address is already associated with another client portal account in this practice. Please use the existing account or another email address.");
            }

            // Case A: Same Org, Same Client ID
            if (orgUser.getClientId() != null && orgUser.getClientId().equals(client.getId())) {
                log.info("Client portal user already exists for client {} (userId={}, status={})", client.getId(), orgUser.getId(), orgUser.getStatus());
                if (orgUser.getStatus() == UserStatus.INVITED || !StringUtils.hasText(orgUser.getPasswordHash())) {
                    sendClientPortalInvitation(client, orgUser, organizationId);
                }
                return toClientPortalUserDto(orgUser, client, roleCode);
            }

            // Case B: Same Org, Orphan CLIENT_USER (clientId == null)
            if (orgUser.getClientId() == null) {
                log.info("Reconciling orphan client portal user id={} with client id={}", orgUser.getId(), client.getId());
                orgUser.setClientId(client.getId());
                if (StringUtils.hasText(request.getFirstName())) {
                    orgUser.setFirstName(request.getFirstName().trim());
                }
                if (request.getLastName() != null) {
                    orgUser.setLastName(request.getLastName().trim());
                }
                if (StringUtils.hasText(request.getPhone())) {
                    orgUser.setPhone(request.getPhone().trim());
                }
                UserEntity saved = userRepository.save(orgUser);

                auditService.logEvent(
                        organizationId,
                        saved.getId(),
                        "CLIENT_PORTAL_USER_RECONCILED",
                        "CLIENT",
                        client.getId().toString(),
                        null,
                        "Reconciled orphan client portal user " + saved.getEmail() + " for client " + client.getDisplayName()
                );

                if (saved.getStatus() == UserStatus.INVITED || !StringUtils.hasText(saved.getPasswordHash())) {
                    sendClientPortalInvitation(client, saved, organizationId);
                }
                return toClientPortalUserDto(saved, client, roleCode);
            }
        }

        // Case F: Check if email exists in another organization
        Optional<UserEntity> globalUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (globalUserOpt.isPresent() && !globalUserOpt.get().getOrganizationId().equals(organizationId)) {
            log.warn("Email {} belongs to another organization {}", normalizedEmail, globalUserOpt.get().getOrganizationId());
            throw new DuplicateResourceException("The email address is already registered and cannot be used for this client portal account.");
        }

        RoleEntity role = roleRepository.findByCodeAndIsSystemRoleTrue(roleCode)
                .or(() -> roleRepository.findByCodeAndOrganizationId(roleCode, organizationId))
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code(roleCode)
                        .name("Client " + ("CLIENT_ADMIN".equals(roleCode) ? "Administrator" : "User"))
                        .isSystemRole(true)
                        .build()));

        if (StringUtils.hasText(request.getPassword())) {
            com.taxoryn.core.security.PasswordSecurityUtils.validatePassword(request.getPassword());
        }

        String passwordHash = StringUtils.hasText(request.getPassword()) ? passwordEncoder.encode(request.getPassword()) : "";

        UserEntity user = UserEntity.builder()
                .organizationId(organizationId)
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .phone(request.getPhone())
                .clientId(client.getId())
                .status(UserStatus.INVITED)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        UserEntity saved;
        try {
            saved = userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            saved = userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, normalizedEmail)
                    .filter(u -> u.getClientId() == null || u.getClientId().equals(client.getId()))
                    .map(u -> {
                        if (u.getClientId() == null) {
                            u.setClientId(client.getId());
                            return userRepository.save(u);
                        }
                        return u;
                    })
                    .orElseThrow(() -> new DuplicateResourceException("User already exists with email: '" + normalizedEmail + "'"));
        }

        log.info("Registered client portal user in INVITED status: id={}, email={}, clientId={}, role={} in tenant={}",
                saved.getId(), saved.getEmail(), client.getId(), roleCode, organizationId);

        sendClientPortalInvitation(client, saved, organizationId);

        return toClientPortalUserDto(saved, client, roleCode);
    }

    private void sendClientPortalInvitation(ClientEntity client, UserEntity user, UUID organizationId) {
        try {
            organizationActivationTokenRepository.invalidateAllPendingTokensForUser(user.getId(), Instant.now());
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

            String recipientName = StringUtils.hasText(user.getFullName())
                    ? user.getFullName()
                    : (StringUtils.hasText(client.getContactPersonName()) ? client.getContactPersonName() : client.getDisplayName());

            emailNotificationService.sendClientPortalInvitationEmail(
                    user.getEmail(),
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
                    "Dispatched client portal invitation email to " + user.getEmail()
            );
        } catch (Exception ex) {
            log.error("Failed to dispatch portal invitation email for user {}: {}", user.getId(), ex.getMessage(), ex);
            auditService.logEvent(
                    organizationId,
                    user.getId(),
                    "CLIENT_PORTAL_INVITATION_FAILED",
                    "CLIENT",
                    client.getId().toString(),
                    null,
                    "Failed to dispatch client portal invitation email: " + ex.getMessage()
            );
        }
    }

    private ClientPortalUserDto toClientPortalUserDto(UserEntity user, ClientEntity client, String fallbackRoleCode) {
        Set<String> roleCodes = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet())
                : Set.of(fallbackRoleCode != null ? fallbackRoleCode : "CLIENT_USER");

        return ClientPortalUserDto.builder()
                .userId(user.getId())
                .clientId(client.getId())
                .clientName(client.getDisplayName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(roleCodes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientPortalDashboardDto getDashboard() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return buildDashboardDto(clientId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientPortalDashboardDto getDashboardForClient(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (!SecurityUtils.isClientPortalUser()) {
            validatePracticeClientAccess(clientId);
        }
        return buildDashboardDto(clientId, organizationId);
    }

    private ClientPortalDashboardDto buildDashboardDto(UUID clientId, UUID organizationId) {
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        // Assigned Practitioner Info
        String practitionerName = null;
        String practitionerEmail = null;
        String practitionerPhone = null;
        if (client.getAssignedEmployeeId() != null) {
            EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), organizationId).orElse(null);
            if (employee != null) {
                practitionerName = employee.getFullName();
                practitionerEmail = employee.getEmail();
                practitionerPhone = employee.getPhone();
            }
        }

        // Multi-Item Document Requests
        List<com.taxoryn.module.docrequest.dto.DocumentRequestDto> multiItemRequests = multiItemDocRequestRepository
                .findAllByOrganizationIdAndClientIdAndStatusIn(organizationId, clientId,
                        List.of(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT,
                                com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED))
                .stream().map(r -> multiItemDocRequestService.getRequestById(r.getId())).toList();

        long pendingDocItems = 0;
        for (var req : multiItemRequests) {
            if (req.getItems() != null) {
                pendingDocItems += req.getItems().stream()
                        .filter(i -> i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING
                                || i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED)
                        .count();
            }
        }

        // Legacy Counts
        long pendingDocs = docRequestRepository != null
                ? docRequestRepository.countByOrganizationIdAndClientIdAndStatus(organizationId, clientId, RequestStatus.PENDING)
                : 0L;

        // Recent GST
        List<ClientGstStatusDto> gstList = gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(organizationId, clientId)
                .stream().limit(10).map(this::mapGstFiling).toList();

        // Recent ITR
        List<ClientItrStatusDto> itrList = itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(organizationId, clientId)
                .stream().limit(10).map(this::mapItrReturn).toList();

        // Pending Document Requests (Legacy)
        List<ClientDocumentRequestDto> docRequests = (docRequestRepository != null)
                ? mapper.toDocRequestDtoList(docRequestRepository.findAllByOrganizationIdAndClientIdAndStatus(organizationId, clientId, RequestStatus.PENDING))
                : List.of();

        // Client Tasks
        // SECURITY: TaskEntity represents internal practitioner staff tasks and lacks a client visibility flag.
        // Default to denying access to records whose client visibility cannot be established.
        List<ClientTaskDto> clientTasks = taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId)
                .stream()
                .filter(t -> false) // Prevent leakage of internal tasks, notes, and assignments
                .limit(10)
                .map(this::mapClientTask)
                .toList();
        long pendingTasks = (long) clientTasks.size();

        // Recent Notifications
        List<ClientNotificationDto> notifications = mapper.toNotificationDtoList(
                notificationRepository.findTop10ByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId));

        // Billing & Invoices
        List<com.taxoryn.module.billing.entity.InvoiceEntity> clientInvoices = invoiceRepository.findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(organizationId, clientId);
        java.math.BigDecimal outstanding = java.math.BigDecimal.ZERO;
        long unpaidCount = 0;
        for (com.taxoryn.module.billing.entity.InvoiceEntity inv : clientInvoices) {
            if (inv.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.DRAFT && inv.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.CANCELLED) {
                outstanding = outstanding.add(inv.getBalanceDue());
                if (inv.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.PAID) {
                    unpaidCount++;
                }
            }
        }
        List<com.taxoryn.module.billing.dto.InvoiceDto> latestInvoices = clientInvoices.stream()
                .filter(inv -> inv.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.DRAFT)
                .limit(10)
                .map(inv -> {
                    com.taxoryn.module.billing.dto.InvoiceDto dto = invoiceMapper.toDto(inv);
                    dto.setClientName(client.getDisplayName());
                    dto.setClientGstin(client.getGstin());
                    dto.setClientPan(client.getPan());
                    return dto;
                }).toList();

        long pendingActionItems = pendingDocs + pendingDocItems + unpaidCount;

        return ClientPortalDashboardDto.builder()
                .clientId(client.getId())
                .displayName(client.getDisplayName())
                .legalName(client.getLegalName())
                .clientType(client.getClientType())
                .pan(client.getPan())
                .gstin(client.getGstin())
                .tan(client.getTan())
                .assignedPractitionerName(practitionerName)
                .assignedPractitionerEmail(practitionerEmail)
                .assignedPractitionerPhone(practitionerPhone)
                .pendingDocumentsCount(pendingDocs + pendingDocItems)
                .pendingTasksCount(pendingTasks)
                .pendingActionItemsCount(pendingActionItems)
                .activeGstReturnsCount(gstList.size())
                .activeItrReturnsCount(itrList.size())
                .unpaidInvoicesCount(unpaidCount)
                .outstandingBalance(outstanding)
                .latestGstFilings(gstList)
                .latestItrReturns(itrList)
                .pendingDocumentRequests(docRequests)
                .activeMultiItemRequests(multiItemRequests)
                .pendingTasks(clientTasks)
                .recentNotifications(notifications)
                .latestInvoices(latestInvoices)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientPortalProfileDto getProfile() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        ClientPortalProfileDto dto = mapper.toProfileDto(client);

        if (client.getAvatarUrl() != null) {
            dto.setAvatarUrl(profileImageService.resolveAvatarUrl(client.getAvatarUrl()));
        }

        if (client.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), organizationId)
                    .ifPresent(emp -> {
                        dto.setAssignedPractitionerName(emp.getFullName());
                        dto.setAssignedPractitionerEmail(emp.getEmail());
                        dto.setAssignedPractitionerPhone(emp.getPhone());
                    });
        }

        return dto;
    }

    @Override
    @Transactional
    public ClientPortalProfileDto updateProfile(UpdateClientPortalProfileRequest request) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        if (StringUtils.hasText(request.getDisplayName())) client.setDisplayName(request.getDisplayName().trim());
        if (StringUtils.hasText(request.getEmail())) client.setEmail(request.getEmail().trim());
        if (StringUtils.hasText(request.getPhone())) client.setPhone(request.getPhone().trim());
        // Avatar mutations MUST ONLY occur through dedicated /avatar endpoints.
        // Ignore request.getAvatarUrl() to prevent untrusted storage key injection.
        if (StringUtils.hasText(request.getAddressLine1())) client.setAddressLine1(request.getAddressLine1().trim());
        if (StringUtils.hasText(request.getAddressLine2())) client.setAddressLine2(request.getAddressLine2().trim());
        if (StringUtils.hasText(request.getCity())) client.setCity(request.getCity().trim());
        if (StringUtils.hasText(request.getState())) client.setState(request.getState().trim());
        if (StringUtils.hasText(request.getPincode())) client.setPincode(request.getPincode().trim());

        ClientEntity saved = clientRepository.save(client);
        log.info("Updated client profile via portal: id={} in tenant={}", clientId, organizationId);

        return getProfile();
    }

    @Override
    @Transactional
    public ClientPortalProfileDto uploadProfileAvatar(org.springframework.web.multipart.MultipartFile file) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        String storedKey = profileImageService.storeAvatar(organizationId, clientId, "client", file, client.getAvatarUrl());
        client.setAvatarUrl(storedKey);
        ClientEntity saved = clientRepository.save(client);
        log.info("Uploaded avatar for client id={} in tenant={}", clientId, organizationId);

        return getProfile();
    }

    @Override
    @Transactional
    public void deleteProfileAvatar() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        if (StringUtils.hasText(client.getAvatarUrl())) {
            profileImageService.deleteAvatar(client.getAvatarUrl());
            client.setAvatarUrl(null);
            clientRepository.save(client);
        }
        log.info("Deleted avatar for client id={} in tenant={}", clientId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public com.taxoryn.module.user.service.ProfileImageService.AvatarContent getProfileAvatar() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        if (!StringUtils.hasText(client.getAvatarUrl())) {
            throw new ResourceNotFoundException("Client avatar", "clientId", clientId);
        }

        return profileImageService.retrieveAvatar(client.getAvatarUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getProfileAvatarContent() {
        return getProfileAvatar().getData();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientGstStatusDto> getGstStatus() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        return gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(organizationId, clientId)
                .stream().map(this::mapGstFiling).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientItrStatusDto> getItrStatus() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        return itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(organizationId, clientId)
                .stream().map(this::mapItrReturn).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> getClientDocuments() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        return documentService.getClientDocuments(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDocumentRequestDto> getPendingDocuments() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        List<ClientDocumentRequestDto> pendingList = new ArrayList<>();

        // 1. Fetch active canonical multi-item requests and project pending items
        List<com.taxoryn.module.docrequest.entity.DocumentRequestEntity> multiItemRequests = multiItemDocRequestRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);

        for (var req : multiItemRequests) {
            if (req.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.CANCELLED
                    || req.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.COMPLETED) {
                continue;
            }
            List<com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity> items = multiItemDocRequestItemRepository
                    .findAllByRequestIdOrderByCreatedAtAsc(req.getId());
            for (var item : items) {
                if (item.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING
                        || item.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED) {
                    pendingList.add(ClientDocumentRequestDto.builder()
                            .id(item.getId())
                            .clientId(req.getClientId())
                            .documentType(item.getDocumentType())
                            .title(item.getTitle())
                            .description(StringUtils.hasText(item.getDescription()) ? item.getDescription() : req.getPurpose())
                            .dueDate(req.getDueDate())
                            .financialYear(req.getFinancialYear())
                            .assessmentYear(req.getAssessmentYear())
                            .status(item.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED
                                    ? RequestStatus.REJECTED
                                    : RequestStatus.PENDING)
                            .uploadedDocumentId(item.getUploadedDocumentId())
                            .rejectionReason(item.getRejectionReason())
                            .requestId(req.getId())
                            .requestNumber(req.getRequestNumber())
                            .build());
                }
            }
        }

        // 2. Include pending legacy requests for backward compatibility
        if (docRequestRepository != null) {
            List<ClientDocumentRequestDto> legacyList = mapper.toDocRequestDtoList(
                    docRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId));
            if (legacyList != null) {
                pendingList.addAll(legacyList.stream()
                        .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.REJECTED)
                        .toList());
            }
        }

        return pendingList;
    }

    @Override
    @Transactional
    public DocumentDto uploadClientDocument(MultipartFile file, UploadDocumentRequest request, UUID documentRequestId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        // Enforce that upload belongs strictly to the client's own vault
        request.setClientId(clientId);

        DocumentDto uploadedDoc = documentService.uploadDocument(file, request);

        // If fulfilling a specific pending document request, update its status
        if (documentRequestId != null) {
            // Check canonical DocumentRequestItem first
            Optional<com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity> itemOpt = multiItemDocRequestItemRepository
                    .findByIdAndOrganizationIdAndClientId(documentRequestId, organizationId, clientId);
            if (itemOpt.isPresent()) {
                var item = itemOpt.get();
                item.setUploadedDocumentId(uploadedDoc.getId());
                item.setUploadedAt(Instant.now());
                item.setStatus(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.UPLOADED);
                item.setRejectionReason(null);
                multiItemDocRequestItemRepository.save(item);

                var parentReq = item.getRequest();
                if (parentReq != null && (parentReq.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT
                        || parentReq.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.DRAFT)) {
                    parentReq.setStatus(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED);
                    multiItemDocRequestRepository.save(parentReq);
                }
            } else {
                // Check canonical parent DocumentRequest
                Optional<com.taxoryn.module.docrequest.entity.DocumentRequestEntity> reqOpt = multiItemDocRequestRepository
                        .findByIdAndOrganizationIdAndClientId(documentRequestId, organizationId, clientId);
                if (reqOpt.isPresent()) {
                    var parentReq = reqOpt.get();
                    List<com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity> items = multiItemDocRequestItemRepository
                            .findAllByRequestIdOrderByCreatedAtAsc(parentReq.getId());
                    items.stream()
                            .filter(i -> i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING
                                    || i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED)
                            .findFirst()
                            .ifPresent(i -> {
                                i.setUploadedDocumentId(uploadedDoc.getId());
                                i.setUploadedAt(Instant.now());
                                i.setStatus(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.UPLOADED);
                                i.setRejectionReason(null);
                                multiItemDocRequestItemRepository.save(i);
                            });
                    if (parentReq.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT
                            || parentReq.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.DRAFT) {
                        parentReq.setStatus(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED);
                        multiItemDocRequestRepository.save(parentReq);
                    }
                } else if (docRequestRepository != null) {
                    // Fallback to legacy request
                    docRequestRepository.findByIdAndOrganizationId(documentRequestId, organizationId)
                            .ifPresent(req -> {
                                if (Objects.equals(req.getClientId(), clientId)) {
                                    req.setStatus(RequestStatus.SUBMITTED);
                                    req.setUploadedDocumentId(uploadedDoc.getId());
                                    docRequestRepository.save(req);
                                }
                            });
                }
            }
        }

        // Add confirmation notification
        createNotification(organizationId, clientId,
                "Document Received: " + uploadedDoc.getFileName(),
                "Your document for " + uploadedDoc.getDocumentType() + " was successfully uploaded.",
                NotificationType.DOCUMENT_REQUESTED);

        return uploadedDoc;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownloadDto downloadClientDocument(UUID documentId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        DocumentEntity doc = documentRepository.findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (!Objects.equals(doc.getClientId(), clientId)) {
            log.warn("Unauthorized attempt by client {} to download foreign document {}", clientId, documentId);
            throw new ForbiddenException("You are not authorized to download this document");
        }

        return documentService.downloadDocument(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownloadDto previewClientDocument(UUID documentId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        DocumentEntity doc = documentRepository.findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (!Objects.equals(doc.getClientId(), clientId)) {
            log.warn("Unauthorized attempt by client {} to preview foreign document {}", clientId, documentId);
            throw new ForbiddenException("You are not authorized to preview this document");
        }

        return documentService.previewDocument(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public com.taxoryn.module.document.dto.PresignedUrlResponse getClientDocumentDownloadUrl(UUID documentId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        DocumentEntity doc = documentRepository.findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (!Objects.equals(doc.getClientId(), clientId)) {
            log.warn("Unauthorized attempt by client {} to get download URL for foreign document {}", clientId, documentId);
            throw new ForbiddenException("You are not authorized to download this document");
        }

        return documentService.getDocumentDownloadUrl(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientTaskDto> getClientTasks() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        // SECURITY: TaskEntity records represent internal staff tasks and lack a client-visibility flag.
        // Prevent portal users from accessing internal practitioner tasks, assignments, and notes.
        // Default to denying access to records whose client visibility cannot be established.
        return taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId)
                .stream()
                .filter(t -> false) // Prevent leakage of internal tasks, notes, and employee assignments
                .map(this::mapClientTask)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientNotificationDto> getClientNotifications() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        return mapper.toNotificationDtoList(
                notificationRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId));
    }

    @Override
    @Transactional
    public void markNotificationRead(UUID notificationId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        notificationRepository.findByIdAndOrganizationId(notificationId, organizationId)
                .ifPresent(n -> {
                    if (Objects.equals(n.getClientId(), clientId)) {
                        n.setRead(true);
                        notificationRepository.save(n);
                    }
                });
    }

    @Override
    @Transactional
    public ClientDocumentRequestDto requestDocumentFromClient(CreateClientDocumentRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        validatePracticeClientAccess(client.getId());

        // Delegate to canonical multi-item DocumentRequestService
        com.taxoryn.module.docrequest.dto.CreateDocumentRequest canonicalRequest = com.taxoryn.module.docrequest.dto.CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose(request.getTitle())
                .message(request.getDescription())
                .dueDate(request.getDueDate())
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .items(List.of(
                        com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem.builder()
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .documentType(request.getDocumentType())
                                .required(true)
                                .build()
                ))
                .build();

        com.taxoryn.module.docrequest.dto.DocumentRequestDto created = multiItemDocRequestService.createAndSendRequest(canonicalRequest);

        UUID itemId = (created.getItems() != null && !created.getItems().isEmpty())
                ? created.getItems().get(0).getId()
                : created.getId();

        // Also add to client-portal in-app notification feed
        createNotification(organizationId, client.getId(),
                "Document Requested: " + request.getTitle(),
                "Your tax consultant has requested the following document: " + request.getTitle() +
                        (request.getDueDate() != null ? " (Due by: " + request.getDueDate() + ")" : ""),
                NotificationType.DOCUMENT_REQUESTED);

        return ClientDocumentRequestDto.builder()
                .id(itemId)
                .clientId(client.getId())
                .documentType(request.getDocumentType())
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .status(RequestStatus.PENDING)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.taxoryn.module.billing.dto.InvoiceDto> getClientInvoices() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        return invoiceRepository.findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(organizationId, clientId).stream()
                .filter(inv -> inv.getStatus() != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.DRAFT)
                .map(inv -> {
                    com.taxoryn.module.billing.dto.InvoiceDto dto = invoiceMapper.toDto(inv);
                    dto.setClientName(client.getDisplayName());
                    dto.setClientGstin(client.getGstin());
                    dto.setClientPan(client.getPan());
                    if (inv.getItems() != null) {
                        dto.setItems(invoiceMapper.toItemDtoList(inv.getItems()));
                    }
                    if (inv.getPayments() != null) {
                        dto.setPayments(invoiceMapper.toPaymentDtoList(inv.getPayments()));
                    }
                    return dto;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public com.taxoryn.module.billing.dto.InvoiceDto getClientInvoiceById(UUID invoiceId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        com.taxoryn.module.billing.entity.InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (!Objects.equals(invoice.getClientId(), clientId)) {
            throw new ForbiddenException("Access denied: You can only view invoices issued to your account");
        }

        com.taxoryn.module.billing.dto.InvoiceDto dto = invoiceMapper.toDto(invoice);
        dto.setClientName(client.getDisplayName());
        dto.setClientGstin(client.getGstin());
        dto.setClientPan(client.getPan());
        if (invoice.getItems() != null) {
            dto.setItems(invoiceMapper.toItemDtoList(invoice.getItems()));
        }
        if (invoice.getPayments() != null) {
            dto.setPayments(invoiceMapper.toPaymentDtoList(invoice.getPayments()));
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientPortalUserDto> getClientPortalUsers(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        // SECURITY: this endpoint is reachable by CLIENT_ADMIN (a client-portal user), not just
        // internal staff (see ClientPortalController @PreAuthorize). Without this check, a
        // Client Admin for Client A could pass another client's ID in the path and enumerate
        // that other client's portal users' names/emails/roles - a cross-client data leak
        // within the same organization (portal users must only ever see their own client).
        if (SecurityUtils.isClientPortalUser()) {
            UUID callerClientId = SecurityUtils.getCurrentClientId().orElse(null);
            if (!Objects.equals(callerClientId, clientId)) {
                log.warn("Unauthorized attempt by client portal user (clientId={}) to list portal users of foreign client {}",
                        callerClientId, clientId);
                throw new ForbiddenException("Access denied: You can only view users for your own account");
            }
        } else {
            validatePracticeClientAccess(clientId);
        }

        return userRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .map(user -> ClientPortalUserDto.builder()
                        .userId(user.getId())
                        .clientId(client.getId())
                        .clientName(client.getDisplayName())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .roles(user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet()))
                        .build())
                .toList();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void createNotification(UUID organizationId, UUID clientId, String title, String message, NotificationType type) {
        ClientNotificationEntity notification = ClientNotificationEntity.builder()
                .clientId(clientId)
                .title(title)
                .message(message)
                .notificationType(type)
                .read(false)
                .build();
        notification.setOrganizationId(organizationId);
        notificationRepository.save(notification);
    }

    private ClientGstStatusDto mapGstFiling(GstReturnFilingEntity entity) {
        return ClientGstStatusDto.builder()
                .id(entity.getId())
                .returnType(entity.getReturnType().name())
                .returnPeriod(entity.getReturnPeriod())
                .financialYear(entity.getFinancialYear())
                .dueDate(entity.getDueDate())
                .filedDate(entity.getFilingDate())
                .status(entity.getFilingStatus().name())
                .arn(entity.getAcknowledgementNumber())
                .totalTaxPayable(entity.getTotalTaxLiability())
                .itcClaimed(entity.getTotalItcClaimed())
                .build();
    }

    private ClientItrStatusDto mapItrReturn(ItrReturnEntity entity) {
        return ClientItrStatusDto.builder()
                .id(entity.getId())
                .assessmentYear(entity.getAssessmentYear())
                .financialYear(entity.getFinancialYear())
                .itrType(entity.getItrType().name())
                .taxpayerType(entity.getTaxpayerType().name())
                .dueDate(entity.getDueDate())
                .filingDate(entity.getFilingDate())
                .acknowledgementNumber(entity.getAcknowledgementNumber())
                .status(entity.getStatus().name())
                .build();
    }

    private ClientTaskDto mapClientTask(TaskEntity entity) {
        return ClientTaskDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .taskCategory(entity.getTaskCategory() != null ? entity.getTaskCategory().name() : null)
                .status(entity.getStatus().name())
                .priority(entity.getPriority().name())
                .dueDate(entity.getDueDate())
                .build();
    }

    // =========================================================================
    // 10. Client Portal Consultation Messaging & Real-Time Chat
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<com.taxoryn.module.portal.dto.ClientPortalMessageDto> getClientMessages() {
        return getClientMessages(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.taxoryn.module.portal.dto.ClientPortalMessageDto> getClientMessages(MessageContextType contextType, UUID contextId) {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        List<com.taxoryn.module.portal.entity.ClientPortalMessageEntity> list;
        if (contextType != null && contextId != null) {
            list = clientPortalMessageRepository.findAllByOrganizationIdAndClientIdAndContextTypeAndContextIdOrderByCreatedAtAsc(
                    organizationId, clientId, contextType, contextId);
        } else if (contextType != null) {
            list = clientPortalMessageRepository.findAllByOrganizationIdAndClientIdAndContextTypeOrderByCreatedAtAsc(
                    organizationId, clientId, contextType);
        } else {
            list = clientPortalMessageRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtAsc(organizationId, clientId);
        }

        return list.stream()
                .map(this::mapMessageToDto)
                .toList();
    }

    @Override
    @Transactional
    public com.taxoryn.module.portal.dto.ClientPortalMessageDto sendClientMessage(com.taxoryn.module.portal.dto.SendClientPortalMessageRequest request) {
        if (!StringUtils.hasText(request.getMessageBody())) {
            throw new BadRequestException("Message body cannot be blank");
        }

        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        MessageContextType contextType = request.getContextType() != null ? request.getContextType() : MessageContextType.GENERAL;
        UUID contextId = request.getContextId();
        String contextTitle = resolveContextTitle(contextType, contextId, organizationId, clientId);
        if (contextTitle == null && StringUtils.hasText(request.getContextTitle())) {
            contextTitle = request.getContextTitle().trim();
        }

        if (request.getParentMessageId() != null) {
            validateParentMessage(request.getParentMessageId(), organizationId, clientId);
        }

        String attachmentsJson = request.getAttachments() != null && !request.getAttachments().isEmpty()
                ? serializeAttachments(request.getAttachments())
                : request.getAttachmentsJson();

        UUID senderUserId = SecurityUtils.getCurrentUserId();
        UserEntity senderUser = userRepository.findById(senderUserId).orElse(null);
        String senderName = senderUser != null
                ? (StringUtils.hasText(senderUser.getFirstName()) ? (senderUser.getFirstName() + (StringUtils.hasText(senderUser.getLastName()) ? " " + senderUser.getLastName() : "")).trim() : senderUser.getEmail())
                : client.getDisplayName();
        String senderEmail = senderUser != null ? senderUser.getEmail() : client.getEmail();

        com.taxoryn.module.portal.entity.ClientPortalMessageEntity entity = com.taxoryn.module.portal.entity.ClientPortalMessageEntity.builder()
                .clientId(clientId)
                .senderType(com.taxoryn.module.portal.entity.PortalMessageSenderType.CLIENT)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .messageBody(request.getMessageBody().trim())
                .contextType(contextType)
                .contextId(contextId)
                .contextTitle(contextTitle)
                .parentMessageId(request.getParentMessageId())
                .attachmentsJson(attachmentsJson)
                .readByClient(true)
                .readByPractice(false)
                .build();
        entity.setOrganizationId(organizationId);

        com.taxoryn.module.portal.entity.ClientPortalMessageEntity saved = clientPortalMessageRepository.save(entity);
        com.taxoryn.module.portal.dto.ClientPortalMessageDto dto = mapMessageToDto(saved);

        // Broadcast over WebSocket in real time
        portalChatEventPublisher.publishNewMessage(organizationId, clientId, dto);

        // In-app Notification to assigned practitioner / firm staff
        try {
            notificationService.notify(
                    organizationId,
                    null,
                    clientId,
                    com.taxoryn.module.notification.entity.NotificationEntity.NotificationType.CLIENT_MESSAGE_RECEIVED,
                    "New Client Message from " + client.getDisplayName(),
                    senderName + ": " + (request.getMessageBody().length() > 100 ? request.getMessageBody().substring(0, 97) + "..." : request.getMessageBody()),
                    Set.of(com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel.IN_APP),
                    "/portal?tab=messages&clientId=" + clientId,
                    "{\"clientId\":\"" + clientId + "\",\"messageId\":\"" + saved.getId() + "\"}"
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch notification for client message: {}", e.getMessage());
        }

        auditService.logEvent(
                "CLIENT_PORTAL_MESSAGE_SENT",
                "CLIENT_PORTAL_MESSAGE",
                saved.getId().toString(),
                organizationId,
                "Client " + client.getDisplayName() + " sent consultation message"
        );

        return dto;
    }

    @Override
    @Transactional
    public void markMessagesReadByClient() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        clientPortalMessageRepository.markAllReadByClient(organizationId, clientId, Instant.now());
        portalChatEventPublisher.publishMessagesRead(organizationId, clientId, "CLIENT");
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountForClient() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return clientPortalMessageRepository.countByOrganizationIdAndClientIdAndReadByClientFalse(organizationId, clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.taxoryn.module.portal.dto.ClientPortalMessageDto> getMessagesForClient(UUID clientId) {
        return getMessagesForClient(clientId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.taxoryn.module.portal.dto.ClientPortalMessageDto> getMessagesForClient(UUID clientId, MessageContextType contextType, UUID contextId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validatePracticeClientAccess(client.getId());

        List<com.taxoryn.module.portal.entity.ClientPortalMessageEntity> list;
        if (contextType != null && contextId != null) {
            list = clientPortalMessageRepository.findAllByOrganizationIdAndClientIdAndContextTypeAndContextIdOrderByCreatedAtAsc(
                    organizationId, clientId, contextType, contextId);
        } else if (contextType != null) {
            list = clientPortalMessageRepository.findAllByOrganizationIdAndClientIdAndContextTypeOrderByCreatedAtAsc(
                    organizationId, clientId, contextType);
        } else {
            list = clientPortalMessageRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtAsc(organizationId, clientId);
        }

        return list.stream()
                .map(this::mapMessageToDto)
                .toList();
    }

    @Override
    @Transactional
    public com.taxoryn.module.portal.dto.ClientPortalMessageDto sendPracticeMessageToClient(UUID clientId, com.taxoryn.module.portal.dto.SendClientPortalMessageRequest request) {
        if (!StringUtils.hasText(request.getMessageBody())) {
            throw new BadRequestException("Message body cannot be blank");
        }

        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validatePracticeClientAccess(client.getId());

        MessageContextType contextType = request.getContextType() != null ? request.getContextType() : MessageContextType.GENERAL;
        UUID contextId = request.getContextId();
        String contextTitle = resolveContextTitle(contextType, contextId, organizationId, clientId);
        if (contextTitle == null && StringUtils.hasText(request.getContextTitle())) {
            contextTitle = request.getContextTitle().trim();
        }

        if (request.getParentMessageId() != null) {
            validateParentMessage(request.getParentMessageId(), organizationId, clientId);
        }

        String attachmentsJson = request.getAttachments() != null && !request.getAttachments().isEmpty()
                ? serializeAttachments(request.getAttachments())
                : request.getAttachmentsJson();

        UUID senderUserId = SecurityUtils.getCurrentUserId();
        UserEntity senderUser = userRepository.findById(senderUserId).orElse(null);
        String senderName = senderUser != null
                ? (StringUtils.hasText(senderUser.getFirstName()) ? (senderUser.getFirstName() + (StringUtils.hasText(senderUser.getLastName()) ? " " + senderUser.getLastName() : "")).trim() : senderUser.getEmail())
                : "Tax Consultant";
        String senderEmail = senderUser != null ? senderUser.getEmail() : SecurityUtils.getCurrentUserEmail();

        com.taxoryn.module.portal.entity.ClientPortalMessageEntity entity = com.taxoryn.module.portal.entity.ClientPortalMessageEntity.builder()
                .clientId(clientId)
                .senderType(com.taxoryn.module.portal.entity.PortalMessageSenderType.PRACTICE)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .messageBody(request.getMessageBody().trim())
                .contextType(contextType)
                .contextId(contextId)
                .contextTitle(contextTitle)
                .parentMessageId(request.getParentMessageId())
                .attachmentsJson(attachmentsJson)
                .readByClient(false)
                .readByPractice(true)
                .build();
        entity.setOrganizationId(organizationId);

        com.taxoryn.module.portal.entity.ClientPortalMessageEntity saved = clientPortalMessageRepository.save(entity);
        com.taxoryn.module.portal.dto.ClientPortalMessageDto dto = mapMessageToDto(saved);

        // Broadcast over WebSocket in real time
        portalChatEventPublisher.publishNewMessage(organizationId, clientId, dto);

        // In-App Notification to Client
        try {
            notificationService.notify(
                    organizationId,
                    null,
                    clientId,
                    com.taxoryn.module.notification.entity.NotificationEntity.NotificationType.PRACTICE_MESSAGE_REPLY,
                    "New Message from your Tax Consultant",
                    senderName + ": " + (request.getMessageBody().length() > 100 ? request.getMessageBody().substring(0, 97) + "..." : request.getMessageBody()),
                    Set.of(com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel.IN_APP),
                    "/portal?tab=messages",
                    "{\"clientId\":\"" + clientId + "\",\"messageId\":\"" + saved.getId() + "\"}"
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch notification for practice message: {}", e.getMessage());
        }

        auditService.logEvent(
                "PRACTICE_PORTAL_MESSAGE_SENT",
                "CLIENT_PORTAL_MESSAGE",
                saved.getId().toString(),
                organizationId,
                "Consultant " + senderName + " sent message to client " + client.getDisplayName()
        );

        return dto;
    }

    @Override
    @Transactional
    public void markMessagesReadByPractice(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validatePracticeClientAccess(clientId);
        clientPortalMessageRepository.markAllReadByPractice(organizationId, clientId, Instant.now());
        portalChatEventPublisher.publishMessagesRead(organizationId, clientId, "PRACTICE");
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountForPractice(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validatePracticeClientAccess(clientId);
        return clientPortalMessageRepository.countByOrganizationIdAndClientIdAndReadByPracticeFalse(organizationId, clientId);
    }

    @Override
    @Transactional
    public PortalMessageAttachmentDto uploadMessageAttachment(MultipartFile file, UUID targetClientId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File payload cannot be empty");
        }

        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID clientId;
        if (SecurityUtils.isClientPortalUser()) {
            clientId = SecurityUtils.requireCurrentClientId();
        } else {
            if (targetClientId == null) {
                throw new BadRequestException("Target client ID is required for practitioner uploads");
            }
            validatePracticeClientAccess(targetClientId);
            clientId = targetClientId;
        }

        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        var uploadReq = com.taxoryn.module.document.dto.UploadDocumentRequest.builder()
                .clientId(clientId)
                .documentType(com.taxoryn.module.document.entity.DocumentEntity.DocumentType.OTHER)
                .notes("Consultation message attachment")
                .build();

        var docDto = documentService.uploadDocument(file, uploadReq);

        return PortalMessageAttachmentDto.builder()
                .documentId(docDto.getId())
                .fileName(docDto.getFileName())
                .fileSize(docDto.getFileSize())
                .contentType(docDto.getContentType())
                .downloadUrl("/api/v1/portal/documents/" + docDto.getId() + "/download")
                .build();
    }

    private String resolveContextTitle(MessageContextType contextType, UUID contextId, UUID organizationId, UUID clientId) {
        if (contextType == null || contextType == MessageContextType.GENERAL || contextId == null) {
            return null;
        }
        return switch (contextType) {
            case DOCUMENT_REQUEST -> {
                var req = multiItemDocRequestRepository.findByIdAndOrganizationId(contextId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Document request", "id", contextId));
                if (!clientId.equals(req.getClientId())) {
                    throw new BadRequestException("Document request does not belong to the active client.");
                }
                yield req.getRequestNumber() + (StringUtils.hasText(req.getPurpose()) ? ": " + req.getPurpose() : "");
            }
            case DOCUMENT_ITEM -> {
                var item = multiItemDocRequestItemRepository.findByIdAndOrganizationId(contextId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Document request item", "id", contextId));
                if (!clientId.equals(item.getClientId())) {
                    throw new BadRequestException("Document request item does not belong to the active client.");
                }
                String reqNum = item.getRequest() != null ? item.getRequest().getRequestNumber() + " / " : "";
                yield reqNum + item.getTitle();
            }
            case COMPLIANCE_WORKFLOW -> {
                if (complianceWorkflowRepository != null) {
                    var wf = complianceWorkflowRepository.findByIdAndOrganizationId(contextId, organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("Compliance workflow", "id", contextId));
                    if (!clientId.equals(wf.getClientId())) {
                        throw new BadRequestException("Compliance workflow does not belong to the active client.");
                    }
                    yield "Compliance Workflow (" + wf.getWorkflowStatus() + ")";
                }
                yield "Compliance Workflow";
            }
            case NOTICE -> {
                if (taxNoticeRepository != null) {
                    var notice = taxNoticeRepository.findByIdAndOrganizationId(contextId, organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("Tax notice", "id", contextId));
                    if (!clientId.equals(notice.getClientId())) {
                        throw new BadRequestException("Tax notice does not belong to the active client.");
                    }
                    yield "Notice: " + (StringUtils.hasText(notice.getNoticeNumber()) ? notice.getNoticeNumber() : notice.getId().toString());
                }
                yield "Tax Notice";
            }
            default -> null;
        };
    }

    private void validateParentMessage(UUID parentMessageId, UUID organizationId, UUID clientId) {
        var parent = clientPortalMessageRepository.findByIdAndOrganizationId(parentMessageId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent message", "id", parentMessageId));
        if (!clientId.equals(parent.getClientId())) {
            throw new BadRequestException("Parent message does not belong to this client conversation.");
        }
    }

    private ObjectMapper getObjectMapper() {
        return objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    private List<PortalMessageAttachmentDto> parseAttachments(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return getObjectMapper().readValue(json, new TypeReference<List<PortalMessageAttachmentDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse attachmentsJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serializeAttachments(List<PortalMessageAttachmentDto> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return getObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to serialize attachments: {}", e.getMessage());
            return null;
        }
    }

    private com.taxoryn.module.portal.dto.ClientPortalMessageDto mapMessageToDto(com.taxoryn.module.portal.entity.ClientPortalMessageEntity entity) {
        if (entity == null) return null;
        List<PortalMessageAttachmentDto> attachments = parseAttachments(entity.getAttachmentsJson());
        return com.taxoryn.module.portal.dto.ClientPortalMessageDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .senderType(entity.getSenderType())
                .senderUserId(entity.getSenderUserId())
                .senderName(entity.getSenderName())
                .senderEmail(entity.getSenderEmail())
                .messageBody(entity.getMessageBody())
                .contextType(entity.getContextType())
                .contextId(entity.getContextId())
                .contextTitle(entity.getContextTitle())
                .parentMessageId(entity.getParentMessageId())
                .attachments(attachments)
                .attachmentsJson(entity.getAttachmentsJson())
                .isReadByClient(entity.isReadByClient())
                .isReadByPractice(entity.isReadByPractice())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validatePracticeClientAccess(UUID clientId) {
        if (clientId == null) return;
        if (securityScopeEvaluator != null) {
            com.taxoryn.core.security.PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            if (scope != null && !scope.isFirmAdmin()) {
                Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (accessibleClientIds == null || !accessibleClientIds.contains(clientId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Access denied: You do not have permission to access messages for this client.");
                }
            }
        }
    }
}
