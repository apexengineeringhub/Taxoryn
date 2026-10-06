package com.taxoryn.module.client.businesscontext.provider;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.dto.EngagementSummaryContextDto;
import com.taxoryn.module.engagement.dto.EngagementDto;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.service.EngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngagementSummaryContextProvider implements BusinessContextProvider<EngagementSummaryContextDto> {

    public static final String PROVIDER_KEY = "ENGAGEMENT_CONTEXT";

    private final EngagementService engagementService;

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public EngagementSummaryContextDto resolve(BusinessContextRequest request, UUID organizationId, UUID resolvedClientId) {
        if (request.getEngagementId() == null || resolvedClientId == null) {
            return null;
        }

        EngagementDto engagement;
        try {
            engagement = engagementService.getEngagementById(request.getEngagementId());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to retrieve engagement context for ID: {}", request.getEngagementId(), e);
            throw new ResourceNotFoundException("Engagement not found with id: " + request.getEngagementId());
        }

        if (engagement == null) {
            throw new ResourceNotFoundException("Engagement not found with id: " + request.getEngagementId());
        }

        if (engagement.getOrganizationId() != null && organizationId != null
                && !engagement.getOrganizationId().equals(organizationId)) {
            throw new ResourceNotFoundException("Engagement not found with id: " + request.getEngagementId());
        }

        if (engagement.getClientId() != null && !engagement.getClientId().equals(resolvedClientId)) {
            throw new BusinessValidationException("Engagement " + request.getEngagementId() + " does not belong to client " + resolvedClientId);
        }

        if (request.getServiceRelationshipId() != null && engagement.getClientServiceId() != null
                && !engagement.getClientServiceId().equals(request.getServiceRelationshipId())) {
            throw new BusinessValidationException("Engagement clientServiceId does not match requested serviceRelationshipId");
        }

        boolean isActive = engagement.getStatus() != null && (
                engagement.getStatus() == EngagementStatus.DRAFT ||
                engagement.getStatus() == EngagementStatus.ACTIVE ||
                engagement.getStatus() == EngagementStatus.ON_HOLD
        );

        return EngagementSummaryContextDto.builder()
                .engagementId(engagement.getId())
                .engagementCode(engagement.getEngagementCode())
                .engagementName(engagement.getName())
                .status(engagement.getStatus())
                .priority(engagement.getPriority())
                .serviceId(engagement.getServiceId())
                .serviceName(engagement.getServiceName())
                .clientServiceId(engagement.getClientServiceId())
                .assignedUserId(engagement.getAssignedUserId())
                .assignedUserName(engagement.getAssignedUserName())
                .reviewerUserId(engagement.getReviewerUserId())
                .reviewerUserName(engagement.getReviewerUserName())
                .startDate(engagement.getStartDate())
                .endDate(engagement.getEndDate())
                .active(isActive)
                .build();
    }
}
