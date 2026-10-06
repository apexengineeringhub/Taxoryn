package com.taxoryn.module.itr.service;

import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscSigningSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscVerifyResultDto;
import com.taxoryn.module.gov.auth.dto.GovEvcChallengeDto;
import com.taxoryn.module.gov.auth.dto.GovEvcVerifyResultDto;
import com.taxoryn.module.itr.dto.ItrDscSignRequest;
import com.taxoryn.module.itr.dto.ItrDscVerifyRequest;
import com.taxoryn.module.itr.dto.ItrEvcStartRequest;
import com.taxoryn.module.itr.dto.ItrEvcVerifyRequest;

import java.util.UUID;

/**
 * High-level Income Tax Return (ITR) authentication, EVC verification, and DSC signing boundary.
 */
public interface ItrAuthenticationService {

    GovEvcChallengeDto startEvc(ItrEvcStartRequest request);

    GovEvcVerifyResultDto verifyEvc(UUID sessionId, ItrEvcVerifyRequest request);

    GovDscSigningSessionDto startDsc(ItrDscSignRequest request);

    GovDscVerifyResultDto verifyDsc(UUID sessionId, ItrDscVerifyRequest request);

    GovAuthSessionDto getStatus(UUID sessionId);

    GovAuthSessionDto cancel(UUID sessionId);
}
