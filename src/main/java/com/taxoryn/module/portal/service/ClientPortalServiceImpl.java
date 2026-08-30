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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientPortalServiceImpl implements ClientPortalService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
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
    private final com.taxoryn.module.docrequest.service.DocumentRequestService multiItemDocRequestService;
    private final com.taxoryn.module.docrequest.repository.DocumentRequestRepository multiItemDocRequestRepository;
    private final com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository multiItemDocRequestItemRepository;

    @Override
    @Transactional
    public ClientPortalUserDto registerClientPortalUser(RegisterClientPortalUserRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("User", "email", normalizedEmail);
        }

        String roleCode = "CLIENT_ADMIN".equalsIgnoreCase(request.getRole()) ? "CLIENT_ADMIN" : "CLIENT_USER";
        RoleEntity role = roleRepository.findByCodeAndIsSystemRoleTrue(roleCode)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code(roleCode)
                        .name("Client " + ("CLIENT_ADMIN".equals(roleCode) ? "Administrator" : "User"))
                        .isSystemRole(true)
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .phone(request.getPhone())
                .clientId(client.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        user.setOrganizationId(organizationId);

        UserEntity saved = userRepository.save(user);
        log.info("Registered client portal user: id={}, email={}, clientId={}, role={} in tenant={}",
                saved.getId(), saved.getEmail(), client.getId(), roleCode, organizationId);

        return ClientPortalUserDto.builder()
                .userId(saved.getId())
                .clientId(client.getId())
                .clientName(client.getDisplayName())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .fullName(saved.getFullName())
                .phone(saved.getPhone())
                .roles(Set.of(roleCode))
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
                .stream().map(r -> multiItemDocRequestService.getClientPortalRequestById(r.getId())).toList();

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
        long pendingDocs = docRequestRepository.countByOrganizationIdAndClientIdAndStatus(organizationId, clientId, RequestStatus.PENDING);
        long pendingTasks = taskRepository.countByOrganizationIdAndClientIdAndStatusNot(organizationId, clientId, TaskStatus.COMPLETED);

        // Recent GST
        List<ClientGstStatusDto> gstList = gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(organizationId, clientId)
                .stream().limit(10).map(this::mapGstFiling).toList();

        // Recent ITR
        List<ClientItrStatusDto> itrList = itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(organizationId, clientId)
                .stream().limit(10).map(this::mapItrReturn).toList();

        // Pending Document Requests (Legacy)
        List<ClientDocumentRequestDto> docRequests = mapper.toDocRequestDtoList(
                docRequestRepository.findAllByOrganizationIdAndClientIdAndStatus(organizationId, clientId, RequestStatus.PENDING));

        // Client Tasks
        List<ClientTaskDto> clientTasks = taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId)
                .stream().limit(10).map(this::mapClientTask).toList();

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

        if (StringUtils.hasText(request.getEmail())) client.setEmail(request.getEmail().trim());
        if (StringUtils.hasText(request.getPhone())) client.setPhone(request.getPhone().trim());
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

        return mapper.toDocRequestDtoList(
                docRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId));
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
            docRequestRepository.findByIdAndOrganizationId(documentRequestId, organizationId)
                    .ifPresent(req -> {
                        if (Objects.equals(req.getClientId(), clientId)) {
                            req.setStatus(RequestStatus.SUBMITTED);
                            req.setUploadedDocumentId(uploadedDoc.getId());
                            docRequestRepository.save(req);
                        }
                    });
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
    public List<ClientTaskDto> getClientTasks() {
        UUID clientId = SecurityUtils.requireCurrentClientId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        return taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId)
                .stream().map(this::mapClientTask).toList();
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

        ClientDocumentRequestEntity entity = ClientDocumentRequestEntity.builder()
                .clientId(client.getId())
                .documentType(request.getDocumentType())
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .status(RequestStatus.PENDING)
                .build();
        entity.setOrganizationId(organizationId);

        ClientDocumentRequestEntity saved = docRequestRepository.save(entity);

        // Notify client (client-portal notification feed)
        createNotification(organizationId, client.getId(),
                "Document Requested: " + request.getTitle(),
                "Your tax consultant has requested the following document: " + request.getTitle() +
                        (request.getDueDate() != null ? " (Due by: " + request.getDueDate() + ")" : ""),
                NotificationType.DOCUMENT_REQUESTED);

        // Also raise it through the central multi-channel notification engine so the client
        // is optionally reached over email/SMS/WhatsApp in addition to the in-app portal feed.
        try {
            notificationService.notify(
                    organizationId, null, client.getId(),
                    com.taxoryn.module.notification.entity.NotificationEntity.NotificationType.DOCUMENT_REQUIRED,
                    "Document Requested: " + request.getTitle(),
                    "Your tax consultant has requested the following document: " + request.getTitle() +
                            (request.getDueDate() != null ? " (Due by: " + request.getDueDate() + ")" : ""),
                    Set.of(
                            com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel.IN_APP,
                            com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel.EMAIL
                    ),
                    "/portal/documents/requests/" + saved.getId(),
                    "{\"documentRequestId\":\"" + saved.getId() + "\"}"
            );
        } catch (Exception ex) {
            log.error("Failed to raise DOCUMENT_REQUIRED notification for request {}: {}", saved.getId(), ex.getMessage(), ex);
        }

        return mapper.toDocRequestDto(saved);
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
}
