package com.taxoryn.module.client.businesscontext.provider;

import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.dto.ServiceSummaryContextDto;
import com.taxoryn.module.client.dto.ClientServiceDto;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.service.ClientEngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceSummaryContextProvider implements BusinessContextProvider<ServiceSummaryContextDto> {

    public static final String PROVIDER_KEY = "SERVICE_CONTEXT";

    private final ClientEngagementService clientEngagementService;

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public ServiceSummaryContextDto resolve(BusinessContextRequest request, UUID organizationId, UUID resolvedClientId) {
        if (request.getServiceRelationshipId() == null || resolvedClientId == null) {
            return null;
        }

        ClientServiceDto clientService = clientEngagementService.getClientServiceById(
                resolvedClientId,
                request.getServiceRelationshipId()
        );

        if (clientService == null) {
            return null;
        }

        return ServiceSummaryContextDto.builder()
                .serviceRelationshipId(clientService.getId())
                .serviceOfferingId(clientService.getServiceOfferingId())
                .serviceName(clientService.getServiceName())
                .serviceCode(clientService.getServiceCode())
                .serviceCategory(clientService.getCategory())
                .status(clientService.getStatus())
                .billingFrequency(clientService.getBillingFrequency())
                .rate(clientService.getAgreedPrice())
                .startDate(clientService.getStartDate())
                .endDate(clientService.getEndDate())
                .active(ClientServiceStatus.ACTIVE.equals(clientService.getStatus()))
                .build();
    }
}
