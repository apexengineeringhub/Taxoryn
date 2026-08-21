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
import com.taxoryn.module.client.dto.ClientOverviewDto.ClientTaskSummary;
import com.taxoryn.module.client.dto.ClientOverviewDto.StatutoryDetails;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientNoteEntity;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientNoteRepository clientNoteRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final ClientMapper clientMapper;
    private final TaskMapper taskMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;

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
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (!scope.isFirmAdmin()) {
            Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            boolean isAssigned = (client.getAssignedEmployeeId() != null && scope.getAccessibleAssigneeIds() != null && scope.getAccessibleAssigneeIds().contains(client.getAssignedEmployeeId()));
            if (!isAssigned && (accessibleClientIds == null || !accessibleClientIds.contains(clientId))) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission to view clients outside your assigned department or portfolio.");
            }
        }

        return enrichDto(client);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClientDto> getClients(ClientFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
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

        ClientStatus oldStatus = client.getStatus();
        client.setStatus(request.getStatus());
        ClientEntity saved = clientRepository.save(client);
        log.info("Updated client status: id={}, newStatus={} for tenant={}", clientId, request.getStatus(), organizationId);
        ClientDto result = enrichDto(saved);
        auditService.logEvent("CLIENT_STATUS_UPDATED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, request.getStatus().name());
        return result;
    }

    @Override
    @Transactional
    public ClientDto assignEmployee(UUID clientId, AssignClientEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

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

        ClientStatus oldStatus = client.getStatus();
        client.setStatus(ClientStatus.ARCHIVED);
        clientRepository.save(client);
        log.info("Archived client: id={} for tenant={}", clientId, organizationId);
        auditService.logEvent("CLIENT_DELETED", "CLIENT", clientId.toString(), oldStatus != null ? oldStatus.name() : null, ClientStatus.ARCHIVED.name());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientOverviewDto getClientOverview(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (!scope.isFirmAdmin()) {
            Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            boolean isAssigned = (client.getAssignedEmployeeId() != null && scope.getAccessibleAssigneeIds() != null && scope.getAccessibleAssigneeIds().contains(client.getAssignedEmployeeId()));
            if (!isAssigned && (accessibleClientIds == null || !accessibleClientIds.contains(clientId))) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission to view clients outside your assigned department or portfolio.");
            }
        }

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

        // 2. Task Summary (scoped to staff deliverables if staff)
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
        long pendingTasks = taskList.stream().filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.UNDER_REVIEW).count();
        long overdueTasks = taskList.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED).count();
        List<TaskDto> recentTasks = taskMapper.toDtoList(taskList.stream().limit(5).toList());

        ClientTaskSummary taskSummary = ClientTaskSummary.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .overdueTasks(overdueTasks)
                .completedTasks(completedTasks)
                .recentTasks(recentTasks)
                .build();

        // 3. Compliance Summary
        ClientComplianceSummary complianceSummary = ClientComplianceSummary.builder()
                .gstStatus(StringUtils.hasText(client.getGstin()) ? "GST Active (Filing on schedule)" : "Not Registered for GST")
                .itrStatus(StringUtils.hasText(client.getPan()) ? "ITR Computation Ready" : "PAN Required for ITR")
                .tdsStatus(StringUtils.hasText(client.getTan()) ? "TAN Registered" : "No TAN Record")
                .accountingStatus("Active Financial Year 2024-25")
                .build();

        // 4. Documents Summary
        ClientDocumentSummary documentSummary = ClientDocumentSummary.builder()
                .totalDocuments(0)
                .documentCategories(List.of("GST Invoices", "ITR Computations", "Audit Reports", "Statutory Certificates"))
                .build();

        // 5. Billing Summary - ZERO TRUST: Null/Redacted for non-billing staff
        ClientBillingSummary billingSummary = null;
        if (securityScopeEvaluator.hasBillingAccess(scope)) {
            billingSummary = ClientBillingSummary.builder()
                    .totalInvoiced(0.0)
                    .totalPaid(0.0)
                    .outstandingBalance(0.0)
                    .currency("INR")
                    .build();
        }

        // 6. Recent Notes
        List<ClientNoteEntity> noteEntities = clientNoteRepository.findTop10ByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        List<ClientNoteDto> recentNotes = clientMapper.toNoteDtoList(noteEntities);

        return ClientOverviewDto.builder()
                .client(clientDto)
                .statutory(statutory)
                .taskSummary(taskSummary)
                .complianceSummary(complianceSummary)
                .documentsSummary(documentSummary)
                .billingSummary(billingSummary)
                .recentNotes(recentNotes)
                .build();
    }

    @Override
    @Transactional
    public ClientNoteDto addClientNote(UUID clientId, CreateClientNoteRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

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
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

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

        int rowNum = 1;
        for (CreateClientRequest req : requests) {
            rowNum++;
            if (!StringUtils.hasText(req.getDisplayName())) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName("Unknown")
                        .pan(req.getPan())
                        .reason("Display name / Business name is required")
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            if (!StringUtils.hasText(req.getPan())) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(req.getDisplayName())
                        .pan("MISSING")
                        .reason("PAN number is required")
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            String pan = req.getPan().toUpperCase().trim();
            if (!pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(req.getDisplayName())
                        .pan(pan)
                        .reason("Invalid PAN format (expected 5 letters, 4 digits, 1 letter)")
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            if (clientRepository.existsByOrganizationIdAndPan(organizationId, pan)) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(req.getDisplayName())
                        .pan(pan)
                        .reason("Duplicate client with PAN " + pan + " already exists")
                        .build());
                result.setTotalSkipped(result.getTotalSkipped() + 1);
                continue;
            }

            String gstin = StringUtils.hasText(req.getGstin()) ? req.getGstin().toUpperCase().trim() : null;
            if (gstin != null && !gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(req.getDisplayName())
                        .pan(pan)
                        .reason("Invalid GSTIN format: " + gstin)
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
                continue;
            }

            try {
                subscriptionService.checkClientLimit(organizationId);

                ClientEntity client = ClientEntity.builder()
                        .clientType(req.getClientType() != null ? req.getClientType() : ClientEntity.ClientType.PRIVATE_LIMITED)
                        .displayName(req.getDisplayName().trim())
                        .legalName(StringUtils.hasText(req.getLegalName()) ? req.getLegalName().trim() : null)
                        .tradeName(StringUtils.hasText(req.getTradeName()) ? req.getTradeName().trim() : null)
                        .pan(pan)
                        .gstin(gstin)
                        .email(StringUtils.hasText(req.getEmail()) ? req.getEmail().trim() : null)
                        .phone(StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null)
                        .city(StringUtils.hasText(req.getCity()) ? req.getCity().trim() : null)
                        .state(StringUtils.hasText(req.getState()) ? req.getState().trim() : null)
                        .pincode(StringUtils.hasText(req.getPincode()) ? req.getPincode().trim() : null)
                        .status(ClientStatus.ACTIVE)
                        .build();
                client.setOrganizationId(organizationId);

                ClientEntity saved = clientRepository.save(client);
                result.getImportedClients().add(enrichDto(saved));
                result.setTotalSuccess(result.getTotalSuccess() + 1);
            } catch (Exception ex) {
                result.getErrors().add(com.taxoryn.module.client.dto.BulkImportResultDto.BulkImportError.builder()
                        .rowNumber(rowNum)
                        .clientName(req.getDisplayName())
                        .pan(pan)
                        .reason(ex.getMessage())
                        .build());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        log.info("Completed bulk client import for orgId={}: {} success, {} failed, {} skipped",
                organizationId, result.getTotalSuccess(), result.getTotalFailed(), result.getTotalSkipped());

        return result;
    }

    private ClientDto enrichDto(ClientEntity client) {
        ClientDto dto = clientMapper.toDto(client);
        if (client.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), client.getOrganizationId())
                    .ifPresent(emp -> dto.setAssignedEmployeeName(emp.getFullName()));
        }
        return dto;
    }
}
