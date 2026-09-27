package com.taxoryn.module.gst.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.gst.dto.CreateGstRegistrationRequest;
import com.taxoryn.module.gst.dto.GstRegistrationDto;
import com.taxoryn.module.gst.dto.GstRegistrationFilterRequest;
import com.taxoryn.module.gst.dto.UpdateGstRegistrationRequest;

import java.util.List;
import java.util.UUID;

public interface GstRegistrationService {

    GstRegistrationDto createRegistration(CreateGstRegistrationRequest request);

    GstRegistrationDto getRegistrationById(UUID id);

    PagedResponse<GstRegistrationDto> getRegistrations(GstRegistrationFilterRequest filterRequest);

    List<GstRegistrationDto> getRegistrationsByClientId(UUID clientId);

    GstRegistrationDto updateRegistration(UUID id, UpdateGstRegistrationRequest request);

    void deleteRegistration(UUID id);
}
