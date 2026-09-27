package com.taxoryn.module.gst.service.impl;

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
import com.taxoryn.module.gst.dto.GstRegistrationDto;
import com.taxoryn.module.gst.dto.GstWorkspaceSummaryDto;
import com.taxoryn.module.gst.service.GstRegistrationService;
import com.taxoryn.module.gst.service.GstWorkspaceService;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleEntitlementService;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GstWorkspaceServiceImpl implements GstWorkspaceService {

    private final GstRegistrationService gstRegistrationService;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
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
    public GstWorkspaceSummaryDto getClientGstWorkspace(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.GST);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        ClientDto clientDto = clientMapper.toDto(client);
        List<GstRegistrationDto> registrations = gstRegistrationService.getRegistrationsByClientId(clientId);

        List<ComplianceObligationDto> gstObligations = getClientGstObligations(clientId);
        List<ComplianceWorkflowDto> gstWorkflows = getClientGstWorkflows(clientId);

        LocalDate today = LocalDate.now();

        List<ComplianceWorkflowDto> currentWorkflows = gstWorkflows.stream()
                .filter(w -> w.getWorkflowStatus() != ComplianceWorkflowStatus.COMPLETED
                        && w.getWorkflowStatus() != ComplianceWorkflowStatus.CANCELLED)
                .collect(Collectors.toList());

        List<ComplianceWorkflowDto> overdueWorkflows = gstWorkflows.stream()
                .filter(w -> w.getWorkflowStatus() != ComplianceWorkflowStatus.COMPLETED
                        && w.getWorkflowStatus() != ComplianceWorkflowStatus.CANCELLED
                        && w.getTargetDate() != null
                        && w.getTargetDate().isBefore(today))
                .collect(Collectors.toList());

        List<ComplianceObligationDto> upcomingDueDates = gstObligations.stream()
                .filter(o -> o.getStatus() != ComplianceObligationStatus.COMPLETED
                        && o.getStatus() != ComplianceObligationStatus.CANCELLED
                        && o.getStatutoryDueDate() != null
                        && !o.getStatutoryDueDate().isBefore(today))
                .sorted(Comparator.comparing(ComplianceObligationDto::getStatutoryDueDate))
                .limit(10)
                .collect(Collectors.toList());

        List<WorkItemDto> linkedWorkItems = workItemService.getWorkItemsByClientId(clientId);
        List<TaskDto> pendingTasks = taskRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .filter(t -> t.getStatus() != TaskEntity.TaskStatus.COMPLETED && t.getStatus() != TaskEntity.TaskStatus.CANCELLED)
                .map(taskMapper::toDto)
                .collect(Collectors.toList());

        List<DocumentRequestDto> pendingDocumentRequests = documentRequestService.getClientRequests(clientId).stream()
                .filter(r -> r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.COMPLETED
                        && r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.CANCELLED
                        && r.getStatus() != com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.DECLINED)
                .collect(Collectors.toList());

        List<DocumentDto> submittedDocuments = documentService.getClientDocuments(clientId);
        List<TimeEntryDto> timeEntries = timeEntryService.getTimeEntriesByClientId(clientId);

        return GstWorkspaceSummaryDto.builder()
                .client(clientDto)
                .registrations(registrations)
                .activeObligations(gstObligations)
                .currentWorkflows(currentWorkflows)
                .overdueWorkflows(overdueWorkflows)
                .upcomingDueDates(upcomingDueDates)
                .linkedWorkItems(linkedWorkItems)
                .pendingTasks(pendingTasks)
                .pendingDocumentRequests(pendingDocumentRequests)
                .submittedDocuments(submittedDocuments)
                .timeEntries(timeEntries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> getClientGstObligations(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.GST);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<ComplianceObligationEntity> obligations = complianceObligationRepository
                .findByOrganizationIdAndClientIdOrderByStatutoryDueDateAsc(organizationId, clientId);

        return obligations.stream()
                .filter(this::isGstObligation)
                .map(complianceMapper::toObligationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceWorkflowDto> getClientGstWorkflows(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, ProductModuleCode.GST);

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<ComplianceWorkflowEntity> workflows = complianceWorkflowRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);

        Map<UUID, ComplianceObligationEntity> obligationMap = obligations.stream()
                .collect(Collectors.toMap(ComplianceObligationEntity::getId, o -> o, (a, b) -> a));

        return workflows.stream()
                .filter(w -> {
                    if (w.getGstRegistrationId() != null) {
                        return true;
                    }
                    ComplianceObligationEntity ob = obligationMap.get(w.getComplianceObligationId());
                    return ob != null && isGstObligation(ob);
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
                .collect(Collectors.toList());
    }

    private boolean isGstObligation(ComplianceObligationEntity ob) {
        if (ob.getGstRegistrationId() != null) {
            return true;
        }
        ComplianceObligationType type = ob.getObligationType();
        if (type == null) {
            return false;
        }
        return type == ComplianceObligationType.GST_RETURN
                || type == ComplianceObligationType.GSTR_1
                || type == ComplianceObligationType.GSTR_3B
                || type == ComplianceObligationType.GSTR_9
                || type == ComplianceObligationType.GSTR_9C
                || type.getRequiredModule() == ProductModuleCode.GST
                || type.getRequiredModule() == ProductModuleCode.GST_COMPLIANCE;
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
