package com.taxoryn.module.itr.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.itr.dto.AssignItrEmployeeRequest;
import com.taxoryn.module.itr.dto.BatchGenerateItrReturnsRequest;
import com.taxoryn.module.itr.dto.BulkItrImportResultDto;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrFilterRequest;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrWorkloadDashboardDto;
import com.taxoryn.module.itr.dto.RecordItrFilingRequest;
import com.taxoryn.module.itr.dto.UpdateItrProfileRequest;
import com.taxoryn.module.itr.dto.UpdateItrReturnRequest;
import com.taxoryn.module.itr.dto.UpdateItrStatusRequest;

import java.util.List;
import java.util.UUID;

public interface ItrService {

    // 1. ITR Profile Management
    ItrProfileDto createProfile(CreateItrProfileRequest request);

    BulkItrImportResultDto bulkCreateProfiles(List<CreateItrProfileRequest> requests);

    ItrProfileDto updateProfile(UUID id, UpdateItrProfileRequest request);

    ItrProfileDto getProfileById(UUID id);

    ItrProfileDto getProfileByClientId(UUID clientId);

    // 2. ITR Returns Lifecycle
    ItrReturnDto createReturn(CreateItrReturnRequest request);

    BulkItrImportResultDto bulkCreateReturns(List<CreateItrReturnRequest> requests);

    List<ItrReturnDto> batchGenerateReturns(BatchGenerateItrReturnsRequest request);

    ItrReturnDto updateReturn(UUID id, UpdateItrReturnRequest request);

    ItrReturnDto getReturnById(UUID id);

    PagedResponse<ItrReturnDto> getReturns(ItrFilterRequest filterRequest);

    ItrReturnDto updateStatus(UUID id, UpdateItrStatusRequest request);

    ItrReturnDto recordFilingDetails(UUID id, RecordItrFilingRequest request);

    ItrReturnDto assignEmployee(UUID id, AssignItrEmployeeRequest request);

    // 3. Upcoming, Overdue, History & Workload
    List<ItrReturnDto> getUpcomingReturns(int daysAhead);

    List<ItrReturnDto> getOverdueReturns();

    List<ItrReturnDto> getClientItrHistory(UUID clientId);

    ItrWorkloadDashboardDto getWorkloadDashboard(String assessmentYear, UUID assignedEmployeeId);

    List<ItrReturnDto> seedDemoData();
}
