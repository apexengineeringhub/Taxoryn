package com.taxoryn.module.portal.service;

import com.taxoryn.module.portal.dto.ClientApprovalDto;
import com.taxoryn.module.portal.dto.CreateClientApprovalRequest;
import com.taxoryn.module.portal.dto.DecideClientApprovalRequest;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.MessageContextType;

import java.util.List;
import java.util.UUID;

public interface ClientApprovalService {

    ClientApprovalDto createApprovalRequest(UUID clientId, CreateClientApprovalRequest request);

    List<ClientApprovalDto> getApprovalRequestsForClient(
            UUID clientId,
            ClientApprovalStatus status,
            MessageContextType contextType,
            UUID contextId);

    List<ClientApprovalDto> getClientOwnApprovalRequests(ClientApprovalStatus status);

    ClientApprovalDto getApprovalRequestById(UUID approvalId);

    ClientApprovalDto decideApprovalRequest(UUID approvalId, DecideClientApprovalRequest request);

    ClientApprovalDto revokeApprovalRequest(UUID clientId, UUID approvalId, String reason);

    long getPendingApprovalsCount(UUID clientId);
}
