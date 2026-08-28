package com.taxoryn.module.dashboard.service;

import com.taxoryn.module.dashboard.dto.PlatformDashboardSummaryDto;

public interface PlatformDashboardService {

    /**
     * Retrieves aggregated, platform-level operations, ecosystem, marketplace, subscription,
     * feedback, and health metrics for the Taxoryn SuperAdmin dashboard.
     * Contains ZERO sensitive practice tax or customer filing data.
     */
    PlatformDashboardSummaryDto getPlatformDashboard();
}
