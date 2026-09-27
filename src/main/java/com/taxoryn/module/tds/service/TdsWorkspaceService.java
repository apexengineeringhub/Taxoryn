package com.taxoryn.module.tds.service;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.tds.dto.TdsPeriodDto;
import com.taxoryn.module.tds.dto.TdsWorkspaceSummaryDto;

import java.util.List;
import java.util.UUID;

public interface TdsWorkspaceService {

    /**
     * Retrieves aggregated TDS compliance workspace summary for a client.
     */
    TdsWorkspaceSummaryDto getClientTdsWorkspace(UUID clientId);

    /**
     * Retrieves filtered TDS compliance obligations for a client.
     */
    List<ComplianceObligationDto> getClientTdsObligations(UUID clientId, String financialYear, String quarter);

    /**
     * Retrieves filtered TDS compliance workflows for a client.
     */
    List<ComplianceWorkflowDto> getClientTdsWorkflows(UUID clientId, String financialYear, String quarter);

    /**
     * Retrieves available distinct compliance periods for a client.
     */
    List<TdsPeriodDto> getPeriods(UUID clientId);

    /**
     * Retrieves available distinct compliance periods for a client.
     */
    List<TdsPeriodDto> getTdsPeriods(UUID clientId);
}
