package com.taxoryn.module.billing.service;

import com.taxoryn.module.billing.dto.BillingProfileDto;
import com.taxoryn.module.billing.dto.CreateBillingProfileRequest;
import com.taxoryn.module.billing.dto.UpdateBillingProfileRequest;

import java.util.List;
import java.util.UUID;

public interface BillingProfileService {

    BillingProfileDto createBillingProfile(CreateBillingProfileRequest request);

    BillingProfileDto getBillingProfileById(UUID id);

    List<BillingProfileDto> getBillingProfilesByClientId(UUID clientId);

    List<BillingProfileDto> getAllBillingProfiles();

    BillingProfileDto updateBillingProfile(UUID id, UpdateBillingProfileRequest request);

    void deleteBillingProfile(UUID id);
}
