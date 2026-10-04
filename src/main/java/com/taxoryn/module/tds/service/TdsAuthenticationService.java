package com.taxoryn.module.tds.service;

import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscSigningSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscVerifyResultDto;
import com.taxoryn.module.gov.auth.dto.GovEvcChallengeDto;
import com.taxoryn.module.gov.auth.dto.GovEvcVerifyResultDto;
import com.taxoryn.module.tds.dto.TdsDscSignRequest;
import com.taxoryn.module.tds.dto.TdsDscVerifyRequest;
import com.taxoryn.module.tds.dto.TdsEvcStartRequest;
import com.taxoryn.module.tds.dto.TdsEvcVerifyRequest;

import java.util.UUID;

/**
 * High-level TDS / TRACES authentication, EVC verification, and DSC signing boundary.
 */
public interface TdsAuthenticationService {

    GovEvcChallengeDto startEvc(TdsEvcStartRequest request);

    GovEvcVerifyResultDto verifyEvc(UUID sessionId, TdsEvcVerifyRequest request);

    GovDscSigningSessionDto startDsc(TdsDscSignRequest request);

    GovDscVerifyResultDto verifyDsc(UUID sessionId, TdsDscVerifyRequest request);

    GovAuthSessionDto getStatus(UUID sessionId);

    GovAuthSessionDto cancel(UUID sessionId);
}
