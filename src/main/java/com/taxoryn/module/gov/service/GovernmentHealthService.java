package com.taxoryn.module.gov.service;

import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovHandshakeRequest;
import com.taxoryn.module.gov.dto.GovHandshakeResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service contract for government connection health checks, handshakes, and status polling.
 */
public interface GovernmentHealthService {

    GovConnectionHealthDto checkConnectionHealth(UUID connectionId);

    GovConnectionHealthDto checkConnectionHealth(UUID connectionId, Map<String, Object> directives);

    List<GovConnectionHealthDto> checkAllActiveConnections();

    GovHandshakeResult executeDirectHandshake(GovHandshakeRequest request);
}
