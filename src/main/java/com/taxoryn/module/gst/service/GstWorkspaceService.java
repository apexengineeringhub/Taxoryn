package com.taxoryn.module.gst.service;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.gst.dto.GstWorkspaceSummaryDto;

import java.util.List;
import java.util.UUID;

public interface GstWorkspaceService {

    GstWorkspaceSummaryDto getClientGstWorkspace(UUID clientId);

    List<ComplianceObligationDto> getClientGstObligations(UUID clientId);

    List<ComplianceWorkflowDto> getClientGstWorkflows(UUID clientId);
}
