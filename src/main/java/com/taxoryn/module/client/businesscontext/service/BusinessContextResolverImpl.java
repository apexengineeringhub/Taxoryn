package com.taxoryn.module.client.businesscontext.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.businesscontext.dto.ActorSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.AttentionSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.dto.ClientSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.EngagementSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.ServiceSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.TemporalSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.WorkSummaryContextDto;
import com.taxoryn.module.client.businesscontext.provider.ClientSummaryContextProvider;
import com.taxoryn.module.client.businesscontext.provider.EngagementSummaryContextProvider;
import com.taxoryn.module.client.businesscontext.provider.IntelligenceSummaryContextProvider;
import com.taxoryn.module.client.businesscontext.provider.ServiceSummaryContextProvider;
import com.taxoryn.module.client.businesscontext.provider.WorkSummaryContextProvider;
import com.taxoryn.module.engagement.dto.EngagementDto;
import com.taxoryn.module.engagement.service.EngagementService;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.service.TaskService;
import com.taxoryn.module.task.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessContextResolverImpl implements BusinessContextResolver {

    private final ClientSummaryContextProvider clientSummaryContextProvider;
    private final ServiceSummaryContextProvider serviceSummaryContextProvider;
    private final EngagementSummaryContextProvider engagementSummaryContextProvider;
    private final WorkSummaryContextProvider workSummaryContextProvider;
    private final IntelligenceSummaryContextProvider intelligenceSummaryContextProvider;

    private final EngagementService engagementService;
    private final TaskService taskService;
    private final WorkItemService workItemService;

    @Override
    public BusinessContextDto resolveContext(BusinessContextRequest request) {
        if (request == null) {
            throw new BusinessValidationException("BusinessContextRequest cannot be null");
        }

        UUID organizationId;
        try {
            organizationId = SecurityUtils.getCurrentOrganizationId();
        } catch (Exception e) {
            throw new UnauthorizedException("Authenticated tenant organization context is required for business context resolution");
        }

        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated tenant organization context is required for business context resolution");
        }

        UUID resolvedClientId = deduceAndValidateClientId(request, organizationId);

        // Resolve context components via dedicated providers
        ClientSummaryContextDto clientContext = clientSummaryContextProvider.resolve(request, organizationId, resolvedClientId);
        ServiceSummaryContextDto serviceContext = serviceSummaryContextProvider.resolve(request, organizationId, resolvedClientId);
        EngagementSummaryContextDto engagementContext = engagementSummaryContextProvider.resolve(request, organizationId, resolvedClientId);
        WorkSummaryContextDto workContext = workSummaryContextProvider.resolve(request, organizationId, resolvedClientId);
        AttentionSummaryContextDto attentionContext = intelligenceSummaryContextProvider.resolve(request, organizationId, resolvedClientId);

        // Derive temporal context
        TemporalSummaryContextDto temporalContext = deriveTemporalContext(serviceContext, engagementContext, workContext);

        // Derive actor / security context
        ActorSummaryContextDto actorContext = deriveActorContext(organizationId);

        return BusinessContextDto.builder()
                .organizationId(organizationId)
                .client(clientContext)
                .service(serviceContext)
                .engagement(engagementContext)
                .work(workContext)
                .attention(attentionContext)
                .temporalContext(temporalContext)
                .actorContext(actorContext)
                .build();
    }

    @Override
    public BusinessContextDto resolveClientContext(UUID clientId) {
        return resolveContext(BusinessContextRequest.builder().clientId(clientId).build());
    }

    @Override
    public BusinessContextDto resolveServiceContext(UUID clientId, UUID serviceRelationshipId) {
        return resolveContext(BusinessContextRequest.builder()
                .clientId(clientId)
                .serviceRelationshipId(serviceRelationshipId)
                .build());
    }

    @Override
    public BusinessContextDto resolveEngagementContext(UUID clientId, UUID engagementId) {
        return resolveContext(BusinessContextRequest.builder()
                .clientId(clientId)
                .engagementId(engagementId)
                .build());
    }

    @Override
    public BusinessContextDto resolveWorkContext(UUID clientId, UUID taskId) {
        return resolveContext(BusinessContextRequest.builder()
                .clientId(clientId)
                .taskId(taskId)
                .build());
    }

    private UUID deduceAndValidateClientId(BusinessContextRequest request, UUID organizationId) {
        if (request.getClientId() != null) {
            return request.getClientId();
        }

        // Deduce from Task
        if (request.getTaskId() != null) {
            TaskDto task = taskService.getTaskById(request.getTaskId());
            if (task == null) {
                throw new ResourceNotFoundException("Task not found with id: " + request.getTaskId());
            }
            if (task.getOrganizationId() != null && !task.getOrganizationId().equals(organizationId)) {
                throw new ResourceNotFoundException("Task not found with id: " + request.getTaskId());
            }
            if (task.getClientId() == null) {
                throw new BusinessValidationException("Task " + request.getTaskId() + " is not linked to any client");
            }
            return task.getClientId();
        }

        // Deduce from Engagement
        if (request.getEngagementId() != null) {
            EngagementDto engagement = engagementService.getEngagementById(request.getEngagementId());
            if (engagement == null) {
                throw new ResourceNotFoundException("Engagement not found with id: " + request.getEngagementId());
            }
            if (engagement.getOrganizationId() != null && !engagement.getOrganizationId().equals(organizationId)) {
                throw new ResourceNotFoundException("Engagement not found with id: " + request.getEngagementId());
            }
            if (engagement.getClientId() == null) {
                throw new BusinessValidationException("Engagement " + request.getEngagementId() + " is not linked to any client");
            }
            return engagement.getClientId();
        }

        // Deduce from Work Item
        if (request.getWorkInstanceId() != null) {
            WorkItemDto workItem = workItemService.getWorkItemById(request.getWorkInstanceId());
            if (workItem == null) {
                throw new ResourceNotFoundException("Work item not found with id: " + request.getWorkInstanceId());
            }
            if (workItem.getClientId() == null) {
                throw new BusinessValidationException("Work item " + request.getWorkInstanceId() + " is not linked to any client");
            }
            return workItem.getClientId();
        }

        throw new BusinessValidationException("At least one business entity identifier (clientId, engagementId, workInstanceId, or taskId) must be supplied");
    }

    private TemporalSummaryContextDto deriveTemporalContext(
            ServiceSummaryContextDto service,
            EngagementSummaryContextDto engagement,
            WorkSummaryContextDto work) {

        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate dueDate = null;
        LocalDate statutoryDueDate = null;

        if (work != null) {
            startDate = work.getStartDate();
            dueDate = work.getDueDate();
            statutoryDueDate = work.getStatutoryDueDate();
        } else if (engagement != null) {
            startDate = engagement.getStartDate();
            dueDate = engagement.getEndDate();
        } else if (service != null) {
            startDate = service.getStartDate();
            dueDate = service.getEndDate();
        }

        boolean isOverdue = (dueDate != null && dueDate.isBefore(today))
                || (work != null && work.isOverdue());

        int year = today.getYear();
        int month = today.getMonthValue();
        String currentFiscalYear = month >= 4 ? "FY " + year + "-" + (year + 1) : "FY " + (year - 1) + "-" + year;

        return TemporalSummaryContextDto.builder()
                .currentDate(today)
                .relevantPeriod(currentFiscalYear)
                .effectiveStartDate(startDate)
                .effectiveDueDate(dueDate)
                .statutoryDueDate(statutoryDueDate)
                .isOverdue(isOverdue)
                .build();
    }

    private ActorSummaryContextDto deriveActorContext(UUID organizationId) {
        UUID currentUserId = SecurityUtils.getCurrentUser().map(SecurityUser::getUserId).orElse(null);
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        Set<String> roles = SecurityUtils.getCurrentRoles();

        boolean canAccessClient = SecurityUtils.hasAuthority("CLIENT_VIEW") || SecurityUtils.isTenantAdmin()
                || SecurityUtils.hasRole("PRACTICE_STAFF") || SecurityUtils.hasRole("PARTNER")
                || SecurityUtils.hasRole("MANAGER") || SecurityUtils.hasRole("ASSOCIATE");

        boolean canAccessEngagement = SecurityUtils.hasAuthority("ENGAGEMENT_VIEW") || SecurityUtils.isTenantAdmin()
                || SecurityUtils.hasRole("PRACTICE_STAFF") || SecurityUtils.hasRole("PARTNER")
                || SecurityUtils.hasRole("MANAGER") || SecurityUtils.hasRole("ASSOCIATE");

        boolean canAccessWork = SecurityUtils.hasAuthority("TASK_VIEW") || SecurityUtils.isTenantAdmin()
                || SecurityUtils.hasRole("PRACTICE_STAFF") || SecurityUtils.hasRole("PARTNER")
                || SecurityUtils.hasRole("MANAGER") || SecurityUtils.hasRole("ASSOCIATE");

        return ActorSummaryContextDto.builder()
                .currentUserId(currentUserId)
                .currentUserEmail(currentUserEmail)
                .organizationId(organizationId)
                .roles(roles != null ? roles : Collections.emptySet())
                .canAccessClient(canAccessClient)
                .canAccessEngagement(canAccessEngagement)
                .canAccessWork(canAccessWork)
                .build();
    }
}
