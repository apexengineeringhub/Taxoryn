package com.taxoryn.module.itr.service.impl;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.mapper.ComplianceMapper;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.service.DocumentRequestService;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.service.DocumentService;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrWorkspaceSummaryDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.mapper.ItrMapper;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.service.ItrWorkspaceService;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleEntitlementService;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.WorkItemService;
import com.taxoryn.module.timetracking.dto.TimeEntryDto;
import com.taxoryn.module.timetracking.service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItrWorkspaceServiceImpl implements ItrWorkspaceService {

    private final ItrProfileRepository itrProfileRepository;
    private final ItrMapper itrMapper;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final LocationRepository locationRepository;
    private final ComplianceObligationRepository complianceObligationRepository;
    private final ComplianceWorkflowRepository complianceWorkflowRepository;
    private final ComplianceMapper complianceMapper;
    private final WorkItemService workItemService;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final DocumentService documentService;
    private final DocumentRequestService documentRequestService;
    private final TimeEntryService timeEntryService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final ModuleEntitlementService moduleEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public ItrWorkspaceSummaryDto getClientItrWorkspace(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.ITR_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        ClientDto clientDto = clientMapper.toDto(client);

        Optional<ItrProfileEntity> profileOpt = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        ItrProfileDto profileDto = profileOpt.map(this::enrichProfileDto).orElse(null);

        List<ComplianceObligationDto> itrObligations = getClientItrObligations(clientId, null);
        List<ComplianceWorkflowDto> itrWorkflows = getClientItrWorkflows(clientId, null);
        List<String> assessmentYears = getAssessmentYears(clientId);

        LocalDate today = LocalDate.now();

        List<ComplianceWorkflowDto> currentWorkflows = itrWorkflows.stream()
                .filter(w -> w.getWorkflowStatus() != ComplianceWorkflowStatus.COMPLETED
                        && w.getWorkflowStatus() != ComplianceWorkflowStatus.CANCELLED)
                .collect(Collectors.toList());

        List<ComplianceWorkflowDto> overdueWorkflows = itrWorkflows.stream()
                .filter(w -> w.getWorkflowStatus() != ComplianceWorkflowStatus.COMPLETED
                        && w.getWorkflowStatus() != ComplianceWorkflowStatus.CANCELLED
                        && w.getStatutoryDueDate() != null
                        && w.getStatutoryDueDate().isBefore(today))
                .collect(Collectors.toList());

        List<ComplianceObligationDto> upcomingDueDates = itrObligations.stream()
                .filter(o -> o.getStatus() != ComplianceObligationStatus.COMPLETED
                        && o.getStatus() != ComplianceObligationStatus.CANCELLED)
                .sorted(Comparator.comparing(ComplianceObligationDto::getStatutoryDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Linked Work Items
        List<WorkItemDto> workItems = List.of();
        try {
            workItems = workItemService.getWorkItemsByClientId(clientId);
        } catch (Exception e) {
            log.warn("Could not load work items for client {}: {}", clientId, e.getMessage());
        }

        // Pending Tasks
        List<TaskDto> pendingTasks = taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .filter(t -> t.getStatus() != TaskEntity.TaskStatus.COMPLETED && t.getStatus() != TaskEntity.TaskStatus.CANCELLED)
                .map(taskMapper::toDto)
                .collect(Collectors.toList());

        // Document Requests
        List<DocumentRequestDto> docRequests = List.of();
        try {
            docRequests = documentRequestService.getClientRequests(clientId).stream()
                    .filter(r -> r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.COMPLETED
                            && r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.CANCELLED
                            && r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.DECLINED)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not load document requests for client {}: {}", clientId, e.getMessage());
        }

        // Submitted Documents
        List<DocumentDto> submittedDocs = List.of();
        try {
            submittedDocs = documentService.getClientDocuments(clientId);
        } catch (Exception e) {
            log.warn("Could not load documents for client {}: {}", clientId, e.getMessage());
        }

        // Time Entries
        List<TimeEntryDto> timeEntries = List.of();
        try {
            timeEntries = timeEntryService.getTimeEntriesByClientId(clientId);
        } catch (Exception e) {
            log.warn("Could not load time entries for client {}: {}", clientId, e.getMessage());
        }

        String defaultAy = profileDto != null && StringUtils.hasText(profileDto.getDefaultAssessmentYear())
                ? profileDto.getDefaultAssessmentYear()
                : (assessmentYears.isEmpty() ? null : assessmentYears.get(0));

        return ItrWorkspaceSummaryDto.builder()
                .client(clientDto)
                .profile(profileDto)
                .pan(profileDto != null && StringUtils.hasText(profileDto.getPan()) ? profileDto.getPan() : client.getPan())
                .taxpayerType(profileDto != null ? profileDto.getTaxpayerType() : null)
                .residentialStatus(profileDto != null ? profileDto.getResidentialStatus() : null)
                .defaultAssessmentYear(defaultAy)
                .applicableReturnType(profileDto != null ? profileDto.getApplicableReturnType() : null)
                .assessmentYears(assessmentYears)
                .activeObligations(itrObligations)
                .currentWorkflows(currentWorkflows)
                .overdueWorkflows(overdueWorkflows)
                .upcomingDueDates(upcomingDueDates)
                .linkedWorkItems(workItems)
                .pendingTasks(pendingTasks)
                .pendingDocumentRequests(docRequests)
                .submittedDocuments(submittedDocs)
                .timeEntries(timeEntries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getClientItrObligations(UUID clientId, String assessmentYear) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.ITR_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<ComplianceObligationEntity> obligations = complianceObligationRepository
                .findAllByOrganizationIdAndClientId(organizationId, clientId)
                .stream()
                .filter(this::isItrObligation)
                .filter(o -> !StringUtils.hasText(assessmentYear) || assessmentYear.equalsIgnoreCase(o.getAssessmentYear()) || assessmentYear.equalsIgnoreCase(o.getPeriodLabel()))
                .sorted(Comparator.comparing(ComplianceObligationEntity::getStatutoryDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return obligations.stream()
                .map(complianceMapper::toObligationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceWorkflowDto> getClientItrWorkflows(UUID clientId, String assessmentYear) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.ITR_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<ComplianceWorkflowEntity> workflows = complianceWorkflowRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);
        Map<UUID, ComplianceObligationEntity> obligationMap = obligations.stream()
                .collect(Collectors.toMap(ComplianceObligationEntity::getId, o -> o, (a, b) -> a));

        return workflows.stream()
                .filter(w -> {
                    if (w.getItrProfileId() != null) return true;
                    if ("ITR".equalsIgnoreCase(w.getWorkflowType())) return true;
                    if (w.getComplianceObligationId() != null) {
                        ComplianceObligationEntity ob = obligationMap.get(w.getComplianceObligationId());
                        return ob != null && isItrObligation(ob);
                    }
                    return false;
                })
                .map(w -> {
                    ComplianceObligationEntity ob = obligationMap.get(w.getComplianceObligationId());
                    return ComplianceWorkflowDto.builder()
                            .id(w.getId())
                            .organizationId(w.getOrganizationId())
                            .clientId(w.getClientId())
                            .clientName(client.getDisplayName())
                            .pan(client.getPan())
                            .clientServiceId(w.getClientServiceId())
                            .complianceObligationId(w.getComplianceObligationId())
                            .obligationTitle(ob != null ? ob.getTitle() : null)
                            .obligationType(ob != null ? ob.getObligationType() : null)
                            .workflowType("ITR")
                            .periodLabel(ob != null ? ob.getPeriodLabel() : null)
                            .statutoryDueDate(ob != null ? ob.getStatutoryDueDate() : w.getStatutoryDueDate())
                            .targetDate(w.getTargetDate())
                            .workflowStatus(w.getWorkflowStatus())
                            .priority(w.getPriority())
                            .locationId(w.getLocationId())
                            .assignedEmployeeId(w.getAssignedEmployeeId())
                            .assignedUserId(w.getAssignedUserId())
                            .waitingForClient(w.isWaitingForClient())
                            .createdAt(w.getCreatedAt())
                            .updatedAt(w.getUpdatedAt())
                            .build();
                })
                .filter(w -> !StringUtils.hasText(assessmentYear) || (w.getPeriodLabel() != null && assessmentYear.equalsIgnoreCase(w.getPeriodLabel())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAssessmentYears(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.ITR_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<ComplianceObligationEntity> obligations = complianceObligationRepository
                .findAllByOrganizationIdAndClientId(organizationId, clientId)
                .stream()
                .filter(this::isItrObligation)
                .toList();

        Set<String> ays = new java.util.LinkedHashSet<>();
        itrProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId)
                .ifPresent(p -> {
                    if (StringUtils.hasText(p.getDefaultAssessmentYear())) {
                        ays.add(p.getDefaultAssessmentYear().trim());
                    }
                });

        for (ComplianceObligationEntity ob : obligations) {
            if (StringUtils.hasText(ob.getAssessmentYear())) {
                ays.add(ob.getAssessmentYear().trim());
            } else if (StringUtils.hasText(ob.getPeriodLabel()) && ob.getPeriodLabel().contains("-")) {
                ays.add(ob.getPeriodLabel().trim());
            }
        }

        if (ays.isEmpty()) {
            ays.add("2026-27");
            ays.add("2025-26");
        }

        return new ArrayList<>(ays);
    }

    private boolean isItrObligation(ComplianceObligationEntity o) {
        if (o.getItrProfileId() != null || o.getItrReturnId() != null) {
            return true;
        }
        if (o.getObligationType() != null) {
            ComplianceObligationType type = o.getObligationType();
            return type == ComplianceObligationType.ITR_FILING
                    || type == ComplianceObligationType.TAX_AUDIT
                    || type == ComplianceObligationType.ADVANCE_TAX
                    || type == ComplianceObligationType.SELF_ASSESSMENT_TAX
                    || type.name().startsWith("ITR_");
        }
        return o.getComplianceType() == com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType.ITR;
    }

    private ItrProfileDto enrichProfileDto(ItrProfileEntity profile) {
        if (profile == null) return null;
        ItrProfileDto dto = itrMapper.toProfileDto(profile);
        if (dto == null) return null;

        clientRepository.findByIdAndOrganizationId(profile.getClientId(), profile.getOrganizationId())
                .ifPresent(c -> dto.setClientName(c.getDisplayName()));

        if (profile.getLocationId() != null) {
            locationRepository.findByIdAndOrganizationId(profile.getLocationId(), profile.getOrganizationId())
                    .ifPresent(loc -> dto.setLocationName(loc.getName()));
        }

        dto.setLocationId(profile.getLocationId());
        dto.setDefaultAssessmentYear(profile.getDefaultAssessmentYear());
        dto.setAssessmentCategory(profile.getAssessmentCategory());
        dto.setApplicableReturnType(profile.getEffectiveReturnType());
        dto.setActive(profile.isActive());
        return dto;
    }

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }

    private void validateAccess(UUID clientId, UUID locationId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (scope.isFirmAdmin()) {
            return;
        }

        if (locationId != null) {
            Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
            if (accessibleLocs != null && !accessibleLocs.isEmpty() && !accessibleLocs.contains(locationId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this location");
            }
        }

        if (clientId != null) {
            Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClients != null && !accessibleClients.contains(clientId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this client");
            }
        }
    }
}
