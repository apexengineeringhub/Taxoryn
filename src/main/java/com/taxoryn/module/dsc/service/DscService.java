package com.taxoryn.module.dsc.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.dsc.dto.CreateDscRequest;
import com.taxoryn.module.dsc.dto.DscDto;
import com.taxoryn.module.dsc.dto.DscFilterRequest;
import com.taxoryn.module.dsc.dto.DscSummaryDto;
import com.taxoryn.module.dsc.dto.UpdateDscRequest;

import java.util.UUID;

public interface DscService {

    DscDto createDsc(CreateDscRequest request);

    DscDto updateDsc(UUID id, UpdateDscRequest request);

    DscDto getDscById(UUID id);

    PagedResponse<DscDto> getDscList(DscFilterRequest request);

    DscSummaryDto getDscSummary();

    DscDto activateDsc(UUID id);

    DscDto deactivateDsc(UUID id);

    DscDto revokeDsc(UUID id, String reason);

    void deleteDsc(UUID id);

    int checkAndTriggerExpiryReminders();
}
