package com.taxoryn.module.dashboard.service;

import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;

public interface DashboardService {

    /**
     * Retrieves organization-level dashboard metrics across clients, employees, tasks, GST, ITR, billing, and workload.
     *
     * @return OrganizationDashboardDto containing aggregated statistics
     */
    OrganizationDashboardDto getOrganizationDashboard();
}
