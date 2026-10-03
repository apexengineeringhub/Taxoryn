package com.taxoryn.module.udin.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.udin.dto.CancelUdinRequest;
import com.taxoryn.module.udin.dto.CreateUdinRequest;
import com.taxoryn.module.udin.dto.UdinDto;
import com.taxoryn.module.udin.dto.UdinFilterRequest;
import com.taxoryn.module.udin.dto.UdinSummaryDto;
import com.taxoryn.module.udin.dto.UpdateUdinRequest;
import com.taxoryn.module.udin.dto.UpdateUdinVerificationRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UdinService {

    UdinDto createUdin(CreateUdinRequest request);

    UdinDto getUdinById(UUID id);

    PagedResponse<UdinDto> getUdins(UdinFilterRequest filter, Pageable pageable);

    UdinSummaryDto getUdinSummary();

    UdinDto updateUdin(UUID id, UpdateUdinRequest request);

    UdinDto updateVerification(UUID id, UpdateUdinVerificationRequest request);

    UdinDto cancelUdin(UUID id, CancelUdinRequest request);

    void deleteUdin(UUID id);

    List<UdinDto> getUdinsByClientId(UUID clientId);
}
