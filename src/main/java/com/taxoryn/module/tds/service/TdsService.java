package com.taxoryn.module.tds.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.tds.dto.*;

import java.util.List;
import java.util.UUID;

public interface TdsService {

    // 1. TDS Profiles (TAN Master)
    TdsProfileDto createProfile(CreateTdsProfileRequest request);

    PagedResponse<TdsProfileDto> getProfiles(TdsProfileFilterRequest filterRequest);

    TdsProfileDto getProfileById(UUID id);

    TdsProfileDto getProfileByClientId(UUID clientId);

    TdsProfileDto updateProfile(UUID id, UpdateTdsProfileRequest request);

    BulkTdsProfileImportResultDto bulkCreateProfiles(List<CreateTdsProfileRequest> requests);

    // 2. TDS Returns Lifecycle
    TdsReturnDto createReturn(CreateTdsReturnRequest request);

    PagedResponse<TdsReturnDto> getReturns(TdsReturnFilterRequest filterRequest);

    TdsReturnDto getReturnById(UUID id);

    TdsReturnDto updateReturn(UUID id, UpdateTdsReturnRequest request);

    TdsReturnDto updateStatus(UUID id, UpdateTdsReturnStatusRequest request);

    TdsReturnDto recordFiling(UUID id, RecordTdsFilingRequest request);

    TdsReturnDto assignEmployee(UUID id, AssignTdsEmployeeRequest request);

    // Task Workflow (reuses Task module)
    TdsReturnDto createTaskForReturn(UUID id);

    // Document Workflow (reuses Document Request module)
    com.taxoryn.module.docrequest.dto.DocumentRequestDto createDocumentRequestForReturn(UUID id, com.taxoryn.module.docrequest.dto.CreateDocumentRequest request);

    java.util.List<com.taxoryn.module.document.dto.DocumentDto> getReturnDocuments(UUID id);

    List<TdsReturnDto> batchGenerateReturns(BatchGenerateTdsReturnsRequest request);

    BulkTdsReturnImportResultDto bulkCreateReturns(List<CreateTdsReturnRequest> requests);

    List<TdsReturnDto> getUpcomingReturns(int daysAhead);

    List<TdsReturnDto> getOverdueReturns();

    List<TdsReturnDto> getClientReturnHistory(UUID clientId);

    // 3. Challans ITNS 281
    TdsChallanDto createChallan(CreateTdsChallanRequest request);

    PagedResponse<TdsChallanDto> getChallans(TdsChallanFilterRequest filterRequest);

    TdsChallanDto getChallanById(UUID id);

    TdsChallanDto updateChallan(UUID id, UpdateTdsChallanRequest request);

    // 4. Deductee Register
    TdsDeducteeEntryDto createDeducteeEntry(CreateTdsDeducteeEntryRequest request);

    List<TdsDeducteeEntryDto> getDeducteesByProfile(UUID tdsProfileId);

    List<TdsDeducteeEntryDto> getDeducteesByReturn(UUID tdsReturnId);

    // 5. Form 16 / 16A Certificates
    TdsCertificateDto createCertificate(CreateTdsCertificateRequest request);

    List<TdsCertificateDto> getCertificatesByProfile(UUID tdsProfileId);

    TdsCertificateDto updateCertificateStatus(UUID id, UpdateTdsCertificateStatusRequest request);

    // 6. Workload Dashboard & Calculation Engine
    TdsWorkloadDashboardDto getWorkloadDashboard(String quarter, String financialYear, UUID assignedEmployeeId);

    TdsComputationResultDto computeTds(TdsComputationRequest request);

    List<TdsSectionRateDto> getSectionRates();

    // 7. Demo Data Seeder
    List<TdsReturnDto> seedDemoData();
}
