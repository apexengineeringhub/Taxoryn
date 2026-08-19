package com.taxoryn.module.gst.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.gst.dto.BatchGenerateFilingsRequest;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.GstFilingFilterRequest;
import com.taxoryn.module.gst.dto.GstMonthlySummaryDto;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstProfileFilterRequest;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto;
import com.taxoryn.module.gst.dto.SaveGstMonthlySummaryRequest;
import com.taxoryn.module.gst.dto.UpdateGstFilingStatusRequest;
import com.taxoryn.module.gst.dto.UpdateGstProfileRequest;
import com.taxoryn.module.gst.dto.UpdateGstProfileStatusRequest;

import java.util.List;
import java.util.UUID;

public interface GstService {

    // 1. Profile Management
    GstProfileDto createProfile(CreateGstProfileRequest request);

    GstProfileDto updateProfile(UUID id, UpdateGstProfileRequest request);

    GstProfileDto getProfileById(UUID id);

    PagedResponse<GstProfileDto> getProfiles(GstProfileFilterRequest filterRequest);

    GstProfileDto updateProfileStatus(UUID id, UpdateGstProfileStatusRequest request);

    // 2. Return Filings Management
    GstReturnFilingDto createFiling(CreateGstReturnFilingRequest request);

    GstReturnFilingDto updateFilingStatus(UUID id, UpdateGstFilingStatusRequest request);

    GstReturnFilingDto getFilingById(UUID id);

    PagedResponse<GstReturnFilingDto> getFilings(GstFilingFilterRequest filterRequest);

    List<GstReturnFilingDto> batchGenerateFilings(BatchGenerateFilingsRequest request);

    // 3. Monthly Computation & Summary
    GstMonthlySummaryDto saveMonthlySummary(SaveGstMonthlySummaryRequest request);

    GstMonthlySummaryDto getMonthlySummary(UUID gstProfileId, String period);

    // 4. Workload Dashboard & History
    GstWorkloadDashboardDto getWorkloadDashboard(String period, UUID assignedEmployeeId);

    List<GstReturnFilingDto> getClientFilingHistory(UUID clientId);
}
