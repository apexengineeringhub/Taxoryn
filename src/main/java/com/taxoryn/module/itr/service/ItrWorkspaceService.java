package com.taxoryn.module.itr.service;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.itr.dto.ItrWorkspaceSummaryDto;

import java.util.List;
import java.util.UUID;

public interface ItrWorkspaceService {

    /**
     * Retrieves aggregated ITR compliance workspace summary for a client.
     */
    ItrWorkspaceSummaryDto getClientItrWorkspace(UUID clientId);

    /**
     * Retrieves filtered ITR compliance obligations for a client.
     */
    List<ComplianceObligationDto> getClientItrObligations(UUID clientId, String assessmentYear);

    /**
     * Retrieves filtered ITR compliance workflows for a client.
     */
    List<ComplianceWorkflowDto> getClientItrWorkflows(UUID clientId, String assessmentYear);

    /**
     * Retrieves available distinct Assessment Years for a client.
     */
    List<String> getAssessmentYears(UUID clientId);
}
