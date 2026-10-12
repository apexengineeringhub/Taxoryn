package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.worktemplate.dto.WorkInventoryFilterRequest;
import com.taxoryn.module.worktemplate.dto.WorkInventoryItemDto;

/**
 * Service providing a unified, multi-domain projection of work items across Taxoryn.
 */
public interface WorkInventoryService {

    /**
     * Retrieves a paginated and filtered view of work across work instances, compliance obligations, and tasks.
     *
     * @param filter query filters including status, assignee, client, work type, date range, search
     * @return paged list of normalized work inventory items
     */
    PagedResponse<WorkInventoryItemDto> getWorkInventory(WorkInventoryFilterRequest filter);
}
