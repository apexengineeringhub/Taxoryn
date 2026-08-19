package com.taxoryn.module.compliance.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.AssignComplianceEmployeeRequest;
import com.taxoryn.module.compliance.dto.ComplianceCalendarFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceDashboardStatsDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;

import java.util.List;
import java.util.UUID;

public interface ComplianceService {

    // 1. Calendar, Upcoming, Overdue, Due Today
    PagedResponse<ComplianceObligationDto> getCalendar(ComplianceCalendarFilterRequest filterRequest);

    List<ComplianceObligationDto> getUpcoming(int daysAhead);

    List<ComplianceObligationDto> getOverdue();

    List<ComplianceObligationDto> getDueToday();

    // 2. Executive Dashboard Statistics
    ComplianceDashboardStatsDto getDashboardStats();

    // 3. Obligation Lifecycle & Assignment
    ComplianceObligationDto createObligation(CreateComplianceObligationRequest request);

    ComplianceObligationDto getObligationById(UUID id);

    ComplianceObligationDto updateStatus(UUID id, UpdateComplianceStatusRequest request);

    ComplianceObligationDto assignEmployee(UUID id, AssignComplianceEmployeeRequest request);

    ComplianceObligationDto createTaskForObligation(UUID id);

    // 4. Batch & Scheduled Generation
    List<ComplianceObligationDto> generateComplianceObligations(GenerateComplianceRequest request);

    int processOverdueObligations();
}
