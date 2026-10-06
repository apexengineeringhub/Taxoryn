package com.taxoryn.module.client.businesscontext.provider;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.client.businesscontext.dto.BranchSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.dto.ClientSummaryContextDto;
import com.taxoryn.module.client.businesscontext.dto.ContactSummaryContextDto;
import com.taxoryn.module.client.dto.ClientBranchDto;
import com.taxoryn.module.client.dto.ClientContactDto;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.service.ClientBranchService;
import com.taxoryn.module.client.service.ClientContactService;
import com.taxoryn.module.client.service.ClientContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientSummaryContextProvider implements BusinessContextProvider<ClientSummaryContextDto> {

    public static final String PROVIDER_KEY = "CLIENT_CONTEXT";

    private final ClientContextService clientContextService;
    private final ClientContactService clientContactService;
    private final ClientBranchService clientBranchService;

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public ClientSummaryContextDto resolve(BusinessContextRequest request, UUID organizationId, UUID resolvedClientId) {
        if (resolvedClientId == null) {
            return null;
        }

        ClientContextSummaryDto summary = clientContextService.findClientContext(organizationId, resolvedClientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + resolvedClientId));

        ContactSummaryContextDto primaryContactSummary = null;
        try {
            primaryContactSummary = clientContactService.getPrimaryContact(resolvedClientId)
                    .map(c -> ContactSummaryContextDto.builder()
                            .id(c.getId())
                            .name(c.getDisplayName() != null ? c.getDisplayName() : c.getFirstName() + (c.getLastName() != null ? " " + c.getLastName() : ""))
                            .role(c.getContactRole())
                            .email(c.getEmail())
                            .phone(c.getPhone())
                            .designation(c.getDesignation())
                            .build())
                    .orElse(null);
        } catch (Exception e) {
            log.debug("No primary contact context available for client: {}", resolvedClientId);
        }

        BranchSummaryContextDto primaryBranchSummary = null;
        try {
            primaryBranchSummary = clientBranchService.getPrimaryBranch(resolvedClientId)
                    .map(b -> BranchSummaryContextDto.builder()
                            .id(b.getId())
                            .branchName(b.getBranchName())
                            .branchType(b.getBranchType())
                            .gstin(b.getGstin())
                            .city(b.getCity())
                            .state(b.getState())
                            .stateCode(b.getStateCode())
                            .build())
                    .orElse(null);
        } catch (Exception e) {
            log.debug("No primary branch context available for client: {}", resolvedClientId);
        }

        return ClientSummaryContextDto.builder()
                .clientId(summary.getClientId())
                .clientCode(summary.getClientCode())
                .displayName(summary.getDisplayName())
                .legalName(summary.getLegalName())
                .clientType(summary.getClientType())
                .lifecycleStatus(summary.getStatus())
                .active(summary.isActive())
                .pan(summary.getPan())
                .gstin(summary.getGstin())
                .email(summary.getEmail())
                .phone(summary.getPhone())
                .locationId(summary.getLocationId())
                .primaryContact(primaryContactSummary)
                .primaryBranch(primaryBranchSummary)
                .activeServicesCount(summary.getActiveServicesCount())
                .totalContactsCount(summary.getContactsCount())
                .totalBranchesCount(summary.getBranchesCount())
                .build();
    }
}
