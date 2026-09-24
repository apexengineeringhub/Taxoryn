package com.taxoryn.module.compliance.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.AssignComplianceEmployeeRequest;
import com.taxoryn.module.compliance.dto.AssignObligationRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarSummaryDto;
import com.taxoryn.module.compliance.dto.ComplianceCycleTemplateDto;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;

import java.util.List;
import java.util.UUID;

public interface ComplianceService {

    // 1. Calendar, Upcoming, Overdue, Due Today
    PagedResponse<ComplianceObligationDto> getCalendar(ComplianceCalendarFilterRequest filterRequest);

    List<ComplianceObligationDto> getUpcoming(int daysAhead);

    List<ComplianceObligationDto> getOverdue();

    List<ComplianceObligationDto> getDueToday();

    // 2. Executive Dashboard & Summary Statistics (Zero-leakage filtered)
    ComplianceCalendarSummaryDto getCalendarSummary();

    ComplianceDashboardStatsDto getDashboardStats();

    // 3. Obligation Lifecycle & Assignment
    ComplianceObligationDto createObligation(CreateComplianceObligationRequest request);

    ComplianceObligationDto getObligationById(UUID id);

    ComplianceObligationDto updateObligation(UUID id, UpdateComplianceObligationRequest request);

    ComplianceObligationDto updateStatus(UUID id, UpdateObligationStatusRequest request);

    ComplianceObligationDto updateStatus(UUID id, UpdateComplianceStatusRequest request);

    ComplianceObligationDto assignEmployee(UUID id, AssignObligationRequest request);

    ComplianceObligationDto assignEmployee(UUID id, AssignComplianceEmployeeRequest request);

    ComplianceObligationDto createTaskForObligation(UUID id);

    void deleteObligation(UUID id);

    List<ComplianceObligationDto> getObligationsByClientId(UUID clientId);

    List<ComplianceObligationDto> getObligationsByServiceId(UUID clientServiceId);

    // 4. Service Period & Cycle Integration
    ComplianceObligationDto generateObligationForServicePeriod(UUID clientServiceId, UUID servicePeriodId);

    List<ComplianceCycleTemplateDto> getCycleTemplates();

    // 5. Batch & Scheduled Generation
    List<ComplianceObligationDto> generateComplianceObligations(GenerateComplianceRequest request);

    int processOverdueObligations();
}
