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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        return dto;
    }
}
