package com.taxoryn.module.timetracking.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.timetracking.dto.CreateTimeEntryRequest;
import com.taxoryn.module.timetracking.dto.TimeEntryDto;
import com.taxoryn.module.timetracking.dto.TimeEntryFilterRequest;
import com.taxoryn.module.timetracking.dto.UpdateTimeEntryRequest;

import java.util.List;
import java.util.UUID;

public interface TimeEntryService {

    TimeEntryDto createTimeEntry(CreateTimeEntryRequest request);

    TimeEntryDto getTimeEntryById(UUID id);

    PagedResponse<TimeEntryDto> getTimeEntries(TimeEntryFilterRequest filterRequest);

    List<TimeEntryDto> getTimeEntriesByClientId(UUID clientId);

    TimeEntryDto updateTimeEntry(UUID id, UpdateTimeEntryRequest request);

    void deleteTimeEntry(UUID id);
}
