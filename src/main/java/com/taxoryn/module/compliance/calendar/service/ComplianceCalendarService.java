package com.taxoryn.module.compliance.calendar.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.calendar.dto.ClientComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceCalendarQueryFilter;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineRadarDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineSummaryDto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface for Compliance Calendar and Deadline Radar queries and projections.
 */
public interface ComplianceCalendarService {

    /**
     * Retrieves paginated compliance calendar deadlines matching filter criteria.
     *
     * @param filter query filter parameters
     * @return paged response of compliance deadlines
     */
    PagedResponse<ComplianceDeadlineDto> getCalendar(ComplianceCalendarQueryFilter filter);

    /**
     * Retrieves the practice-wide Compliance Deadline Radar overview.
     *
     * @param referenceDate reference date (defaults to current date if null)
     * @return practice deadline radar overview
     */
    ComplianceDeadlineRadarDto getPracticeDeadlineRadar(LocalDate referenceDate);

    /**
     * Retrieves practice aggregate summary metrics across deadline buckets.
     *
     * @param referenceDate reference date (defaults to current date if null)
     * @return aggregate summary metrics
     */
    ComplianceDeadlineSummaryDto getDeadlineSummary(LocalDate referenceDate);

    /**
     * Retrieves compact deadline summary and next deadline for a specific client (Client 360).
     *
     * @param clientId      client identifier
     * @param referenceDate reference date (defaults to current date if null)
     * @return client deadline summary
     */
    ClientComplianceDeadlineSummaryDto getClientDeadlineSummary(UUID clientId, LocalDate referenceDate);

    /**
     * Retrieves paginated compliance calendar deadlines for a specific client.
     *
     * @param clientId client identifier
     * @param filter   query filter parameters
     * @return paged response of client compliance deadlines
     */
    PagedResponse<ComplianceDeadlineDto> getClientCalendar(UUID clientId, ComplianceCalendarQueryFilter filter);
}
