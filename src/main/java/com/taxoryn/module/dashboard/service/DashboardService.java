package com.taxoryn.module.dashboard.service;

import com.taxoryn.module.dashboard.dto.BillingDashboardSummaryDto;
import com.taxoryn.module.dashboard.dto.ClientSummaryDashboardDto;
import com.taxoryn.module.dashboard.dto.ComplianceDashboardDto;
import com.taxoryn.module.dashboard.dto.DashboardFilterRequest;
import com.taxoryn.module.dashboard.dto.DocumentDashboardDto;
import com.taxoryn.module.dashboard.dto.NoticeDashboardDto;
import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;
import com.taxoryn.module.dashboard.dto.PracticeDashboardOverviewDto;
import com.taxoryn.module.dashboard.dto.WorkDashboardDto;

public interface DashboardService {

    /**
     * Retrieves high-level operational overview KPIs for the practice within security and filter scope.
     */
    PracticeDashboardOverviewDto getOverview(DashboardFilterRequest filter);

    /**
     * Retrieves multi-dimensional compliance metrics across GST, ITR, TDS, and Tax Notices.
     */
    ComplianceDashboardDto getComplianceDashboard(DashboardFilterRequest filter);

    /**
     * Retrieves work item and task statuses and team workload distribution.
     */
    WorkDashboardDto getWorkDashboard(DashboardFilterRequest filter);

    /**
     * Retrieves document volumes, active workflow attachments, and document request statuses.
     */
    DocumentDashboardDto getDocumentDashboard(DashboardFilterRequest filter);

    /**
     * Retrieves tax notice operational metrics, hearings, response deadlines, and demand amounts.
     */
    NoticeDashboardDto getNoticeDashboard(DashboardFilterRequest filter);

    /**
     * Retrieves practice client billing metrics, aging receivables, and service revenue.
     */
    BillingDashboardSummaryDto getBillingDashboard(DashboardFilterRequest filter);

    /**
     * Retrieves client summary counts and prioritized operational attention items.
     */
    ClientSummaryDashboardDto getClientDashboard(DashboardFilterRequest filter);

    /**
     * Legacy organization dashboard method preserved for backward compatibility.
     */
    OrganizationDashboardDto getOrganizationDashboard();
}
