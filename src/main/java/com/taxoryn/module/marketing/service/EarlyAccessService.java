package com.taxoryn.module.marketing.service;

import com.taxoryn.module.marketing.dto.CreateEarlyAccessRequest;
import com.taxoryn.module.marketing.dto.EarlyAccessResponse;

public interface EarlyAccessService {

    EarlyAccessResponse submitEarlyAccessRequest(CreateEarlyAccessRequest request, String ipAddress, String userAgent);
}
