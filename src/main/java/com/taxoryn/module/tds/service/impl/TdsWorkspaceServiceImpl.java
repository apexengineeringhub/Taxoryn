package com.taxoryn.module.tds.service.impl;

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
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleEntitlementService;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.WorkItemService;
import com.taxoryn.module.tds.dto.TdsChallanDto;
import com.taxoryn.module.tds.dto.TdsPeriodDto;
import com.taxoryn.module.tds.dto.TdsProfileDto;
import com.taxoryn.module.tds.dto.TdsReturnDto;
import com.taxoryn.module.tds.dto.TdsWorkspaceSummaryDto;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.mapper.TdsMapper;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsWorkspaceService;
import com.taxoryn.module.timetracking.dto.TimeEntryDto;
import com.taxoryn.module.timetracking.service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TdsWorkspaceServiceImpl implements TdsWorkspaceService {

    private final ClientRepository clientRepository;
    private final LocationRepository locationRepository;
    private final TdsProfileRepository tdsProfileRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final TdsChallanRepository tdsChallanRepository;
    private final ComplianceObligationRepository complianceObligationRepository;
    private final ComplianceWorkflowRepository complianceWorkflowRepository;
    private final TaskRepository taskRepository;
    private final DocumentRequestService documentRequestService;
    private final DocumentService documentService;
    private final WorkItemService workItemService;
    private final TimeEntryService timeEntryService;
    private final ModuleEntitlementService moduleEntitlementService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final ClientMapper clientMapper;
    private final TdsMapper tdsMapper;
    private final ComplianceMapper complianceMapper;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public TdsWorkspaceSummaryDto getClientTdsWorkspace(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.TDS_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        ClientDto clientDto = clientMapper.toDto(client);

        TdsProfileEntity profileEntity = tdsProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId).orElse(null);
        TdsProfileDto profileDto = enrichProfileDto(profileEntity);

        List<TdsPeriodDto> periods = getTdsPeriods(clientId);

        // TDS Obligations
        List<ComplianceObligationEntity> allObligations = complianceObligationRepository
                .findAllByOrganizationIdAndClientId(organizationId, clientId);

        List<ComplianceObligationDto> tdsObligations = allObligations.stream()
                .filter(this::isTdsObligation)
                .map(complianceMapper::toObligationDto)
                .sorted(Comparator.comparing(ComplianceObligationDto::getStatutoryDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        Map<UUID, ComplianceObligationEntity> obligationMap = allObligations.stream()
                .collect(Collectors.toMap(ComplianceObligationEntity::getId, o -> o, (a, b) -> a));

        // Workflows
        List<ComplianceWorkflowEntity> allWorkflows = complianceWorkflowRepository
                .findByOrganizationIdAndClientId(organizationId, clientId);

        LocalDate today = LocalDate.now();

        List<ComplianceWorkflowDto> currentWorkflows = allWorkflows.stream()
                .filter(w -> {
                    ComplianceObligationEntity ob = obligationMap.get(w.getComplianceObligationId());
                    return (w.getTdsProfileId() != null || (ob != null && isTdsObligation(ob)))
                            && w.getWorkflowStatus() != ComplianceWorkflowStatus.COMPLETED
                            && w.getWorkflowStatus() != ComplianceWorkflowStatus.CANCELLED;
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
                            .workflowType("TDS")
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
                .sorted(Comparator.comparing(ComplianceWorkflowDto::getStatutoryDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        List<ComplianceWorkflowDto> overdueWorkflows = currentWorkflows.stream()
                .filter(w -> w.getStatutoryDueDate() != null && w.getStatutoryDueDate().isBefore(today))
                .collect(Collectors.toList());

        List<ComplianceObligationDto> upcomingDueDates = tdsObligations.stream()
                .filter(o -> o.getStatutoryDueDate() != null
                        && !o.getStatutoryDueDate().isBefore(today)
                        && o.getStatus() != ComplianceObligationStatus.COMPLETED
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

        // Recent Returns & Challans
        List<TdsReturnDto> recentReturns = tdsReturnRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .map(tdsMapper::toReturnDto)
                .collect(Collectors.toList());

        List<TdsChallanDto> recentChallans = profileEntity != null
                ? tdsChallanRepository.findAllByOrganizationIdAndTdsProfileId(organizationId, profileEntity.getId()).stream()
                        .map(tdsMapper::toChallanDto)
                        .collect(Collectors.toList())
                : List.of();

        String defaultFy = !periods.isEmpty() ? periods.get(0).getFinancialYear() : "2026-27";
        String defaultQtr = !periods.isEmpty() ? periods.get(0).getQuarter() : "Q1";

        return TdsWorkspaceSummaryDto.builder()
                .client(clientDto)
                .profile(profileDto)
                .tan(profileDto != null && StringUtils.hasText(profileDto.getTan()) ? profileDto.getTan() : client.getTan())
                .deductorType(profileDto != null ? profileDto.getDeductorType() : null)
                .defaultFinancialYear(defaultFy)
                .defaultQuarter(defaultQtr)
                .periods(periods)
                .activeObligations(tdsObligations)
                .currentWorkflows(currentWorkflows)
                .overdueWorkflows(overdueWorkflows)
                .upcomingDueDates(upcomingDueDates)
                .linkedWorkItems(workItems)
                .pendingTasks(pendingTasks)
                .pendingDocumentRequests(docRequests)
                .submittedDocuments(submittedDocs)
                .timeEntries(timeEntries)
                .recentReturns(recentReturns)
                .recentChallans(recentChallans)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getClientTdsObligations(UUID clientId, String financialYear, String quarter) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.TDS_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        return complianceObligationRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .filter(this::isTdsObligation)
                .filter(o -> !StringUtils.hasText(financialYear) || financialYear.equalsIgnoreCase(o.getFinancialYear()))
                .filter(o -> !StringUtils.hasText(quarter) || (o.getPeriodLabel() != null && o.getPeriodLabel().toUpperCase().contains(quarter.toUpperCase())))
                .map(complianceMapper::toObligationDto)
                .sorted(Comparator.comparing(ComplianceObligationDto::getStatutoryDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceWorkflowDto> getClientTdsWorkflows(UUID clientId, String financialYear, String quarter) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.TDS_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<ComplianceObligationEntity> obligations = complianceObligationRepository
                .findAllByOrganizationIdAndClientId(organizationId, clientId);
        Map<UUID, ComplianceObligationEntity> obligationMap = obligations.stream()
                .collect(Collectors.toMap(ComplianceObligationEntity::getId, o -> o, (a, b) -> a));

        return complianceWorkflowRepository.findByOrganizationIdAndClientId(organizationId, clientId).stream()
                .filter(w -> {
                    ComplianceObligationEntity ob = obligationMap.get(w.getComplianceObligationId());
                    return (w.getTdsProfileId() != null || (ob != null && isTdsObligation(ob)));
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
                            .workflowType("TDS")
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
                .filter(w -> !StringUtils.hasText(financialYear) || (w.getPeriodLabel() != null && w.getPeriodLabel().contains(financialYear)))
                .filter(w -> !StringUtils.hasText(quarter) || (w.getPeriodLabel() != null && w.getPeriodLabel().toUpperCase().contains(quarter.toUpperCase())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsPeriodDto> getPeriods(UUID clientId) {
        return getTdsPeriods(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsPeriodDto> getTdsPeriods(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.TDS_COMPLIANCE);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<String> returnTypes = List.of("FORM_24Q", "FORM_26Q", "FORM_27Q", "FORM_27EQ");
        List<String> commonSections = List.of("192", "194A", "194C", "194H", "194I", "194J", "194Q", "195");

        List<TdsPeriodDto> list = new ArrayList<>();
        list.add(TdsPeriodDto.builder()
                .financialYear("2026-27")
                .quarter("Q1")
                .periodLabel("FY 2026-27 Q1 (Apr - Jun 2026)")
                .returnTypes(returnTypes)
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .commonSections(commonSections)
                .build());

        list.add(TdsPeriodDto.builder()
                .financialYear("2026-27")
                .quarter("Q2")
                .periodLabel("FY 2026-27 Q2 (Jul - Sep 2026)")
                .returnTypes(returnTypes)
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .commonSections(commonSections)
                .build());

        list.add(TdsPeriodDto.builder()
                .financialYear("2026-27")
                .quarter("Q3")
                .periodLabel("FY 2026-27 Q3 (Oct - Dec 2026)")
                .returnTypes(returnTypes)
                .statutoryDueDate(LocalDate.of(2027, 1, 31))
                .commonSections(commonSections)
                .build());

        list.add(TdsPeriodDto.builder()
                .financialYear("2026-27")
                .quarter("Q4")
                .periodLabel("FY 2026-27 Q4 (Jan - Mar 2027)")
                .returnTypes(returnTypes)
                .statutoryDueDate(LocalDate.of(2027, 5, 31))
                .commonSections(commonSections)
                .build());

        return list;
    }

    private boolean isTdsObligation(ComplianceObligationEntity o) {
        if (o.getTdsProfileId() != null) {
            return true;
        }
        if (o.getObligationType() != null) {
            ComplianceObligationType type = o.getObligationType();
            return type == ComplianceObligationType.TDS_RETURN
                    || type.name().startsWith("TDS_")
                    || type.getRequiredModule() == ProductModuleCode.TDS
                    || type.getRequiredModule() == ProductModuleCode.TDS_COMPLIANCE;
        }
        return o.getComplianceType() == com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType.TDS;
    }

    private TdsProfileDto enrichProfileDto(TdsProfileEntity profile) {
        if (profile == null) return null;
        TdsProfileDto dto = tdsMapper.toProfileDto(profile);
        if (dto == null) return null;

        dto.setActive(profile.isActive());
        dto.setLocationId(profile.getLocationId());

        clientRepository.findByIdAndOrganizationId(profile.getClientId(), profile.getOrganizationId())
                .ifPresent(c -> dto.setClientName(c.getDisplayName() != null ? c.getDisplayName() : c.getLegalName()));

        if (profile.getLocationId() != null) {
            locationRepository.findByIdAndOrganizationId(profile.getLocationId(), profile.getOrganizationId())
                    .ifPresent(loc -> dto.setLocationName(loc.getName()));
        }

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
        if (scope == null || scope.isFirmAdmin()) {
            return;
        }

        if (locationId != null) {
            Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
            if (accessibleLocs != null && !accessibleLocs.isEmpty() && !accessibleLocs.contains(locationId)) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission for this location");
            }
        }

        if (clientId != null) {
            Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClientIds != null && !accessibleClientIds.contains(clientId)) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission for this client");
            }
        }
    }
}
