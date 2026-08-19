package com.taxoryn.module.portal.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Portal Single-Screen Dashboard Overview")
public class ClientPortalDashboardDto {

    private UUID clientId;
    private String displayName;
    private String legalName;
    private ClientType clientType;
    private String pan;
    private String gstin;
    private String tan;

    private String assignedPractitionerName;
    private String assignedPractitionerEmail;
    private String assignedPractitionerPhone;

    private long pendingDocumentsCount;
    private long pendingTasksCount;
    private long activeGstReturnsCount;
    private long activeItrReturnsCount;

    private List<ClientGstStatusDto> latestGstFilings;
    private List<ClientItrStatusDto> latestItrReturns;
    private List<ClientDocumentRequestDto> pendingDocumentRequests;
    private List<ClientTaskDto> pendingTasks;
    private List<ClientNotificationDto> recentNotifications;
}
