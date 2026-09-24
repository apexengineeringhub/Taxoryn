package com.taxoryn.module.compliance.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkItemDto;
import com.taxoryn.module.compliance.dto.CreateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceWorkItemEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import com.taxoryn.module.compliance.entity.ComplianceWorkType;
import com.taxoryn.module.compliance.repository.ComplianceWorkItemRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceWorkItemServiceImpl implements ComplianceWorkItemService {

    private final ComplianceWorkItemRepository workItemRepository;
    private final ClientRepository clientRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final EmployeeRepository employeeRepository;
    private final ModuleConfigurationService moduleConfigurationService;
    private final AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Override
    @Transactional
    public ComplianceWorkItemDto createComplianceWorkItem(CreateComplianceWorkItemRequest request) {
        UUID organizationId = requireOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        if (request.getClientId() == null) {
            throw new BusinessValidationException("Client ID is required");
        }
        if (request.getClientServiceId() == null) {
            throw new BusinessValidationException("Client Service ID is required");
        }
        if (request.getWorkType() == null) {
            throw new BusinessValidationException("Work Type is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isBlank()) {
            throw new BusinessValidationException("Title is required");
        }

        // 1. Validate Client & Portfolio Scope
        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
        validateClientAccess(client.getId());

        // 2. Validate Client Service
        ClientServiceEntity clientService = clientServiceRepository
                .findByIdAndOrganizationIdAndClientId(request.getClientServiceId(), organizationId, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", request.getClientServiceId()));

        // 3. Validate Module Entitlement
        validateEntitlement(organizationId, request.getWorkType(), clientService);

        // 4. Validate Assignees
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }
        if (request.getReviewerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getReviewerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer Employee", "id", request.getReviewerEmployeeId()));
        }

        // 5. Validate Dates
        if (request.getStatutoryDueDate() != null && request.getInternalTargetDate() != null
                && request.getInternalTargetDate().isAfter(request.getStatutoryDueDate())) {
            throw new BusinessValidationException("Internal target date cannot be after the statutory due date");
        }

        ComplianceWorkItemEntity entity = ComplianceWorkItemEntity.builder()
                .clientId(client.getId())
                .clientServiceId(clientService.getId())
                .workType(request.getWorkType())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .compliancePeriod(request.getCompliancePeriod())
                .status(ComplianceWorkStatus.NOT_STARTED)
                .statutoryDueDate(request.getStatutoryDueDate())
                .internalTargetDate(request.getInternalTargetDate())
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .reviewerEmployeeId(request.getReviewerEmployeeId())
                .build();
        entity.setOrganizationId(organizationId);

        ComplianceWorkItemEntity saved = workItemRepository.save(entity);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_WORK_CREATED",
                "COMPLIANCE_WORK_ITEM",
                saved.getId().toString(),
                null,
                "Created compliance work item '" + saved.getTitle() + "' for client " + client.getDisplayName()
        );

        return mapToDto(saved, client, clientService);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceWorkItemDto getComplianceWorkItemById(UUID id) {
        UUID organizationId = requireOrganizationId();

        ComplianceWorkItemEntity entity = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkItem", "id", id));

        validateWorkItemAccess(entity);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null);
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(entity.getClientServiceId(), organizationId).orElse(null);

        return mapToDto(entity, client, service);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ComplianceWorkItemDto> getComplianceWorkItems(ComplianceWorkFilterRequest filterRequest) {
        UUID organizationId = requireOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        if (accessibleClientIds != null && accessibleClientIds.isEmpty()) {
            return PagedResponse.<ComplianceWorkItemDto>builder()
                    .content(Collections.emptyList())
                    .pageNumber(filterRequest.getPage())
                    .pageSize(filterRequest.getSize())
                    .totalElements(0)
                    .totalPages(0)
                    .isLast(true)
                    .build();
        }

        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(filterRequest.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "statutoryDueDate"
        );
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), sort);

        Specification<ComplianceWorkItemEntity> spec = createSpecification(organizationId, filterRequest, accessibleClientIds, scope);
        Page<ComplianceWorkItemEntity> page = workItemRepository.findAll(spec, pageable);

        Map<UUID, ClientEntity> clientMap = loadClients(organizationId, page.getContent());
        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, page.getContent());
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        List<ComplianceWorkItemDto> dtos = page.getContent().stream()
                .map(item -> mapToDtoWithCache(item, clientMap.get(item.getClientId()), serviceMap.get(item.getClientServiceId()), employeeMap))
                .toList();

        return PagedResponse.<ComplianceWorkItemDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceWorkItemDto> getComplianceWorkItemsByClient(UUID clientId) {
        UUID organizationId = requireOrganizationId();
        validateClientAccess(clientId);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        List<ComplianceWorkItemEntity> items = workItemRepository
                .findAllByOrganizationIdAndClientIdOrderByStatutoryDueDateAsc(organizationId, clientId);

        Map<UUID, ClientServiceEntity> serviceMap = loadServices(organizationId, items);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return items.stream()
                .map(item -> mapToDtoWithCache(item, client, serviceMap.get(item.getClientServiceId()), employeeMap))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceWorkItemDto> getComplianceWorkItemsByService(UUID serviceId) {
        UUID organizationId = requireOrganizationId();

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(serviceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", serviceId));

        validateClientAccess(service.getClientId());

        ClientEntity client = clientRepository.findByIdAndOrganizationId(service.getClientId(), organizationId).orElse(null);
        List<ComplianceWorkItemEntity> items = workItemRepository.findAllByOrganizationIdAndClientServiceId(organizationId, serviceId);
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(organizationId);

        return items.stream()
                .map(item -> mapToDtoWithCache(item, client, service, employeeMap))
                .toList();
    }

    @Override
    @Transactional
    public ComplianceWorkItemDto updateComplianceWorkItem(UUID id, UpdateComplianceWorkItemRequest request) {
        UUID organizationId = requireOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceWorkItemEntity entity = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkItem", "id", id));

        validateWorkItemAccess(entity);

        // 1. Validate Target Status if changed
        if (request.getStatus() != null && request.getStatus() != entity.getStatus()) {
            if (!entity.getStatus().canTransitionTo(request.getStatus())) {
                throw new BusinessValidationException("Invalid status transition from " + entity.getStatus() + " to " + request.getStatus());
            }
            entity.setStatus(request.getStatus());
            updateTimestampsForStatus(entity, request.getStatus());
        }

        // 2. Validate Assignees
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
            entity.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }
        if (request.getReviewerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getReviewerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer Employee", "id", request.getReviewerEmployeeId()));
            entity.setReviewerEmployeeId(request.getReviewerEmployeeId());
        }

        // 3. Validate Dates
        LocalDate dueDate = request.getStatutoryDueDate() != null ? request.getStatutoryDueDate() : entity.getStatutoryDueDate();
        LocalDate targetDate = request.getInternalTargetDate() != null ? request.getInternalTargetDate() : entity.getInternalTargetDate();
        if (dueDate != null && targetDate != null && targetDate.isAfter(dueDate)) {
            throw new BusinessValidationException("Internal target date cannot be after the statutory due date");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isBlank()) {
            entity.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getWorkType() != null) {
            entity.setWorkType(request.getWorkType());
        }
        if (request.getFinancialYear() != null) {
            entity.setFinancialYear(request.getFinancialYear());
        }
        if (request.getAssessmentYear() != null) {
            entity.setAssessmentYear(request.getAssessmentYear());
        }
        if (request.getCompliancePeriod() != null) {
            entity.setCompliancePeriod(request.getCompliancePeriod());
        }
        if (request.getStatutoryDueDate() != null) {
            entity.setStatutoryDueDate(request.getStatutoryDueDate());
        }
        if (request.getInternalTargetDate() != null) {
            entity.setInternalTargetDate(request.getInternalTargetDate());
        }

        ComplianceWorkItemEntity saved = workItemRepository.save(entity);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_WORK_UPDATED",
                "COMPLIANCE_WORK_ITEM",
                saved.getId().toString(),
                null,
                "Updated compliance work item '" + saved.getTitle() + "'"
        );

        ClientEntity client = clientRepository.findByIdAndOrganizationId(saved.getClientId(), organizationId).orElse(null);
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(saved.getClientServiceId(), organizationId).orElse(null);

        return mapToDto(saved, client, service);
    }

    @Override
    @Transactional
    public ComplianceWorkItemDto updateComplianceWorkStatus(UUID id, UpdateComplianceWorkStatusRequest request) {
        UUID organizationId = requireOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        if (request.getStatus() == null) {
            throw new BusinessValidationException("Target status is required");
        }

        ComplianceWorkItemEntity entity = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkItem", "id", id));

        validateWorkItemAccess(entity);

        ComplianceWorkStatus oldStatus = entity.getStatus();
        ComplianceWorkStatus newStatus = request.getStatus();

        if (oldStatus != newStatus) {
            if (!oldStatus.canTransitionTo(newStatus)) {
                throw new BusinessValidationException("Invalid status transition from " + oldStatus + " to " + newStatus);
            }
            entity.setStatus(newStatus);
            updateTimestampsForStatus(entity, newStatus);
            ComplianceWorkItemEntity saved = workItemRepository.save(entity);

            String auditAction = switch (newStatus) {
                case COMPLETED -> "COMPLIANCE_WORK_COMPLETED";
                case CANCELLED -> "COMPLIANCE_WORK_CANCELLED";
                default -> "COMPLIANCE_WORK_STATUS_CHANGED";
            };

            auditService.logEvent(
                    organizationId,
                    userId,
                    auditAction,
                    "COMPLIANCE_WORK_ITEM",
                    saved.getId().toString(),
                    oldStatus.name(),
                    "Status changed to " + newStatus.name() + (request.getNotes() != null ? " - " + request.getNotes() : "")
            );

            ClientEntity client = clientRepository.findByIdAndOrganizationId(saved.getClientId(), organizationId).orElse(null);
            ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(saved.getClientServiceId(), organizationId).orElse(null);
            return mapToDto(saved, client, service);
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(entity.getClientId(), organizationId).orElse(null);
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(entity.getClientServiceId(), organizationId).orElse(null);
        return mapToDto(entity, client, service);
    }

    @Override
    @Transactional
    public ComplianceWorkItemDto assignComplianceWork(UUID id, AssignComplianceWorkRequest request) {
        UUID organizationId = requireOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceWorkItemEntity entity = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkItem", "id", id));

        validateWorkItemAccess(entity);

        UUID oldAssignee = entity.getAssignedEmployeeId();
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
            entity.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }

        if (request.getReviewerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getReviewerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer Employee", "id", request.getReviewerEmployeeId()));
            entity.setReviewerEmployeeId(request.getReviewerEmployeeId());
        }

        ComplianceWorkItemEntity saved = workItemRepository.save(entity);

        String action = oldAssignee != null ? "COMPLIANCE_WORK_REASSIGNED" : "COMPLIANCE_WORK_ASSIGNED";
        auditService.logEvent(
                organizationId,
                userId,
                action,
                "COMPLIANCE_WORK_ITEM",
                saved.getId().toString(),
                oldAssignee != null ? oldAssignee.toString() : null,
                "Assigned to employee " + saved.getAssignedEmployeeId()
        );

        ClientEntity client = clientRepository.findByIdAndOrganizationId(saved.getClientId(), organizationId).orElse(null);
        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationId(saved.getClientServiceId(), organizationId).orElse(null);
        return mapToDto(saved, client, service);
    }

    @Override
    @Transactional
    public void deleteComplianceWorkItem(UUID id) {
        UUID organizationId = requireOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();

        ComplianceWorkItemEntity entity = workItemRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceWorkItem", "id", id));

        validateWorkItemAccess(entity);

        workItemRepository.delete(entity);

        auditService.logEvent(
                organizationId,
                userId,
                "COMPLIANCE_WORK_CANCELLED",
                "COMPLIANCE_WORK_ITEM",
                id.toString(),
                null,
                "Deleted compliance work item '" + entity.getTitle() + "'"
        );
    }

    // =========================================================================
    // HELPER & SECURITY VALIDATION METHODS
    // =========================================================================

    private UUID requireOrganizationId() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        return organizationId;
    }

    private void validateClientAccess(UUID clientId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds != null && !accessibleClientIds.contains(clientId)) {
            throw new ForbiddenException("Access denied: Client is outside authorized portfolio scope");
        }
    }

    private void validateWorkItemAccess(ComplianceWorkItemEntity entity) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        // If firm admin or client is in portfolio, allowed
        if (accessibleClientIds == null || accessibleClientIds.contains(entity.getClientId())) {
            return;
        }

        // Direct assignee or reviewer is allowed to access their assigned work item
        UUID callerEmployeeId = scope.getEmployeeId();
        UUID callerUserId = scope.getUserId();
        boolean isDirectAssignee = (callerEmployeeId != null && (callerEmployeeId.equals(entity.getAssignedEmployeeId()) || callerEmployeeId.equals(entity.getReviewerEmployeeId())))
                || (callerUserId != null && (callerUserId.equals(entity.getAssignedEmployeeId()) || callerUserId.equals(entity.getReviewerEmployeeId())));

        if (!isDirectAssignee) {
            throw new ForbiddenException("Access denied: Compliance work item is outside authorized portfolio scope");
        }
    }

    private void validateEntitlement(UUID organizationId, ComplianceWorkType workType, ClientServiceEntity clientService) {
        ProductModuleCode module = workType.getAssociatedModule();
        if (module == null && clientService != null && clientService.getServiceType() != null) {
            module = clientService.getServiceType().getAssociatedModule();
        }

        if (module != null) {
            final ProductModuleCode targetModule = module;
            try {
                List<com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto> orgModules = moduleConfigurationService.getOrganizationModules(organizationId);
                Optional<com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto> moduleOpt = orgModules.stream()
                        .filter(m -> m.getModuleCode() == targetModule)
                        .findFirst();

                if (moduleOpt.isPresent()) {
                    com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto orgMod = moduleOpt.get();
                    if (!orgMod.isEnabled()) {
                        throw new BusinessValidationException("The module '" + module + "' is disabled in your organization settings.");
                    }
                    if (!orgMod.isEntitled()) {
                        throw new ForbiddenException("Your current subscription plan is not entitled to use the '" + module + "' module.");
                    }
                }
            } catch (BusinessValidationException | ForbiddenException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Could not verify module entitlement for work type {}: {}", workType, e.getMessage());
            }
        }
    }

    private void updateTimestampsForStatus(ComplianceWorkItemEntity entity, ComplianceWorkStatus newStatus) {
        if (newStatus == ComplianceWorkStatus.IN_PREPARATION && entity.getStartedAt() == null) {
            entity.setStartedAt(Instant.now());
        } else if ((newStatus == ComplianceWorkStatus.FILED || newStatus == ComplianceWorkStatus.COMPLETED) && entity.getCompletedAt() == null) {
            entity.setCompletedAt(Instant.now());
        }
    }

    private Specification<ComplianceWorkItemEntity> createSpecification(
            UUID organizationId,
            ComplianceWorkFilterRequest filter,
            Set<UUID> accessibleClientIds,
            PracticeSecurityScope scope
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Portfolio Scoping: if not firm admin, restrict to accessible clients OR directly assigned work
            if (accessibleClientIds != null) {
                Predicate inClientPortfolio = root.get("clientId").in(accessibleClientIds);
                if (scope.getEmployeeId() != null) {
                    Predicate isAssignee = cb.equal(root.get("assignedEmployeeId"), scope.getEmployeeId());
                    Predicate isReviewer = cb.equal(root.get("reviewerEmployeeId"), scope.getEmployeeId());
                    predicates.add(cb.or(inClientPortfolio, isAssignee, isReviewer));
                } else {
                    predicates.add(inClientPortfolio);
                }
            }

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }
            if (filter.getClientServiceId() != null) {
                predicates.add(cb.equal(root.get("clientServiceId"), filter.getClientServiceId()));
            }
            if (filter.getWorkType() != null) {
                predicates.add(cb.equal(root.get("workType"), filter.getWorkType()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filter.getAssignedEmployeeId()));
            }
            if (filter.getReviewerEmployeeId() != null) {
                predicates.add(cb.equal(root.get("reviewerEmployeeId"), filter.getReviewerEmployeeId()));
            }
            if (filter.getFinancialYear() != null && !filter.getFinancialYear().isBlank()) {
                predicates.add(cb.equal(root.get("financialYear"), filter.getFinancialYear().trim()));
            }
            if (filter.getAssessmentYear() != null && !filter.getAssessmentYear().isBlank()) {
                predicates.add(cb.equal(root.get("assessmentYear"), filter.getAssessmentYear().trim()));
            }
            if (filter.getCompliancePeriod() != null && !filter.getCompliancePeriod().isBlank()) {
                predicates.add(cb.equal(root.get("compliancePeriod"), filter.getCompliancePeriod().trim()));
            }
            if (filter.getDueFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("statutoryDueDate"), filter.getDueFrom()));
            }
            if (filter.getDueTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("statutoryDueDate"), filter.getDueTo()));
            }

            if (Boolean.TRUE.equals(filter.getOverdue())) {
                predicates.add(cb.lessThan(root.get("statutoryDueDate"), LocalDate.now()));
                predicates.add(root.get("status").in(ComplianceWorkStatus.COMPLETED, ComplianceWorkStatus.CANCELLED).not());
            }

            if (Boolean.TRUE.equals(filter.getMyWorkOnly()) && scope.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), scope.getEmployeeId()));
            }

            if (Boolean.TRUE.equals(filter.getPendingReviewOnly())) {
                predicates.add(cb.equal(root.get("status"), ComplianceWorkStatus.IN_REVIEW));
                if (scope.getEmployeeId() != null) {
                    predicates.add(cb.equal(root.get("reviewerEmployeeId"), scope.getEmployeeId()));
                }
            }

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String searchPattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ComplianceWorkItemDto mapToDto(ComplianceWorkItemEntity entity, ClientEntity client, ClientServiceEntity service) {
        Map<UUID, EmployeeEntity> employeeMap = loadEmployees(entity.getOrganizationId());
        return mapToDtoWithCache(entity, client, service, employeeMap);
    }

    private ComplianceWorkItemDto mapToDtoWithCache(
            ComplianceWorkItemEntity entity,
            ClientEntity client,
            ClientServiceEntity service,
            Map<UUID, EmployeeEntity> employeeMap
    ) {
        EmployeeEntity assignee = entity.getAssignedEmployeeId() != null ? employeeMap.get(entity.getAssignedEmployeeId()) : null;
        EmployeeEntity reviewer = entity.getReviewerEmployeeId() != null ? employeeMap.get(entity.getReviewerEmployeeId()) : null;

        boolean isOverdue = entity.getStatutoryDueDate() != null
                && entity.getStatutoryDueDate().isBefore(LocalDate.now())
                && entity.getStatus() != ComplianceWorkStatus.COMPLETED
                && entity.getStatus() != ComplianceWorkStatus.CANCELLED;

        return ComplianceWorkItemDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientServiceId(entity.getClientServiceId())
                .serviceName(service != null ? service.getNotes() != null && !service.getNotes().isBlank() ? service.getNotes() : service.getServiceType().getDisplayName() : null)
                .serviceType(service != null && service.getServiceType() != null ? service.getServiceType().name() : null)
                .workType(entity.getWorkType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .compliancePeriod(entity.getCompliancePeriod())
                .status(entity.getStatus())
                .statutoryDueDate(entity.getStatutoryDueDate())
                .internalTargetDate(entity.getInternalTargetDate())
                .assignedEmployeeId(entity.getAssignedEmployeeId())
                .assignedEmployeeName(assignee != null ? assignee.getFirstName() + (assignee.getLastName() != null ? " " + assignee.getLastName() : "") : null)
                .assignedEmployeeEmail(assignee != null ? assignee.getEmail() : null)
                .reviewerEmployeeId(entity.getReviewerEmployeeId())
                .reviewerEmployeeName(reviewer != null ? reviewer.getFirstName() + (reviewer.getLastName() != null ? " " + reviewer.getLastName() : "") : null)
                .reviewerEmployeeEmail(reviewer != null ? reviewer.getEmail() : null)
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .overdue(isOverdue)
                .build();
    }

    private Map<UUID, ClientEntity> loadClients(UUID organizationId, List<ComplianceWorkItemEntity> items) {
        Set<UUID> clientIds = items.stream().map(ComplianceWorkItemEntity::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (clientIds.isEmpty()) return Collections.emptyMap();
        return clientRepository.findAllById(clientIds).stream()
                .filter(c -> organizationId.equals(c.getOrganizationId()))
                .collect(Collectors.toMap(ClientEntity::getId, c -> c));
    }

    private Map<UUID, ClientServiceEntity> loadServices(UUID organizationId, List<ComplianceWorkItemEntity> items) {
        Set<UUID> serviceIds = items.stream().map(ComplianceWorkItemEntity::getClientServiceId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (serviceIds.isEmpty()) return Collections.emptyMap();
        return clientServiceRepository.findAllById(serviceIds).stream()
                .filter(s -> organizationId.equals(s.getOrganizationId()))
                .collect(Collectors.toMap(ClientServiceEntity::getId, s -> s));
    }

    private Map<UUID, EmployeeEntity> loadEmployees(UUID organizationId) {
        return employeeRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(EmployeeEntity::getId, e -> e, (a, b) -> a));
    }
}
