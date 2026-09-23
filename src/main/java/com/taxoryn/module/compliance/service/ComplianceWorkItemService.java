package com.taxoryn.module.compliance.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkItemDto;
import com.taxoryn.module.compliance.dto.CreateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkStatusRequest;

import java.util.List;
import java.util.UUID;

public interface ComplianceWorkItemService {

    ComplianceWorkItemDto createComplianceWorkItem(CreateComplianceWorkItemRequest request);

    ComplianceWorkItemDto getComplianceWorkItemById(UUID id);

    PagedResponse<ComplianceWorkItemDto> getComplianceWorkItems(ComplianceWorkFilterRequest filterRequest);

    List<ComplianceWorkItemDto> getComplianceWorkItemsByClient(UUID clientId);

    List<ComplianceWorkItemDto> getComplianceWorkItemsByService(UUID serviceId);

    ComplianceWorkItemDto updateComplianceWorkItem(UUID id, UpdateComplianceWorkItemRequest request);

    ComplianceWorkItemDto updateComplianceWorkStatus(UUID id, UpdateComplianceWorkStatusRequest request);

    ComplianceWorkItemDto assignComplianceWork(UUID id, AssignComplianceWorkRequest request);

    void deleteComplianceWorkItem(UUID id);
}
